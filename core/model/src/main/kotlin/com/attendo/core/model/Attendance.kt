package com.attendo.core.model

import java.time.LocalDate

enum class AttendanceStatus {
    PRESENT,
    ABSENT,

    /** The class did not happen; it does not count towards the percentage. */
    CANCELLED,
}

/** One subject's attendance on one day. v0.1 supports a single class per subject per day. */
data class AttendanceRecord(
    val subjectId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
    val updatedAt: Long = 0L,
)
