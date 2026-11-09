package com.attendo.core.model

/**
 * A course/subject whose attendance is tracked.
 *
 * @property targetPercent minimum attendance the student wants (or must) keep, 0..100.
 * @property lectureHours total lecture hours of the course; one lecture is one hour, so this is
 * also the number of lectures in the course. `null` when the student hasn't entered it.
 */
data class Subject(
    val id: String,
    val name: String,
    val code: String = "",
    val targetPercent: Int = DEFAULT_TARGET_PERCENT,
    val createdAt: Long = 0L,
    val lectureHours: Int? = null,
) {
    companion object {
        const val DEFAULT_TARGET_PERCENT = 75
    }
}
