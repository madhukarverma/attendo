package com.attendo.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.component.AttendoCard
import com.attendo.core.designsystem.component.StatusLabel
import com.attendo.core.model.Subject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.SettingsRepository
import com.attendo.core.designsystem.component.BarIconButton
import com.attendo.core.designsystem.component.DayStrip
import com.attendo.core.designsystem.component.PentagonMascot
import com.attendo.core.designsystem.component.PillBottomBar
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.StatsGlyph
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import java.time.LocalDate

@Composable
fun HomeRoute(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    onAddSubject: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel { HomeViewModel(settings, subjects) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onSelectDate = vm::selectDate,
        onAddSubject = onAddSubject,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onAddSubject: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Greeting(state.userName) }
            item {
                DayStrip(
                    days = state.days,
                    selected = state.selectedDate,
                    markedDays = emptySet(),
                    onSelect = onSelectDate,
                )
            }
            item {
                MascotHeader(
                    message = if (state.subjects.isEmpty()) {
                        "Add your subjects and start tracking your attendance!"
                    } else {
                        "Showing up is half the battle. Let's go!"
                    },
                )
            }
            items(state.subjects, key = { it.id }) { subject ->
                SubjectRow(subject, Modifier.padding(horizontal = 20.dp))
            }
            item { Spacer(Modifier.height(96.dp)) }
        }

        PillBottomBar(
            primaryLabel = "New subject",
            onPrimary = onAddSubject,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BarIconButton(onClick = onOpenStats, contentDescription = "Statistics") { StatsGlyph() }
            BarIconButton(onClick = onOpenSettings, contentDescription = "Settings") {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = AttendoColors.Cream)
            }
        }
    }
}

@Composable
internal fun Greeting(name: String) {
    Text(
        buildAnnotatedString {
            append("Hey, ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name.ifBlank { "there" }) }
            append("!")
        },
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
    )
}

@Composable
internal fun MascotHeader(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SpeechBubble(text = message, tail = TailSide.BottomStart, modifier = Modifier.width(230.dp))
        }
        PentagonMascot(Modifier.size(170.dp))
    }
}

/** Rounded square with the subject's initials, standing in for the row icon in the design. */
@Composable
internal fun SubjectBadge(subject: Subject) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AttendoColors.CreamDeep),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            subject.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun SubjectRow(subject: Subject, modifier: Modifier = Modifier) {
    AttendoCard(modifier) {
        SubjectBadge(subject)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(subject.name, style = MaterialTheme.typography.titleMedium)
            StatusLabel(text = subject.code.ifBlank { "New" }, color = AttendoColors.Pink, symbol = "✱")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${subject.targetPercent}%", style = MaterialTheme.typography.titleMedium)
            Text("target", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    AttendoTheme {
        HomeScreen(
            HomeUiState(userName = "Madhukar", subjects = listOf(Subject("1", "Data Structures", "CS201"))),
            {}, {}, {}, {},
        )
    }
}
