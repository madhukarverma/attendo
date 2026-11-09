package com.attendo.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.PomodoroRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.data.TodoRepository
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.data.pomodoro.PomodoroTimerState
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.AttendanceSummary
import com.attendo.core.model.CalendarEvent
import com.attendo.core.model.DayKind
import com.attendo.core.model.PomodoroStats
import com.attendo.core.model.PomodoroSummary
import com.attendo.core.model.Todo
import com.attendo.core.model.TodoAgenda
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the Home summary shows, one snapshot of every section. */
data class DashboardUiState(
    val loaded: Boolean = false,
    val nickname: String = "",
    val now: LocalDateTime = LocalDateTime.now(),
    val summaries: List<AttendanceSummary> = emptyList(),
    /** Subjects with a mark (present / absent / no class) today. */
    val markedToday: Int = 0,
    val todayEvents: List<CalendarEvent> = emptyList(),
    val isWeekend: Boolean = false,
    val todayTodos: List<Todo> = emptyList(),
    val overdueCount: Int = 0,
    val reminders: List<Todo> = emptyList(),
    val exams: List<CalendarEvent> = emptyList(),
    val focus: PomodoroSummary? = null,
) {
    val today: LocalDate get() = now.toLocalDate()

    /** Overall attendance across subjects, or null when nothing has been held yet. */
    val overallPercent: Double?
        get() {
            val held = summaries.sumOf { it.held }
            return if (held == 0) null else summaries.sumOf { it.present } * 100.0 / held
        }

    val belowTarget: List<AttendanceSummary> get() = summaries.filterNot { it.isOnTrack }

    /** Lectures left across subjects whose lecture hours are set; null when none are. */
    val lecturesLeft: Int? get() = summaries.mapNotNull { it.lecturesLeft }.takeIf { it.isNotEmpty() }?.sum()

    /** A listed holiday today (weekends are handled by [isWeekend]). */
    val holiday: CalendarEvent? get() = todayEvents.firstOrNull { it.kind == DayKind.HOLIDAY }
}

class DashboardViewModel(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    private val todos: TodoRepository,
    pomodoro: PomodoroRepository,
    private val timer: PomodoroTimer,
    private val calendar: AcademicCalendar,
) : ViewModel() {

    /** Ticks every minute so the day, reminders and countdowns stay current while open. */
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    private val attendanceFlow = combine(subjects.subjects, attendance.records) { list, records ->
        list.map { AttendanceSummary.from(it, records) } to records
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        settings.settings.filterNotNull(),
        attendanceFlow,
        todos.todos,
        pomodoro.sessions,
        clock,
    ) { s, (summaries, records), todoList, sessions, now ->
        val today = now.toLocalDate()
        val agenda = TodoAgenda.group(todoList, today)
        DashboardUiState(
            loaded = true,
            nickname = s.nickname,
            now = now,
            summaries = summaries,
            markedToday = records.filter { it.date == today }.mapTo(HashSet()) { it.subjectId }.size,
            todayEvents = calendar.eventsOn(today),
            isWeekend = calendar.isWeekend(today),
            todayTodos = agenda.today,
            overdueCount = agenda.overdue.size,
            reminders = TodoAgenda.upcomingReminders(todoList, now).take(MAX_ROWS),
            exams = calendar.events.filter { it.kind == DayKind.EXAM && !it.end.isBefore(today) }.take(MAX_ROWS),
            focus = PomodoroStats.summarize(sessions, summaries.map { it.subject }, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** The Pomodoro timer ticks every second, so it's collected on its own. */
    val timerState: StateFlow<PomodoroTimerState> = timer.state

    fun toggleTimer() = timer.toggle()

    fun toggleTodo(todo: Todo) {
        viewModelScope.launch { todos.setDone(todo.id, !todo.done) }
    }

    private companion object {
        const val MAX_ROWS = 3
    }
}
