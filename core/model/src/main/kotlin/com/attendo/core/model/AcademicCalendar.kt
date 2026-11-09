package com.attendo.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DayKind { CLASSES_START, CLASSES_END, HOLIDAY, EXAM, DEADLINE, EVENT }

/** One calendar entry; multi-day entries (assessments, fests) span [start]..[end]. */
data class CalendarEvent(
    val start: LocalDate,
    val end: LocalDate,
    val kind: DayKind,
    val title: String,
) {
    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)
}

/** The semester's academic calendar (loaded from academic_calendar.csv). */
data class AcademicCalendar(val events: List<CalendarEvent>) {

    val classesStart: LocalDate? = events.firstOrNull { it.kind == DayKind.CLASSES_START }?.start
    val classesEnd: LocalDate? = events.firstOrNull { it.kind == DayKind.CLASSES_END }?.start

    /** Last day of the semester's assessments (end of final exams). */
    val semesterEnd: LocalDate? = events.filter { it.kind == DayKind.EXAM }.maxOfOrNull { it.end } ?: classesEnd

    fun eventsOn(date: LocalDate): List<CalendarEvent> = events.filter { date in it }

    fun isHoliday(date: LocalDate) = eventsOn(date).any { it.kind == DayKind.HOLIDAY }

    /** Next event starting within [days] days after [date] (exclusive of [date]). */
    fun upcoming(date: LocalDate, days: Long = 3): Pair<CalendarEvent, Long>? =
        events.filter { it.start.isAfter(date) && ChronoUnit.DAYS.between(date, it.start) <= days }
            .minByOrNull { it.start }
            ?.let { it to ChronoUnit.DAYS.between(date, it.start) }

    /**
     * Days shown in the home day strip: the whole semester (classes start → final exams), widened
     * to include [today] and a week either side when today falls outside it.
     */
    fun stripDays(today: LocalDate): List<LocalDate> {
        val start = listOfNotNull(classesStart, today.minusDays(7)).min()
        val end = listOfNotNull(semesterEnd, today.plusDays(7)).max()
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    }

    /** What the mascot should say about [date], or null when it is an ordinary day. */
    fun mascotLine(date: LocalDate): String? {
        val today = eventsOn(date)
        val e = today.sortedBy { priority(it.kind) }.firstOrNull()
        if (e != null) {
            val day = ChronoUnit.DAYS.between(e.start, date) + 1
            val total = ChronoUnit.DAYS.between(e.start, e.end) + 1
            val dayOf = if (total > 1) " (day $day of $total)" else ""
            return when (e.kind) {
                DayKind.CLASSES_START -> "Classes start today! Fresh semester, fresh streak."
                DayKind.CLASSES_END -> "Last day of class work! Check every subject before the prevention list."
                DayKind.HOLIDAY -> "Happy ${e.title}! It's a holiday, no classes today."
                DayKind.EXAM -> "${e.title}$dayOf. Deep breath, you've got this!"
                DayKind.DEADLINE -> "Reminder: ${e.title.replaceFirstChar { it.lowercase() }}."
                DayKind.EVENT -> "${e.title}$dayOf! Have fun, but don't forget your classes."
            }
        }
        val (next, inDays) = upcoming(date) ?: return null
        val whenText = if (inDays == 1L) "tomorrow" else "in $inDays days"
        return "Heads up: ${next.title} $whenText."
    }

    private fun priority(kind: DayKind) = when (kind) {
        DayKind.HOLIDAY -> 0
        DayKind.EXAM -> 1
        DayKind.CLASSES_START, DayKind.CLASSES_END -> 2
        DayKind.EVENT -> 3
        DayKind.DEADLINE -> 4
    }

    companion object {
        val EMPTY = AcademicCalendar(emptyList())
    }
}
