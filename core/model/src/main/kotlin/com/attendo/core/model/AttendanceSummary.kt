package com.attendo.core.model

import kotlin.math.ceil
import kotlin.math.floor

/** Aggregated attendance for one subject, plus the "bunk calculator" maths. */
data class AttendanceSummary(
    val subject: Subject,
    val present: Int,
    val absent: Int,
) {
    val held: Int get() = present + absent

    /** Attendance percentage; 0.0 when no class has been held. */
    val percent: Double get() = if (held == 0) 0.0 else present * 100.0 / held

    val isOnTrack: Boolean get() = held == 0 || percent + EPSILON >= subject.targetPercent

    /** Consecutive classes that must be attended to reach the target. */
    fun classesNeeded(): Int {
        val t = subject.targetPercent / 100.0
        if (t <= 0.0 || isOnTrack) return 0
        if (t >= 1.0) return Int.MAX_VALUE
        // (p + n) / (h + n) >= t  =>  n >= (t*h - p) / (1 - t)
        return ceil((t * held - present) / (1 - t) - EPSILON).toInt().coerceAtLeast(0)
    }

    /** Classes that can be missed while staying at or above the target. */
    fun classesCanSkip(): Int {
        val t = subject.targetPercent / 100.0
        if (t <= 0.0) return Int.MAX_VALUE
        // p / (h + n) >= t  =>  n <= p/t - h
        return floor(present / t - held + EPSILON).toInt().coerceAtLeast(0)
    }

    companion object {
        private const val EPSILON = 1e-9

        fun from(subject: Subject, records: List<AttendanceRecord>): AttendanceSummary {
            val own = records.filter { it.subjectId == subject.id }
            return AttendanceSummary(
                subject = subject,
                present = own.count { it.status == AttendanceStatus.PRESENT },
                absent = own.count { it.status == AttendanceStatus.ABSENT },
            )
        }
    }
}
