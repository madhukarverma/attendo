package com.attendo.feature.stats

import com.attendo.core.designsystem.motion.PageHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DateRange
import com.attendo.core.designsystem.component.EmptyState
import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space
import com.attendo.core.designsystem.component.ProgressBar
import com.attendo.core.data.GradeRepository
import com.attendo.core.model.Grades
import com.attendo.core.model.SubjectGrades
import com.attendo.core.model.CourseCatalog
import kotlinx.coroutines.flow.flowOf
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.size
import com.attendo.core.designsystem.component.TabBarClearance
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import com.attendo.core.designsystem.component.AnimatedNumber
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.AttendanceSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

data class StatsUiState(
    val summaries: List<AttendanceSummary> = emptyList(),
    val cancelled: Int = 0,
    /** Subjects with at least one assessment, best first. */
    val grades: List<SubjectGrades> = emptyList(),
) {
    val present: Int get() = summaries.sumOf { it.present }
    val absent: Int get() = summaries.sumOf { it.absent }
    val held: Int get() = present + absent
    val overallPercent: Double? get() = if (held == 0) null else present * 100.0 / held
}

class StatsViewModel(subjects: SubjectRepository, attendance: AttendanceRepository, grades: GradeRepository? = null) : ViewModel() {
    val uiState: StateFlow<StatsUiState> =
        combine(subjects.subjects, attendance.records, grades?.assessments ?: flowOf(emptyList())) { list, records, marks ->
            val ids = list.mapTo(HashSet()) { it.id }
            StatsUiState(
                summaries = list.map { AttendanceSummary.from(it, records) }.sortedBy { it.percent },
                cancelled = records.count { it.subjectId in ids && it.status == AttendanceStatus.CANCELLED },
                grades = Grades.bySubject(list, marks).filter { it.percent != null }.sortedByDescending { it.percent },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

@Composable
fun StatsRoute(
    subjects: SubjectRepository,
    attendance: AttendanceRepository,
    onClose: (() -> Unit)?,
    grades: GradeRepository? = null,
) {
    val vm: StatsViewModel = viewModel { StatsViewModel(subjects, attendance, grades) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    StatsScreen(state, onClose)
}

@Composable
fun StatsScreen(state: StatsUiState, onClose: (() -> Unit)?) {
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
    // As a tab it has no back bar, so its title lines up with the other tabs (PageHeader).
    if (onClose != null) CollapsingTopBar(title = "Statistics", collapseFraction = { collapse }, onBack = onClose, backLabel = "Home")
    BounceContainer(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = Space.L),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Space.S),
    ) {
        item { if (onClose != null) LargeTitle("Statistics") else PageHeader("Statistics") }
        item { OverallCard(state) }
        item {
            Text("By subject", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = Space.XS))
        }
        if (state.summaries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = "No statistics yet",
                    message = "Add your subjects and mark a few classes. Each subject's attendance shows up here.",
                )
            }
        }
        items(state.summaries, key = { it.subject.id }) { SubjectBar(it, Modifier.animateItem()) }
        if (state.grades.isNotEmpty()) {
            item(key = "grades") { GradesCard(state.grades, Modifier.animateItem().padding(top = Space.XS)) }
        }
        item { Spacer(Modifier.height(TabBarClearance)) }
    }
    }
    }
}

@Composable
private fun OverallCard(state: StatsUiState) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.Hero)
            .padding(Space.L),
    ) {
        Text("OVERALL", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
        val overall = state.overallPercent
        if (overall == null) {
            Text("–", style = MaterialTheme.typography.displayLarge, color = AttendoColors.Green)
        } else {
            AnimatedNumber(overall.roundToInt(), "%", MaterialTheme.typography.displayLarge, AttendoColors.Green)
        }
        Spacer(Modifier.height(Space.S))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Stat("Present", state.present)
            Stat("Absent", state.absent)
            Stat("No class", state.cancelled)
            Stat("Held", state.held)
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = AttendoColors.OnHero)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

/** Horizontal progress bar with a tick at the subject's target percentage. */
@Composable
private fun SubjectBar(summary: AttendanceSummary, modifier: Modifier = Modifier) {
    // Bars grow from zero on first show, then spring to new values.
    val target = (summary.percent / 100).toFloat().coerceIn(0f, 1f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .padding(Space.M),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(summary.subject.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (summary.held == 0) "–" else "${summary.percent.roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (summary.isOnTrack) AttendoColors.Green else AttendoColors.Pink,
            )
        }
        Spacer(Modifier.height(Space.S))
        ProgressBar(
            fraction = target,
            fill = if (summary.isOnTrack) AttendoColors.Green else AttendoColors.Pink,
            track = AttendoColors.CreamDeep,
            modifier = Modifier.fillMaxWidth().height(12.dp),
            marker = summary.subject.targetPercent / 100f,
        )
        Spacer(Modifier.height(Space.XS))
        Text(
            "${summary.present} present · ${summary.absent} absent · target ${summary.subject.targetPercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
        )
    }
}

/** Private grades overview: overall and per subject (only subjects with marks). */
@Composable
private fun GradesCard(grades: List<SubjectGrades>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .padding(Space.M),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grades", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(AttendoColors.CreamDeep)
                    .padding(horizontal = Space.XS, vertical = Space.XXS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(Space.XXS))
                Text("Only you", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
            }
        }
        Grades.overall(grades)?.let { overall ->
            Text(
                "${overall.roundToInt()}% overall · grade ${Grades.letter(overall)}",
                style = MaterialTheme.typography.labelLarge,
                color = AttendoColors.Muted,
                modifier = Modifier.padding(top = Space.XXS, bottom = Space.XS),
            )
        }
        grades.forEach { g ->
            val percent = g.percent ?: return@forEach
            Row(Modifier.fillMaxWidth().padding(vertical = Space.XS), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(CourseCatalog.shortName(g.subject), style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${g.assessments.size} assessment${if (g.assessments.size == 1) "" else "s"} · ${Grades.format(g.scored)}/${Grades.format(g.maxMarks)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                    )
                }
                Text("${percent.roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(Space.S))
                Text(
                    g.grade.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = AttendoColors.OnAccent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AttendoColors.GreenFill)
                        .padding(horizontal = Space.XS, vertical = Space.XXS),
                )
            }
        }
    }
}
