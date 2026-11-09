package com.attendo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val subjects: List<Subject> = emptyList(),
) {
    /** Two weeks back and one week ahead of today. */
    val days: List<LocalDate> get() = (-14L..7L).map { today.plusDays(it) }
}

class HomeViewModel(
    settings: SettingsRepository,
    subjects: SubjectRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        settings.settings.filterNotNull(),
        subjects.subjects,
        selectedDate,
    ) { s, subjectList, date ->
        HomeUiState(userName = s.userName, selectedDate = date, subjects = subjectList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }
}
