package com.attendo.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A task that repeats on chosen days of the week ("Gym" on Mon/Wed/Fri), ticked off per day like a
 * habit tracker.
 */
data class Habit(
    val id: String,
    val title: String,
    val days: Set<DayOfWeek>,
    /** Order in the checklist, 0 first. */
    val position: Int = 0,
    val createdAt: Long = 0L,
) {
    fun isDueOn(date: LocalDate): Boolean = date.dayOfWeek in days
}

/** One tick: [habitId] was done on [date]. */
data class HabitCheck(val habitId: String, val date: LocalDate)

/** Week and streak maths for the checklist. */
object HabitWeek {

    /** Monday to Sunday of the week containing [date]. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return (0L..6L).map(monday::plusDays)
    }

    /** Tasks scheduled for [date], in checklist order. */
    fun dueOn(habits: List<Habit>, date: LocalDate): List<Habit> =
        habits.filter { it.isDueOn(date) }.sortedWith(compareBy({ it.position }, { it.createdAt }))

    /** Days each habit was ticked. */
    fun checkedDays(checks: List<HabitCheck>): Map<String, Set<LocalDate>> =
        checks.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }

    /** `done to total` for the tasks scheduled on [date]. */
    fun progress(habits: List<Habit>, checked: Map<String, Set<LocalDate>>, date: LocalDate): Pair<Int, Int> {
        val due = habits.filter { it.isDueOn(date) }
        return due.count { date in checked[it.id].orEmpty() } to due.size
    }

    /**
     * Scheduled days in a row that were ticked, counting back from [today]. An unticked today
     * doesn't break the streak (the day isn't over); a missed earlier scheduled day does. Days
     * before [startDate] (when the habit was created) aren't held against it.
     */
    fun streak(habit: Habit, checkedOn: Set<LocalDate>, today: LocalDate, startDate: LocalDate? = null): Int {
        if (habit.days.isEmpty()) return 0
        var streak = 0
        var day = today
        repeat(MAX_LOOKBACK_DAYS) {
            if (startDate != null && day < startDate && day !in checkedOn) return streak
            if (habit.isDueOn(day)) {
                when {
                    day in checkedOn -> streak++
                    day == today -> Unit
                    else -> return streak
                }
            }
            day = day.minusDays(1)
        }
        return streak
    }

    private const val MAX_LOOKBACK_DAYS = 730
}
