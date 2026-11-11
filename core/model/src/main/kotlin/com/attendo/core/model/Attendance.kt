package com.attendo.core.model

import java.time.LocalDate

enum class AttendanceStatus {
    PRESENT,
    ABSENT,

    /** The class did not happen; it does not count towards the percentage. */
    CANCELLED,
}

/**
 * One subject's attendance on one day.
 *
 * @property count how many classes the mark stands for: 2 when the teacher took a double class
 * (both present or both absent). Ignored for [AttendanceStatus.CANCELLED].
 */
data class AttendanceRecord(
    val subjectId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
    val updatedAt: Long = 0L,
    val count: Int = 1,
) {
    companion object {
        const val MAX_COUNT = 4
    }
}
