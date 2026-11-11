package com.attendo.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.PomodoroRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.data.TodoRepository
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.data.pomodoro.PomodoroTimerState
import com.attendo.core.designsystem.component.AnimatedNumber
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.component.PentagonMascot
import com.attendo.core.designsystem.component.TabBarClearance
import com.attendo.core.designsystem.component.TimerGlyph
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.CalendarEvent
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.PomodoroKind
import com.attendo.core.model.Todo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Home: a one-glance summary of every section (attendance, today's to-dos, upcoming reminders,
 * upcoming exams, the Pomodoro timer). Each card opens its section.
 */
@Composable
fun DashboardRoute(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    todos: TodoRepository,
    pomodoro: PomodoroRepository,
    timer: PomodoroTimer,
    calendar: AcademicCalendar,
    onOpenAttendance: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel {
        DashboardViewModel(settings, subjects, attendance, todos, pomodoro, timer, calendar)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val timerState by vm.timerState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        timer = timerState,
        onToggleTimer = vm::toggleTimer,
        onToggleTodo = vm::toggleTodo,
        onOpenAttendance = onOpenAttendance,
        onOpenTodo = onOpenTodo,
        onOpenPomodoro = onOpenPomodoro,
        onOpenCalendar = onOpenCalendar,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    timer: PomodoroTimerState,
    onToggleTimer: () -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        BounceContainer(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header(state, onOpenSettings)
                AttendanceCard(state, onOpenAttendance)
                FocusCard(state, timer, onToggleTimer, onOpenPomodoro)
                TodayTodosCard(state, onToggleTodo, onOpenTodo)
                RemindersCard(state, onOpenTodo)
                ExamsCard(state, onOpenCalendar)
                Spacer(Modifier.height(TabBarClearance))
            }
        }
    }
}

private val LongDate = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val ShortDate = DateTimeFormatter.ofPattern("d MMM")
private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")

private fun greeting(now: LocalDateTime): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hey"
}

@Composable
private fun Header(state: DashboardUiState, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("${greeting(state.now)},", style = MaterialTheme.typography.headlineLarge)
            Text(
                state.nickname.ifBlank { "there" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(state.today.format(LongDate), style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .iosPress(PressStyle.Scale, onClickLabel = "Settings", onClick = onOpenSettings)
                .background(AttendoColors.CreamDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** Big dark card: overall attendance, who's below target, today's marking, Penta's mood. */
@Composable
private fun AttendanceCard(state: DashboardUiState, onOpen: () -> Unit) {
    val inClear = state.belowTarget.isEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.Hero)
            .iosPress(PressStyle.Highlight, onClickLabel = "Open classes", onClick = onOpen)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CardLabel("ATTENDANCE", AttendoColors.Muted)
            val overall = state.overallPercent
            if (overall == null) {
                Text("–", style = MaterialTheme.typography.displayLarge, color = AttendoColors.OnHero)
            } else {
                AnimatedNumber(
                    value = overall.roundToInt(),
                    suffix = "%",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (inClear) AttendoColors.Green else AttendoColors.Pink,
                )
            }
            Text(
                when {
                    state.summaries.isEmpty() -> "Add your subjects to start"
                    inClear -> "All ${state.summaries.size} subjects on track"
                    else -> "Below target: " + state.belowTarget.joinToString(", ") { CourseCatalog.shortName(it.subject) }
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (inClear) AttendoColors.OnHero else AttendoColors.Pink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                todayLine(state),
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.OnHero.copy(alpha = 0.7f),
            )
            state.lecturesLeft?.let { left ->
                Text(
                    "$left lecture${if (left == 1) "" else "s"} left across your courses",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.OnHero.copy(alpha = 0.7f),
                )
            }
        }
        PentagonMascot(Modifier.size(92.dp), mood = if (inClear) Mood.Happy else Mood.Sad)
    }
}

private fun todayLine(state: DashboardUiState): String {
    val holiday = state.holiday
    return when {
        holiday != null -> "Today: holiday (${holiday.title})"
        state.isWeekend -> "Today: weekend, no classes"
        state.summaries.isEmpty() -> "Today: nothing to mark yet"
        else -> "Today: ${state.markedToday} of ${state.summaries.size} marked"
    }
}

/** Pomodoro: the live countdown while a run is in progress, otherwise today's focus and streak. */
@Composable
private fun FocusCard(state: DashboardUiState, timer: PomodoroTimerState, onToggle: () -> Unit, onOpen: () -> Unit) {
    Card(onOpen, "Open Pomodoro timer") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(AttendoColors.Hero),
                contentAlignment = Alignment.Center,
            ) { TimerGlyph(color = AttendoColors.OnHero, size = 20.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                CardLabel("POMODORO", AttendoColors.Muted)
                if (timer.isFresh) {
                    val focus = state.focus
                    Text(
                        "${focus?.todayFocusMinutes ?: 0} min focused today",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        focus?.streakDays?.takeIf { it > 0 }?.let { "$it-day streak · ${focus.weekFocusMinutes} min this week" }
                            ?: "Start a focus session",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                    )
                } else {
                    Text(
                        "${timer.kind.label()} · ${formatClock(timer.remainingMillis)}" + if (timer.running) "" else " (paused)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(timer.progress, if (timer.kind == PomodoroKind.FOCUS) AttendoColors.Green else AttendoColors.Blue)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .iosPress(PressStyle.Scale, onClickLabel = if (timer.running) "Pause" else "Start", onClick = onToggle)
                    .clip(CircleShape)
                    .background(AttendoColors.GreenFill),
                contentAlignment = Alignment.Center,
            ) {
                if (timer.running) PauseGlyph(AttendoColors.OnAccent)
                else Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = AttendoColors.OnAccent)
            }
        }
    }
}

private fun PomodoroKind.label() = when (this) {
    PomodoroKind.FOCUS -> "Focus"
    PomodoroKind.SHORT_BREAK -> "Short break"
    PomodoroKind.LONG_BREAK -> "Long break"
}

private fun formatClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun TodayTodosCard(state: DashboardUiState, onToggle: (Todo) -> Unit, onOpen: () -> Unit) {
    Card(onOpen, "Open to-do") {
        val open = state.todayTodos.count { !it.done }
        CardHeader("TODAY'S TO-DOS", if (state.todayTodos.isEmpty()) null else "$open left")
        if (state.todayTodos.isEmpty()) {
            EmptyLine("Nothing due today. Tap to add a to-do.")
        } else {
            state.todayTodos.take(4).forEach { todo -> TodoLine(todo, onToggle) }
            if (state.todayTodos.size > 4) EmptyLine("+ ${state.todayTodos.size - 4} more")
        }
        if (state.overdueCount > 0) {
            Text(
                "${state.overdueCount} overdue from earlier days",
                style = MaterialTheme.typography.labelMedium,
                color = AttendoColors.Pink,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun TodoLine(todo: Todo, onToggle: (Todo) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .iosPress(PressStyle.Highlight, onClickLabel = if (todo.done) "Mark not done" else "Mark done") { onToggle(todo) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(if (todo.done) Modifier.background(AttendoColors.Green) else Modifier.border(2.dp, AttendoColors.Green, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (todo.done) Icon(Icons.Filled.Check, contentDescription = null, tint = AttendoColors.Cream, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.done) AttendoColors.Muted else AttendoColors.Ink,
            textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        todo.remindAt?.let {
            Text(it.format(TimeFormat), style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
    }
}

@Composable
private fun RemindersCard(state: DashboardUiState, onOpen: () -> Unit) {
    Card(onOpen, "Open reminders") {
        CardHeader("UPCOMING REMINDERS", null)
        if (state.reminders.isEmpty()) {
            EmptyLine("No reminders set. Give a to-do a time to get one.")
        }
        state.reminders.forEach { todo ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = AttendoColors.Green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${relativeDay(todo.date, state.today)} · ${todo.remindAt?.format(TimeFormat).orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun ExamsCard(state: DashboardUiState, onOpen: () -> Unit) {
    Card(onOpen, "Open academic calendar") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeader("UPCOMING EXAMS", null, Modifier.weight(1f))
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(16.dp))
        }
        if (state.exams.isEmpty()) {
            EmptyLine("No more exams on the academic calendar.")
        }
        state.exams.forEach { exam -> ExamLine(exam, state.today) }
    }
}

@Composable
private fun ExamLine(exam: CalendarEvent, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(AttendoColors.Blue))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(exam.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (exam.start == exam.end) exam.start.format(ShortDate) else "${exam.start.format(ShortDate)} – ${exam.end.format(ShortDate)}",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
            )
        }
        val days = ChronoUnit.DAYS.between(today, exam.start)
        Text(
            when {
                today in exam -> "Now"
                days == 1L -> "Tomorrow"
                else -> "in $days days"
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (days <= 3) AttendoColors.Pink else AttendoColors.Blue,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(ShortDate)
}

@Composable
private fun Card(onClick: () -> Unit, label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard)
            .iosPress(PressStyle.Highlight, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun CardLabel(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, letterSpacing = 1.sp)
}

@Composable
private fun CardHeader(title: String, trailing: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = AttendoColors.Muted,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = AttendoColors.Green)
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(AttendoColors.CreamDeep),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Two bars (material-icons-core has no pause icon). */
@Composable
private fun PauseGlyph(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { Box(Modifier.width(4.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(color)) }
    }
}
