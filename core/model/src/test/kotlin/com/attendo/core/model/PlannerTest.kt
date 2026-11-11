package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PlannerTest {
    // Thursday 12 Nov 2026.
    private val thursday = LocalDate.of(2026, 11, 12)
    private fun slot(id: String, subject: String, day: DayOfWeek, from: Int, to: Int) =
        TimetableSlot(id, subject, day, LocalTime.of(from, 0), LocalTime.of(to, 0))

    private val slots = listOf(
        slot("b", "os", DayOfWeek.THURSDAY, 11, 12),
        slot("a", "dsa", DayOfWeek.THURSDAY, 9, 10),
        slot("c", "dsa", DayOfWeek.THURSDAY, 14, 16),
        slot("d", "maths", DayOfWeek.FRIDAY, 9, 10),
    )

    @Test
    fun dayAndNext() {
        assertEquals(listOf("a", "b", "c"), Timetable.on(slots, thursday).map { it.id })
        assertEquals(listOf("a", "c"), Timetable.bySubjectOn(slots, thursday)["dsa"]?.map { it.id })
        assertEquals("b", Timetable.next(slots, thursday.atTime(10, 30))?.id)
        assertEquals("b", Timetable.next(slots, thursday.atTime(11, 30))?.id) // in progress
        assertEquals(null, Timetable.next(slots, thursday.atTime(16, 0)))
    }

    @Test
    fun overlapsNeedSameDayAndTime() {
        assertTrue(Timetable.overlaps(slot("x", "a", DayOfWeek.MONDAY, 9, 11), slot("y", "b", DayOfWeek.MONDAY, 10, 12)))
        assertFalse(Timetable.overlaps(slot("x", "a", DayOfWeek.MONDAY, 9, 10), slot("y", "b", DayOfWeek.MONDAY, 10, 11)))
        assertFalse(Timetable.overlaps(slot("x", "a", DayOfWeek.MONDAY, 9, 11), slot("y", "b", DayOfWeek.TUESDAY, 9, 11)))
    }

    @Test
    fun personalEventsMergeIntoTheCalendar() {
        val base = AcademicCalendar(listOf(CalendarEvent(thursday, thursday, DayKind.HOLIDAY, "Holiday")))
        val quiz = PersonalEvent("q", "DSA quiz", thursday.plusDays(1), kind = DayKind.EXAM)
        val merged = base.withPersonal(listOf(quiz))
        assertEquals(listOf("Holiday", "DSA quiz"), merged.events.map { it.title })
        assertTrue(merged.eventsOn(thursday.plusDays(1)).single().personal)
        assertEquals("q", merged.events.last().id)
        // Merging again replaces, never duplicates, personal entries.
        assertEquals(2, merged.withPersonal(listOf(quiz)).events.size)
        assertEquals(base, base.withPersonal(emptyList()))
    }
}
