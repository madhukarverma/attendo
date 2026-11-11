package com.attendo.feature.calendar

import com.attendo.core.model.PersonalEvent
import com.attendo.core.model.Subject
import com.attendo.core.model.TimetableSlot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.attendo.core.model.Todo
import com.attendo.core.model.TodoAgenda
import androidx.compose.ui.text.style.TextDecoration
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
 * Tapping a day opens it on the Classes tab.
 */
@Composable
fun CalendarScreen(
    calendar: AcademicCalendar,
    selected: LocalDate,
    onPickDate: (LocalDate) -> Unit,
    onClose: () -> Unit,
    today: LocalDate = LocalDate.now(),
    /** The user's to-dos and reminders, marked on their days and listed per month. */
    todos: List<Todo> = emptyList(),
    slots: List<TimetableSlot> = emptyList(),
    subjects: List<Subject> = emptyList(),
    onSaveEvent: (PersonalEvent) -> Unit = {},
    onDeleteEvent: (id: String) -> Unit = {},
    onOpenTimetable: () -> Unit = {},
) {
    var agendaDay by remember { mutableStateOf<LocalDate?>(null) }
    var eventDraft by remember { mutableStateOf<PersonalEvent?>(null) }
    val months = remember(calendar) { monthsOf(calendar, today) }
    val todoDays = remember(todos) { TodoAgenda.openDays(todos) }
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
        CollapsingTopBar(title = "Calendar", collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { LargeTitle("Calendar") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionPill(
                            if (slots.isEmpty()) "Set timetable" else "Timetable",
                            Icons.Filled.DateRange,
                            Modifier.weight(1f),
                            onOpenTimetable,
                        )
                        ActionPill("Add event", Icons.Filled.Add, Modifier.weight(1f), { eventDraft = PersonalEvent("", "", today) }, primary = true)
                    }
                }
                item { SemesterCard(calendar, today) }
                item { Legend() }
                item { ComingUp(calendar, today) }
                item { YourTodos(todos, today) }
                items(months, key = { it.toString() }) { month ->
                    MonthCard(month, calendar, today, selected, { agendaDay = it }, todos, todoDays) { id ->
                        eventDraft = personalEvent(calendar, id)
                    }
                }
                item {
                    Text(
                        "Tap a day to see its classes and events. Saturdays and Sundays are holidays. " +
                            "Source: NIT Trichy academic calendar, July 2026 session; entries marked \u201cYours\u201d are your own.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                    )
                }
            }
        }
    }

    agendaDay?.let { day ->
        DayAgendaSheet(
            day = day,
            calendar = calendar,
            slots = slots,
            subjects = subjects,
            todos = todos,
            onDismiss = { agendaDay = null },
            onAddEvent = { eventDraft = PersonalEvent("", "", day) },
            onEditEvent = { id -> eventDraft = personalEvent(calendar, id) },
            onMarkAttendance = { agendaDay = null; onPickDate(day) },
            onOpenTimetable = { agendaDay = null; onOpenTimetable() },
        )
    }
    eventDraft?.let { draft ->
        EventDialog(
            event = draft,
            onDismiss = { eventDraft = null },
            onSave = { eventDraft = null; onSaveEvent(it) },
            onDelete = if (draft.id.isEmpty()) null else ({ eventDraft = null; onDeleteEvent(draft.id) }),
        )
    }
}

private const val HEADER_ITEMS = 6

/** Rebuilds the editable event behind a personal calendar entry. */
private fun personalEvent(calendar: AcademicCalendar, id: String): PersonalEvent? =
    calendar.events.firstOrNull { it.personal && it.id == id }?.let {
        PersonalEvent(id = id, title = it.title, start = it.start, end = it.end, kind = it.kind, note = it.note)
    }

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
            .background(AttendoColors.Hero)
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
            Text("Class work", style = MaterialTheme.typography.labelLarge, color = AttendoColors.OnHero)
            Spacer(Modifier.height(14.dp))
            val total = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
            val done = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(AttendoColors.OnHero.copy(alpha = 0.15f)),
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
        Text(value, style = MaterialTheme.typography.titleMedium, color = AttendoColors.OnHero)
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
        LegendChip("Reminder", AttendoColors.Ink, filled = false)
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

private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")

/** The next open to-dos and reminders, under the academic "Coming up" card. */
@Composable
private fun YourTodos(todos: List<Todo>, today: LocalDate) {
    val next = todos.filter { !it.done && !it.date.isBefore(today) }
        .sortedWith(compareBy<Todo>({ it.date }, { it.remindAt ?: java.time.LocalTime.MAX }))
        .take(4)
    if (next.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard)
            .padding(18.dp),
    ) {
        Text("Your reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        next.forEach { TodoRow(it) }
    }
}

@Composable
private fun TodoRow(todo: Todo) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).border(2.dp, AttendoColors.Ink, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            todo.date.format(Short),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(78.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.done) AttendoColors.Muted else AttendoColors.Ink,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            )
            Text(
                todo.remindAt?.let { "Reminder · " + it.format(TimeFormat) } ?: "To-do",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
            )
        }
    }
}

@Composable
private fun EventRow(e: CalendarEvent, trailing: String? = null, onEdit: ((String) -> Unit)? = null) {
    val editable = e.personal && e.id != null && onEdit != null
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.iosPress(PressStyle.Highlight, onClickLabel = "Edit ${e.title}") { onEdit?.invoke(e.id!!) } else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            Text(
                if (e.personal) "${e.kind.label()} · Yours" else e.kind.label(),
                style = MaterialTheme.typography.labelSmall,
                color = if (e.personal) AttendoColors.Green else AttendoColors.Muted,
            )
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
    todos: List<Todo>,
    todoDays: Set<LocalDate>,
    onEditEvent: (String) -> Unit = {},
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
                        if (day != null) DayCell(day, calendar, today, selected, onPickDate, hasTodo = day in todoDays)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val monthEvents = calendar.events.filter {
            YearMonth.from(it.start) == month || (YearMonth.from(it.end) == month && YearMonth.from(it.start) != month)
        }
        val monthTodos = todos.filter { YearMonth.from(it.date) == month }
            .sortedWith(compareBy<Todo>({ it.date }, { it.remindAt ?: java.time.LocalTime.MAX }))
        if (monthEvents.isNotEmpty() || monthTodos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AttendoColors.CreamDeep))
            Spacer(Modifier.height(6.dp))
            monthEvents.forEach { EventRow(it, onEdit = onEditEvent) }
            monthTodos.forEach { TodoRow(it) }
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
    hasTodo: Boolean = false,
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
        // Markers under the number: yellow = deadline, ring = your to-do.
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasDeadline || !hasTodo) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (hasDeadline) AttendoColors.Yellow else Color.Transparent),
                )
            }
            if (hasTodo) Box(Modifier.size(5.dp).border(1.5.dp, textColor, CircleShape))
        }
    }
}
