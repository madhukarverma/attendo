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

    /**
     * Lectures still to come in the course (1 lecture = 1 hour): lecture hours minus classes held.
     * Cancelled classes don't count as held. `null` when the course hours aren't set.
     */
    val lecturesLeft: Int?
        get() = subject.lectureHours?.let { (it - held).coerceAtLeast(0) }

    /**
     * Course outlook once the total is known: how many of the remaining lectures can still be
     * missed while finishing the course at or above the target. Negative means the target can no
     * longer be reached even by attending everything; `null` when the course hours aren't set.
     */
    fun courseSkipsLeft(): Int? {
        val left = lecturesLeft ?: return null
        val total = held + left
        val mustAttendInTotal = ceil(subject.targetPercent / 100.0 * total - EPSILON).toInt()
        val stillToAttend = (mustAttendInTotal - present).coerceAtLeast(0)
        return left - stillToAttend
    }

    /** Final percentage if every remaining lecture is attended; `null` when the course hours aren't set. */
    fun bestFinalPercent(): Double? {
        val left = lecturesLeft ?: return null
        val total = held + left
        return if (total == 0) null else (present + left) * 100.0 / total
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
