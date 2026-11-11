package com.attendo.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One class in the weekly timetable. */
data class TimetableSlot(
    val id: String,
    val subjectId: String,
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val room: String = "",
)

/** An entry the student adds to their own calendar (a class test, a submission, a trip...). */
data class PersonalEvent(
    val id: String,
    val title: String,
    val start: LocalDate,
    val end: LocalDate = start,
    val kind: DayKind = DayKind.EVENT,
    val note: String = "",
    val createdAt: Long = 0L,
)

/** Timetable maths: what's on a given day, what's next. */
object Timetable {

    private val order = compareBy<TimetableSlot>({ it.day }, { it.start }, { it.end })

    fun sorted(slots: List<TimetableSlot>): List<TimetableSlot> = slots.sortedWith(order)

    /** Classes on [date]'s weekday, earliest first. */
    fun on(slots: List<TimetableSlot>, date: LocalDate): List<TimetableSlot> =
        slots.filter { it.day == date.dayOfWeek }.sortedWith(order)

    /** Classes per subject on [date], each subject's earliest first. */
    fun bySubjectOn(slots: List<TimetableSlot>, date: LocalDate): Map<String, List<TimetableSlot>> =
        on(slots, date).groupBy { it.subjectId }

    /** The next class still to start today, or the one in progress, at [now]. */
    fun next(slots: List<TimetableSlot>, now: LocalDateTime): TimetableSlot? =
        on(slots, now.toLocalDate()).firstOrNull { now.toLocalTime() < it.end }

    /** True when the two slots are on the same day and their times overlap. */
    fun overlaps(a: TimetableSlot, b: TimetableSlot): Boolean = a.day == b.day && a.start < b.end && b.start < a.end
}

/** The academic calendar with the student's own events merged in (marked [CalendarEvent.personal]). */
fun AcademicCalendar.withPersonal(personal: List<PersonalEvent>): AcademicCalendar =
    if (personal.isEmpty()) {
        this
    } else {
        copy(
            events = (events.filterNot { it.personal } + personal.map {
                CalendarEvent(it.start, maxOf(it.start, it.end), it.kind, it.title, personal = true, id = it.id, note = it.note)
            }).sortedWith(compareBy({ it.start }, { it.end })),
        )
    }
