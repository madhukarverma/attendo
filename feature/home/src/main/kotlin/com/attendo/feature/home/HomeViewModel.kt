package com.attendo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.AttendanceSummary
import com.attendo.core.model.CourseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A subject row on home: overall summary + the mark for the selected day. */
data class SubjectDayItem(
    val summary: AttendanceSummary,
    val statusOnDay: AttendanceStatus?,
)

data class HomeUiState(
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val items: List<SubjectDayItem> = emptyList(),
    val markedDays: Set<LocalDate> = emptySet(),
) {
    /** Two weeks back and one week ahead of today. */
    val days: List<LocalDate> get() = (-14L..7L).map { today.plusDays(it) }

    /** Overall percentage across all subjects, or null when nothing has been held yet. */
    val overallPercent: Double?
        get() {
            val held = items.sumOf { it.summary.held }
            return if (held == 0) null else items.sumOf { it.summary.present } * 100.0 / held
        }

    val subjectsBelowTarget: Int get() = items.count { !it.summary.isOnTrack }
}

class HomeViewModel(
    settings: SettingsRepository,
    private val subjects: SubjectRepository,
    private val attendance: AttendanceRepository,
) : ViewModel() {

    fun addCourseSubjects() {
        viewModelScope.launch { subjects.addFromCatalog(CourseCatalog.CURRENT_SEMESTER) }
    }

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        settings.settings.filterNotNull(),
        subjects.subjects,
        attendance.records,
        selectedDate,
    ) { s, subjectList, records, date ->
        val onDay = records.filter { it.date == date }.associateBy { it.subjectId }
        HomeUiState(
            userName = s.userName,
            selectedDate = date,
            items = subjectList.map { subject ->
                SubjectDayItem(AttendanceSummary.from(subject, records), onDay[subject.id]?.status)
            },
            markedDays = records.mapTo(HashSet()) { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    /** Tapping the active status again clears it. */
    fun mark(subjectId: String, status: AttendanceStatus) {
        val date = selectedDate.value
        val current = uiState.value.items.firstOrNull { it.summary.subject.id == subjectId }?.statusOnDay
        viewModelScope.launch {
            attendance.mark(subjectId, date, if (current == status) null else status)
        }
    }
}
