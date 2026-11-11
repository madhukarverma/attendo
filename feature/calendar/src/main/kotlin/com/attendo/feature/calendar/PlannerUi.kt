package com.attendo.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.DayKind
import com.attendo.core.model.PersonalEvent
import com.attendo.core.model.Subject
import com.attendo.core.model.Timetable
import com.attendo.core.model.TimetableSlot
import com.attendo.core.model.Todo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SlotTime = DateTimeFormatter.ofPattern("H:mm")
private val AgendaTitle = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val ShortDay = DateTimeFormatter.ofPattern("EEE, d MMM")

internal fun TimetableSlot.timeRange(): String = "${start.format(SlotTime)}–${end.format(SlotTime)}"

/** Kinds a student can give their own events, in menu order. */
private val EVENT_KINDS = listOf(DayKind.EXAM, DayKind.DEADLINE, DayKind.EVENT, DayKind.HOLIDAY)

private fun DayKind.userLabel(): String = when (this) {
    DayKind.EXAM -> "Exam / test"
    DayKind.DEADLINE -> "Deadline"
    DayKind.HOLIDAY -> "Holiday"
    else -> "Event"
}

private fun subjectColor(index: Int): Color {
    val palette = listOf(AttendoColors.Green, AttendoColors.Blue, AttendoColors.Pink, AttendoColors.Yellow)
    return palette[Math.floorMod(index, palette.size)]
}

// ---------------------------------------------------------------------------------------------
// Day agenda
// ---------------------------------------------------------------------------------------------

/**
 * Everything on one day: timetable classes, official and personal calendar entries, reminders.
 * From here the student adds an event on that day or marks the day's attendance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayAgendaSheet(
    day: LocalDate,
    calendar: AcademicCalendar,
    slots: List<TimetableSlot>,
    subjects: List<Subject>,
    todos: List<Todo>,
    onDismiss: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (id: String) -> Unit,
    onMarkAttendance: () -> Unit,
    onOpenTimetable: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val names = subjects.associate { it.id to CourseCatalog.shortName(it) }
    val classes = Timetable.on(slots, day)
    val events = calendar.eventsOn(day)
    val reminders = todos.filter { it.date == day }.sortedBy { it.remindAt }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = AttendoColors.Cream) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(day.format(AgendaTitle), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                when {
                    calendar.isWeekend(day) -> "Weekend"
                    calendar.isHoliday(day) -> "Holiday"
                    calendar.isClassDay(day) -> "Class day"
                    else -> "Outside class work"
                },
                style = MaterialTheme.typography.labelLarge,
                color = AttendoColors.Muted,
            )

            AgendaHeader("Classes")
            when {
                slots.isEmpty() -> AgendaLink("Add your weekly timetable to see classes here", onOpenTimetable)
                classes.isEmpty() -> AgendaEmpty("No classes on your timetable")
                else -> classes.forEach { slot ->
                    AgendaRow(
                        dot = subjectColor(subjects.indexOfFirst { it.id == slot.subjectId }),
                        title = names[slot.subjectId] ?: "Deleted subject",
                        detail = slot.timeRange() + if (slot.room.isNotBlank()) " · ${slot.room}" else "",
                    )
                }
            }

            AgendaHeader("Calendar")
            if (events.isEmpty()) AgendaEmpty("Nothing on the calendar")
            events.forEach { e ->
                AgendaRow(
                    dot = e.kind.color(),
                    title = e.title,
                    detail = listOfNotNull(
                        if (e.personal) "Yours" else "Academic calendar",
                        if (e.start != e.end) "${e.start.format(ShortDay)} – ${e.end.format(ShortDay)}" else null,
                        e.note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    onEdit = e.id?.takeIf { e.personal }?.let { id -> { onEditEvent(id) } },
                )
            }

            if (reminders.isNotEmpty()) {
                AgendaHeader("Reminders")
                reminders.forEach { r ->
                    AgendaRow(
                        dot = AttendoColors.Ink,
                        title = r.title,
                        detail = (r.remindAt?.format(SlotTime) ?: "") + if (r.done) " · done" else "",
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill("Add event", Icons.Filled.Add, Modifier.weight(1f), onAddEvent)
                ActionPill("Mark attendance", Icons.Filled.DateRange, Modifier.weight(1f), onMarkAttendance, primary = true)
            }
        }
    }
}

@Composable
private fun AgendaHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AttendoColors.Muted,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun AgendaEmpty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted)
}

@Composable
private fun AgendaLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AttendoColors.Green,
        modifier = Modifier.iosPress(PressStyle.Dim, onClickLabel = text, onClick = onClick),
    )
}

@Composable
private fun AgendaRow(dot: Color, title: String, detail: String, onEdit: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
        if (onEdit != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .iosPress(PressStyle.Dim, onClickLabel = "Edit $title", onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Edit, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
internal fun ActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val container = if (primary) AttendoColors.GreenFill else AttendoColors.CreamCard
    val content = if (primary) AttendoColors.OnAccent else AttendoColors.Ink
    Row(
        modifier
            .height(46.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = text, onClick = onClick)
            .background(container)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Personal event editor
// ---------------------------------------------------------------------------------------------

/** Add or edit one of the student's own calendar entries. [event] with an empty id = new. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventDialog(
    event: PersonalEvent,
    onDismiss: () -> Unit,
    onSave: (PersonalEvent) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(event.title) }
    var kind by rememberSaveable { mutableStateOf(event.kind.takeIf { it in EVENT_KINDS } ?: DayKind.EVENT) }
    var start by rememberSaveable { mutableStateOf(event.start) }
    var end by rememberSaveable { mutableStateOf(event.end) }
    var note by rememberSaveable { mutableStateOf(event.note) }
    var picking by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event.id.isEmpty()) "New event" else "Edit event") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. DSA class test") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EVENT_KINDS.forEach { k -> Chip(k.userLabel(), selected = kind == k, dot = k.color()) { kind = k } }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip(start.format(ShortDay), selected = false) { picking = "start" }
                    Text("to", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
                    Chip(end.format(ShortDay), selected = false) { picking = "end" }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Note (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = fieldColors(),
                )
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "Tap again to delete" else "Delete event", color = AttendoColors.Pink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(event.copy(title = title.trim(), kind = kind, start = start, end = maxOf(start, end), note = note.trim())) },
                enabled = title.isNotBlank(),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )

    when (picking) {
        "start" -> PlannerDatePicker(start, onDismiss = { picking = null }) { picked ->
            if (end == start || end < picked) end = picked
            start = picked
        }
        "end" -> PlannerDatePicker(end, onDismiss = { picking = null }) { picked -> end = maxOf(picked, start) }
    }
}

// ---------------------------------------------------------------------------------------------
// Weekly timetable
// ---------------------------------------------------------------------------------------------

/** The weekly timetable: classes per weekday; tap one to edit, "+" to add. */
@Composable
fun TimetableScreen(
    slots: List<TimetableSlot>,
    subjects: List<Subject>,
    onSave: (TimetableSlot) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    var editing by remember { mutableStateOf<TimetableSlot?>(null) }
    val names = subjects.associate { it.id to CourseCatalog.shortName(it) }
    // Weekend days only appear once they have classes (most weeks run Monday to Friday).
    val days = DayOfWeek.entries.filter { it <= DayOfWeek.FRIDAY || slots.any { s -> s.day == it } }
    val hiddenWeekend = DayOfWeek.entries.filterNot { it in days }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = "Timetable", collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { LargeTitle("Timetable") }
                if (subjects.isEmpty()) {
                    item {
                        Text(
                            "Add your subjects first (Classes tab), then put them on the timetable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AttendoColors.Muted,
                        )
                    }
                }
                days.forEach { day ->
                    item(key = day.name) {
                        DayCard(
                            day = day,
                            slots = slots.filter { it.day == day }.sortedBy { it.start },
                            names = names,
                            colorOf = { id -> subjectColor(subjects.indexOfFirst { it.id == id }) },
                            canAdd = subjects.isNotEmpty(),
                            onAdd = { editing = newSlot(day, slots, subjects) },
                            onEdit = { editing = it },
                        )
                    }
                }
                if (hiddenWeekend.isNotEmpty() && subjects.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            hiddenWeekend.forEach { day ->
                                ActionPill(
                                    "Add ${day.getDisplayName(TextStyle.SHORT, Locale.getDefault())} class",
                                    Icons.Filled.Add,
                                    onClick = { editing = newSlot(day, slots, subjects) },
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "The Classes tab lists each day's subjects in timetable order, and the calendar's day view shows them.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                    )
                }
            }
        }
    }

    editing?.let { slot ->
        SlotDialog(
            slot = slot,
            subjects = subjects,
            others = slots.filter { it.id != slot.id },
            onDismiss = { editing = null },
            onSave = { editing = null; onSave(it) },
            onDelete = if (slot.id.isEmpty()) null else ({ editing = null; onDelete(slot.id) }),
        )
    }
}

/** A new slot on [day], starting when that day's last class ends (9:00 on an empty day). */
private fun newSlot(day: DayOfWeek, slots: List<TimetableSlot>, subjects: List<Subject>): TimetableSlot {
    val start = slots.filter { it.day == day }.maxOfOrNull { it.end } ?: LocalTime.of(9, 0)
    val end = start.plusMinutes(50).takeIf { it > start } ?: LocalTime.of(23, 59)
    return TimetableSlot("", subjects.firstOrNull()?.id.orEmpty(), day, start, end)
}

@Composable
private fun DayCard(
    day: DayOfWeek,
    slots: List<TimetableSlot>,
    names: Map<String, String>,
    colorOf: (String) -> Color,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onEdit: (TimetableSlot) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AttendoColors.CreamCard)
            .padding(vertical = 6.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (canAdd) TextButton(onClick = onAdd) { Text("+ Add", color = AttendoColors.Green) }
        }
        if (slots.isEmpty()) {
            Text(
                "No classes",
                style = MaterialTheme.typography.bodyMedium,
                color = AttendoColors.Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        slots.forEach { slot ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .iosPress(PressStyle.Highlight, onClickLabel = "Edit ${names[slot.subjectId]}") { onEdit(slot) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colorOf(slot.subjectId)))
                Spacer(Modifier.width(10.dp))
                Text(slot.timeRange(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
                Text(
                    names[slot.subjectId] ?: "Deleted subject",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (slot.room.isNotBlank()) Text(slot.room, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotDialog(
    slot: TimetableSlot,
    subjects: List<Subject>,
    others: List<TimetableSlot>,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var subjectId by rememberSaveable { mutableStateOf(slot.subjectId) }
    var day by rememberSaveable { mutableStateOf(slot.day) }
    var start by rememberSaveable { mutableStateOf(slot.start) }
    var end by rememberSaveable { mutableStateOf(slot.end) }
    var room by rememberSaveable { mutableStateOf(slot.room) }
    var picking by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val draft = slot.copy(subjectId = subjectId, day = day, start = start, end = end, room = room)
    val clash = others.firstOrNull { Timetable.overlaps(it, draft) }
    val valid = subjects.any { it.id == subjectId } && end > start

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (slot.id.isEmpty()) "Add class" else "Edit class") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Label("Subject")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjects.forEach { s -> Chip(CourseCatalog.shortName(s), selected = s.id == subjectId) { subjectId = s.id } }
                }
                Label("Day")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        Chip(d.getDisplayName(TextStyle.SHORT, Locale.getDefault()), selected = d == day) { day = d }
                    }
                }
                Label("Time")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip(start.format(SlotTime), selected = false) { picking = "start" }
                    Text("to", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
                    Chip(end.format(SlotTime), selected = false) { picking = "end" }
                }
                if (end <= start) {
                    Text("The class has to end after it starts.", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Pink)
                }
                clash?.let {
                    Text(
                        "Overlaps another class at ${it.timeRange()}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Yellow,
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it.take(20) },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = fieldColors(),
                )
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "Tap again to remove" else "Remove from timetable", color = AttendoColors.Pink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(draft) }, enabled = valid) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )

    when (picking) {
        "start" -> PlannerTimePicker(start, onDismiss = { picking = null }) { picked ->
            val length = java.time.Duration.between(start, end).takeIf { !it.isNegative && !it.isZero } ?: java.time.Duration.ofMinutes(50)
            start = picked
            end = picked.plus(length).takeIf { it > picked } ?: LocalTime.of(23, 59)
        }
        "end" -> PlannerTimePicker(end, onDismiss = { picking = null }) { end = it }
    }
}

// ---------------------------------------------------------------------------------------------
// Small shared pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
private fun Chip(text: String, selected: Boolean, dot: Color? = null, onClick: () -> Unit) {
    val content = if (selected) AttendoColors.Cream else AttendoColors.Ink
    Row(
        Modifier
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = text, onClick = onClick)
            .background(if (selected) AttendoColors.Ink else AttendoColors.CreamCard)
            .border(1.dp, if (selected) AttendoColors.Ink else AttendoColors.CreamDeep, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AttendoColors.Green,
    unfocusedBorderColor = AttendoColors.CreamDeep,
    cursorColor = AttendoColors.Green,
    focusedLabelColor = AttendoColors.Green,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerTimePicker(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = AttendoColors.GreenFill,
                    timeSelectorSelectedContentColor = AttendoColors.OnAccent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); onDismiss() }) { Text("OK", color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.CreamCard,
    )
}
