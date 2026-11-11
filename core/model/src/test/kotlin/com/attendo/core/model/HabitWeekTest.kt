package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitWeekTest {
    // Wednesday 11 Nov 2026.
    private val today = LocalDate.of(2026, 11, 11)
    private val gym = Habit("gym", "Gym", setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    private val read = Habit("read", "Read", DayOfWeek.entries.toSet(), position = 1)

    @Test
    fun weekRunsMondayToSunday() {
        val week = HabitWeek.weekOf(today)
        assertEquals(LocalDate.of(2026, 11, 9), week.first())
        assertEquals(LocalDate.of(2026, 11, 15), week.last())
        assertEquals(7, week.size)
        assertEquals(week, HabitWeek.weekOf(week.last()))
    }

    @Test
    fun dueAndProgressFollowTheWeekday() {
        val habits = listOf(read, gym)
        assertEquals(listOf("gym", "read"), HabitWeek.dueOn(habits, today).map { it.id })
        assertEquals(listOf("read"), HabitWeek.dueOn(habits, today.plusDays(1)).map { it.id })
        val checked = HabitWeek.checkedDays(listOf(HabitCheck("gym", today), HabitCheck("read", today.minusDays(1))))
        assertEquals(1 to 2, HabitWeek.progress(habits, checked, today))
        assertEquals(0 to 1, HabitWeek.progress(habits, checked, today.plusDays(1)))
    }

    @Test
    fun streakCountsScheduledDaysOnly() {
        // Gym is Mon/Wed/Fri: ticked Fri 6th, Mon 9th and today (Wed 11th) = 3, the days between don't matter.
        val ticks = setOf(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9), today)
        assertEquals(3, HabitWeek.streak(gym, ticks, today))
        // Today not ticked yet: still 2, not broken.
        assertEquals(2, HabitWeek.streak(gym, ticks - today, today))
        // Monday missed: only today counts.
        assertEquals(1, HabitWeek.streak(gym, setOf(LocalDate.of(2026, 11, 6), today), today))
        assertEquals(0, HabitWeek.streak(gym, emptySet(), today))
    }

    @Test
    fun streakStopsAtTheStartDate() {
        // Created on Monday the 9th and ticked every scheduled day since: earlier days don't count against it.
        val ticks = setOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10), today)
        assertEquals(3, HabitWeek.streak(read, ticks, today, startDate = LocalDate.of(2026, 11, 9)))
        assertEquals(0, HabitWeek.streak(Habit("none", "No days", emptySet()), ticks, today))
    }
}
