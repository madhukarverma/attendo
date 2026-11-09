package com.attendo.feature.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.PomodoroRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.model.PomodoroSession
import com.attendo.core.model.PomodoroStats
import com.attendo.core.model.PomodoroSummary
import com.attendo.core.model.Subject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class PomodoroStatsUiState(
    val summary: PomodoroSummary? = null,
    val recent: List<Pair<PomodoroSession, String>> = emptyList(),
)

class PomodoroViewModel(
    val timer: PomodoroTimer,
    pomodoro: PomodoroRepository,
    subjects: SubjectRepository,
) : ViewModel() {

    val timerState = timer.state

    val subjects: StateFlow<List<Subject>> =
        subjects.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<PomodoroStatsUiState> = combine(pomodoro.sessions, subjects.subjects) { sessions, list ->
        val names = list.associate { it.id to it.name }
        PomodoroStatsUiState(
            summary = PomodoroStats.summarize(sessions, list, LocalDate.now()),
            recent = sessions.sortedByDescending { it.startedAt }.take(15).map { s ->
                s to (s.subjectId?.let(names::get) ?: "General")
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PomodoroStatsUiState())
}
