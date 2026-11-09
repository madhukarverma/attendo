package com.attendo.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.CalendarEvent
import com.attendo.core.model.DayKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Colour used for each kind of calendar entry (shared with the home day-strip dots). */
fun DayKind.color(): Color = when (this) {
    DayKind.HOLIDAY -> AttendoColors.Pink
    DayKind.EXAM -> AttendoColors.Blue
    DayKind.EVENT, DayKind.CLASSES_START, DayKind.CLASSES_END -> AttendoColors.Green
    DayKind.DEADLINE -> AttendoColors.Yellow
}

private fun DayKind.label(): String = when (this) {
    DayKind.HOLIDAY -> "Holiday"
    DayKind.EXAM -> "Assessment"
    DayKind.EVENT -> "Event"
    DayKind.CLASSES_START -> "Classes begin"
    DayKind.CLASSES_END -> "Classes end"
    DayKind.DEADLINE -> "Deadline"
}

/**
 * The academic calendar as month cards: colour-coded days (holidays, weekends, assessments, events,
 * deadlines), a semester summary, what's coming up next, and every entry listed per month.
 * Tapping a day selects it on Home.
 */
@Composable
fun CalendarScreen(
    calendar: AcademicCalendar,
    selected: LocalDate,
    onPickDate: (LocalDate) -> Unit,
    onClose: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val months = remember(calendar) { monthsOf(calendar, today) }
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    // Open on the month of the selected day (header items come first).
    LaunchedEffect(Unit) {
        val idx = months.indexOf(YearMonth.from(selected))
        if (idx > 0) listState.scrollToItem(HEADER_ITEMS + idx)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = "Academic Calendar", collapseFraction = { collapse }, onBack = onClose, backLabel = "Home")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { LargeTitle("Calendar") }
                item { SemesterCard(calendar, today) }
                item { Legend() }
                item { ComingUp(calendar, today) }
                items(months, key = { it.toString() }) { month ->
                    MonthCard(month, calendar, today, selected, onPickDate)
                }
                item {
                    Text(
                        "Saturdays and Sundays are holidays. Source: NIT Trichy academic calendar, July 2026 session.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                    )
                }
            }
        }
    }
}

private const val HEADER_ITEMS = 4

private fun monthsOf(calendar: AcademicCalendar, today: LocalDate): List<YearMonth> {
    val first = calendar.events.minOfOrNull { it.start } ?: today
    val last = calendar.events.maxOfOrNull { it.end } ?: today
    return generateSequence(YearMonth.from(first)) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(YearMonth.from(last)) }
        .toList()
}

private val Short = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun SemesterCard(calendar: AcademicCalendar, today: LocalDate) {
    val start = calendar.classesStart
    val end = calendar.classesEnd
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.Ink)
            .padding(22.dp),
    ) {
        Text("JULY 2026 SESSION", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
        Spacer(Modifier.height(6.dp))
        if (start != null && end != null) {
            Text(
                "${start.format(Short)} – ${end.format(Short)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = AttendoColors.Green,
            )
            Text("Class work", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Cream)
            Spacer(Modifier.height(14.dp))
            val total = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
            val done = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(AttendoColors.Cream.copy(alpha = 0.15f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(done.toFloat() / total)
                        .clip(CircleShape)
                        .background(AttendoColors.Green),
                )
            }
            Spacer(Modifier.height(14.dp))
            val week = (ChronoUnit.DAYS.between(start, today) / 7 + 1).coerceIn(1, total / 7 + 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Class days", calendar.classDayCount().toString())
                Stat("Week", if (today.isBefore(start)) "–" else if (today.isAfter(end)) "Done" else "$week")
                Stat("Holidays", calendar.events.count { it.kind == DayKind.HOLIDAY }.toString())
                Stat("Finals end", calendar.semesterEnd?.format(Short) ?: "–")
            }
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
private fun Legend() {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendChip("Holiday", AttendoColors.Pink, filled = true)
        LegendChip("Weekend", AttendoColors.Pink, filled = false)
        LegendChip("Assessment", AttendoColors.Blue, filled = true)
        LegendChip("Event", AttendoColors.Green, filled = true)
        LegendChip("Deadline", AttendoColors.Yellow, filled = true)
    }
}

@Composable
private fun LegendChip(label: String, color: Color, filled: Boolean) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(AttendoColors.CreamCard)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(color) else Modifier.border(2.dp, color, CircleShape)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ComingUp(calendar: AcademicCalendar, today: LocalDate) {
    val next = calendar.events.filter { !it.end.isBefore(today) }.take(4)
    if (next.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard)
            .padding(18.dp),
    ) {
        Text("Coming up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        next.forEach { e ->
            val days = ChronoUnit.DAYS.between(today, e.start)
            EventRow(
                e,
                trailing = when {
                    today in e -> "Now"
                    days == 1L -> "Tomorrow"
                    else -> "in $days days"
                },
            )
        }
    }
}

@Composable
private fun EventRow(e: CalendarEvent, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(e.kind.color()))
        Spacer(Modifier.width(10.dp))
        Text(
            if (e.start == e.end) e.start.format(Short) else "${e.start.dayOfMonth}–${e.end.format(Short)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(78.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.bodyMedium)
            Text(e.kind.label(), style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = AttendoColors.Green)
        }
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    calendar: AcademicCalendar,
    today: LocalDate,
    selected: LocalDate,
    onPickDate: (LocalDate) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.CreamCard)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(month.year.toString(), style = MaterialTheme.typography.titleMedium, color = AttendoColors.Muted)
        }
        Spacer(Modifier.height(12.dp))
        // Weekday header, Monday first.
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (d in calendar.weekend) AttendoColors.Pink else AttendoColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, calendar, today, selected, onPickDate)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val monthEvents = calendar.events.filter {
            YearMonth.from(it.start) == month || (YearMonth.from(it.end) == month && YearMonth.from(it.start) != month)
        }
        if (monthEvents.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AttendoColors.CreamDeep))
            Spacer(Modifier.height(6.dp))
            monthEvents.forEach { EventRow(it) }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    calendar: AcademicCalendar,
    today: LocalDate,
    selected: LocalDate,
    onPickDate: (LocalDate) -> Unit,
) {
    val events = calendar.eventsOn(day)
    val main = events.firstOrNull { it.kind != DayKind.DEADLINE }
    val hasDeadline = events.any { it.kind == DayKind.DEADLINE }
    val weekend = calendar.isWeekend(day)
    val fill = when {
        day == selected -> AttendoColors.Ink
        main != null -> main.kind.color()
        else -> Color.Transparent
    }
    val textColor = when {
        day == selected -> AttendoColors.Cream
        main != null && main.kind != DayKind.DEADLINE -> AttendoColors.Cream
        weekend -> AttendoColors.Pink
        else -> AttendoColors.Ink
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Scale, onClickLabel = day.toString()) { onPickDate(day) }
            .background(fill)
            .then(if (day == today && day != selected) Modifier.border(2.dp, AttendoColors.Ink, CircleShape) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (main != null || day == today) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasDeadline) AttendoColors.Yellow else Color.Transparent),
        )
    }
}
