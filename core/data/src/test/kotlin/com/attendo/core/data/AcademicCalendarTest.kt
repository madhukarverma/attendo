package com.attendo.core.data

import com.attendo.core.data.calendar.AcademicCalendarLoader
import com.attendo.core.model.DayKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AcademicCalendarTest {
    private val cal = AcademicCalendarLoader.bundled()

    @Test
    fun bundledCalendarLoadsSemesterBounds() {
        assertEquals(31, cal.events.size)
        assertEquals(LocalDate.of(2026, 8, 13), cal.classesStart)
        assertEquals(LocalDate.of(2026, 11, 25), cal.classesEnd)
        assertEquals(LocalDate.of(2026, 12, 17), cal.semesterEnd)
    }

    @Test
    fun multiDayEventsAndHolidays() {
        assertTrue(cal.isHoliday(LocalDate.of(2026, 10, 2)))
        val exam = cal.eventsOn(LocalDate.of(2026, 9, 17)).single()
        assertEquals(DayKind.EXAM, exam.kind)
        assertEquals("1st Assessment (day 2 of 3). Deep breath, you've got this!", cal.mascotLine(LocalDate.of(2026, 9, 17)))
    }

    @Test
    fun mascotLineMentionsUpcomingEventsAndStaysQuietOtherwise() {
        assertEquals("Heads up: Diwali (Deepavali) in 2 days.", cal.mascotLine(LocalDate.of(2026, 11, 6)))
        assertNull(cal.mascotLine(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun stripCoversWholeSemester() {
        val days = cal.stripDays(LocalDate.of(2026, 11, 9))
        assertEquals(LocalDate.of(2026, 8, 13), days.first())
        assertEquals(LocalDate.of(2026, 12, 17), days.last())
    }
}
