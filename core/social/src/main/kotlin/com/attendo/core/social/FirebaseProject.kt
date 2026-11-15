package com.attendo.core.social

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Firebase project Attendo talks to, shared by Friends ([SocialService]) and feedback
 * ([FirestoreFeedbackSender]). Everything is null when the build has no Firebase settings.
 */
class FirebaseProject(context: Context, val config: FirebaseConfig?) {
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

    val auth: FirebaseAuth? get() = firebase?.let(FirebaseAuth::getInstance)
    val db: FirebaseFirestore? get() = firebase?.let(FirebaseFirestore::getInstance)

    private companion object {
        const val APP_NAME = "attendo-social"
    }
}

internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
