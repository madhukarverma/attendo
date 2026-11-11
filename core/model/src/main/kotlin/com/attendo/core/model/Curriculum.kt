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

/** One unit of one course, e.g. CA717 unit III; codes and numerals are stored upper-case. */
data class UnitKey(val code: String, val unit: String)

/** How much of a course the student has covered. */
data class CourseProgress(val covered: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else covered.toFloat() / total
}

/** The programme's curriculum (bundled from the institute's PDF). */
data class Curriculum(val title: String, val courses: List<CurriculumCourse>) {

    private val byCode = courses.associateBy { it.code.uppercase() }

    fun course(code: String): CurriculumCourse? = byCode[code.trim().uppercase()]

    val totalHours: Int get() = courses.sumOf { it.contactHours }

    val totalUnits: Int get() = courses.sumOf { it.units.size }

    fun progress(course: CurriculumCourse, covered: Set<UnitKey>): CourseProgress =
        CourseProgress(course.units.count { UnitKey(course.code.uppercase(), it.number.uppercase()) in covered }, course.units.size)

    /** Units covered across the whole curriculum (ignores keys of courses that aren't in it). */
    fun coveredUnits(covered: Set<UnitKey>): Int = courses.sumOf { progress(it, covered).covered }

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
