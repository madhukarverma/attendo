package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSummaryTest {
    private val subject = Subject(id = "s1", name = "Maths", targetPercent = 75)

    @Test
    fun doubleClassCountsAsTwo() {
        val day = java.time.LocalDate.of(2026, 11, 9)
        val records = listOf(
            AttendanceRecord("s1", day, AttendanceStatus.PRESENT, count = 2),
            AttendanceRecord("s1", day.plusDays(1), AttendanceStatus.ABSENT),
            AttendanceRecord("s1", day.plusDays(2), AttendanceStatus.CANCELLED, count = 2),
            AttendanceRecord("other", day, AttendanceStatus.ABSENT, count = 2),
        )
        val s = AttendanceSummary.from(subject, records)
        assertEquals(2, s.present)
        assertEquals(1, s.absent)
        assertEquals(3, s.held)
    }

    @Test
    fun lecturesLeftNeedsCourseHours() {
        val s = AttendanceSummary(subject, present = 8, absent = 2)
        assertEquals(null, s.lecturesLeft)
        assertEquals(null, s.courseSkipsLeft())
    }

    @Test
    fun lectureHoursGiveLecturesLeftAndSkips() {
        // 40-hour course, 10 held (8 present). 75% of 40 = 30 present needed: 22 of the 30 left.
        val s = AttendanceSummary(subject.copy(lectureHours = 40), present = 8, absent = 2)
        assertEquals(30, s.lecturesLeft)
        assertEquals(8, s.courseSkipsLeft())
        assertEquals(95.0, s.bestFinalPercent()!!, 1e-9)
    }

    @Test
    fun unreachableTargetIsNegative() {
        // 20-hour course, 10 held, only 2 present: even 10/10 more gives 12/20 = 60% < 75%.
        val s = AttendanceSummary(subject.copy(lectureHours = 20), present = 2, absent = 8)
        assertEquals(10, s.lecturesLeft)
        assertTrue(s.courseSkipsLeft()!! < 0)
        assertEquals(60.0, s.bestFinalPercent()!!, 1e-9)
    }

    @Test
    fun moreClassesThanHoursLeavesNoneLeft() {
        val s = AttendanceSummary(subject.copy(lectureHours = 5), present = 6, absent = 0)
        assertEquals(0, s.lecturesLeft)
        assertEquals(0, s.courseSkipsLeft())
    }

    @Test
    fun emptySubjectIsOnTrack() {
        val s = AttendanceSummary(subject, present = 0, absent = 0)
        assertTrue(s.isOnTrack)
        assertEquals(0.0, s.percent, 0.0)
        assertEquals(0, s.classesNeeded())
    }

    @Test
    fun belowTargetNeedsClasses() {
        // 5/10 = 50%. Need n with (5+n)/(10+n) >= .75 -> n = 10
        val s = AttendanceSummary(subject, present = 5, absent = 5)
        assertFalse(s.isOnTrack)
        assertEquals(10, s.classesNeeded())
        assertEquals(0, s.classesCanSkip())
    }

    @Test
    fun aboveTargetCanSkip() {
        // 9/10 = 90%. 9/(10+n) >= .75 -> n <= 2
        val s = AttendanceSummary(subject, present = 9, absent = 1)
        assertTrue(s.isOnTrack)
        assertEquals(2, s.classesCanSkip())
        assertEquals(0, s.classesNeeded())
    }

    @Test
    fun exactlyOnTarget() {
        val s = AttendanceSummary(subject, present = 3, absent = 1)
        assertTrue(s.isOnTrack)
        assertEquals(0, s.classesCanSkip())
    }
}
