package com.attendo.feature.tasks

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendo.core.designsystem.component.TriangleMascot
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The to-do page of the Tasks tab. A to-do belongs to a day; giving it a time makes it a reminder
 * that the app notifies about (scheduled by the app module from todos.csv).
 *
 * @param bottomPadding room left under the list for a bottom bar.
 */
@Composable
internal fun TodoPageHost(vm: TodoViewModel, bottomPadding: Dp = 24.dp) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Android 13+: reminders need the notification permission, asked for when the first one is set.
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var permissionTick by remember { mutableStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedOnce = true
        permissionTick++
    }
    val notificationsOff = remember(state, permissionTick) { !context.canPostNotifications() }

    TodoPage(
        state = state,
        onAdd = { title, date, time ->
            vm.add(title, date, time)
            if (time != null && notificationsOff && !askedOnce && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onToggle = vm::toggle,
        onDelete = vm::delete,
        onClearDone = vm::clearDone,
        notificationsOff = notificationsOff,
        onEnableNotifications = {
            if (!askedOnce && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.openNotificationSettings()
            }
        },
        bottomPadding = bottomPadding,
    )
}

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.openNotificationSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
internal fun TodoPage(
    state: TodoUiState,
    onAdd: (title: String, date: LocalDate, remindAt: LocalTime?) -> Unit,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onClearDone: () -> Unit,
    notificationsOff: Boolean = false,
    onEnableNotifications: () -> Unit = {},
    bottomPadding: Dp = 24.dp,
) {
    val groups = state.groups
    val hasReminders = (groups.today + groups.upcoming).any { it.hasPendingReminder(state.now) }

    Box(Modifier.fillMaxSize()) {
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "add") { AddTodoCard(today = state.today, now = state.now, onAdd = onAdd) }
                if (notificationsOff && hasReminders) {
                    item(key = "notifications") { NotificationsBanner(onEnableNotifications, Modifier.animateItem()) }
                }
                if (state.loaded && state.isEmpty) {
                    item(key = "empty") { EmptyState(Modifier.animateItem()) }
                }
                section("Overdue", groups.overdue, AttendoColors.Pink, showDate = true, state, onToggle, onDelete)
                section("Today", groups.today, AttendoColors.Ink, showDate = false, state, onToggle, onDelete)
                section("Upcoming", groups.upcoming, AttendoColors.Ink, showDate = true, state, onToggle, onDelete)
                if (groups.done.isNotEmpty()) {
                    item(key = "header-done") {
                        Row(Modifier.animateItem().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Done", AttendoColors.Muted, Modifier.weight(1f))
                            TextButton(onClick = onClearDone) { Text("Clear", color = AttendoColors.Pink) }
                        }
                    }
                    items(groups.done.take(DONE_SHOWN), key = { "todo-${it.id}" }) { todo ->
                        TodoRow(todo, showDate = true, state, onToggle, onDelete, Modifier.animateItem())
                    }
                }
                item(key = "bottom") { Spacer(Modifier.height(bottomPadding)) }
            }
        }
    }
}

private const val DONE_SHOWN = 20

private fun LazyListScope.section(
    title: String,
    todos: List<Todo>,
    color: Color,
    showDate: Boolean,
    state: TodoUiState,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    if (todos.isEmpty()) return
    item(key = "header-$title") { SectionTitle(title, color, Modifier.animateItem().padding(top = 16.dp)) }
    items(todos, key = { "todo-${it.id}" }) { todo ->
        TodoRow(todo, showDate, state, onToggle, onDelete, Modifier.animateItem())
    }
}

@Composable
private fun SectionTitle(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineSmall, color = color, modifier = modifier)
}

private val DayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")

internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(DayFormat)
}

@Composable
private fun AddTodoCard(today: LocalDate, now: LocalDateTime, onAdd: (String, LocalDate, LocalTime?) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today) }
    var time by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val timePassed = time?.let { !date.atTime(it).isAfter(now) } == true
    // Plain to-dos moved to the board and the weekday checklist in v0.25: a reminder needs a time.
    val canAdd = title.isNotBlank() && time != null && !timePassed

    fun submit() {
        if (!canAdd) return
        onAdd(title.trim(), date, time)
        title = ""
        time = null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard)
            .padding(14.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Remind me to…") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AttendoColors.Cream,
                unfocusedContainerColor = AttendoColors.Cream,
                focusedBorderColor = AttendoColors.Green,
                unfocusedBorderColor = AttendoColors.Cream,
                cursorColor = AttendoColors.Green,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(Icons.Filled.DateRange, dayLabel(date, today), selected = false) { pickDate = true }
            OptionChip(
                Icons.Filled.Notifications,
                time?.format(TimeFormat) ?: "Pick a time",
                selected = time != null,
                onClear = if (time != null) ({ time = null }) else null,
            ) { pickTime = true }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .iosPress(PressStyle.Scale, enabled = canAdd, onClickLabel = "Add to-do") { submit() }
                    .clip(CircleShape)
                    .background(if (canAdd) AttendoColors.Green else AttendoColors.CreamDeep),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = if (canAdd) AttendoColors.Cream else AttendoColors.Muted)
            }
        }
        if (time == null && title.isNotBlank()) {
            Text(
                "Pick a time for the reminder.",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
        if (timePassed) {
            Text(
                "That time has already passed. Pick a later time or another day.",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Pink,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
    }

    if (pickDate) {
        TaskDatePicker(initial = date, onDismiss = { pickDate = false }, onPick = { date = it })
    }
    if (pickTime) {
        TodoTimePicker(
            initial = time ?: LocalTime.of(9, 0),
            onDismiss = { pickTime = false },
            onPick = { time = it },
        )
    }
}

@Composable
private fun OptionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClear: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = label, onClick = onClick)
            .background(if (selected) AttendoColors.Green else AttendoColors.Cream)
            .padding(start = 12.dp, end = if (onClear != null) 6.dp else 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) AttendoColors.Cream else AttendoColors.Ink
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
        if (onClear != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove reminder",
                tint = fg,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .iosPress(PressStyle.Dim, onClickLabel = "Remove reminder", onClick = onClear)
                    .padding(3.dp),
            )
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    showDate: Boolean,
    state: TodoUiState,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = !todo.done && todo.date < state.today
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .iosPress(PressStyle.Highlight, onClickLabel = if (todo.done) "Mark not done" else "Mark done") { onToggle(todo) }
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(todo.done)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (todo.done) AttendoColors.Muted else AttendoColors.Ink,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            )
            val parts = buildList {
                if (showDate) add(dayLabel(todo.date, state.today))
                todo.remindAt?.let { t ->
                    val sent = !todo.done && todo.reminderDateTime?.isAfter(state.now) == false
                    add(t.format(TimeFormat) + if (sent) " (reminded)" else "")
                }
            }
            if (parts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.remindAt != null) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Reminder",
                            tint = if (overdue) AttendoColors.Pink else AttendoColors.Muted,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        parts.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) AttendoColors.Pink else AttendoColors.Muted,
                    )
                }
            }
        }
        IconButton(onClick = { onDelete(todo) }) {
            Icon(Icons.Filled.Close, contentDescription = "Delete ${todo.title}", tint = AttendoColors.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (done) Modifier.background(AttendoColors.Green)
                else Modifier.border(2.dp, AttendoColors.Green, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = AttendoColors.Cream, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun NotificationsBanner(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.Pink.copy(alpha = 0.14f))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = AttendoColors.Pink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Notifications are off, so reminders won't pop up.",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEnable) { Text("Turn on", color = AttendoColors.Pink, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TriangleMascot(Modifier.size(120.dp))
        Spacer(Modifier.height(12.dp))
        Text("No reminders yet.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add something above, pick a day and a time, and you'll get a notification.",
            style = MaterialTheme.typography.bodyMedium,
            color = AttendoColors.Muted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // The Material date picker works in UTC midnight millis.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoTimePicker(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remind me at") },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = AttendoColors.Green,
                    timeSelectorSelectedContentColor = AttendoColors.Cream,
                    periodSelectorSelectedContainerColor = AttendoColors.Green,
                    periodSelectorSelectedContentColor = AttendoColors.Cream,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text("Set", color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.CreamCard,
    )
}
