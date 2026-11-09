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
    val tinderMode: Boolean = false,
    val canUndoSwipe: Boolean = false,
) {
    /** Cards still to swipe for the selected day. */
    val unmarked: List<SubjectDayItem> get() = items.filter { it.statusOnDay == null }

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
    private val settings: SettingsRepository,
    private val subjects: SubjectRepository,
    private val attendance: AttendanceRepository,
) : ViewModel() {

    /** Swipes made in this session, newest last, for the Tinder-style undo button. */
    private val swipeHistory = MutableStateFlow<List<Pair<String, LocalDate>>>(emptyList())

    fun setTinderMode(enabled: Boolean) {
        viewModelScope.launch { settings.setTinderMode(enabled) }
    }

    /** A card swipe: sets the status directly (no toggle) and remembers it for undo. */
    fun swipe(subjectId: String, status: AttendanceStatus) {
        val date = selectedDate.value
        swipeHistory.value = swipeHistory.value + (subjectId to date)
        viewModelScope.launch { attendance.mark(subjectId, date, status) }
    }

    fun undoSwipe() {
        val last = swipeHistory.value.lastOrNull() ?: return
        swipeHistory.value = swipeHistory.value.dropLast(1)
        selectedDate.value = last.second
        viewModelScope.launch { attendance.mark(last.first, last.second, null) }
    }

    fun addCourseSubjects() {
        viewModelScope.launch { subjects.addFromCatalog(CourseCatalog.CURRENT_SEMESTER) }
    }

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        settings.settings.filterNotNull(),
        subjects.subjects,
        attendance.records,
        selectedDate,
        swipeHistory,
    ) { s, subjectList, records, date, history ->
        val onDay = records.filter { it.date == date }.associateBy { it.subjectId }
        HomeUiState(
            userName = s.userName,
            selectedDate = date,
            items = subjectList.map { subject ->
                SubjectDayItem(AttendanceSummary.from(subject, records), onDay[subject.id]?.status)
            },
            markedDays = records.mapTo(HashSet()) { it.date },
            tinderMode = s.tinderMode,
            canUndoSwipe = history.isNotEmpty(),
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
