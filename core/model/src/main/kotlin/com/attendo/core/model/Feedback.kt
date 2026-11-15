package com.attendo.core.model

/** What a piece of feedback is about. [key] is what's stored on the server. */
enum class FeedbackCategory(val key: String, val label: String, val prompt: String) {
    BUG("bug", "Problem", "What happened, and what did you expect instead?"),
    IDEA("idea", "Idea", "What would make Attendo better for you?"),
    PRAISE("praise", "Praise", "What do you like about Attendo?"),
    OTHER("other", "Other", "Tell us anything."),
}

/** The phone details a student can choose to attach. */
data class FeedbackDiagnostics(val appVersion: String, val versionCode: Int, val androidSdk: Int, val device: String) {
    /** "Attendo 0.35.0 · Android SDK 35 · Google Pixel 7", shown next to the switch. */
    val summary: String get() = "Attendo $appVersion · Android SDK $androidSdk · $device"
}

/**
 * A review and/or message sent from Settings › Feedback. [rating] is 1–5 stars, or 0 for none.
 * Firestore keeps the same limits (`firebase/firestore.rules`, `validFeedback`).
 */
data class Feedback(
    val rating: Int,
    val category: FeedbackCategory,
    val message: String,
    val contact: String = "",
    val diagnostics: FeedbackDiagnostics? = null,
) {
    /** What stops this from being sent, or null when it's ready. */
    fun problem(): ErrorCode? = when {
        rating !in 0..5 -> ErrorCode.FEEDBACK_EMPTY
        rating == 0 && message.trim().length < MIN_MESSAGE -> ErrorCode.FEEDBACK_EMPTY
        contact.isNotBlank() && !looksLikeEmail(contact.trim()) -> ErrorCode.FEEDBACK_BAD_EMAIL
        else -> null
    }

    companion object {
        const val MIN_MESSAGE = 10
        const val MAX_MESSAGE = 2000
        const val MAX_CONTACT = 100

        /** Shortest time between two submissions from one phone. */
        const val COOLDOWN_MS = 60_000L

        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

        fun looksLikeEmail(s: String): Boolean = s.length <= MAX_CONTACT && EMAIL.matches(s)

        /** How the star rating reads, for the label under the stars. */
        fun ratingLabel(stars: Int): String = when (stars) {
            1 -> "Not good"
            2 -> "Could be better"
            3 -> "It's okay"
            4 -> "Good"
            5 -> "Love it"
            else -> "Tap a star to rate"
        }
    }
}

/** How sending went. [reference] is a short id the student can quote ("7F3K9QAB"). */
sealed interface FeedbackResult {
    data class Sent(val reference: String) : FeedbackResult

    /** Saved on the phone; it goes out automatically once there's a connection. */
    data class Queued(val reference: String) : FeedbackResult

    data class Failed(val error: ErrorCode, val detail: String? = null) : FeedbackResult {
        val message: String get() = error.message(detail)
    }
}

/** Sends feedback to wherever the developer reads it (Firestore, in `core:social`). */
interface FeedbackSender {
    suspend fun send(feedback: Feedback): FeedbackResult
}
