package com.attendo.core.model

import java.time.LocalDate

/** Marks in one assessment of a subject (a test, assignment, lab exam or the end-semester exam). */
data class Assessment(
    val id: String,
    val subjectId: String,
    val title: String,
    val scored: Double,
    val maxMarks: Double,
    val date: LocalDate? = null,
    val createdAt: Long = 0L,
) {
    /** Score as a percentage of the maximum, 0..100 (0 when the maximum is 0). */
    val percent: Double get() = if (maxMarks <= 0.0) 0.0 else (scored / maxMarks * 100.0).coerceIn(0.0, 100.0)
}

/** A subject's assessments, newest first, with the running total. */
data class SubjectGrades(val subject: Subject, val assessments: List<Assessment>) {
    val scored: Double get() = assessments.sumOf { it.scored }
    val maxMarks: Double get() = assessments.sumOf { it.maxMarks }

    /** Total marks over total maximum, or null when nothing is recorded yet. */
    val percent: Double? get() = if (maxMarks <= 0.0) null else scored / maxMarks * 100.0

    val grade: String? get() = percent?.let(Grades::letter)
}

object Grades {

    /** Usual names of assessments, offered as one-tap titles (from the academic calendar). */
    val SUGGESTED_TITLES = listOf("1st Assessment", "2nd Assessment", "Compensation", "Assignment", "Lab exam", "End semester")

    /** Bounds for what can be entered. */
    const val MAX_MARKS_LIMIT = 1000.0

    /**
     * A letter grade on the 10-point scale used at NITs (S ≥ 90, A ≥ 80, B ≥ 70, C ≥ 60, D ≥ 50,
     * E ≥ 35, else F). Shown as a guide only: the institute's relative grading decides the real one.
     */
    fun letter(percent: Double): String = when {
        percent >= 90 -> "S"
        percent >= 80 -> "A"
        percent >= 70 -> "B"
        percent >= 60 -> "C"
        percent >= 50 -> "D"
        percent >= 35 -> "E"
        else -> "F"
    }

    /** Groups [assessments] by subject, newest (by date, then entry) first; subjects with none included. */
    fun bySubject(subjects: List<Subject>, assessments: List<Assessment>): List<SubjectGrades> {
        val grouped = assessments.groupBy { it.subjectId }
        return subjects.map { s ->
            SubjectGrades(
                s,
                grouped[s.id].orEmpty().sortedWith(compareByDescending<Assessment> { it.date ?: LocalDate.MIN }.thenByDescending { it.createdAt }),
            )
        }
    }

    /** Overall percentage across every recorded assessment, or null when there are none. */
    fun overall(grades: List<SubjectGrades>): Double? {
        val max = grades.sumOf { it.maxMarks }
        return if (max <= 0.0) null else grades.sumOf { it.scored } / max * 100.0
    }

    /** "18", "18.5": marks without a pointless ".0". */
    fun format(marks: Double): String =
        if (marks == Math.floor(marks)) marks.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, marks)
}
