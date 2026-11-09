package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PomodoroStatsTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val os = Subject("os", "Operating Systems", "CA719")

    private fun focus(daysAgo: Long, minutes: Int, subject: String? = "os", completed: Boolean = true) =
        PomodoroSession("$daysAgo-$minutes", today.minusDays(daysAgo), 0, PomodoroKind.FOCUS, subject, 1500, minutes * 60, completed)

    @Test
    fun summarizesFocusTime() {
        val sessions = listOf(
            focus(0, 25), focus(0, 10, completed = false), focus(1, 25, subject = null), focus(9, 50),
            PomodoroSession("b", today, 0, PomodoroKind.SHORT_BREAK, null, 300, 300, true),
        )
        val s = PomodoroStats.summarize(sessions, listOf(os), today)
        assertEquals(35, s.todayFocusMinutes)
        assertEquals(60, s.weekFocusMinutes)
        assertEquals(110, s.totalFocusMinutes)
        assertEquals(3, s.completedFocusSessions)
        assertEquals(2, s.streakDays)
        assertEquals(7, s.last7Days.size)
        assertEquals(today to 35, s.last7Days.last())
        assertEquals(listOf("Operating Systems" to 85, "General" to 25), s.bySubject)
    }

    @Test
    fun streakCountsFromYesterdayWhenTodayIsEmpty() {
        val s = PomodoroStats.summarize(listOf(focus(1, 25), focus(2, 25), focus(4, 25)), emptyList(), today)
        assertEquals(2, s.streakDays)
    }
}
