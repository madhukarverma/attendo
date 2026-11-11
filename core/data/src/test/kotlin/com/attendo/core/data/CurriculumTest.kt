package com.attendo.core.data

import com.attendo.core.data.curriculum.CurriculumLoader
import com.attendo.core.model.Curriculum
import com.attendo.core.model.CourseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {

    @Test
    fun bundledCurriculumHasEverySemesterOneTheoryCourse() {
        val curriculum = CurriculumLoader.bundled()
        assertEquals(listOf("CA711", "CA713", "CA715", "CA717", "CA719"), curriculum.courses.map { it.code })
        curriculum.courses.forEach { course ->
            assertEquals(40, course.contactHours)
            assertEquals(listOf("I", "II", "III", "IV", "V"), course.units.map { it.number })
            assertTrue(course.units.all { it.topics.size >= 3 })
            assertEquals(3, course.outcomes.size)
        }
        assertEquals(200, curriculum.totalHours)
        assertEquals("Operating Systems", curriculum.course("ca719")?.name)
        // The catalog's lecture hours come from the same document.
        CourseCatalog.CURRENT_SEMESTER.filterNot { it.isLab }.forEach { s ->
            assertEquals(curriculum.course(s.code)?.contactHours, s.lectureHours)
        }
    }

    @Test
    fun topicsKeepBracketedListsTogether() {
        assertEquals(
            listOf("Lists (traversing, slices)", "Tuples", "Map", "Filter", "Reduce"),
            Curriculum.splitTopics("Lists (traversing, slices), Tuples, Map, Filter, and Reduce."),
        )
        assertEquals(listOf("Explain a", "Analyze b"), Curriculum.splitOutcomes("explain a; analyze b."))
    }
}
