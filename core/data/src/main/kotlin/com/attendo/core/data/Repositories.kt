package com.attendo.core.data

import com.attendo.core.model.AnimationMode
import com.attendo.core.model.AppSettings
import com.attendo.core.model.ThemeMode
import com.attendo.core.model.AttendanceRecord
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CatalogSubject
import com.attendo.core.model.PomodoroSession
import com.attendo.core.model.Subject
import com.attendo.core.model.Todo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/*
 * Repository contracts. Features depend only on these interfaces, so the CSV implementation can
 * be swapped (Room, cloud sync, ...) without touching UI code.
 */

interface SettingsRepository {
    /** Emits `null` while settings are still loading. */
    val settings: Flow<AppSettings?>
    suspend fun setUserName(name: String)
    suspend fun setOnboarded(onboarded: Boolean)
    suspend fun setAnimationMode(mode: AnimationMode)
    suspend fun setTheme(mode: ThemeMode)

    /** Saves custom-theme colours (palette key -> "#RRGGBB"), replacing the given keys. */
    suspend fun setCustomColors(colors: Map<String, String>)

    /** Records whether a launch image is set and the background colour sampled from it. */
    suspend fun setLaunchImage(enabled: Boolean, backgroundHex: String?)
    suspend fun setTinderMode(enabled: Boolean)
}

interface SubjectRepository {
    val subjects: Flow<List<Subject>>
    suspend fun add(name: String, code: String, targetPercent: Int, lectureHours: Int? = null): Subject

    /** Adds catalog subjects whose code isn't present yet, in one write. Returns how many were added. */
    suspend fun addFromCatalog(items: List<CatalogSubject>, targetPercent: Int = Subject.DEFAULT_TARGET_PERCENT): Int
    suspend fun update(subject: Subject)
    suspend fun delete(subjectId: String)
}

interface PomodoroRepository {
    val sessions: Flow<List<PomodoroSession>>
    suspend fun add(session: PomodoroSession)
}

interface AttendanceRepository {
    val records: Flow<List<AttendanceRecord>>

    /** Sets the status for a subject on a date; `null` clears the mark. */
    suspend fun mark(subjectId: String, date: LocalDate, status: AttendanceStatus?)
    suspend fun deleteForSubject(subjectId: String)
}

interface TodoRepository {
    val todos: Flow<List<Todo>>

    /** Adds a to-do on [date]; with [remindAt] it is also a reminder at that time. */
    suspend fun add(title: String, date: LocalDate, remindAt: LocalTime? = null): Todo
    suspend fun update(todo: Todo)
    suspend fun setDone(id: String, done: Boolean)
    suspend fun delete(id: String)

    /** Removes every completed to-do. */
    suspend fun clearDone()
}
