package com.attendo.core.model

/**
 * A course/subject whose attendance is tracked.
 *
 * @property targetPercent minimum attendance the student wants (or must) keep, 0..100.
 */
data class Subject(
    val id: String,
    val name: String,
    val code: String = "",
    val targetPercent: Int = DEFAULT_TARGET_PERCENT,
    val createdAt: Long = 0L,
) {
    companion object {
        const val DEFAULT_TARGET_PERCENT = 75
    }
}
