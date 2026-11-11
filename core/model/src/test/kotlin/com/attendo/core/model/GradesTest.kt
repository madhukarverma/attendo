package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GradesTest {
    private val os = Subject("os", "Operating Systems", "CA719")
    private val dsa = Subject("dsa", "Data Structures", "CA717")

    @Test
    fun totalsAndLetters() {
        val a = Assessment("1", "os", "1st Assessment", scored = 18.0, maxMarks = 20.0, date = LocalDate.of(2026, 9, 17))
        val b = Assessment("2", "os", "2nd Assessment", scored = 13.5, maxMarks = 20.0, date = LocalDate.of(2026, 11, 4))
        val grades = Grades.bySubject(listOf(os, dsa), listOf(a, b))
        val osGrades = grades.first()
        assertEquals(listOf("2", "1"), osGrades.assessments.map { it.id }) // newest first
        assertEquals(78.75, osGrades.percent!!, 1e-9)
        assertEquals("B", osGrades.grade)
        assertNull(grades.last().percent)
        assertEquals(78.75, Grades.overall(grades)!!, 1e-9)
        assertEquals(90.0, a.percent, 1e-9)
    }

    @Test
    fun formatAndBounds() {
        assertEquals("18", Grades.format(18.0))
        assertEquals("13.5", Grades.format(13.5))
        assertEquals("S", Grades.letter(90.0))
        assertEquals("F", Grades.letter(34.9))
        assertEquals(0.0, Assessment("x", "os", "Zero max", 5.0, 0.0).percent, 0.0)
        assertNull(Grades.overall(emptyList()))
    }
}
