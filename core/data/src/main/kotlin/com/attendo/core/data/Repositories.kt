package com.attendo.core.data

import com.attendo.core.model.AnimationMode
import com.attendo.core.model.AppSettings
import com.attendo.core.model.ThemeMode
import com.attendo.core.model.AttendanceRecord
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CatalogSubject
import com.attendo.core.model.PomodoroSession
import com.attendo.core.model.Subject
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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
    suspend fun setTinderMode(enabled: Boolean)
}

interface SubjectRepository {
    val subjects: Flow<List<Subject>>
    suspend fun add(name: String, code: String, targetPercent: Int): Subject

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
