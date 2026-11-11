package com.attendo.feature.tasks

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.CardPriority
import com.attendo.core.model.KanbanCard
import com.attendo.core.model.KanbanColumn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MAX_TITLE = 80
private const val MAX_NOTES = 500
private const val MAX_COLUMN_TITLE = 24
private val LongDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AttendoColors.Green,
    unfocusedBorderColor = AttendoColors.CreamDeep,
    cursorColor = AttendoColors.Green,
    focusedLabelColor = AttendoColors.Green,
)

/** Add or edit a card. [card] null = new card. */
@Composable
internal fun CardEditorDialog(
    card: KanbanCard?,
    columns: List<KanbanColumn>,
    initialColumnId: String,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, due: LocalDate?, priority: CardPriority, columnId: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(card?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(card?.notes.orEmpty()) }
    var due by rememberSaveable { mutableStateOf(card?.due) }
    var priority by rememberSaveable { mutableStateOf(card?.priority ?: CardPriority.NONE) }
    var columnId by rememberSaveable { mutableStateOf(initialColumnId) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "New card" else "Edit card") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_TITLE) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(MAX_NOTES) },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = fieldColors(),
                )

                FieldLabel("Due date")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Choice(
                        label = due?.format(LongDate) ?: "No due date",
                        selected = due != null,
                        icon = Icons.Filled.DateRange,
                    ) { pickDate = true }
                    if (due != null) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .iosPress(PressStyle.Dim, onClickLabel = "Remove due date") { due = null },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                FieldLabel("Priority")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CardPriority.entries.forEach { p ->
                        Choice(
                            label = p.label(),
                            selected = priority == p,
                            dot = if (p == CardPriority.NONE) null else p.color(),
                        ) { priority = p }
                    }
                }

                if (columns.size > 1) {
                    FieldLabel("Column")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        columns.forEach { column ->
                            Choice(label = column.title, selected = columnId == column.id) { columnId = column.id }
                        }
                    }
                }

                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(
                            if (confirmDelete) "Tap again to delete this card" else "Delete card",
                            color = AttendoColors.Pink,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), notes.trim(), due, priority, columnId) },
                enabled = title.isNotBlank(),
            ) { Text(if (card == null) "Add" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )

    if (pickDate) {
        TaskDatePicker(initial = due ?: LocalDate.now(), onDismiss = { pickDate = false }, onPick = { due = it })
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = AttendoColors.Muted,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}

/** A selectable pill (single choice among siblings). */
@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    dot: Color? = null,
    onClick: () -> Unit,
) {
    val content = if (selected) AttendoColors.Cream else AttendoColors.Ink
    Row(
        Modifier
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = label, onClick = onClick)
            .background(if (selected) AttendoColors.Ink else AttendoColors.CreamCard)
            .border(1.dp, if (selected) AttendoColors.Ink else AttendoColors.CreamDeep, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (dot != null) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

/** Add / rename / delete a column. */
@Composable
internal fun ColumnDialog(
    edit: ColumnEdit,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (KanbanColumn, String) -> Unit,
    onDelete: (KanbanColumn) -> Unit,
) {
    if (edit is ColumnEdit.Delete) {
        val cards = edit.cardCount
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete \"${edit.column.title}\"?") },
            text = {
                Text(
                    when (cards) {
                        0 -> "The column is empty."
                        1 -> "Its 1 card will be deleted too."
                        else -> "Its $cards cards will be deleted too."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(edit.column) }) { Text("Delete", color = AttendoColors.Pink, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
            containerColor = AttendoColors.Cream,
        )
        return
    }
    val existing = (edit as? ColumnEdit.Rename)?.column
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New column" else "Rename column") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MAX_COLUMN_TITLE) },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (existing == null) onAdd(title.trim()) else onRename(existing, title.trim()) },
                enabled = title.isNotBlank(),
            ) { Text(if (existing == null) "Add" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )
}
