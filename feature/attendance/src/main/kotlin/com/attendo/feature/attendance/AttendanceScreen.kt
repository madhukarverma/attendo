package com.attendo.feature.attendance

import com.attendo.core.designsystem.component.TabBarClearance
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.component.DayStrip
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.component.PentagonMascot
import com.attendo.core.designsystem.component.SpeechBubble
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
fun AttendanceRoute(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    onAddSubject: () -> Unit,
    onOpenSubject: (String) -> Unit,
    calendar: AcademicCalendar = AcademicCalendar.EMPTY,
    onOpenCalendar: (LocalDate) -> Unit = {},
    /** A date picked on the calendar screen; selected once, then [onRequestedDateHandled] clears it. */
    requestedDate: LocalDate? = null,
    onRequestedDateHandled: () -> Unit = {},
) {
    val vm: AttendanceViewModel = viewModel { AttendanceViewModel(settings, subjects, attendance, calendar) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(requestedDate) {
        if (requestedDate != null) {
            vm.selectDate(requestedDate)
            onRequestedDateHandled()
        }
    }
    AttendanceScreen(
        state = state,
        onSelectDate = vm::selectDate,
        onMark = vm::mark,
        onAddCourseSubjects = vm::addCourseSubjects,
        onTinderModeChange = vm::setTinderMode,
        onSwipe = vm::swipe,
        onUndoSwipe = vm::undoSwipe,
        onOpenCalendar = onOpenCalendar,
        onAddSubject = onAddSubject,
        onOpenSubject = onOpenSubject,
    )
}

@Composable
fun AttendanceScreen(
    state: AttendanceUiState,
    onSelectDate: (LocalDate) -> Unit,
    onMark: (subjectId: String, status: AttendanceStatus) -> Unit,
    onAddSubject: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onAddCourseSubjects: () -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Header(state.selectedDate) { onOpenCalendar(state.selectedDate) } }
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
                    val (messages, mood) = pentaSays(state)
                    MascotHeader(messages = messages, mood = mood, modifier = Modifier.animateItem())
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
                // "New subject" used to be the bar's main button; it now lives under the list.
                item(key = "addSubject") {
                    AttendoCard(
                        modifier = Modifier.animateItem().padding(horizontal = 20.dp),
                        onClick = onAddSubject,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = AttendoColors.Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Add a subject", style = MaterialTheme.typography.titleMedium, color = AttendoColors.Green)
                    }
                }
            }
            // Room for the app's tab bar, which floats over the bottom of every tab.
            item { Spacer(Modifier.height(TabBarClearance)) }
        }
        }
    }
}

/**
 * What Penta (the pentagon mascot) feels and says.
 *
 * Mood never depends on the day: Penta is happy only when the student is "in the clear", i.e. every
 * subject is at or above its target (75% by default), and sad otherwise.
 *
 * Bubble: in the clear it shows one steady line (the day's significance, else a friendly default).
 * Not in the clear, it alternates between the day's significance and the attendance warning.
 */
private fun pentaSays(state: AttendanceUiState): Pair<List<String>, Mood> {
    val dayLine = state.calendar.mascotLine(state.selectedDate)
    val inClear = state.subjectsBelowTarget == 0
    if (!inClear) {
        val warning = "${state.subjectsBelowTarget} subject(s) below target. Time to show up!"
        return listOfNotNull(dayLine, warning) to Mood.Sad
    }
    val overall = state.overallPercent
    val default = when {
        state.items.isEmpty() -> "Add your subjects and start tracking your attendance!"
        overall == null -> "Mark today's classes below. Tap again to undo."
        else -> "You're at ${overall.roundToInt()}% overall. Good job!"
    }
    return listOf(dayLine ?: default) to Mood.Happy
}

/** Seconds each bubble line stays up when Penta alternates between messages. */
private const val BUBBLE_PHASE_MILLIS = 4_000L

/** Header top padding; tightened slightly in v0.14 (was 24.dp: change back here if preferred). */
private val HeaderTopPadding = 14.dp

private val HeaderDate = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")

/** "Classes" title (the greeting moved to Home in v0.19), the calendar button and the selected date. */
@Composable
internal fun Header(selectedDate: LocalDate, onOpenCalendar: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = HeaderTopPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Classes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
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
            color = if (selectedDate == LocalDate.now()) AttendoColors.OnAccent else AttendoColors.Ink,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selectedDate == LocalDate.now()) AttendoColors.GreenFill else AttendoColors.CreamDeep)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun MascotHeader(messages: List<String>, mood: Mood, modifier: Modifier = Modifier) {
    // Cycle through the lines when there's more than one ("phase change"); a single line stays put.
    var phase by remember(messages) { mutableIntStateOf(0) }
    LaunchedEffect(messages) {
        while (messages.size > 1) {
            delay(BUBBLE_PHASE_MILLIS)
            phase++
        }
    }
    val message = messages.getOrElse(phase % messages.size.coerceAtLeast(1)) { "" }
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AnimatedContent(
                targetState = message,
                transitionSpec = { fadeIn(Motion.settle()) togetherWith fadeOut(Motion.settle()) },
                label = "bubble",
            ) { text ->
                SpeechBubble(text = text, tail = TailSide.BottomStart, modifier = Modifier.width(230.dp))
            }
        }
        // Penta, slightly smaller since v0.14 (was 170.dp).
        PentagonMascot(Modifier.size(156.dp), mood = mood)
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
            MarkChip("Present", AttendoColors.Green, item.statusOnDay == AttendanceStatus.PRESENT, Modifier.weight(1f), AttendoColors.GreenFill) {
                onMark(AttendanceStatus.PRESENT)
            }
            MarkChip("Absent", AttendoColors.Pink, item.statusOnDay == AttendanceStatus.ABSENT, Modifier.weight(1f), AttendoColors.PinkFill) {
                onMark(AttendanceStatus.ABSENT)
            }
            MarkChip("No class", AttendoColors.Blue, item.statusOnDay == AttendanceStatus.CANCELLED, Modifier.weight(1f), AttendoColors.BlueFill) {
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
        // Lectures left in the course, once its lecture hours are set (1 lecture = 1 hour).
        summary.lecturesLeft?.let { left ->
            Text("$left left", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
    }
}

@Composable
private fun MarkChip(
    text: String,
    /** Label colour while unselected. */
    color: Color,
    selected: Boolean,
    modifier: Modifier,
    /** Chip colour while selected. */
    fill: Color = color,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        if (selected) fill else AttendoColors.Cream,
        animationSpec = Motion.bouncy(),
        label = "chipBg",
    )
    val content by animateColorAsState(if (selected) AttendoColors.on(fill) else color, label = "chipFg")
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
private fun AttendancePreview() {
    val subject = Subject("1", "Data Structures", "CS201")
    AttendoTheme {
        AttendanceScreen(
            AttendanceUiState(
                userName = "Madhukar",
                items = listOf(SubjectDayItem(AttendanceSummary(subject, 8, 2), AttendanceStatus.PRESENT)),
            ),
            {}, { _, _ -> }, {}, {},
        )
    }
}
