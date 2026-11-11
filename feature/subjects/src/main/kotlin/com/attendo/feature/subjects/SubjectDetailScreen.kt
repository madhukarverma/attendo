package com.attendo.feature.subjects

import com.attendo.core.data.PlannerRepository
import com.attendo.core.data.GradeRepository
import com.attendo.core.model.Assessment
import com.attendo.core.model.Grades
import com.attendo.core.model.SubjectGrades
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import com.attendo.core.model.Curriculum
import com.attendo.core.model.CurriculumCourse
import com.attendo.feature.curriculum.CourseDetails
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.component.AttendoCard
import com.attendo.core.designsystem.component.Confetti
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.component.PercentPill
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.component.TriangleMascot
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AttendanceRecord
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.AttendanceSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class SubjectDetailUiState(
    val summary: AttendanceSummary? = null,
    val history: List<AttendanceRecord> = emptyList(),
    val grades: SubjectGrades? = null,
)

class SubjectDetailViewModel(
    private val subjectId: String,
    private val subjects: SubjectRepository,
    private val attendance: AttendanceRepository,
    private val grades: GradeRepository? = null,
    private val planner: PlannerRepository? = null,
) : ViewModel() {

    val uiState: StateFlow<SubjectDetailUiState> =
        combine(subjects.subjects, attendance.records, grades?.assessments ?: flowOf(emptyList())) { list, records, marks ->
            val subject = list.firstOrNull { it.id == subjectId }
            SubjectDetailUiState(
                summary = subject?.let { AttendanceSummary.from(it, records) },
                history = records.filter { it.subjectId == subjectId }.sortedByDescending { it.date },
                grades = subject?.let { Grades.bySubject(listOf(it), marks).single() },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectDetailUiState())

    fun addMarks(title: String, scored: Double, maxMarks: Double, date: LocalDate?) {
        val repo = grades ?: return
        viewModelScope.launch { repo.add(subjectId, title, scored, maxMarks, date) }
    }

    fun updateMarks(assessment: Assessment) {
        val repo = grades ?: return
        viewModelScope.launch { repo.update(assessment) }
    }

    fun deleteMarks(assessment: Assessment) {
        val repo = grades ?: return
        viewModelScope.launch { repo.delete(assessment.id) }
    }

    /** Sets the course's total lecture hours (1 lecture = 1 hour); `null` clears it. */
    fun setLectureHours(hours: Int?) {
        val subject = uiState.value.summary?.subject ?: return
        viewModelScope.launch { subjects.update(subject.copy(lectureHours = hours?.takeIf { it > 0 })) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            attendance.deleteForSubject(subjectId)
            grades?.deleteForSubject(subjectId)
            planner?.deleteSlotsForSubject(subjectId)
            subjects.delete(subjectId)
            onDeleted()
        }
    }
}

@Composable
fun SubjectDetailRoute(
    subjectId: String,
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    onClose: () -> Unit,
    grades: GradeRepository? = null,
    planner: PlannerRepository? = null,
    curriculum: Curriculum = Curriculum.EMPTY,
    onOpenCurriculum: (code: String) -> Unit = {},
) {
    val vm: SubjectDetailViewModel = viewModel(key = subjectId) {
        SubjectDetailViewModel(subjectId, subjects, attendance, grades, planner)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    SubjectDetailScreen(
        state = state,
        onClose = onClose,
        onDelete = { vm.delete(onClose) },
        onSetLectureHours = vm::setLectureHours,
        syllabus = state.summary?.subject?.code?.let(curriculum::course),
        onOpenCurriculum = onOpenCurriculum,
        onAddMarks = vm::addMarks,
        onUpdateMarks = vm::updateMarks,
        onDeleteMarks = vm::deleteMarks,
    )
}

private val DateFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

@Composable
fun SubjectDetailScreen(
    state: SubjectDetailUiState,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onSetLectureHours: (Int?) -> Unit = {},
    syllabus: CurriculumCourse? = null,
    onOpenCurriculum: (code: String) -> Unit = {},
    onAddMarks: (title: String, scored: Double, maxMarks: Double, date: LocalDate?) -> Unit = { _, _, _, _ -> },
    onUpdateMarks: (Assessment) -> Unit = {},
    onDeleteMarks: (Assessment) -> Unit = {},
) {
    val summary = state.summary ?: run {
        Box(Modifier.fillMaxSize().background(AttendoColors.Cream))
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var editHours by remember { mutableStateOf(false) }
    val subject = summary.subject

    val scroll = rememberScrollState()
    val collapse by rememberCollapseFraction(scroll)
    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
    CollapsingTopBar(title = subject.name, collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
    BounceContainer(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp),
    ) {
        LargeTitle(subject.name)

        Box(Modifier.fillMaxWidth().height(260.dp)) {
            Confetti(Modifier.fillMaxSize(), seed = subject.id.hashCode())
            TriangleMascot(
                Modifier.size(200.dp).align(Alignment.Center),
                mood = if (summary.isOnTrack) Mood.Happy else Mood.Sad,
            )
            SpeechBubble(
                text = when {
                    summary.held == 0 -> "Let's begin!"
                    summary.isOnTrack -> "Good job!"
                    else -> "Keep going!"
                },
                tail = TailSide.BottomStart,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = 30.dp),
            )
        }

        PercentPill(
            if (summary.held == 0) "No classes marked yet" else
                "${summary.percent.roundToInt()}%  ·  ${summary.present} of ${summary.held} classes",
        )

        Text("Helpful tips", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        val target = subject.targetPercent
        TipRow(Icons.Filled.Star, "Target: $target% minimum attendance")
        Spacer(Modifier.height(8.dp))
        TipRow(
            Icons.Filled.Info,
            when {
                summary.held == 0 -> "Mark your first class from the home screen"
                summary.isOnTrack -> "You can miss ${summary.classesCanSkip()} more class(es) and stay at $target%"
                summary.classesNeeded() == Int.MAX_VALUE -> "A 100% target can't be recovered after an absence"
                else -> "Attend the next ${summary.classesNeeded()} class(es) to get back to $target%"
            },
        )

        Text("Course hours", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        CourseHours(summary, onEdit = { editHours = true })

        if (syllabus != null) {
            Row(Modifier.padding(top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Syllabus", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { onOpenCurriculum(syllabus.code) }) { Text("Full curriculum", color = AttendoColors.Green) }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AttendoColors.CreamCard)
                    .padding(16.dp),
            ) {
                Text(
                    "${syllabus.contactHours} contact hours · ${syllabus.type} · ${syllabus.assessment}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
                Spacer(Modifier.height(12.dp))
                CourseDetails(syllabus)
            }
        }

        state.grades?.let { GradesSection(it, onAddMarks, onUpdateMarks, onDeleteMarks) }

        if (state.history.isNotEmpty()) {
            Text("History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            state.history.take(30).forEach { record ->
                val (label, color) = when (record.status) {
                    AttendanceStatus.PRESENT -> "Present" to AttendoColors.Green
                    AttendanceStatus.ABSENT -> "Absent" to AttendoColors.Pink
                    AttendanceStatus.CANCELLED -> "No class" to AttendoColors.Blue
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp)) {
                    Text(record.date.format(DateFormat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (record.count > 1 && record.status != AttendanceStatus.CANCELLED) "$label ×${record.count}" else label,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Finish", onClick = onClose)
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Delete subject", color = AttendoColors.Pink)
        }
        Spacer(Modifier.height(16.dp))
    }
    }
    }

    if (editHours) {
        LectureHoursDialog(
            current = subject.lectureHours,
            onDismiss = { editHours = false },
            onSave = { hours -> editHours = false; onSetLectureHours(hours) },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${subject.name}?") },
            text = { Text("This removes the subject and all of its attendance records.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = AttendoColors.Pink) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = AttendoColors.Cream,
        )
    }
}

@Composable
private fun TipRow(icon: ImageVector, text: String) {
    AttendoCard {
        Icon(icon, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Lectures left in the course (1 lecture = 1 hour, so lecture hours = lectures) and what that
 * means for the target. Prompts for the hours when they aren't set.
 */
@Composable
private fun CourseHours(summary: AttendanceSummary, onEdit: () -> Unit) {
    val hours = summary.subject.lectureHours
    val left = summary.lecturesLeft
    if (hours == null || left == null) {
        AttendoCard(onClick = onEdit) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Set the course's lecture hours", style = MaterialTheme.typography.titleMedium)
                Text(
                    "1 lecture = 1 hour. Shows how many lectures are left.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
            Text("Set", color = AttendoColors.Green, style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    val total = (summary.held + left).coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$left", style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp, lineHeight = 40.sp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (left == 1) "lecture left" else "lectures left",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp).weight(1f),
            )
            TextButton(onClick = onEdit) { Text("Edit", color = AttendoColors.Green) }
        }
        Text(
            "$hours lecture hours · ${summary.held} held so far",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(AttendoColors.CreamDeep),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(summary.held.toFloat() / total)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(AttendoColors.Green),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    val target = summary.subject.targetPercent
    val skips = summary.courseSkipsLeft() ?: 0
    TipRow(
        if (skips >= 0) Icons.Filled.Star else Icons.Filled.Info,
        when {
            left == 0 -> "Course finished at ${summary.percent.roundToInt()}%"
            skips > 0 -> "You can miss $skips of the $left left and still finish at $target%"
            skips == 0 -> "Attend all $left remaining lectures to finish at $target%"
            else -> "Even attending all $left left, you'd finish at ${summary.bestFinalPercent()?.roundToInt() ?: 0}%"
        },
    )
}

@Composable
private fun LectureHoursDialog(current: Int?, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var text by remember { mutableStateOf(current?.toString().orEmpty()) }
    val value = text.toIntOrNull()?.takeIf { it in 1..999 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lecture hours") },
        text = {
            Column {
                Text(
                    "Total lecture hours for this course. One lecture is one hour.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    placeholder = { Text("e.g. 45") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AttendoColors.Green,
                        cursorColor = AttendoColors.Green,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value != null) { Text("Save", color = AttendoColors.Green) }
        },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text("Clear", color = AttendoColors.Pink) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        containerColor = AttendoColors.Cream,
    )
}
