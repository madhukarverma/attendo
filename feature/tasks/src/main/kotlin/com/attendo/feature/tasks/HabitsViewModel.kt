package com.attendo.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.HabitRepository
import com.attendo.core.model.Habit
import com.attendo.core.model.HabitWeek
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How one day of the week looks for a habit. */
internal enum class DayMark {
    /** Not scheduled that day. */
    OFF,

    /** Scheduled, still to come (or today, not ticked yet). */
    PLANNED,

    /** Scheduled on a past day and not ticked. */
    MISSED,
    DONE,
}

internal data class HabitRow(
    val habit: Habit,
    /** Ticked on the selected day. */
    val checked: Boolean,
    val streak: Int,
    /** Monday to Sunday of the shown week. */
    val week: List<DayMark>,
)

internal data class HabitsUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val week: List<LocalDate> = HabitWeek.weekOf(LocalDate.now()),
    /** `done to total` per day of the shown week. */
    val progress: Map<LocalDate, Pair<Int, Int>> = emptyMap(),
    /** Tasks scheduled for the selected day. */
    val rows: List<HabitRow> = emptyList(),
    val totalHabits: Int = 0,
) {
    /** Days after today can be planned but not ticked. */
    val canTick: Boolean get() = !selected.isAfter(today)
    val selectedProgress: Pair<Int, Int> get() = progress[selected] ?: (0 to 0)
}

internal class HabitsViewModel(private val repository: HabitRepository) : ViewModel() {

    private val selected = MutableStateFlow(LocalDate.now())

    /** Ticks every minute so "today" rolls over at midnight while the page is open. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }

    val uiState: StateFlow<HabitsUiState> =
        combine(repository.habits, repository.checks, selected, today) { habits, checks, day, now ->
            val checked = HabitWeek.checkedDays(checks)
            val week = HabitWeek.weekOf(day)
            HabitsUiState(
                loaded = true,
                today = now,
                selected = day,
                week = week,
                progress = week.associateWith { HabitWeek.progress(habits, checked, it) },
                rows = HabitWeek.dueOn(habits, day).map { habit ->
                    val ticks = checked[habit.id].orEmpty()
                    HabitRow(
                        habit = habit,
                        checked = day in ticks,
                        streak = HabitWeek.streak(habit, ticks, now, habit.startDate()),
                        week = week.map { d ->
                            when {
                                !habit.isDueOn(d) -> DayMark.OFF
                                d in ticks -> DayMark.DONE
                                d < now -> DayMark.MISSED
                                else -> DayMark.PLANNED
                            }
                        },
                    )
                },
                totalHabits = habits.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun select(date: LocalDate) {
        selected.value = date
    }

    /** Moves the shown week back or forward, keeping the same weekday selected. */
    fun shiftWeek(by: Int) {
        selected.value = selected.value.plusWeeks(by.toLong())
    }

    fun toggle(row: HabitRow) {
        val state = uiState.value
        if (!state.canTick) return
        viewModelScope.launch { repository.setChecked(row.habit.id, state.selected, !row.checked) }
    }

    fun add(title: String, days: Set<DayOfWeek>) {
        if (title.isBlank() || days.isEmpty()) return
        viewModelScope.launch { repository.add(title, days) }
    }

    fun update(habit: Habit) {
        if (habit.title.isBlank() || habit.days.isEmpty()) return
        viewModelScope.launch { repository.update(habit) }
    }

    fun delete(habit: Habit) {
        viewModelScope.launch { repository.delete(habit.id) }
    }
}

private fun Habit.startDate(): LocalDate? =
    if (createdAt <= 0L) null else Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
