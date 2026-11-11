package com.attendo.core.data

import com.attendo.core.model.HabitCheck
import java.time.DayOfWeek
import com.attendo.core.model.CardPriority
import java.time.LocalTime
import com.attendo.core.model.AnimationMode
import com.attendo.core.model.ThemeMode
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

        val maths = data.subjects.add("Maths", "MA101", 75, lectureHours = 45)
        data.attendance.mark(maths.id, LocalDate.of(2026, 11, 9), AttendanceStatus.PRESENT)
        data.settings.setUserName("Madhukar")

        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher) { 2L }
        advanceUntilIdle()
        assertEquals(listOf(maths), reloaded.subjects.subjects.first())
        assertEquals(45, reloaded.subjects.subjects.first().single().lectureHours)
        assertEquals(AttendanceStatus.PRESENT, reloaded.attendance.records.first().single().status)
        assertEquals("Madhukar", reloaded.settings.settings.first()?.userName)
        assertEquals(AnimationMode.IOS, reloaded.settings.settings.first()?.animationMode)
        reloaded.settings.setAnimationMode(AnimationMode.SMOOTH)
        assertEquals(AnimationMode.SMOOTH, reloaded.settings.settings.first()?.animationMode)
        reloaded.settings.setTheme(ThemeMode.CUSTOM)
        reloaded.settings.setCustomColors(mapOf("background" to "#101010", "accent" to "#FF8800"))
        reloaded.settings.setCustomColors(mapOf("accent" to "#00AAFF"))
        val s = reloaded.settings.settings.first()!!
        assertEquals(ThemeMode.CUSTOM, s.theme)
        assertEquals(mapOf("background" to "#101010", "accent" to "#00AAFF"), s.customColors)
        assertEquals(setOf("subjects.csv", "attendance.csv", "settings.csv"), dir.list()!!.toSet())

        dir.deleteRecursively()
    }

    @Test
    fun todosSurviveReload() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher) { 5L }
        advanceUntilIdle()
        val day = LocalDate.of(2026, 11, 18)
        val reminder = data.todos.add("Revise OS, chapter 4", day, LocalTime.of(19, 30, 12))
        val plain = data.todos.add("Buy lab record", day)
        data.todos.setDone(plain.id, true)

        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        val todos = reloaded.todos.todos.first()
        assertEquals(LocalTime.of(19, 30), todos.single { it.id == reminder.id }.remindAt)
        assertEquals(true, todos.single { it.id == plain.id }.done)
        reloaded.todos.clearDone()
        assertEquals(listOf(reminder.id), reloaded.todos.todos.first().map { it.id })
        dir.deleteRecursively()
    }

    @Test
    fun csvImportMatchesTablesByHeaderAndReplaces() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        data.subjects.add("Old subject", "X1", 75)

        val plan = data.planCsvImport(
            listOf(
                // Renamed file, BOM and reordered columns (as Excel might save it); one bad row.
                "subjects (1).csv" to "\uFEFFname,id,target_percent,code\r\nDSA,s1,75,CA717\r\nOS,s2,80,CA719\r\n,,,\r\n",
                "marks.csv" to "subject_id,date,status,updated_at\ns1,2026-11-09,PRESENT,0\n",
                "notes.csv" to "foo,bar\n1,2\n",
            ),
        )
        assertEquals(listOf("subjects.csv", "attendance.csv"), plan.tables.map { it.file })
        assertEquals(listOf("notes.csv"), plan.unrecognised)

        val kept = data.applyCsvImport(plan)
        assertEquals(mapOf("subjects.csv" to 2, "attendance.csv" to 1), kept)
        assertEquals(listOf("DSA", "OS"), data.subjects.subjects.first().map { it.name })
        assertEquals(80, data.subjects.subjects.first().last().targetPercent)

        // A full snapshot restore empties tables that aren't in the backup.
        val snapshot = data.snapshot()
        data.todos.add("Temp", LocalDate.of(2026, 11, 10))
        data.restoreSnapshot(snapshot)
        assertEquals(emptyList<Any>(), data.todos.todos.first())
        assertEquals(2, data.subjects.subjects.first().size)
        dir.deleteRecursively()
    }

    @Test
    fun kanbanBoardSurvivesReload() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher) { 7L }
        advanceUntilIdle()
        data.kanban.ensureDefaultColumns()
        data.kanban.ensureDefaultColumns() // second call must not duplicate
        val (todo, doing, done) = data.kanban.board.first().sortedColumns
        assertEquals(listOf("To do", "Doing", "Done"), listOf(todo.title, doing.title, done.title))
        assertEquals(true, done.done)

        val a = data.kanban.addCard(todo.id, "Write DSA notes", notes = "Unit II, \"hashing\"", due = LocalDate.of(2026, 11, 20), priority = CardPriority.HIGH)
        val b = data.kanban.addCard(todo.id, "Lab record")
        data.kanban.moveCard(a.id, doing.id, 0)
        data.kanban.updateCard(b.copy(title = "Lab record (OS)", columnId = "ignored", position = 42))

        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher).kanban.let {
            advanceUntilIdle()
            it.board.first()
        }
        assertEquals(listOf(a.id), reloaded.cardsIn(doing.id).map { it.id })
        assertEquals(CardPriority.HIGH, reloaded.cardsIn(doing.id).single().priority)
        assertEquals("Unit II, \"hashing\"", reloaded.cardsIn(doing.id).single().notes)
        val kept = reloaded.cardsIn(todo.id).single()
        assertEquals("Lab record (OS)", kept.title)
        assertEquals(0, kept.position)

        data.kanban.deleteColumn(todo.id)
        val after = data.kanban.board.first()
        assertEquals(listOf("Doing", "Done"), after.sortedColumns.map { it.title })
        assertEquals(listOf(0, 1), after.sortedColumns.map { it.position })
        assertEquals(listOf(a.id), after.cards.map { it.id })
        dir.deleteRecursively()
    }

    @Test
    fun habitsAndTicksSurviveReload() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher) { 3L }
        advanceUntilIdle()
        val gym = data.habits.add("Gym", setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val read = data.habits.add("Read 10 pages", DayOfWeek.entries.toSet())
        val day = LocalDate.of(2026, 11, 9)
        data.habits.setChecked(gym.id, day, true)
        data.habits.setChecked(gym.id, day, true) // ticking twice keeps one row
        data.habits.setChecked(read.id, day, true)
        data.habits.setChecked(read.id, day, false)

        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), reloaded.habits.habits.first().first { it.id == gym.id }.days)
        assertEquals(listOf(0, 1), reloaded.habits.habits.first().map { it.position })
        assertEquals(listOf(HabitCheck(gym.id, day)), reloaded.habits.checks.first())

        reloaded.habits.delete(gym.id)
        assertEquals(emptyList<HabitCheck>(), reloaded.habits.checks.first())
        assertEquals(listOf(read.id), reloaded.habits.habits.first().map { it.id })
        dir.deleteRecursively()
    }

    @Test
    fun plainTodosMoveToTheBoardOnce() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        val day = LocalDate.of(2026, 11, 12)
        data.todos.add("Buy lab record", day)
        val finished = data.todos.add("Submit form", day.minusDays(2))
        data.todos.setDone(finished.id, true)
        val reminder = data.todos.add("Call home", day, LocalTime.of(20, 0))

        assertEquals(2, data.migratePlainTodosToBoard())
        assertEquals(0, data.migratePlainTodosToBoard())
        assertEquals(listOf(reminder.id), data.todos.todos.first().map { it.id })
        val board = data.kanban.board.first()
        val (todo, _, done) = board.sortedColumns
        assertEquals(listOf("Buy lab record"), board.cardsIn(todo.id).map { it.title })
        assertEquals(day, board.cardsIn(todo.id).single().due)
        assertEquals(listOf("Submit form"), board.cardsIn(done.id).map { it.title })
        dir.deleteRecursively()
    }

    @Test
    fun catalogLectureHoursFillOnlyEmptyValues() = runTest {
        val dir = Files.createTempDirectory("attendo").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        data.subjects.add("Operating Systems", "CA719", 75)
        data.subjects.add("DSA", "ca717", 75, lectureHours = 45)
        data.subjects.add("Python Lab", "CA701", 75)
        assertEquals(1, data.fillCatalogLectureHours())
        assertEquals(0, data.fillCatalogLectureHours())
        assertEquals(listOf(40, 45, null), data.subjects.subjects.first().map { it.lectureHours })
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
        // A double class is stored with count 2 and survives a reload.
        data.attendance.mark("s", day, AttendanceStatus.PRESENT, count = 2)
        val reloaded = AttendoData(dir, TestScope(dispatcher), dispatcher)
        advanceUntilIdle()
        assertEquals(2, reloaded.attendance.records.first().single().count)
        data.attendance.mark("s", day, null)
        assertEquals(0, data.attendance.records.first().size)
        dir.deleteRecursively()
    }
}
