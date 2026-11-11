package com.attendo.core.social

/** The Firebase project the friends feature talks to (from `firebase.properties` at build time). */
data class FirebaseConfig(
    val projectId: String,
    val appId: String,
    val apiKey: String,
    /** OAuth "Web client" ID of the project, needed to get a Google ID token for Firebase Auth. */
    val webClientId: String,
) {
    val isComplete: Boolean get() = listOf(projectId, appId, apiKey, webClientId).all { it.isNotBlank() }
}

/** What other students can see about someone: name, friend code and programme. */
data class StudentProfile(val uid: String, val name: String, val code: String, val programme: String = "")

data class FriendRequest(
    val from: String,
    val to: String,
    val fromName: String,
    val fromCode: String,
    val toName: String,
)

data class SharedSubject(val code: String, val name: String, val present: Int, val held: Int, val percent: Double, val target: Int)

/** Attendance a student shares with friends. */
data class SharedAttendance(val overall: Double?, val subjects: List<SharedSubject>, val updatedAt: Long)

data class SharedGradeSubject(val code: String, val name: String, val percent: Double, val grade: String, val assessments: Int)

/** Grades a student chose to share with friends (off by default). */
data class SharedGrades(val overall: Double?, val subjects: List<SharedGradeSubject>, val updatedAt: Long)

/** Another student's page. [attendance]/[grades] are null when not shared or not visible to you. */
data class FriendView(
    val profile: StudentProfile,
    val isFriend: Boolean,
    val attendance: SharedAttendance?,
    val grades: SharedGrades?,
    val friends: List<StudentProfile>,
)

data class SocialState(
    /** A Firebase project is compiled in. */
    val configured: Boolean = false,
    val loaded: Boolean = false,
    /** Signed-in student, or null. */
    val me: StudentProfile? = null,
    val shareAttendance: Boolean = true,
    val shareGrades: Boolean = false,
    val friends: List<StudentProfile> = emptyList(),
    val incoming: List<FriendRequest> = emptyList(),
    val outgoing: List<FriendRequest> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    fun isFriend(uid: String) = friends.any { it.uid == uid }
    fun requestedTo(uid: String) = outgoing.any { it.to == uid }
    fun requestFrom(uid: String) = incoming.firstOrNull { it.from == uid }
}

/** A student's short public code: the first characters of their account id, upper-case. */
fun friendCode(uid: String): String = uid.filter(Char::isLetterOrDigit).take(6).uppercase()
