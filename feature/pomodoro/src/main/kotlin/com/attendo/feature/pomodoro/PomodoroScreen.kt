package com.attendo.feature.pomodoro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.PomodoroRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.data.pomodoro.PomodoroTimerState
import com.attendo.core.designsystem.component.AnimatedNumber
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.model.CourseCatalog
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.IosMotion
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.PomodoroKind
import com.attendo.core.model.PomodoroSummary
import com.attendo.core.model.Subject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

@Composable
fun PomodoroRoute(
    timer: PomodoroTimer,
    pomodoro: PomodoroRepository,
    subjects: SubjectRepository,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
) {
    val vm: PomodoroViewModel = viewModel { PomodoroViewModel(timer, pomodoro, subjects) }
    val state by vm.timerState.collectAsStateWithLifecycle()
    val subjectList by vm.subjects.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(timer) { timer.finished.collect { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } }

    // Keep the display on while a run is in progress.
    val view = LocalView.current
    DisposableEffect(state.running) {
        view.keepScreenOn = state.running
        onDispose { view.keepScreenOn = false }
    }

    PomodoroScreen(
        state = state,
        subjects = subjectList,
        stats = stats,
        onToggle = timer::toggle,
        onReset = timer::reset,
        onSkip = timer::skip,
        onSelectSubject = timer::selectSubject,
        onFocusMinutes = timer::setFocusMinutes,
        onExportCsv = onExportCsv,
        onClose = onClose,
    )
}

@Composable
fun PomodoroScreen(
    state: PomodoroTimerState,
    subjects: List<Subject>,
    stats: PomodoroStatsUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectSubject: (String?) -> Unit,
    onFocusMinutes: (Int) -> Unit,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val collapse by rememberCollapseFraction(scroll)

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = "Focus", collapseFraction = { collapse }, onBack = onClose, backLabel = "Home")
        BounceContainer(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp),
            ) {
                LargeTitle("Focus")
                SegmentedControl(listOf("Timer", "Stats"), tab, { tab = it }, Modifier.padding(vertical = 8.dp))
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(IosMotion.push()) { it / 4 * dir } + fadeIn(IosMotion.push()))
                            .togetherWith(slideOutHorizontally(IosMotion.push()) { -it / 4 * dir } + fadeOut(IosMotion.push()))
                    },
                    label = "tab",
                ) { page ->
                    if (page == 0) {
                        TimerTab(state, subjects, onToggle, onReset, onSkip, onSelectSubject, onFocusMinutes)
                    } else {
                        StatsTab(stats, onExportCsv)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** iOS-style segmented control with a sliding thumb. */
@Composable
private fun SegmentedControl(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AttendoColors.CreamDeep)
            .padding(3.dp),
    ) {
        val segment = maxWidth / items.size
        val x by animateDpAsState(segment * selected, IosMotion.bouncy(), label = "thumb")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(AttendoColors.Ink),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) AttendoColors.Cream else AttendoColors.Ink, label = "seg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .iosPress(PressStyle.Dim, onClickLabel = label) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun PomodoroKind.label() = when (this) {
    PomodoroKind.FOCUS -> "FOCUS"
    PomodoroKind.SHORT_BREAK -> "SHORT BREAK"
    PomodoroKind.LONG_BREAK -> "LONG BREAK"
}

private fun PomodoroKind.color() = if (this == PomodoroKind.FOCUS) AttendoColors.Green else AttendoColors.Blue

private fun formatClock(millis: Long): String {
    val total = ceil(millis / 1000.0).toLong()
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
private fun TimerTab(
    state: PomodoroTimerState,
    subjects: List<Subject>,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectSubject: (String?) -> Unit,
    onFocusMinutes: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        TimerRing(state, Modifier.size(260.dp))
        CycleDots(state)
        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = when {
                state.running -> "Pause"
                state.isFresh -> "Start ${state.kind.label().lowercase().replaceFirstChar { it.uppercase() }}"
                else -> "Resume"
            },
            onClick = onToggle,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onReset, enabled = !state.isFresh) { Text("Reset", color = AttendoColors.Pink) }
            TextButton(onClick = onSkip) { Text("Skip", color = AttendoColors.Ink) }
        }

        SectionLabel("Studying")
        ChipRow {
            Chip("General", state.subjectId == null) { onSelectSubject(null) }
            // Short names (DSA, DLCO, Python, OS, Maths); labs aren't study sessions, so they're left out.
            subjects.filterNot { CourseCatalog.isLab(it) }.forEach { s ->
                Chip(CourseCatalog.shortName(s), state.subjectId == s.id) { onSelectSubject(s.id) }
            }
        }

        SectionLabel("Focus length")
        ChipRow {
            listOf(15, 25, 45, 60).forEach { m ->
                Chip("$m min", state.config.focusMinutes == m, enabled = !state.running) { onFocusMinutes(m) }
            }
        }
    }
}

@Composable
private fun TimerRing(state: PomodoroTimerState, modifier: Modifier) {
    val color by animateColorAsState(state.kind.color(), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(AttendoColors.CreamDeep, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color,
                -90f,
                360f * (1f - state.progress),
                false,
                Offset(inset, inset),
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.kind.label(), style = MaterialTheme.typography.labelMedium, color = color, letterSpacing = 2.sp)
            Text(
                formatClock(state.remainingMillis),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CycleDots(state: PomodoroTimerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(state.config.longBreakEvery) { i ->
            val filled = i < state.focusDoneInCycle
            val color by animateColorAsState(if (filled) AttendoColors.Green else AttendoColors.CreamDeep, label = "dot")
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = AttendoColors.Muted,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) AttendoColors.Ink else AttendoColors.CreamCard, IosMotion.bouncy(), label = "chip")
    val fg by animateColorAsState(if (selected) AttendoColors.Cream else AttendoColors.Ink, label = "chipFg")
    Box(
        Modifier
            .iosPress(PressStyle.Scale, enabled = enabled, onClickLabel = label, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, color = if (enabled || selected) fg else AttendoColors.Muted, style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------- Stats tab ----------------

private val TimeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun StatsTab(stats: PomodoroStatsUiState, onExportCsv: () -> Unit) {
    val summary = stats.summary
    Column {
        Spacer(Modifier.height(12.dp))
        if (summary == null || summary.totalFocusMinutes == 0 && stats.recent.isEmpty()) {
            Text(
                "No focus sessions yet. Finish a Pomodoro and your stats will show up here.",
                color = AttendoColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        if (summary != null) {
            OverviewCard(summary)
            SectionLabel("Last 7 days (minutes)")
            WeekChart(summary)
            if (summary.bySubject.isNotEmpty()) {
                SectionLabel("By subject")
                val max = summary.bySubject.maxOf { it.second }.coerceAtLeast(1)
                summary.bySubject.forEach { (name, minutes) -> SubjectFocusBar(name, minutes, minutes / max.toFloat()) }
            }
        }
        if (stats.recent.isNotEmpty()) {
            SectionLabel("Recent sessions")
            stats.recent.forEach { (s, name) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (s.kind == PomodoroKind.FOCUS) name else s.kind.label().lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).format(TimeFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = AttendoColors.Muted,
                        )
                    }
                    Text(
                        "${s.actualSeconds / 60} min " + if (s.completed) "✓" else "·  stopped",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (s.completed) s.kind.color() else AttendoColors.Muted,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Export pomodoro.csv", onClick = onExportCsv)
    }
}

@Composable
private fun OverviewCard(summary: PomodoroSummary) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.Ink)
            .padding(24.dp),
    ) {
        Text("FOCUSED TODAY", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedNumber(summary.todayFocusMinutes, "", MaterialTheme.typography.displayLarge, AttendoColors.Green)
            Text(" min", style = MaterialTheme.typography.titleMedium, color = AttendoColors.Green, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("This week", "${summary.weekFocusMinutes}m")
            Stat("All time", "${summary.totalFocusMinutes / 60}h ${summary.totalFocusMinutes % 60}m")
            Stat("Sessions", summary.completedFocusSessions.toString())
            Stat("Streak", "${summary.streakDays}d")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, color = AttendoColors.Cream)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

@Composable
private fun WeekChart(summary: PomodoroSummary) {
    val max = summary.last7Days.maxOf { it.second }.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        summary.last7Days.forEachIndexed { i, (day, minutes) ->
            val target = minutes / max.toFloat()
            val anim = remember { Animatable(0f) }
            LaunchedEffect(target) { anim.animateTo(target, IosMotion.settle()) }
            val isToday = i == summary.last7Days.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(if (minutes > 0) "$minutes" else "", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                Box(Modifier.height(86.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .fillMaxHeight(anim.value.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isToday) AttendoColors.Green else AttendoColors.Ink.copy(alpha = 0.75f)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SubjectFocusBar(name: String, minutes: Int, fraction: Float) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction, IosMotion.settle()) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text("$minutes min", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(AttendoColors.CreamDeep),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(anim.value)
                    .clip(CircleShape)
                    .background(AttendoColors.Green),
            )
        }
    }
}
