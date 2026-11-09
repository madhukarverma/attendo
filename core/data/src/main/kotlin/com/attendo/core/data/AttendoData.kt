package com.attendo.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Entry point of the data layer. All app data lives as plain CSV files in [dir]:
 *
 * - `settings.csv`   key,value
 * - `subjects.csv`   id,name,code,target_percent,created_at
 * - `attendance.csv` subject_id,date,status,updated_at
 */
class AttendoData(
    val dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher = Dispatchers.IO,
    clock: () -> Long = System::currentTimeMillis,
) {
    val settings: SettingsRepository = CsvSettingsRepository(dir, scope, io)
    val subjects: SubjectRepository = CsvSubjectRepository(dir, scope, io, clock)
    val attendance: AttendanceRepository = CsvAttendanceRepository(dir, scope, io, clock)

    /** The CSV files that currently exist, e.g. for export/sharing. */
    fun csvFiles(): List<File> =
        listOf(SUBJECTS_FILE, ATTENDANCE_FILE, SETTINGS_FILE).map { File(dir, it) }.filter { it.exists() }

    companion object {
        const val SETTINGS_FILE = "settings.csv"
        const val SUBJECTS_FILE = "subjects.csv"
        const val ATTENDANCE_FILE = "attendance.csv"
    }
}
