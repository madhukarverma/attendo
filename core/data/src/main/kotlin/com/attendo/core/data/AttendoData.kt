package com.attendo.core.data

import com.attendo.core.data.backup.CsvImportPlan
import com.attendo.core.data.backup.CsvSchema
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.pomodoro.CsvPomodoroRepository
import com.attendo.core.data.todo.CsvTodoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Entry point of the data layer. All app data lives as plain CSV files in [dir]:
 *
 * - `settings.csv`   key,value
 * - `subjects.csv`   id,name,code,target_percent,created_at,lecture_hours
 * - `attendance.csv` subject_id,date,status,updated_at,count
 * - `pomodoro.csv`   id,date,started_at,kind,subject_id,planned_seconds,actual_seconds,completed
 * - `todos.csv`      id,title,date,remind_at,done,created_at
 */
class AttendoData(
    val dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher = Dispatchers.IO,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val settingsCsv = CsvSettingsRepository(dir, scope, io)
    private val subjectsCsv = CsvSubjectRepository(dir, scope, io, clock)
    private val attendanceCsv = CsvAttendanceRepository(dir, scope, io, clock)
    private val pomodoroCsv = CsvPomodoroRepository(dir, scope, io)
    private val todosCsv = CsvTodoRepository(dir, scope, io, clock)

    val settings: SettingsRepository = settingsCsv
    val subjects: SubjectRepository = subjectsCsv
    val attendance: AttendanceRepository = attendanceCsv
    val pomodoro: PomodoroRepository = pomodoroCsv
    val todos: TodoRepository = todosCsv

    /** Every table by file name, for backup and restore. */
    private val stores: Map<String, CsvStore<*>>
        get() = mapOf(
            SETTINGS_FILE to settingsCsv.store,
            SUBJECTS_FILE to subjectsCsv.store,
            ATTENDANCE_FILE to attendanceCsv.store,
            POMODORO_FILE to pomodoroCsv.store,
            TODOS_FILE to todosCsv.store,
        )

    /** Matches picked CSV files (display name to text) to tables; nothing is changed yet. */
    fun planCsvImport(files: List<Pair<String, String>>): CsvImportPlan = CsvSchema.plan(files)

    /** Replaces each planned table with the imported rows. Returns rows kept per file. */
    suspend fun applyCsvImport(plan: CsvImportPlan): Map<String, Int> =
        plan.tables.associate { t -> t.file to (stores[t.file]?.replaceRows(t.rows) ?: 0) }

    /** CSV text of every table file that exists on disk, by file name (for backups). */
    fun snapshot(): Map<String, String> =
        csvFiles().associate { it.name to it.readText(Charsets.UTF_8) }

    /**
     * Full restore from a backup snapshot: every table is replaced, and tables missing from
     * [files] are emptied, so the phone ends up exactly like the backup. Returns rows per file.
     */
    suspend fun restoreSnapshot(files: Map<String, String>): Map<String, Int> =
        stores.mapValues { (name, store) ->
            val rows = files[name]?.let { CsvSchema.read(it)?.second }.orEmpty()
            store.replaceRows(rows)
        }

    /** True when there's nothing worth keeping yet (no subjects, marks, sessions or to-dos). */
    fun hasNoUserData(): Boolean =
        listOf(SUBJECTS_FILE, ATTENDANCE_FILE, POMODORO_FILE, TODOS_FILE).all { name ->
            (stores[name]?.items?.value?.isEmpty()) ?: true
        }

    /** The CSV files that currently exist, e.g. for export/sharing. */
    fun csvFiles(names: List<String> = ALL_FILES): List<File> =
        names.map { File(dir, it) }.filter { it.exists() }

    companion object {
        const val POMODORO_FILE = "pomodoro.csv"
        const val TODOS_FILE = "todos.csv"
        val ALL_FILES get() = listOf(SUBJECTS_FILE, ATTENDANCE_FILE, POMODORO_FILE, TODOS_FILE, SETTINGS_FILE)
        const val SETTINGS_FILE = "settings.csv"
        const val SUBJECTS_FILE = "subjects.csv"
        const val ATTENDANCE_FILE = "attendance.csv"
    }
}
