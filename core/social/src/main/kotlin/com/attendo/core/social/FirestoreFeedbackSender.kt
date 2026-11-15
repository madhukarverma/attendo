package com.attendo.core.social

import android.content.Context
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.ErrorCode
import com.attendo.core.model.Feedback
import com.attendo.core.model.FeedbackResult
import com.attendo.core.model.FeedbackSender
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Sends Settings › Feedback to the `feedback` collection in Firestore, where the developer reads it
 * in the Firebase console (docs/FEEDBACK.md). No sign-in is needed and nothing identifies the
 * student unless they type an email. The rules let the app create feedback but never read, change
 * or delete it.
 *
 * Offline, Firestore keeps the write on the phone and sends it later: the result is then
 * [FeedbackResult.Queued], and the next app start nudges any unsent feedback out.
 */
class FirestoreFeedbackSender(
    context: Context,
    private val project: FirebaseProject,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : FeedbackSender {

    /** When feedback was last sent, and whether a send is still waiting for a connection. */
    private val state = CsvTable(File(context.applicationContext.filesDir, "social/feedback.csv"), listOf("key", "value"))
    private val lock = Mutex()

    init {
        scope.launch { runCatching { flushPending() } }
    }

    override suspend fun send(feedback: Feedback): FeedbackResult = lock.withLock {
        feedback.problem()?.let { return@withLock FeedbackResult.Failed(it) }
        val store = project.db ?: return@withLock FeedbackResult.Failed(ErrorCode.FEEDBACK_NOT_SET_UP)
        val saved = readState()
        val last = saved[LAST_SENT]?.toLongOrNull() ?: 0L
        if (now() - last in 0 until Feedback.COOLDOWN_MS) return@withLock FeedbackResult.Failed(ErrorCode.FEEDBACK_TOO_SOON)

        val doc = store.collection(COLLECTION).document()
        val reference = doc.id.take(8).uppercase()
        val write = doc.set(toDocument(feedback))
        writeState(lastSent = now(), pending = true)
        try {
            // Task<Void> resolves to null, so mark delivery explicitly.
            val delivered = withTimeoutOrNull(SEND_TIMEOUT_MS) { write.await(); true } == true
            if (delivered) {
                writeState(lastSent = now(), pending = false)
                FeedbackResult.Sent(reference)
            } else {
                FeedbackResult.Queued(reference)
            }
        } catch (e: CancellationException) {
            throw e // the screen closed; Firestore still sends the write
        } catch (e: FirebaseFirestoreException) {
            // Refused by the server, so nothing is waiting to go out and the student can retry now.
            writeState(lastSent = 0L, pending = false)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                FeedbackResult.Failed(ErrorCode.FEEDBACK_REJECTED)
            } else {
                FeedbackResult.Failed(ErrorCode.FEEDBACK_UNEXPECTED, e.code.name.lowercase().replace('_', ' '))
            }
        } catch (e: Exception) {
            writeState(lastSent = 0L, pending = false)
            FeedbackResult.Failed(ErrorCode.FEEDBACK_UNEXPECTED, e.message ?: e.javaClass.simpleName)
        }
    }

    /** Exactly the fields `validFeedback` in firebase/firestore.rules accepts. */
    private fun toDocument(f: Feedback): Map<String, Any> = buildMap {
        put("rating", f.rating)
        put("category", f.category.key)
        put("message", f.message.trim().take(Feedback.MAX_MESSAGE))
        f.contact.trim().takeIf { it.isNotEmpty() }?.let { put("contact", it.take(Feedback.MAX_CONTACT)) }
        f.diagnostics?.let {
            put("app", it.appVersion.take(20))
            put("versionCode", it.versionCode)
            put("android", it.androidSdk)
            put("device", it.device.take(60))
        }
        put("createdAt", FieldValue.serverTimestamp())
    }

    /** Waits for feedback written while offline; Firestore sends it once it starts. */
    private suspend fun flushPending() {
        if (readState()[PENDING] != "true") return
        val store = project.db ?: return
        store.waitForPendingWrites().await()
        writeState(lastSent = readState()[LAST_SENT]?.toLongOrNull() ?: 0L, pending = false)
    }

    private suspend fun readState(): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching { state.read().associate { it["key"].orEmpty() to it["value"].orEmpty() } }.getOrDefault(emptyMap())
    }

    private suspend fun writeState(lastSent: Long, pending: Boolean) = withContext(Dispatchers.IO) {
        runCatching {
            state.write(
                listOf(
                    mapOf("key" to LAST_SENT, "value" to lastSent.toString()),
                    mapOf("key" to PENDING, "value" to pending.toString()),
                ),
            )
        }
    }

    private companion object {
        const val COLLECTION = "feedback"
        const val LAST_SENT = "last_sent"
        const val PENDING = "pending"

        /** After this long without an answer the feedback counts as queued, not failed. */
        const val SEND_TIMEOUT_MS = 8_000L
    }
}
