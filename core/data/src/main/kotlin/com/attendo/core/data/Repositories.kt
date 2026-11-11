package com.attendo.core.data

import com.attendo.core.model.Habit
import com.attendo.core.model.HabitCheck
import java.time.DayOfWeek
import com.attendo.core.model.CardPriority
import com.attendo.core.model.KanbanBoard
import com.attendo.core.model.KanbanCard
import com.attendo.core.model.KanbanColumn
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

    /** Sets the status for a subject on a date; `null` clears the mark. [count] = 2 for a double class. */
    suspend fun mark(subjectId: String, date: LocalDate, status: AttendanceStatus?, count: Int = 1)
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

interface KanbanRepository {
    val board: Flow<KanbanBoard>

    /** Creates the starting columns ("To do", "Doing", "Done") when the board has none. */
    suspend fun ensureDefaultColumns()
    suspend fun addColumn(title: String): KanbanColumn
    suspend fun renameColumn(columnId: String, title: String)

    /** Marks a column as holding finished cards (they stop counting as open or overdue). */
    suspend fun setColumnDone(columnId: String, done: Boolean)
    suspend fun moveColumn(columnId: String, toIndex: Int)

    /** Deletes a column and every card in it. */
    suspend fun deleteColumn(columnId: String)

    suspend fun addCard(
        columnId: String,
        title: String,
        notes: String = "",
        due: LocalDate? = null,
        priority: CardPriority = CardPriority.NONE,
    ): KanbanCard

    /** Saves a card's title, notes, due date and priority (not its place on the board). */
    suspend fun updateCard(card: KanbanCard)

    /** Moves a card to [toIndex] in [toColumnId] (also used to reorder within a column). */
    suspend fun moveCard(cardId: String, toColumnId: String, toIndex: Int)
    suspend fun deleteCard(cardId: String)
}

/** Weekday checklist ("habit tracker"): tasks that repeat on chosen weekdays, ticked per day. */
interface HabitRepository {
    val habits: Flow<List<Habit>>
    val checks: Flow<List<HabitCheck>>
    suspend fun add(title: String, days: Set<DayOfWeek>): Habit
    suspend fun update(habit: Habit)

    /** Deletes a habit and its ticks. */
    suspend fun delete(habitId: String)
    suspend fun setChecked(habitId: String, date: LocalDate, checked: Boolean)
}
