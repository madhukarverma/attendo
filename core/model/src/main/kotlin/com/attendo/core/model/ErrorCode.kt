package com.attendo.core.model

/**
 * Every problem the app reports has a stable code, shown at the end of the message ("… (BK-303)")
 * so it can be looked up in `docs/ERROR_CODES.md` or quoted in feedback. Codes never change
 * meaning; a retired code is not reused.
 *
 * Areas: ST storage · IM CSV restore · BK Google backup · FR Friends · FB feedback.
 */
enum class ErrorCode(val code: String, val text: String) {
    // Storage (files in the app's data folder)
    FILE_UNREADABLE_KEPT("ST-101", "%s couldn't be read, so it starts empty. The old file was kept as %s."),
    FILE_UNREADABLE("ST-102", "%s couldn't be read, so it starts empty."),
    FILE_LOCKED("ST-103", "%s couldn't be read. Changes to it won't be saved until the app is restarted."),
    SAVE_BLOCKED("ST-104", "Changes to %s can't be saved because the file couldn't be read. Restart the app."),
    SAVE_FAILED("ST-105", "Couldn't save your change to %s. Your phone may be out of storage space."),

    // CSV restore
    IMPORT_UNREADABLE("IM-201", "Couldn't read those files. Pick CSV files exported from Attendo."),
    IMPORT_FAILED("IM-202", "Restore failed: %s"),
    IMPORT_NOT_ATTENDO("IM-203", "Those files don't look like Attendo CSV exports."),

    // Google backup
    BACKUP_SIGN_IN_AGAIN("BK-301", "Google needs you to sign in again. Tap Sign in with Google."),
    BACKUP_NOT_SET_UP("BK-302", "Google sign-in isn't set up for this build of the app."),
    BACKUP_OFFLINE("BK-303", "No internet connection. Backup will retry after your next change."),
    BACKUP_DRIVE_REFUSED("BK-304", "Google Drive refused access. The app's Google project may need the Drive API turned on."),
    BACKUP_UNEXPECTED("BK-399", "Something went wrong with the backup: %s"),

    // Friends
    FRIENDS_NOT_SET_UP("FR-401", "Friends isn't set up in this build of the app."),
    FRIENDS_NO_ACCOUNT("FR-402", "No Google account found on this phone. Add one in system settings, then try again."),
    FRIENDS_SIGN_IN_FAILED("FR-403", "Google sign-in failed: %s"),
    FRIENDS_OFFLINE("FR-404", "No internet connection."),
    FRIENDS_NOT_ALLOWED("FR-405", "The Friends server refused this. Its privacy rules may need updating."),
    FRIENDS_UNAVAILABLE("FR-406", "The Friends server can't be reached. Check your connection."),
    FRIENDS_STUDENT_GONE("FR-407", "This student isn't on Attendo any more."),
    FRIENDS_SIGN_IN_FIRST("FR-408", "Sign in to Friends first."),
    FRIENDS_UNEXPECTED("FR-499", "Friends server error: %s"),

    // Feedback
    FEEDBACK_NOT_SET_UP("FB-501", "Feedback can't be sent from this build of the app."),
    FEEDBACK_TOO_SOON("FB-502", "You just sent feedback. Try again in a minute."),
    FEEDBACK_EMPTY("FB-503", "Add a rating, or a message of at least ${Feedback.MIN_MESSAGE} characters."),
    FEEDBACK_BAD_EMAIL("FB-504", "That email address doesn't look right."),
    FEEDBACK_REJECTED("FB-505", "The server refused the feedback. Its rules may need updating."),
    FEEDBACK_UNEXPECTED("FB-599", "Couldn't send feedback: %s"),
    ;

    /**
     * The text with [args] filled in, followed by the code: "No internet connection. (FR-404)".
     * Missing or null arguments read "unknown error", so a message can never fail to build.
     */
    fun message(vararg args: Any?): String {
        val slots = PLACEHOLDER.findAll(text).count()
        val filled = List(slots) { i -> args.getOrNull(i)?.toString() ?: "unknown error" }
        return "${text.format(*filled.toTypedArray())} ($code)"
    }

    private companion object {
        val PLACEHOLDER = Regex("%s")
    }
}

/** A failure that already carries its user-facing message and code. */
class AppException(val error: ErrorCode, detail: String? = null) : Exception(error.message(detail))
