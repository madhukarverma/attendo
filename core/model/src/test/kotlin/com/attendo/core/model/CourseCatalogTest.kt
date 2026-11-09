package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseCatalogTest {

    @Test
    fun catalogHasSevenSubjectsWithUniqueCodes() {
        val codes = CourseCatalog.CURRENT_SEMESTER.map { it.code }
        assertEquals(7, codes.size)
        assertEquals(7, codes.toSet().size)
        assertEquals(2, CourseCatalog.CURRENT_SEMESTER.count { it.isLab })
    }

    @Test
    fun missingIgnoresAlreadyAddedCodesCaseInsensitively() {
        val existing = listOf(Subject("1", "Operating Systems", code = "ca719 "))
        val missing = CourseCatalog.missing(existing)
        assertEquals(6, missing.size)
        assertEquals(false, missing.any { it.code == "CA719" })
    }
}
