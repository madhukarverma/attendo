package com.attendo.core.data

import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CourseCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {

    @Test
    fun dataSurvivesReload() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher) { 1L }
        advanceUntilIdle()

        val maths = data.subjects.add("Maths", "MA101", 75)
        data.attendance.mark(maths.id, LocalDate.of(2026, 11, 9), AttendanceStatus.PRESENT)
        data.settings.setUserName("Madhukar")

        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher) { 2L }
        advanceUntilIdle()
        assertEquals(listOf(maths), reloaded.subjects.subjects.first())
        assertEquals(AttendanceStatus.PRESENT, reloaded.attendance.records.first().single().status)
        assertEquals("Madhukar", reloaded.settings.settings.first()?.userName)
        assertEquals(true, reloaded.settings.settings.first()?.iosMotion)
        reloaded.settings.setIosMotion(false)
        assertEquals(false, reloaded.settings.settings.first()?.iosMotion)
        assertEquals(setOf("subjects.csv", "attendance.csv", "settings.csv"), dir.list()!!.toSet())

        dir.deleteRecursively()
    }

    @Test
    fun addFromCatalogSkipsExistingCodes() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        data.subjects.add("OS", "CA719", 80)
        assertEquals(6, data.subjects.addFromCatalog(CourseCatalog.CURRENT_SEMESTER))
        assertEquals(0, data.subjects.addFromCatalog(CourseCatalog.CURRENT_SEMESTER))
        assertEquals(7, data.subjects.subjects.first().size)
        dir.deleteRecursively()
    }

    @Test
    fun clearingMarkRemovesRecord() = runTest {
        val dir: File = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        val day = LocalDate.of(2026, 11, 9)
        data.attendance.mark("s", day, AttendanceStatus.ABSENT)
        data.attendance.mark("s", day, AttendanceStatus.PRESENT)
        assertEquals(1, data.attendance.records.first().size)
        data.attendance.mark("s", day, null)
        assertEquals(0, data.attendance.records.first().size)
        dir.deleteRecursively()
    }
}
