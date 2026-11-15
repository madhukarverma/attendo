package com.attendo.feature.tasks

import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space
import com.attendo.core.designsystem.component.attendoFilledFieldColors
import com.attendo.core.designsystem.component.attendoFieldColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.component.TriangleMascot
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MAX_HABIT_TITLE = 60
private val DayTitle = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val ShortDate = DateTimeFormatter.ofPattern("d MMM")

/**
 * Weekday checklist, like a habit tracker: each task repeats on chosen days of the week. Pick a
 * day in the week strip to see and tick that day's tasks; each row also shows its whole week and
 * its streak. Days after today can be planned but not ticked.
 */
@Composable
internal fun HabitsPage(
    state: HabitsUiState,
    onSelect: (LocalDate) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onToggle: (HabitRow) -> Unit,
    onAdd: (title: String, days: Set<DayOfWeek>) -> Unit,
    onUpdate: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    bottomPadding: Dp,
) {
    var editing by remember { mutableStateOf<Habit?>(null) }

    BounceContainer(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = Space.L),
            verticalArrangement = Arrangement.spacedBy(Space.XS),
        ) {
            item(key = "week") { WeekCard(state, onSelect, onShiftWeek) }
            item(key = "day") { DayHeader(state) }
            if (state.loaded && state.rows.isEmpty()) {
                item(key = "empty") { EmptyDay(state, Modifier.animateItem()) }
            }
            items(state.rows, key = { it.habit.id }) { row ->
                HabitRowView(
                    row = row,
                    canTick = state.canTick,
                    onToggle = { onToggle(row) },
                    onEdit = { editing = row.habit },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                AddHabitCard(selectedDay = state.selected.dayOfWeek, onAdd = onAdd, modifier = Modifier.padding(top = Space.XS))
            }
            item(key = "bottom") { Spacer(Modifier.height(bottomPadding)) }
        }
    }

    editing?.let { habit ->
        EditHabitDialog(
            habit = habit,
            onDismiss = { editing = null },
            onSave = { editing = null; onUpdate(it) },
            onDelete = { editing = null; onDelete(habit) },
        )
    }
}

/** Week title with previous/next, and the seven days with each day's progress. */
@Composable
private fun WeekCard(state: HabitsUiState, onSelect: (LocalDate) -> Unit, onShiftWeek: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .padding(horizontal = Space.XS, vertical = Space.XS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week") { onShiftWeek(-1) }
            Text(
                if (state.today in state.week) "This week" else "${state.week.first().format(ShortDate)} – ${state.week.last().format(ShortDate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = Space.XXS),
            )
            if (state.selected != state.today) {
                TextButton(onClick = { onSelect(state.today) }) { Text("Today", color = AttendoColors.Green) }
            }
            ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week") { onShiftWeek(1) }
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.XXS), horizontalArrangement = Arrangement.spacedBy(Space.XXS)) {
            state.week.forEach { day ->
                DayCell(
                    day = day,
                    selected = day == state.selected,
                    isToday = day == state.today,
                    progress = state.progress[day] ?: (0 to 0),
                    modifier = Modifier.weight(1f),
                ) { onSelect(day) }
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = AttendoColors.Ink) }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    progress: Pair<Int, Int>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val (done, total) = progress
    val content = if (selected) AttendoColors.Cream else AttendoColors.Ink
    Column(
        modifier
            .clip(Corner.Shape)
            .iosPress(PressStyle.Scale, onClickLabel = day.format(DayTitle), onClick = onClick)
            .background(if (selected) AttendoColors.Ink else AttendoColors.Cream)
            .then(if (isToday && !selected) Modifier.border(2.dp, AttendoColors.Green, Corner.Shape) else Modifier)
            .padding(vertical = Space.XS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) content else AttendoColors.Muted,
        )
        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
        Spacer(Modifier.height(Space.XXS))
        // Progress for the day: empty track when nothing is planned, full when everything is ticked.
        Box(
            Modifier
                .width(22.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (selected) content.copy(alpha = 0.25f) else AttendoColors.CreamDeep),
        ) {
            if (total > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(done.toFloat() / total)
                        .clip(CircleShape)
                        .background(AttendoColors.Green),
                )
            }
        }
    }
}

@Composable
private fun DayHeader(state: HabitsUiState) {
    val (done, total) = state.selectedProgress
    Row(Modifier.fillMaxWidth().padding(top = Space.S, bottom = Space.XXS), verticalAlignment = Alignment.CenterVertically) {
        Text(
            state.selected.format(DayTitle),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                if (done == total) "All done" else "$done of $total",
                style = MaterialTheme.typography.labelLarge,
                color = if (done == total) AttendoColors.Green else AttendoColors.Muted,
            )
        }
    }
}

@Composable
private fun HabitRowView(row: HabitRow, canTick: Boolean, onToggle: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .iosPress(
                PressStyle.Highlight,
                enabled = canTick,
                onClickLabel = if (row.checked) "Untick ${row.habit.title}" else "Tick ${row.habit.title}",
                onClick = onToggle,
            )
            .padding(start = Space.M, end = Space.XXS, top = Space.S, bottom = Space.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .alpha(if (canTick) 1f else 0.4f)
                .clip(CircleShape)
                .then(
                    if (row.checked) Modifier.background(AttendoColors.GreenFill)
                    else Modifier.border(2.dp, AttendoColors.Green, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (row.checked) Icon(Icons.Filled.Check, contentDescription = null, tint = AttendoColors.OnAccent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(Space.S))
        Column(Modifier.weight(1f)) {
            Text(
                row.habit.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (row.checked) AttendoColors.Muted else AttendoColors.Ink,
                textDecoration = if (row.checked) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.XXS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeekDots(row.week)
                Spacer(Modifier.width(Space.S))
                Text(
                    when {
                        !canTick -> "Planned"
                        row.streak > 1 -> "${row.streak} in a row"
                        row.streak == 1 -> "1 done"
                        else -> "No streak yet"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.streak > 1 && canTick) AttendoColors.Green else AttendoColors.Muted,
                )
            }
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .iosPress(PressStyle.Dim, onClickLabel = "Edit ${row.habit.title}", onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(16.dp))
        }
    }
}

/** The habit's week, Monday to Sunday: filled = ticked, ring = planned, pink ring = missed, dot = off. */
@Composable
private fun WeekDots(week: List<DayMark>) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.XXS), verticalAlignment = Alignment.CenterVertically) {
        week.forEach { mark ->
            Box(Modifier.size(10.dp), contentAlignment = Alignment.Center) {
                when (mark) {
                    DayMark.DONE -> Box(Modifier.fillMaxSize().clip(CircleShape).background(AttendoColors.Green))
                    DayMark.PLANNED -> Box(Modifier.fillMaxSize().border(1.5.dp, AttendoColors.Green, CircleShape))
                    DayMark.MISSED -> Box(Modifier.fillMaxSize().border(1.5.dp, AttendoColors.Pink, CircleShape))
                    DayMark.OFF -> Box(Modifier.size(4.dp).clip(CircleShape).background(AttendoColors.CreamDeep))
                }
            }
        }
    }
}

@Composable
private fun EmptyDay(state: HabitsUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = Space.S), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.totalHabits == 0) {
            TriangleMascot(Modifier.size(110.dp))
            Spacer(Modifier.height(Space.S))
            Text("No tasks yet.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add a task below and pick the days of the week it repeats on.",
                style = MaterialTheme.typography.bodyMedium,
                color = AttendoColors.Muted,
            )
        } else {
            Text(
                "Nothing planned for ${state.selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}.",
                style = MaterialTheme.typography.bodyMedium,
                color = AttendoColors.Muted,
            )
        }
    }
}

@Composable
private fun AddHabitCard(selectedDay: DayOfWeek, onAdd: (String, Set<DayOfWeek>) -> Unit, modifier: Modifier = Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    // Starts on the day being viewed; follows it until the user picks days themselves.
    var picked by remember { mutableStateOf<Set<DayOfWeek>?>(null) }
    val days = picked ?: setOf(selectedDay)
    val canAdd = title.isNotBlank() && days.isNotEmpty()

    fun submit() {
        if (!canAdd) return
        onAdd(title.trim(), days)
        title = ""
        picked = null
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .padding(Space.M),
    ) {
        Text("New task", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted, modifier = Modifier.padding(start = Space.XXS, bottom = Space.XS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MAX_HABIT_TITLE) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("e.g. Revise DSA") },
                shape = Corner.Shape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = attendoFilledFieldColors(AttendoColors.Cream),
            )
            Spacer(Modifier.width(Space.S))
            Box(
                Modifier
                    .size(46.dp)
                    .iosPress(PressStyle.Scale, enabled = canAdd, onClickLabel = "Add task") { submit() }
                    .clip(CircleShape)
                    .background(if (canAdd) AttendoColors.GreenFill else AttendoColors.CreamDeep),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = if (canAdd) AttendoColors.OnAccent else AttendoColors.Muted)
            }
        }
        Spacer(Modifier.height(Space.S))
        WeekdayToggles(days) { picked = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Repeats on the highlighted days.",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
                modifier = Modifier.weight(1f).padding(start = Space.XXS),
            )
            TextButton(onClick = { picked = DayOfWeek.entries.toSet() }) { Text("Every day", color = AttendoColors.Green) }
        }
    }
}

/** Seven round toggles, Monday to Sunday. */
@Composable
private fun WeekdayToggles(days: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val on = day in days
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .iosPress(PressStyle.Scale, onClickLabel = day.getDisplayName(TextStyle.FULL, Locale.getDefault())) {
                        onChange(if (on) days - day else days + day)
                    }
                    .background(if (on) AttendoColors.Ink else AttendoColors.Cream)
                    .border(1.dp, if (on) AttendoColors.Ink else AttendoColors.CreamDeep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) AttendoColors.Cream else AttendoColors.Ink,
                )
            }
        }
    }
}

@Composable
private fun EditHabitDialog(habit: Habit, onDismiss: () -> Unit, onSave: (Habit) -> Unit, onDelete: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(habit.title) }
    var days by remember { mutableStateOf(habit.days) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_HABIT_TITLE) },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Corner.Shape,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = attendoFieldColors(),
                )
                Spacer(Modifier.height(Space.M))
                Text("Repeats on", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
                Spacer(Modifier.height(Space.XS))
                WeekdayToggles(days) { days = it }
                Spacer(Modifier.height(Space.XS))
                TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                    Text(
                        if (confirmDelete) "Tap again to delete it and its history" else "Delete task",
                        color = AttendoColors.Pink,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(habit.copy(title = title.trim(), days = days)) },
                enabled = title.isNotBlank() && days.isNotEmpty(),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )
}
