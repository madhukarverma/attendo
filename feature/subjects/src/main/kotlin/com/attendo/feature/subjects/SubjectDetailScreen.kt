package com.attendo.feature.subjects

import androidx.compose.foundation.background
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
)

class SubjectDetailViewModel(
    private val subjectId: String,
    private val subjects: SubjectRepository,
    private val attendance: AttendanceRepository,
) : ViewModel() {

    val uiState: StateFlow<SubjectDetailUiState> =
        combine(subjects.subjects, attendance.records) { list, records ->
            val subject = list.firstOrNull { it.id == subjectId }
            SubjectDetailUiState(
                summary = subject?.let { AttendanceSummary.from(it, records) },
                history = records.filter { it.subjectId == subjectId }.sortedByDescending { it.date },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectDetailUiState())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            attendance.deleteForSubject(subjectId)
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
) {
    val vm: SubjectDetailViewModel = viewModel(key = subjectId) {
        SubjectDetailViewModel(subjectId, subjects, attendance)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    SubjectDetailScreen(state = state, onClose = onClose, onDelete = { vm.delete(onClose) })
}

private val DateFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

@Composable
fun SubjectDetailScreen(state: SubjectDetailUiState, onClose: () -> Unit, onDelete: () -> Unit) {
    val summary = state.summary ?: run {
        Box(Modifier.fillMaxSize().background(AttendoColors.Cream))
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val subject = summary.subject

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(subject.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

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
                    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
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
