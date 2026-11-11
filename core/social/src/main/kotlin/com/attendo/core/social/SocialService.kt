package com.attendo.core.social

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.attendo.core.data.AttendoData
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.AttendanceSummary
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.Grades
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Friends, LeetCode-style, on Firebase (free Spark plan): sign in with Google, find classmates by
 * name or friend code, send a request the other student approves, then see each other's friends
 * and (if shared) attendance. Attendance sharing is a toggle; grades are private unless the student
 * turns sharing on.
 *
 * Firestore layout (enforced by `firebase/firestore.rules`):
 * - `users/{uid}`: name, searchName, code, programme, updatedAt — readable by signed-in students
 * - `users/{uid}/friends/{friendUid}`: name, since — readable by the owner and the owner's friends
 * - `users/{uid}/shared/attendance|grades` — readable by the owner and the owner's friends;
 *   the document only exists while that sharing switch is on
 * - `requests/{from}_{to}`: pending friend requests, readable by the two students involved
 */
class SocialService(
    context: Context,
    private val config: FirebaseConfig?,
    private val scope: CoroutineScope,
    private val data: AttendoData,
    private val programme: String,
) {
    private val app = context.applicationContext
    private val firebase: FirebaseApp? by lazy {
        val cfg = config?.takeIf { it.isComplete } ?: return@lazy null
        runCatching {
            FirebaseApp.getApps(app).firstOrNull { it.name == APP_NAME } ?: FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setProjectId(cfg.projectId)
                    .setApplicationId(cfg.appId)
                    .setApiKey(cfg.apiKey)
                    .build(),
                APP_NAME,
            )
        }.getOrNull()
    }
    private val auth: FirebaseAuth? get() = firebase?.let(FirebaseAuth::getInstance)
    private val db: FirebaseFirestore? get() = firebase?.let(FirebaseFirestore::getInstance)

    /** Local sharing switches (kept outside the backed-up CSV folder). */
    private val prefs = CsvTable(File(app.filesDir, "social/prefs.csv"), listOf("key", "value"))

    private val _state = MutableStateFlow(SocialState(configured = config?.isComplete == true))
    val state: StateFlow<SocialState> = _state.asStateFlow()

    private var listeners: Job? = null
    private var lastPublished: Map<String, Any?> = emptyMap()

    init {
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { prefs.read().associate { it["key"].orEmpty() to it["value"].orEmpty() } }.getOrDefault(emptyMap())
            }
            _state.update {
                it.copy(
                    shareAttendance = saved["share_attendance"] != "false",
                    shareGrades = saved["share_grades"] == "true",
                )
            }
            val user = runCatching { auth?.currentUser }.getOrNull()
            if (user != null) runCatching { onSignedIn(user.uid, user.displayName) }.onFailure(::fail)
            _state.update { it.copy(loaded = true) }
            publishOnChanges()
        }
    }

    // ---- Sign-in -------------------------------------------------------------------------------

    /** Shows Google's account picker (Credential Manager) and signs in to Firebase with it. */
    suspend fun signIn(activity: Activity) {
        val cfg = config?.takeIf { it.isComplete } ?: return fail(IllegalStateException(NOT_SET_UP))
        val firebaseAuth = auth ?: return fail(IllegalStateException(NOT_SET_UP))
        working("Signing in…")
        runCatching {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(cfg.webClientId).build())
                .build()
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Google didn't return an account"
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val user = firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await().user
                ?: error("Sign-in failed")
            onSignedIn(user.uid, google.displayName)
        }.onSuccess { done("Signed in. Find classmates by name or friend code.") }.onFailure(::fail)
    }

    fun signOut(activity: Activity) {
        scope.launch {
            listeners?.cancel()
            runCatching { auth?.signOut() }
            runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
            lastPublished = emptyMap()
            _state.update {
                it.copy(me = null, friends = emptyList(), incoming = emptyList(), outgoing = emptyList(), busy = false,
                    message = "Signed out. What you shared stays visible to friends until you sign in and turn it off.", isError = false)
            }
        }
    }

    /** Creates or refreshes the student's public profile, then follows friends and requests. */
    private suspend fun onSignedIn(uid: String, googleName: String?) {
        val store = db ?: return
        val settings = data.settings.settings.filterNotNull().let { flow -> runCatching { flowFirst(flow) }.getOrNull() }
        val name = settings?.userName?.takeIf { it.isNotBlank() } ?: googleName?.takeIf { it.isNotBlank() } ?: "Student"
        val me = StudentProfile(uid, name.trim().take(MAX_NAME), friendCode(uid), programme)
        store.collection(USERS).document(uid).set(profileMap(me)).await()
        _state.update { it.copy(me = me) }
        listen(uid)
    }

    private fun profileMap(p: StudentProfile): Map<String, Any> = mapOf(
        "name" to p.name,
        "searchName" to p.name.lowercase(),
        "code" to p.code,
        "programme" to p.programme.take(MAX_NAME),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun listen(uid: String) {
        val store = db ?: return
        listeners?.cancel()
        listeners = scope.launch {
            launch {
                store.collection(USERS).document(uid).collection(FRIENDS).snapshots()
                    .catch { fail(it) }
                    .collect { snap ->
                        _state.update { s -> s.copy(friends = snap.documents.map { it.toFriend() }.sortedBy { it.name.lowercase() }) }
                    }
            }
            launch {
                store.collection(REQUESTS).whereEqualTo("to", uid).snapshots()
                    .catch { fail(it) }
                    .collect { snap -> _state.update { s -> s.copy(incoming = snap.documents.mapNotNull { it.toRequest() }) } }
            }
            launch {
                store.collection(REQUESTS).whereEqualTo("from", uid).snapshots()
                    .catch { fail(it) }
                    .collect { snap -> _state.update { s -> s.copy(outgoing = snap.documents.mapNotNull { it.toRequest() }) } }
            }
        }
    }

    // ---- Search and requests -------------------------------------------------------------------

    /** Students whose name starts with [query], or whose friend code is [query]. */
    suspend fun search(query: String): List<StudentProfile> {
        val store = db ?: return emptyList()
        val me = _state.value.me ?: return emptyList()
        val q = query.trim().take(MAX_NAME)
        if (q.length < 2) return emptyList()
        return runCatching {
            val byName = store.collection(USERS).orderBy("searchName").startAt(q.lowercase()).endAt(q.lowercase() + "")
                .limit(SEARCH_LIMIT).get().await().documents
            val byCode = if (q.length in 4..8 && q.all(Char::isLetterOrDigit)) {
                store.collection(USERS).whereEqualTo("code", q.uppercase()).limit(5).get().await().documents
            } else {
                emptyList()
            }
            (byCode + byName).mapNotNull { it.toProfile() }.distinctBy { it.uid }.filter { it.uid != me.uid }
        }.onFailure(::fail).getOrDefault(emptyList())
    }

    fun sendRequest(to: StudentProfile) = action("Sending request…", "Request sent to ${to.name}.") {
        val me = requireMe()
        // They already asked us: accepting is the same as asking back.
        _state.value.requestFrom(to.uid)?.let { return@action acceptNow(it) }
        db!!.collection(REQUESTS).document("${me.uid}_${to.uid}").set(
            mapOf(
                "from" to me.uid,
                "to" to to.uid,
                "fromName" to me.name,
                "fromCode" to me.code,
                "toName" to to.name.take(MAX_NAME),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    fun accept(request: FriendRequest) = action("Accepting…", "You and ${request.fromName} are now friends.") { acceptNow(request) }

    private suspend fun acceptNow(request: FriendRequest) {
        val me = requireMe()
        val store = db!!
        val batch = store.batch()
        val since = FieldValue.serverTimestamp()
        batch.set(store.collection(USERS).document(me.uid).collection(FRIENDS).document(request.from), mapOf("name" to request.fromName, "since" to since))
        batch.set(store.collection(USERS).document(request.from).collection(FRIENDS).document(me.uid), mapOf("name" to me.name, "since" to since))
        batch.delete(store.collection(REQUESTS).document("${request.from}_${me.uid}"))
        batch.commit().await()
    }

    fun decline(request: FriendRequest) = action("Declining…", null) {
        db!!.collection(REQUESTS).document("${request.from}_${request.to}").delete().await()
    }

    fun cancel(request: FriendRequest) = action("Cancelling…", null) {
        db!!.collection(REQUESTS).document("${request.from}_${request.to}").delete().await()
    }

    fun unfriend(friend: StudentProfile) = action("Removing…", "Removed ${friend.name} from your friends.") {
        val me = requireMe()
        val store = db!!
        val batch = store.batch()
        batch.delete(store.collection(USERS).document(me.uid).collection(FRIENDS).document(friend.uid))
        batch.delete(store.collection(USERS).document(friend.uid).collection(FRIENDS).document(me.uid))
        batch.commit().await()
    }

    /** Another student's page: profile, and (when you're friends) their friends and what they share. */
    suspend fun loadStudent(uid: String): Result<FriendView> = runCatching {
        val store = db ?: error(NOT_SET_UP)
        requireMe()
        val user = store.collection(USERS).document(uid).get().await()
        val profile = user.toProfile() ?: error("This student isn't on Attendo any more")
        val isFriend = _state.value.isFriend(uid)
        if (!isFriend) return@runCatching FriendView(profile, false, null, null, emptyList())
        val shared = store.collection(USERS).document(uid).collection(SHARED)
        FriendView(
            profile = profile,
            isFriend = true,
            attendance = shared.document(ATTENDANCE).get().await().toAttendance(),
            grades = shared.document(GRADES_DOC).get().await().toGrades(),
            friends = store.collection(USERS).document(uid).collection(FRIENDS).get().await().documents.map { it.toFriend() }
                .sortedBy { it.name.lowercase() },
        )
    }

    // ---- Sharing -------------------------------------------------------------------------------

    fun setShareAttendance(enabled: Boolean) {
        _state.update { it.copy(shareAttendance = enabled) }
        scope.launch { savePrefs(); publishNow() }
    }

    fun setShareGrades(enabled: Boolean) {
        _state.update { it.copy(shareGrades = enabled) }
        scope.launch { savePrefs(); publishNow() }
    }

    /** Keeps the shared documents in step with the phone's data a few seconds after changes. */
    @OptIn(FlowPreview::class)
    private fun publishOnChanges() {
        scope.launch {
            combine(
                data.subjects.subjects,
                data.attendance.records,
                data.grades.assessments,
                _state.map { Triple(it.me?.uid, it.shareAttendance, it.shareGrades) }.distinctUntilChanged(),
            ) { _, _, _, _ -> Unit }
                .debounce(PUBLISH_DELAY_MS)
                .collect { publishNow() }
        }
    }

    private suspend fun publishNow() {
        val s = _state.value
        val me = s.me ?: return
        val store = db ?: return
        runCatching {
            val subjects = flowFirst(data.subjects.subjects)
            val records = flowFirst(data.attendance.records)
            val marks = flowFirst(data.grades.assessments)
            val shared = store.collection(USERS).document(me.uid).collection(SHARED)

            val attendance: Map<String, Any>? = if (!s.shareAttendance) {
                null
            } else {
                val summaries = subjects.map { AttendanceSummary.from(it, records) }
                val held = summaries.sumOf { it.held }
                mapOf(
                    "overall" to (if (held == 0) -1.0 else summaries.sumOf { it.present } * 100.0 / held),
                    "subjects" to summaries.take(MAX_SUBJECTS).map {
                        mapOf(
                            "code" to it.subject.code.take(12),
                            "name" to CourseCatalog.shortName(it.subject).take(MAX_NAME),
                            "present" to it.present,
                            "held" to it.held,
                            "percent" to it.percent,
                            "target" to it.subject.targetPercent,
                        )
                    },
                )
            }
            val grades: Map<String, Any>? = if (!s.shareGrades) {
                null
            } else {
                val bySubject = Grades.bySubject(subjects, marks).filter { it.percent != null }
                mapOf(
                    "overall" to (Grades.overall(bySubject) ?: -1.0),
                    "subjects" to bySubject.take(MAX_SUBJECTS).map {
                        mapOf(
                            "code" to it.subject.code.take(12),
                            "name" to CourseCatalog.shortName(it.subject).take(MAX_NAME),
                            "percent" to (it.percent ?: 0.0),
                            "grade" to it.grade.orEmpty(),
                            "assessments" to it.assessments.size,
                        )
                    },
                )
            }
            val payload = mapOf("uid" to me.uid, ATTENDANCE to attendance, GRADES_DOC to grades)
            if (payload == lastPublished) return@runCatching
            if (attendance == null) shared.document(ATTENDANCE).delete().await()
            else shared.document(ATTENDANCE).set(attendance + ("updatedAt" to FieldValue.serverTimestamp())).await()
            if (grades == null) shared.document(GRADES_DOC).delete().await()
            else shared.document(GRADES_DOC).set(grades + ("updatedAt" to FieldValue.serverTimestamp())).await()
            lastPublished = payload
        }.onFailure { if (it !is FirebaseNetworkException) fail(it) }
    }

    private suspend fun savePrefs() = withContext(Dispatchers.IO) {
        val s = _state.value
        runCatching {
            prefs.write(
                listOf(
                    mapOf("key" to "share_attendance", "value" to s.shareAttendance.toString()),
                    mapOf("key" to "share_grades", "value" to s.shareGrades.toString()),
                ),
            )
        }
    }

    // ---- Plumbing ------------------------------------------------------------------------------

    private fun requireMe(): StudentProfile = _state.value.me ?: error("Sign in first")

    private fun action(working: String, success: String?, block: suspend () -> Unit) {
        scope.launch {
            working(working)
            runCatching { block() }.onSuccess { done(success) }.onFailure(::fail)
        }
    }

    private fun working(message: String) = _state.update { it.copy(busy = true, message = message, isError = false) }

    private fun done(message: String?) = _state.update { it.copy(busy = false, message = message, isError = false) }

    private fun fail(e: Throwable) = _state.update { it.copy(busy = false, message = describe(e), isError = true) }

    private fun describe(e: Throwable): String = when (e) {
        is GetCredentialCancellationException -> "Sign-in cancelled."
        is NoCredentialException -> "No Google account found on this phone. Add one in system settings, then try again."
        is GetCredentialException -> "Google sign-in failed: ${e.message ?: e.type}"
        is FirebaseNetworkException -> "No internet connection."
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Not allowed. Is the Firestore rules file published? (firebase/firestore.rules)"
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Friends server unreachable. Check your connection."
            else -> "Friends server error: ${e.code.name.lowercase().replace('_', ' ')}"
        }
        else -> e.message ?: e.javaClass.simpleName
    }

    private fun DocumentSnapshot.toProfile(): StudentProfile? {
        val name = getString("name") ?: return null
        return StudentProfile(id, name, getString("code") ?: friendCode(id), getString("programme").orEmpty())
    }

    private fun DocumentSnapshot.toFriend() = StudentProfile(id, getString("name") ?: "Friend", friendCode(id))

    private fun DocumentSnapshot.toRequest(): FriendRequest? {
        val from = getString("from") ?: return null
        val to = getString("to") ?: return null
        return FriendRequest(from, to, getString("fromName") ?: "Student", getString("fromCode") ?: friendCode(from), getString("toName") ?: "Student")
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.subjectMaps(): List<Map<String, Any?>> =
        (get("subjects") as? List<*>)?.filterIsInstance<Map<String, Any?>>().orEmpty()

    private fun DocumentSnapshot.toAttendance(): SharedAttendance? {
        if (!exists()) return null
        return SharedAttendance(
            overall = getDouble("overall")?.takeIf { it >= 0 },
            subjects = subjectMaps().map {
                SharedSubject(
                    code = it["code"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    present = (it["present"] as? Number)?.toInt() ?: 0,
                    held = (it["held"] as? Number)?.toInt() ?: 0,
                    percent = (it["percent"] as? Number)?.toDouble() ?: 0.0,
                    target = (it["target"] as? Number)?.toInt() ?: 75,
                )
            },
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
        )
    }

    private fun DocumentSnapshot.toGrades(): SharedGrades? {
        if (!exists()) return null
        return SharedGrades(
            overall = getDouble("overall")?.takeIf { it >= 0 },
            subjects = subjectMaps().map {
                SharedGradeSubject(
                    code = it["code"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    percent = (it["percent"] as? Number)?.toDouble() ?: 0.0,
                    grade = it["grade"] as? String ?: "",
                    assessments = (it["assessments"] as? Number)?.toInt() ?: 0,
                )
            },
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val APP_NAME = "attendo-social"
        const val NOT_SET_UP = "Friends isn't set up in this build yet (no Firebase project). Steps: docs/FRIENDS_SETUP.md."
        const val USERS = "users"
        const val FRIENDS = "friends"
        const val SHARED = "shared"
        const val REQUESTS = "requests"
        const val ATTENDANCE = "attendance"
        const val GRADES_DOC = "grades"
        const val MAX_NAME = 40
        const val MAX_SUBJECTS = 20
        const val SEARCH_LIMIT = 20L
        const val PUBLISH_DELAY_MS = 3_000L
    }
}

private suspend fun <T> flowFirst(flow: Flow<T>): T = flow.first()

private fun Query.snapshots(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
