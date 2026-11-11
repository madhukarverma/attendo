package com.attendo.core.model

/** One unit of a course's syllabus. */
data class CurriculumUnit(
    /** Roman numeral as printed in the curriculum ("I" to "V"). */
    val number: String,
    val title: String,
    val topics: List<String>,
)

/** A course as described in the official curriculum. */
data class CurriculumCourse(
    val code: String,
    val name: String,
    val contactHours: Int,
    val type: String,
    val assessment: String,
    val units: List<CurriculumUnit>,
    /** Course outcomes, one per sentence part. */
    val outcomes: List<String>,
)

/** The programme's curriculum (bundled from the institute's PDF). */
data class Curriculum(val title: String, val courses: List<CurriculumCourse>) {

    private val byCode = courses.associateBy { it.code.uppercase() }

    fun course(code: String): CurriculumCourse? = byCode[code.trim().uppercase()]

    val totalHours: Int get() = courses.sumOf { it.contactHours }

    companion object {
        val EMPTY = Curriculum("", emptyList())

        /**
         * Splits a comma-separated topic list into topics. Commas inside brackets stay with their
         * topic ("Lists (traversing, slices)"), and a leading "and"/"along with" is dropped.
         */
        fun splitTopics(text: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var depth = 0
            for (c in text) {
                when {
                    c == '(' || c == '[' -> { depth++; current.append(c) }
                    (c == ')' || c == ']') && depth > 0 -> { depth--; current.append(c) }
                    c == ',' && depth == 0 -> { parts += current.toString(); current.clear() }
                    else -> current.append(c)
                }
            }
            parts += current.toString()
            return parts
                .map { it.trim().removeSuffix(".").trim() }
                .map { it.replaceFirst(Regex("^(and|along with)\\s+", RegexOption.IGNORE_CASE), "") }
                .filter { it.isNotEmpty() }
                .map { it.replaceFirstChar(Char::uppercaseChar) }
        }

        /** Course outcomes are written as one line separated by semicolons. */
        fun splitOutcomes(text: String): List<String> =
            text.split(';').map { it.trim().removeSuffix(".").trim() }.filter { it.isNotEmpty() }
                .map { it.replaceFirstChar(Char::uppercaseChar) }
    }
}
