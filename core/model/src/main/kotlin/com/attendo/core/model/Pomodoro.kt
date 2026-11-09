package com.attendo.core.model

import java.time.LocalDate

enum class PomodoroKind { FOCUS, SHORT_BREAK, LONG_BREAK }

/** One finished (or stopped) timer run, as stored in pomodoro.csv. */
data class PomodoroSession(
    val id: String,
    val date: LocalDate,
    val startedAt: Long,
    val kind: PomodoroKind,
    /** Subject studied during a focus session; null for "General". */
    val subjectId: String?,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    /** False when the user reset/skipped before the timer ran out. */
    val completed: Boolean,
)

data class PomodoroSummary(
    val todayFocusMinutes: Int,
    val weekFocusMinutes: Int,
    val totalFocusMinutes: Int,
    val completedFocusSessions: Int,
    val streakDays: Int,
    /** Focus minutes for each of the last 7 days, oldest first. */
    val last7Days: List<Pair<LocalDate, Int>>,
    /** Focus minutes per subject (name), largest first; "General" for no subject. */
    val bySubject: List<Pair<String, Int>>,
)

object PomodoroStats {

    fun summarize(sessions: List<PomodoroSession>, subjects: List<Subject>, today: LocalDate): PomodoroSummary {
        val focus = sessions.filter { it.kind == PomodoroKind.FOCUS }
        fun minutesOn(day: LocalDate) = focus.filter { it.date == day }.sumOf { it.actualSeconds } / 60
        val weekStart = today.minusDays(6)
        val names = subjects.associate { it.id to it.name }

        // Consecutive days (ending today, or yesterday if nothing yet today) with a completed focus session.
        val activeDays = focus.filter { it.completed }.mapTo(HashSet()) { it.date }
        var day = if (today in activeDays) today else today.minusDays(1)
        var streak = 0
        while (day in activeDays) {
            streak++
            day = day.minusDays(1)
        }

        return PomodoroSummary(
            todayFocusMinutes = minutesOn(today),
            weekFocusMinutes = focus.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
                .sumOf { it.actualSeconds } / 60,
            totalFocusMinutes = focus.sumOf { it.actualSeconds } / 60,
            completedFocusSessions = focus.count { it.completed },
            streakDays = streak,
            last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to minutesOn(it) },
            bySubject = focus.groupBy { it.subjectId?.let(names::get) ?: "General" }
                .map { (name, list) -> name to list.sumOf { it.actualSeconds } / 60 }
                .filter { it.second > 0 }
                .sortedByDescending { it.second },
        )
    }
}
