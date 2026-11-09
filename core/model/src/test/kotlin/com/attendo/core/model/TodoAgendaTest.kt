package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TodoAgendaTest {
    private val today = LocalDate.of(2026, 11, 9)

    private fun todo(id: String, date: LocalDate, time: LocalTime? = null, done: Boolean = false, created: Long = 0) =
        Todo(id, "Task $id", date, time, done, created)

    @Test
    fun groupsByDayAndStatus() {
        val todos = listOf(
            todo("late", today.minusDays(2)),
            todo("lateDone", today.minusDays(1), done = true),
            todo("todayPlain", today, created = 1),
            todo("todayAt9", today, LocalTime.of(9, 0), created = 2),
            todo("todayDone", today, done = true),
            todo("next", today.plusDays(3)),
            todo("soon", today.plusDays(1), LocalTime.of(18, 0)),
        )
        val g = TodoAgenda.group(todos, today)
        assertEquals(listOf("late"), g.overdue.map { it.id })
        // Reminders first by time, plain to-dos after, done items last.
        assertEquals(listOf("todayAt9", "todayPlain", "todayDone"), g.today.map { it.id })
        assertEquals(listOf("soon", "next"), g.upcoming.map { it.id })
        assertEquals(listOf("lateDone"), g.done.map { it.id })
        assertEquals(5, g.openCount)
    }

    @Test
    fun pendingRemindersAreFutureAndOpen() {
        val now = today.atTime(12, 0)
        val past = todo("past", today, LocalTime.of(9, 0))
        val later = todo("later", today, LocalTime.of(15, 0))
        val tomorrow = todo("tomorrow", today.plusDays(1), LocalTime.of(8, 0))
        val ticked = todo("ticked", today, LocalTime.of(16, 0), done = true)
        val plain = todo("plain", today.plusDays(1))
        assertFalse(past.hasPendingReminder(now))
        assertTrue(later.hasPendingReminder(now))
        assertEquals(
            listOf("later", "tomorrow"),
            TodoAgenda.upcomingReminders(listOf(tomorrow, ticked, past, later, plain), now).map { it.id },
        )
    }
}
