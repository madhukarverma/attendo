package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSummaryTest {
    private val subject = Subject(id = "s1", name = "Maths", targetPercent = 75)

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
