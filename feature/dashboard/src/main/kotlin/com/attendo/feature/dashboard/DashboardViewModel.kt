package com.attendo.feature.dashboard

import com.attendo.core.data.PlannerRepository
import com.attendo.core.model.AttendanceRecord
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.Timetable
import com.attendo.core.model.TimetableSlot
import java.time.LocalTime
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.HabitRepository
import com.attendo.core.data.KanbanRepository
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
import com.attendo.core.model.Habit
import com.attendo.core.model.HabitCheck
import com.attendo.core.model.HabitWeek
import com.attendo.core.model.KanbanBoard
import com.attendo.core.model.KanbanCard
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

/** The class happening now or next today, from the timetable. */
data class NextClass(val subject: String, val start: LocalTime, val end: LocalTime, val room: String, val inProgress: Boolean)

/** One line of today's checklist on Home. */
data class ChecklistItem(val habit: Habit, val checked: Boolean)

/** The board at a glance: cards per column, and the open cards due soonest. */
data class BoardSummary(
    val columns: List<Pair<String, Int>>,
    val openCards: Int,
    val due: List<KanbanCard>,
    val overdueIds: Set<String>,
)

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
    /** Checklist tasks scheduled for today. */
    val checklist: List<ChecklistItem> = emptyList(),
    val board: BoardSummary? = null,
    val reminders: List<Todo> = emptyList(),
    val exams: List<CalendarEvent> = emptyList(),
    val focus: PomodoroSummary? = null,
    val nextClass: NextClass? = null,
    /** Classes on today's timetable (0 without a timetable). */
    val classesToday: Int = 0,
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
    todos: TodoRepository,
    private val habits: HabitRepository,
    kanban: KanbanRepository,
    pomodoro: PomodoroRepository,
    private val timer: PomodoroTimer,
    private val calendar: AcademicCalendar,
    planner: PlannerRepository? = null,
) : ViewModel() {

    private val plan = combine(
        planner?.calendar(calendar) ?: flowOf(calendar),
        planner?.slots ?: flowOf(emptyList()),
    ) { cal, slots -> cal to slots }

    /** Ticks every minute so the day, reminders and countdowns stay current while open. */
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    private class AttendanceData(val summaries: List<AttendanceSummary>, val records: List<AttendanceRecord>, val calendar: AcademicCalendar, val slots: List<TimetableSlot>)

    private val attendanceFlow = combine(subjects.subjects, attendance.records, plan) { list, records, (cal, slots) ->
        AttendanceData(list.map { AttendanceSummary.from(it, records) }, records, cal, slots)
    }

    private class Tasks(val reminders: List<Todo>, val habits: List<Habit>, val checks: List<HabitCheck>, val board: KanbanBoard)

    private val tasksFlow = combine(todos.todos, habits.habits, habits.checks, kanban.board, ::Tasks)

    val uiState: StateFlow<DashboardUiState> = combine(
        settings.settings.filterNotNull(),
        attendanceFlow,
        tasksFlow,
        pomodoro.sessions,
        clock,
    ) { s, att, tasks, sessions, now ->
        val today = now.toLocalDate()
        val summaries = att.summaries
        val records = att.records
        val calendar = att.calendar
        val names = summaries.associate { it.subject.id to CourseCatalog.shortName(it.subject) }
        val next = Timetable.next(att.slots, now)
        val ticked = HabitWeek.checkedDays(tasks.checks)
        val board = tasks.board
        DashboardUiState(
            loaded = true,
            nickname = s.nickname,
            now = now,
            summaries = summaries,
            markedToday = records.filter { it.date == today }.mapTo(HashSet()) { it.subjectId }.size,
            todayEvents = calendar.eventsOn(today),
            isWeekend = calendar.isWeekend(today),
            checklist = HabitWeek.dueOn(tasks.habits, today).map { ChecklistItem(it, today in ticked[it.id].orEmpty()) },
            board = if (board.columns.isEmpty()) {
                null
            } else {
                val due = board.dueSoon().take(MAX_ROWS)
                BoardSummary(
                    columns = board.sortedColumns.map { it.title to board.cardsIn(it.id).size },
                    openCards = board.openCards.size,
                    due = due,
                    overdueIds = due.filter { board.isOverdue(it, today) }.mapTo(HashSet()) { it.id },
                )
            },
            reminders = TodoAgenda.upcomingReminders(tasks.reminders, now).take(MAX_ROWS),
            exams = calendar.events.filter { it.kind == DayKind.EXAM && !it.end.isBefore(today) }.take(MAX_ROWS),
            focus = PomodoroStats.summarize(sessions, summaries.map { it.subject }, today),
            nextClass = next?.let {
                NextClass(names[it.subjectId] ?: "Class", it.start, it.end, it.room, inProgress = now.toLocalTime() >= it.start)
            },
            classesToday = Timetable.on(att.slots, today).size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** The Pomodoro timer ticks every second, so it's collected on its own. */
    val timerState: StateFlow<PomodoroTimerState> = timer.state

    fun toggleTimer() = timer.toggle()

    /** Ticks or unticks one of today's checklist tasks. */
    fun toggleChecklist(item: ChecklistItem) {
        viewModelScope.launch { habits.setChecked(item.habit.id, LocalDate.now(), !item.checked) }
    }

    private companion object {
        const val MAX_ROWS = 3
    }
}
