package com.attendo.feature.attendance

import com.attendo.core.data.PlannerRepository
import com.attendo.core.model.Timetable
import com.attendo.core.model.TimetableSlot
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.AttendanceRepository
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CalendarEvent
import com.attendo.core.model.DayKind
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
    /** 2 when the selected day's mark is a double class. */
    val countOnDay: Int = 1,
    /** This subject's classes on the selected day's timetable, earliest first. */
    val slots: List<TimetableSlot> = emptyList(),
)

data class AttendanceUiState(
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val items: List<SubjectDayItem> = emptyList(),
    val markedDays: Set<LocalDate> = emptySet(),
    val tinderMode: Boolean = false,
    val canUndoSwipe: Boolean = false,
    val calendar: AcademicCalendar = AcademicCalendar.EMPTY,
    /** The student has entered a weekly timetable. */
    val hasTimetable: Boolean = false,
) {
    /** Subjects on the selected day's timetable, in class order (every subject without a timetable). */
    val scheduled: List<SubjectDayItem>
        get() = if (!hasTimetable) items else items.filter { it.slots.isNotEmpty() }.sortedBy { it.slots.first().start }

    /** Subjects not on the selected day's timetable (empty without a timetable). */
    val others: List<SubjectDayItem> get() = if (!hasTimetable) emptyList() else items.filter { it.slots.isEmpty() }

    /** What Tinder mode swipes through: the day's scheduled subjects, or all when none are scheduled. */
    val deck: List<SubjectDayItem> get() = scheduled.ifEmpty { items }

    /** Cards still to swipe for the selected day. */
    val unmarked: List<SubjectDayItem> get() = deck.filter { it.statusOnDay == null }

    /** The whole semester from the academic calendar (falls back to ±2 weeks around today). */
    val days: List<LocalDate>
        get() = if (calendar.events.isEmpty()) (-14L..7L).map { today.plusDays(it) } else calendar.stripDays(today)

    /** Most important calendar entry kind per day, for the coloured dots in the day strip. */
    val dayKinds: Map<LocalDate, DayKind>
        get() = days.mapNotNull { d ->
            calendar.eventsOn(d).minByOrNull { it.kind.ordinal.let { o -> if (it.kind == DayKind.HOLIDAY) -1 else o } }
                ?.let { d to it.kind }
        }.toMap()

    /** Calendar entries on the selected day. */
    val selectedDayEvents: List<CalendarEvent> get() = calendar.eventsOn(selectedDate)

    /** Overall percentage across all subjects, or null when nothing has been held yet. */
    val overallPercent: Double?
        get() {
            val held = items.sumOf { it.summary.held }
            return if (held == 0) null else items.sumOf { it.summary.present } * 100.0 / held
        }

    val subjectsBelowTarget: Int get() = items.count { !it.summary.isOnTrack }
}

class AttendanceViewModel(
    private val settings: SettingsRepository,
    private val subjects: SubjectRepository,
    private val attendance: AttendanceRepository,
    private val calendar: AcademicCalendar = AcademicCalendar.EMPTY,
    planner: PlannerRepository? = null,
) : ViewModel() {

    /** Official calendar plus the student's own events, and the weekly timetable. */
    private val plan = combine(
        planner?.calendar(calendar) ?: flowOf(calendar),
        planner?.slots ?: flowOf(emptyList()),
    ) { cal, slots -> cal to slots }

    /** Swipes made in this session, newest last, for the Tinder-style undo button. */
    private val swipeHistory = MutableStateFlow<List<Pair<String, LocalDate>>>(emptyList())

    fun setTinderMode(enabled: Boolean) {
        viewModelScope.launch { settings.setTinderMode(enabled) }
    }

    /** A card swipe: sets the status directly (no toggle) and remembers it for undo. */
    fun swipe(subjectId: String, status: AttendanceStatus, count: Int = 1) {
        val date = selectedDate.value
        swipeHistory.value = swipeHistory.value + (subjectId to date)
        viewModelScope.launch { attendance.mark(subjectId, date, status, count) }
    }

    /** Switches the selected day's present/absent mark between one class and a double class. */
    fun toggleDouble(subjectId: String) {
        val item = uiState.value.items.firstOrNull { it.summary.subject.id == subjectId } ?: return
        val status = item.statusOnDay?.takeIf { it != AttendanceStatus.CANCELLED } ?: return
        val date = selectedDate.value
        viewModelScope.launch { attendance.mark(subjectId, date, status, if (item.countOnDay >= 2) 1 else 2) }
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

    val uiState: StateFlow<AttendanceUiState> = combine(
        settings.settings.filterNotNull(),
        subjects.subjects,
        attendance.records,
        selectedDate,
        combine(swipeHistory, plan) { history, p -> history to p },
    ) { s, subjectList, records, date, (history, p) ->
        val (cal, slots) = p
        val onDay = records.filter { it.date == date }.associateBy { it.subjectId }
        val slotsToday = Timetable.bySubjectOn(slots, date)
        AttendanceUiState(
            userName = s.nickname,
            selectedDate = date,
            items = subjectList.map { subject ->
                SubjectDayItem(
                    AttendanceSummary.from(subject, records),
                    onDay[subject.id]?.status,
                    onDay[subject.id]?.count ?: 1,
                    slotsToday[subject.id].orEmpty(),
                )
            },
            markedDays = records.mapTo(HashSet()) { it.date },
            tinderMode = s.tinderMode,
            canUndoSwipe = history.isNotEmpty(),
            calendar = cal,
            hasTimetable = slots.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttendanceUiState())

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
