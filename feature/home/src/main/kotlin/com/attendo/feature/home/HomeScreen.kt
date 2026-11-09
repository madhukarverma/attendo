package com.attendo.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.attendo.core.designsystem.component.AnimatedNumber
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.component.BarIconButton
import com.attendo.core.designsystem.component.DayStrip
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.component.PentagonMascot
import com.attendo.core.designsystem.component.PillBottomBar
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.StatsGlyph
import com.attendo.core.designsystem.component.TimerGlyph
import com.attendo.core.designsystem.component.StatusLabel
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.AttendanceSummary
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.DayKind
import com.attendo.core.designsystem.component.AttendoCard
import com.attendo.core.model.Subject
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    onAddSubject: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPomodoro: () -> Unit,
    calendar: AcademicCalendar = AcademicCalendar.EMPTY,
    onOpenCalendar: (LocalDate) -> Unit = {},
    /** A date picked on the calendar screen; selected once, then [onRequestedDateHandled] clears it. */
    requestedDate: LocalDate? = null,
    onRequestedDateHandled: () -> Unit = {},
) {
    val vm: HomeViewModel = viewModel { HomeViewModel(settings, subjects, attendance, calendar) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(requestedDate) {
        if (requestedDate != null) {
            vm.selectDate(requestedDate)
            onRequestedDateHandled()
        }
    }
    HomeScreen(
        state = state,
        onSelectDate = vm::selectDate,
        onMark = vm::mark,
        onAddCourseSubjects = vm::addCourseSubjects,
        onOpenPomodoro = onOpenPomodoro,
        onTinderModeChange = vm::setTinderMode,
        onSwipe = vm::swipe,
        onUndoSwipe = vm::undoSwipe,
        onOpenCalendar = onOpenCalendar,
        onAddSubject = onAddSubject,
        onOpenSubject = onOpenSubject,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onMark: (subjectId: String, status: AttendanceStatus) -> Unit,
    onAddSubject: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddCourseSubjects: () -> Unit = {},
    onOpenPomodoro: () -> Unit = {},
    onTinderModeChange: (Boolean) -> Unit = {},
    onSwipe: (subjectId: String, status: AttendanceStatus) -> Unit = { _, _ -> },
    onUndoSwipe: () -> Unit = {},
    onOpenCalendar: (LocalDate) -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        BounceContainer(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Greeting(state.userName, state.selectedDate) { onOpenCalendar(state.selectedDate) } }
            item {
                DayStrip(
                    days = state.days,
                    selected = state.selectedDate,
                    markedDays = state.markedDays,
                    onSelect = onSelectDate,
                    dots = state.dayKinds.mapValues { (_, kind) -> kind.dotColor() },
                    isOffDay = state.calendar::isWeekend,
                )
            }
            // In Tinder mode the mascot steps aside so the card stack sits in the middle of the screen.
            if (!(state.tinderMode && state.items.isNotEmpty())) {
                item(key = "mascot") {
                    val (message, mood) = mascotMessage(state)
                    MascotHeader(message = message, mood = mood, modifier = Modifier.animateItem())
                }
            }
            if (state.items.isEmpty()) {
                item(key = "course") {
                    AttendoCard(
                        modifier = Modifier.animateItem().padding(horizontal = 20.dp),
                        onClick = onAddCourseSubjects,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Add my semester subjects", style = MaterialTheme.typography.titleMedium)
                            Text(
                                CourseCatalog.CURRENT_SEMESTER.joinToString(" · ") { it.code },
                                style = MaterialTheme.typography.labelSmall,
                                color = AttendoColors.Muted,
                            )
                        }
                        Text("+ ${CourseCatalog.CURRENT_SEMESTER.size}", color = AttendoColors.Green, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (state.items.isNotEmpty()) {
                item(key = "tinderSwitch") {
                    TinderModeSwitch(state.tinderMode, onTinderModeChange, Modifier.animateItem().padding(horizontal = 20.dp))
                }
            }
            if (state.tinderMode && state.items.isNotEmpty()) {
                item(key = "deck") {
                    SwipeDeck(
                        cards = state.unmarked,
                        totalToday = state.items.size,
                        canUndo = state.canUndoSwipe,
                        onSwipe = onSwipe,
                        onUndo = onUndoSwipe,
                        colorFor = { id -> cardColor(state.items.indexOfFirst { it.summary.subject.id == id }) },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                items(state.items, key = { it.summary.subject.id }) { item ->
                    SubjectDayRow(
                        item = item,
                        onMark = { status -> onMark(item.summary.subject.id, status) },
                        onClick = { onOpenSubject(item.summary.subject.id) },
                        modifier = Modifier.animateItem().padding(horizontal = 20.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        }

        PillBottomBar(
            primaryLabel = "New subject",
            onPrimary = onAddSubject,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BarIconButton(onClick = onOpenPomodoro, contentDescription = "Pomodoro timer") { TimerGlyph() }
            BarIconButton(onClick = onOpenStats, contentDescription = "Statistics") { StatsGlyph() }
            BarIconButton(onClick = onOpenSettings, contentDescription = "Settings") {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = AttendoColors.OnHero)
            }
        }
    }
}

private fun mascotMessage(state: HomeUiState): Pair<String, Mood> {
    val overall = state.overallPercent
    // 1. A significant day in the academic calendar always wins.
    val event = state.selectedDayEvents.firstOrNull()
    val calendarLine = state.calendar.mascotLine(state.selectedDate)
    if (event != null && calendarLine != null) {
        val mood = when (event.kind) {
            DayKind.HOLIDAY, DayKind.EVENT, DayKind.CLASSES_START -> Mood.Happy
            DayKind.EXAM, DayKind.DEADLINE, DayKind.CLASSES_END -> Mood.Neutral
        }
        return calendarLine to mood
    }
    // 2. Attendance warnings; 3. upcoming calendar heads-up; 4. defaults.
    if (state.items.isNotEmpty() && state.subjectsBelowTarget > 0) {
        return "${state.subjectsBelowTarget} subject(s) below target. Time to show up!" to Mood.Sad
    }
    if (calendarLine != null) return calendarLine to Mood.Happy
    return when {
        state.items.isEmpty() -> "Add your subjects and start tracking your attendance!" to Mood.Happy
        overall == null -> "Mark today's classes below. Tap again to undo." to Mood.Happy
        state.subjectsBelowTarget > 0 ->
            "${state.subjectsBelowTarget} subject(s) below target. Time to show up!" to Mood.Sad
        else -> "You're at ${overall.roundToInt()}% overall. Good job!" to Mood.Happy
    }
}

private val HeaderDate = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")

@Composable
internal fun Greeting(name: String, selectedDate: LocalDate, onOpenCalendar: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append("Hey, ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name.ifBlank { "there" }) }
                append("!")
            },
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        // Academic calendar button, then the selected date (top right).
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .iosPress(PressStyle.Scale, onClickLabel = "Open academic calendar", onClick = onOpenCalendar)
                .background(AttendoColors.CreamDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            selectedDate.format(HeaderDate),
            style = MaterialTheme.typography.labelLarge,
            color = if (selectedDate == LocalDate.now()) AttendoColors.Cream else AttendoColors.Ink,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selectedDate == LocalDate.now()) AttendoColors.Green else AttendoColors.CreamDeep)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun MascotHeader(message: String, mood: Mood, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SpeechBubble(text = message, tail = TailSide.BottomStart, modifier = Modifier.width(230.dp))
        }
        PentagonMascot(Modifier.size(170.dp), mood = mood)
    }
}

/** Day-strip dot colour: holidays pink, exams blue, fests/semester milestones green, deadlines yellow. */
private fun DayKind.dotColor(): Color = when (this) {
    DayKind.HOLIDAY -> AttendoColors.Pink
    DayKind.EXAM -> AttendoColors.Blue
    DayKind.EVENT, DayKind.CLASSES_START, DayKind.CLASSES_END -> AttendoColors.Green
    DayKind.DEADLINE -> AttendoColors.Yellow
}

@Composable
private fun TinderModeSwitch(enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    AttendoCard(modifier = modifier, onClick = { onChange(!enabled) }) {
        Column(Modifier.weight(1f)) {
            Text("Tinder mode", style = MaterialTheme.typography.titleMedium)
            Text(
                "Swipe right present · left absent · down no class",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AttendoColors.Pink,
                checkedThumbColor = AttendoColors.Cream,
                checkedBorderColor = AttendoColors.Pink,
                uncheckedTrackColor = AttendoColors.CreamDeep,
                uncheckedThumbColor = AttendoColors.Cream,
                uncheckedBorderColor = AttendoColors.CreamDeep,
            ),
        )
    }
}

/** Rounded square with the subject's initials, standing in for the row icon in the design. */
@Composable
internal fun SubjectBadge(subject: Subject) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AttendoColors.CreamDeep),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            subject.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun AttendanceStatus?.label(): Triple<String, Color, String> = when (this) {
    AttendanceStatus.PRESENT -> Triple("Present", AttendoColors.Green, "✓")
    AttendanceStatus.ABSENT -> Triple("Absent", AttendoColors.Pink, "✕")
    AttendanceStatus.CANCELLED -> Triple("Cancelled", AttendoColors.Blue, "↷")
    null -> Triple("Not marked", AttendoColors.Muted, "✱")
}

@Composable
internal fun SubjectDayRow(
    item: SubjectDayItem,
    onMark: (AttendanceStatus) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subject = item.summary.subject
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .iosPress(PressStyle.Highlight, onClickLabel = "Open ${subject.name}", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(subject)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.statusOnDay == AttendanceStatus.CANCELLED) AttendoColors.Muted else AttendoColors.Ink,
                    textDecoration = if (item.statusOnDay == AttendanceStatus.CANCELLED) TextDecoration.LineThrough else null,
                )
                val (text, color, symbol) = item.statusOnDay.label()
                StatusLabel(text = text, color = color, symbol = symbol)
            }
            Box(Modifier.width(1.dp).height(40.dp).background(AttendoColors.CreamDeep))
            PercentColumn(item.summary)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarkChip("Present", AttendoColors.Green, item.statusOnDay == AttendanceStatus.PRESENT, Modifier.weight(1f)) {
                onMark(AttendanceStatus.PRESENT)
            }
            MarkChip("Absent", AttendoColors.Pink, item.statusOnDay == AttendanceStatus.ABSENT, Modifier.weight(1f)) {
                onMark(AttendanceStatus.ABSENT)
            }
            MarkChip("No class", AttendoColors.Blue, item.statusOnDay == AttendanceStatus.CANCELLED, Modifier.weight(1f)) {
                onMark(AttendanceStatus.CANCELLED)
            }
        }
    }
}

@Composable
private fun PercentColumn(summary: AttendanceSummary) {
    Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val color by animateColorAsState(if (summary.isOnTrack) AttendoColors.Ink else AttendoColors.Pink, label = "pct")
        if (summary.held == 0) {
            Text("–", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            AnimatedNumber(
                value = summary.percent.roundToInt(),
                suffix = "%",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "${summary.present}/${summary.held}",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
        )
    }
}

@Composable
private fun MarkChip(text: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        if (selected) color else AttendoColors.Cream,
        animationSpec = Motion.bouncy(),
        label = "chipBg",
    )
    val content by animateColorAsState(if (selected) AttendoColors.Cream else color, label = "chipFg")
    Box(
        modifier
            .iosPress(PressStyle.Scale, onClickLabel = "Mark $text") {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .clip(CircleShape)
            .background(background)
            .padding(vertical = 9.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Preview
@Composable
private fun HomePreview() {
    val subject = Subject("1", "Data Structures", "CS201")
    AttendoTheme {
        HomeScreen(
            HomeUiState(
                userName = "Madhukar",
                items = listOf(SubjectDayItem(AttendanceSummary(subject, 8, 2), AttendanceStatus.PRESENT)),
            ),
            {}, { _, _ -> }, {}, {}, {}, {},
        )
    }
}
