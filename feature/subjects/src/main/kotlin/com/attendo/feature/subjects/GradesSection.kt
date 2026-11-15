package com.attendo.feature.subjects

import com.attendo.core.designsystem.theme.ContinuousShape
import androidx.compose.foundation.shape.CircleShape
import com.attendo.core.designsystem.theme.Space
import com.attendo.core.designsystem.component.AttendoDatePicker
import com.attendo.core.designsystem.component.attendoFieldColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.Assessment
import com.attendo.core.model.Grades
import com.attendo.core.model.SubjectGrades
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val GradeDate = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Small "Private" tag: grades stay on this phone unless the user chooses to share them. */
@Composable
internal fun PrivateTag(text: String = "Private") {
    Row(
        Modifier
            .clip(CircleShape)
            .background(AttendoColors.CreamDeep)
            .padding(horizontal = Space.XS, vertical = Space.XXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(Space.XXS))
        Text(text, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

/** Subject page › Grades: total, each assessment (tap to edit) and "Add marks". */
@Composable
internal fun GradesSection(
    grades: SubjectGrades,
    onAdd: (title: String, scored: Double, maxMarks: Double, date: LocalDate?) -> Unit,
    onUpdate: (Assessment) -> Unit,
    onDelete: (Assessment) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Assessment?>(null) }

    Row(Modifier.padding(top = Space.L, bottom = Space.S), verticalAlignment = Alignment.CenterVertically) {
        Text("Grades", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(Space.S))
        PrivateTag()
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { adding = true }) { Text("Add marks", color = AttendoColors.Green) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ContinuousShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(vertical = Space.XS),
    ) {
        val percent = grades.percent
        if (percent == null) {
            Text(
                "No marks yet. Add each assessment as you get it back; only you can see them.",
                style = MaterialTheme.typography.bodyMedium,
                color = AttendoColors.Muted,
                modifier = Modifier.padding(horizontal = Space.M, vertical = Space.S),
            )
        } else {
            Row(Modifier.padding(horizontal = Space.M, vertical = Space.XS), verticalAlignment = Alignment.Bottom) {
                Text("${percent.roundToInt()}%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(Space.XS))
                Text(
                    "${Grades.format(grades.scored)} / ${Grades.format(grades.maxMarks)} marks · grade ${grades.grade}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AttendoColors.Muted,
                    modifier = Modifier.padding(bottom = Space.XXS),
                )
            }
            grades.assessments.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .iosPress(PressStyle.Highlight, onClickLabel = "Edit ${a.title}") { editing = a }
                        .padding(horizontal = Space.M, vertical = Space.S),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.titleSmall)
                        a.date?.let { Text(it.format(GradeDate), style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${Grades.format(a.scored)} / ${Grades.format(a.maxMarks)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${a.percent.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                    }
                }
            }
        }
    }
    Text(
        "Letter grades follow the 10-point S–F scale as a guide; the institute's relative grading decides the final one.",
        style = MaterialTheme.typography.labelSmall,
        color = AttendoColors.Muted,
        modifier = Modifier.padding(top = Space.XS, start = Space.XXS),
    )

    if (adding) {
        AssessmentDialog(
            existing = null,
            onDismiss = { adding = false },
            onSave = { title, scored, max, date -> adding = false; onAdd(title, scored, max, date) },
            onDelete = null,
        )
    }
    editing?.let { a ->
        AssessmentDialog(
            existing = a,
            onDismiss = { editing = null },
            onSave = { title, scored, max, date -> editing = null; onUpdate(a.copy(title = title, scored = scored, maxMarks = max, date = date)) },
            onDelete = { editing = null; onDelete(a) },
        )
    }
}

/** Parses "18", "18.5" or "18,5"; null when not a valid, finite, non-negative number. */
private fun parseMarks(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 && it <= Grades.MAX_MARKS_LIMIT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssessmentDialog(
    existing: Assessment?,
    onDismiss: () -> Unit,
    onSave: (title: String, scored: Double, maxMarks: Double, date: LocalDate?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var scored by rememberSaveable { mutableStateOf(existing?.let { Grades.format(it.scored) }.orEmpty()) }
    var max by rememberSaveable { mutableStateOf(existing?.let { Grades.format(it.maxMarks) } ?: "20") }
    var date by rememberSaveable { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scoredValue = parseMarks(scored)
    val maxValue = parseMarks(max)?.takeIf { it > 0.0 }
    val tooHigh = scoredValue != null && maxValue != null && scoredValue > maxValue
    val valid = title.isNotBlank() && scoredValue != null && maxValue != null && !tooHigh

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add marks" else "Edit marks") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(40) },
                    label = { Text("Assessment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = attendoFieldColors(),
                )
                Spacer(Modifier.height(Space.XS))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.XS), verticalArrangement = Arrangement.spacedBy(Space.XS)) {
                    Grades.SUGGESTED_TITLES.forEach { suggestion ->
                        val selected = title == suggestion
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AttendoColors.Cream else AttendoColors.Ink,
                            modifier = Modifier
                                .clip(CircleShape)
                                .iosPress(PressStyle.Dim, onClickLabel = suggestion) { title = suggestion }
                                .background(if (selected) AttendoColors.Ink else AttendoColors.CreamCard)
                                .border(1.dp, AttendoColors.CreamDeep, CircleShape)
                                .padding(horizontal = Space.S, vertical = Space.XS),
                        )
                    }
                }
                Spacer(Modifier.height(Space.S))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = scored,
                        onValueChange = { scored = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                        label = { Text("Scored") },
                        singleLine = true,
                        isError = scored.isNotEmpty() && (scoredValue == null || tooHigh),
                        modifier = Modifier.weight(1f),
                        shape = ContinuousShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = attendoFieldColors(),
                    )
                    Text(" / ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Space.XS))
                    OutlinedTextField(
                        value = max,
                        onValueChange = { max = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                        label = { Text("Out of") },
                        singleLine = true,
                        isError = max.isNotEmpty() && maxValue == null,
                        modifier = Modifier.weight(1f),
                        shape = ContinuousShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = attendoFieldColors(),
                    )
                }
                if (tooHigh) {
                    Text("Marks scored can't be more than the maximum.", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Pink)
                }
                Spacer(Modifier.height(Space.S))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .iosPress(PressStyle.Dim, onClickLabel = "Change date") { pickDate = true }
                        .background(AttendoColors.CreamCard)
                        .padding(horizontal = Space.S, vertical = Space.XS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Space.XS))
                    Text(date.format(GradeDate), style = MaterialTheme.typography.labelLarge)
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(Space.XS))
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "Tap again to delete" else "Delete", color = AttendoColors.Pink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onSave(title.trim(), scoredValue!!, maxValue!!, date) },
                enabled = valid,
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.Cream,
    )

    if (pickDate) AttendoDatePicker(date, onDismiss = { pickDate = false }) { date = it }
}

