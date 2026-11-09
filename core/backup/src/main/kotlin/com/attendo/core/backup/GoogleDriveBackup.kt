package com.attendo.core.backup

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.attendo.core.data.AttendoData
import com.attendo.core.data.backup.BackupArchive
import com.attendo.core.data.csv.CsvTable
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Settings › Backup › Google account.
 *
 * Signing in authorizes the `drive.appdata` scope: the app may keep files in a hidden folder of the
 * user's own Google Drive that only Attendo can see. The whole data set (every CSV file, plus the
 * launch image) is stored there as one zip, [BackupArchive.FILE_NAME]. After a change it is
 * re-uploaded a few seconds later; after reinstalling, signing in again restores it.
 *
 * Requires an Android OAuth client for this package + signing certificate in a Google Cloud project
 * with the Drive API enabled (see docs/GOOGLE_BACKUP_SETUP.md). No client ID is compiled in: Google
 * Play services matches the app by package name and certificate.
 */
class GoogleDriveBackup(
    context: Context,
    private val data: AttendoData,
    private val scope: CoroutineScope,
    private val appVersion: String,
    /** Extra app files backed up byte-for-byte, by entry name (e.g. the launch image). */
    private val extraFiles: Map<String, File> = emptyMap(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    data class State(
        val loaded: Boolean = false,
        /** Signed-in Google account (email), or null. */
        val account: String? = null,
        val displayName: String? = null,
        val autoBackup: Boolean = true,
        val lastBackupAt: Long? = null,
        val busy: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false,
        /** Time of a backup found at sign-in while this phone also has data: the user must choose. */
        val pendingRestoreFrom: Long? = null,
    )

    private val app = context.applicationContext
    private val authClient by lazy { Identity.getAuthorizationClient(app) }
    private val driveScope = Scope(DRIVE_APPDATA)
    private val authRequest
        get() = AuthorizationRequest.builder().setRequestedScopes(listOf(driveScope)).build()

    /** Kept outside the backed-up CSV folder, so a restore never overwrites who is signed in. */
    private val stateTable = CsvTable(File(app.filesDir, "backup/account.csv"), listOf("key", "value"))
    private var lastHash: String? = null

    private val mutex = Mutex()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { stateTable.read().associate { it["key"].orEmpty() to it["value"].orEmpty() } }.getOrDefault(emptyMap())
            }
            lastHash = saved["last_hash"]?.takeIf { it.isNotBlank() }
            _state.update {
                it.copy(
                    loaded = true,
                    account = saved["email"]?.takeIf(String::isNotBlank),
                    displayName = saved["name"]?.takeIf(String::isNotBlank),
                    autoBackup = saved["auto_backup"] != "false",
                    lastBackupAt = saved["last_backup_at"]?.toLongOrNull(),
                )
            }
            watchForChanges()
        }
    }

    // ---- Sign-in -------------------------------------------------------------------------------

    /**
     * Starts Google sign-in. Returns an [IntentSender] for Google's account picker / consent screen
     * when the user has to act (launch it and pass the result to [onSignInResult]); returns null
     * when access was already granted, in which case sign-in finishes on its own.
     */
    suspend fun signIn(): IntentSender? {
        _state.update { it.copy(busy = true, message = "Connecting to Google…", isError = false) }
        val result = try {
            authClient.authorize(authRequest).await()
        } catch (e: Exception) {
            fail(e)
            return null
        }
        if (result.hasResolution()) {
            return result.pendingIntent?.intentSender ?: run {
                fail(IllegalStateException("Google didn't provide a sign-in screen"))
                null
            }
        }
        scope.launch { finishSignIn(result.accessToken) }
        return null
    }

    fun onSignInResult(resultCode: Int, intent: Intent?) {
        scope.launch {
            if (resultCode != Activity.RESULT_OK) {
                _state.update { it.copy(busy = false, message = "Sign-in cancelled.", isError = false) }
                return@launch
            }
            val result = try {
                authClient.getAuthorizationResultFromIntent(intent)
            } catch (e: Exception) {
                fail(e)
                return@launch
            }
            finishSignIn(result.accessToken)
        }
    }

    private suspend fun finishSignIn(token: String?) = operation("Checking your Google account…") {
        val drive = DriveAppDataClient(token ?: error("Google didn't return an access token"))
        val (email, name) = io { drive.about() }
        saveAccount(email, name)
        val remote = io { drive.list(BackupArchive.FILE_NAME).firstOrNull() }
        when {
            remote == null -> {
                upload(drive)
                "Signed in as $email. No earlier backup was found, so this phone's data is backed up there now."
            }
            data.hasNoUserData() -> {
                restoreFrom(drive, remote)
                "Restored your data from $email."
            }
            else -> {
                _state.update { it.copy(pendingRestoreFrom = remote.modifiedAt) }
                "Found a backup in $email."
            }
        }
    }

    /** Answer to "a backup was found": restore it, or keep this phone's data (and back that up). */
    fun resolvePendingRestore(restore: Boolean) {
        _state.update { it.copy(pendingRestoreFrom = null) }
        if (restore) restoreNow() else backUpNow()
    }

    /** Forgets the account here and revokes the app's Drive access. The backup stays in the account. */
    fun signOut() {
        scope.launch {
            operation("Signing out…") {
                val email = _state.value.account
                if (email != null) {
                    runCatching {
                        authClient.revokeAccess(
                            RevokeAccessRequest.builder().setAccount(Account(email, "com.google")).setScopes(listOf(driveScope)).build(),
                        ).await()
                    }
                }
                lastHash = null
                io { stateTable.write(emptyList()) }
                _state.update { it.copy(account = null, displayName = null, lastBackupAt = null, pendingRestoreFrom = null) }
                "Signed out. Your backup stays in your Google account; sign in again to restore it."
            }
        }
    }

    // ---- Backup / restore ----------------------------------------------------------------------

    fun backUpNow() {
        scope.launch { operation("Backing up…") { withDrive { upload(it) }; "Backed up just now." } }
    }

    fun restoreNow() {
        scope.launch {
            operation("Restoring…") {
                withDrive { drive ->
                    val remote = io { drive.list(BackupArchive.FILE_NAME).firstOrNull() }
                        ?: return@withDrive "There's no backup in your Google account yet."
                    restoreFrom(drive, remote)
                    "Restored your data from Google."
                }
            }
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        _state.update { it.copy(autoBackup = enabled) }
        scope.launch { persist() }
    }

    /** Re-uploads a few seconds after any data change, while signed in with auto backup on. */
    @OptIn(FlowPreview::class)
    private fun watchForChanges() {
        scope.launch {
            combine(
                data.settings.settings,
                data.subjects.subjects,
                data.attendance.records,
                data.pomodoro.sessions,
                data.todos.todos,
            ) { a, b, c, d, e -> listOf(a, b, c, d, e) }
                .drop(1)
                .debounce(AUTO_BACKUP_DELAY_MS)
                .collect {
                    val s = _state.value
                    if (s.account == null || !s.autoBackup || s.pendingRestoreFrom != null) return@collect
                    val files = io { localFiles() }
                    if (digest(files) == lastHash) return@collect // e.g. right after a restore
                    operation("Backing up…") { withDrive { upload(it) }; "Backed up automatically." }
                }
        }
    }

    private suspend fun upload(drive: DriveAppDataClient) {
        val files = io { localFiles() }
        val created = io {
            val created = drive.create(BackupArchive.FILE_NAME, BackupArchive.pack(files, appVersion, clock()), "application/zip")
            // Keep only the newest copy (create first, then delete, so there's always one).
            drive.list(BackupArchive.FILE_NAME).filter { it.id != created.id }.forEach { old -> runCatching { drive.delete(old.id) } }
            created
        }
        lastHash = digest(files)
        _state.update { it.copy(lastBackupAt = created.modifiedAt.takeIf { t -> t > 0 } ?: clock()) }
        persist()
    }

    private suspend fun restoreFrom(drive: DriveAppDataClient, remote: RemoteFile) {
        val contents = io { BackupArchive.unpack(drive.download(remote.id)) }
        val csv = contents.files.filterKeys { it.endsWith(".csv") }.mapValues { (_, bytes) -> bytes.toString(Charsets.UTF_8) }
        data.restoreSnapshot(csv)
        io {
            extraFiles.forEach { (name, file) ->
                val bytes = contents.files[name]
                if (bytes == null) {
                    file.delete()
                } else {
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                }
            }
        }
        // The phone now matches the backup: no need to upload it straight back.
        lastHash = digest(io { localFiles() })
        _state.update { it.copy(lastBackupAt = remote.modifiedAt.takeIf { t -> t > 0 } ?: contents.createdAt) }
        persist()
    }

    /** Every CSV file plus the extras that exist, as backup entries. */
    private fun localFiles(): Map<String, ByteArray> =
        data.snapshot().mapValues { (_, text) -> text.toByteArray(Charsets.UTF_8) } +
            extraFiles.filterValues { it.exists() }.mapValues { (_, f) -> f.readBytes() }

    // ---- Plumbing ------------------------------------------------------------------------------

    /** Runs [block] with a Drive client, refreshing the access token once if it has expired. */
    private suspend fun <T> withDrive(block: suspend (DriveAppDataClient) -> T): T {
        val token = silentToken()
        return try {
            block(DriveAppDataClient(token))
        } catch (e: DriveHttpException) {
            if (e.code != 401) throw e
            runCatching { authClient.clearToken(ClearTokenRequest.builder().setToken(token).build()).await() }
            block(DriveAppDataClient(silentToken()))
        }
    }

    private suspend fun silentToken(): String {
        val result = authClient.authorize(authRequest).await()
        if (result.hasResolution()) throw NeedsSignInException()
        return result.accessToken ?: throw NeedsSignInException()
    }

    /** One Drive operation at a time; shows progress and the outcome in [state]. */
    private suspend fun operation(working: String, action: suspend () -> String) = mutex.withLock {
        _state.update { it.copy(busy = true, message = working, isError = false) }
        val outcome = runCatching { action() }
        _state.update {
            it.copy(busy = false, message = outcome.getOrElse(::describe), isError = outcome.isFailure)
        }
    }

    private fun fail(e: Throwable) = _state.update { it.copy(busy = false, message = describe(e), isError = true) }

    private fun describe(e: Throwable): String = when {
        e is NeedsSignInException -> "Google needs you to sign in again. Tap Sign in with Google."
        e is ApiException && e.statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
            "Google sign-in isn't set up for this build yet (no OAuth client for this app in Google Cloud)."
        e is ApiException && e.statusCode == CommonStatusCodes.NETWORK_ERROR -> "No internet connection."
        e is ApiException && e.statusCode == CommonStatusCodes.CANCELED -> "Sign-in cancelled."
        e is DriveHttpException && e.code == 403 -> "Google Drive refused access. Is the Drive API enabled for the app's project?"
        e is IOException && e !is DriveHttpException -> "No internet connection. Backup will retry after your next change."
        else -> "Something went wrong: ${e.message ?: e.javaClass.simpleName}"
    }

    private suspend fun saveAccount(email: String, name: String?) {
        _state.update { it.copy(account = email, displayName = name) }
        persist()
    }

    private suspend fun persist() {
        val s = _state.value
        val rows = listOfNotNull(
            s.account?.let { "email" to it },
            s.displayName?.let { "name" to it },
            "auto_backup" to s.autoBackup.toString(),
            s.lastBackupAt?.let { "last_backup_at" to it.toString() },
            lastHash?.let { "last_hash" to it },
        ).map { (k, v) -> mapOf("key" to k, "value" to v) }
        io { runCatching { stateTable.write(rows) } }
    }

    private class NeedsSignInException : Exception()

    private companion object {
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val AUTO_BACKUP_DELAY_MS = 4_000L

        fun digest(files: Map<String, ByteArray>): String {
            val md = MessageDigest.getInstance("SHA-256")
            files.toSortedMap().forEach { (name, bytes) ->
                md.update(name.toByteArray())
                md.update(0)
                md.update(bytes)
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

/** Awaits a Play services [Task] without the extra coroutines-play-services dependency. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
