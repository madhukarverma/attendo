package com.attendo.core.data.backup

import com.attendo.core.data.AttendoData
import com.attendo.core.data.csv.Csv

/**
 * CSV files picked for a restore, each matched to one of the app's tables by its header row (not
 * its file name, so "subjects (1).csv" or a file re-saved from Excel still works).
 */
data class CsvImportPlan(
    val tables: List<Table>,
    /** Display names of files that didn't look like any Attendo table. */
    val unrecognised: List<String>,
) {
    val isEmpty: Boolean get() = tables.isEmpty()

    data class Table(
        /** The app's file this replaces, e.g. "subjects.csv". */
        val file: String,
        /** The picked file's display name. */
        val source: String,
        internal val rows: List<Map<String, String>>,
    ) {
        val label: String get() = CsvSchema.label(file)
        val rowCount: Int get() = rows.size
    }
}

/** Which columns identify each table, and how to name it to the user. */
internal object CsvSchema {
    private val required = linkedMapOf(
        AttendoData.SUBJECTS_FILE to setOf("id", "name", "target_percent"),
        AttendoData.ATTENDANCE_FILE to setOf("subject_id", "date", "status"),
        AttendoData.GRADES_FILE to setOf("id", "subject_id", "scored", "max_marks"),
        AttendoData.TIMETABLE_FILE to setOf("id", "subject_id", "day", "start", "end"),
        // Before to-dos: an events file also has id, title and a date-like column.
        AttendoData.EVENTS_FILE to setOf("id", "title", "start", "kind"),
        AttendoData.POMODORO_FILE to setOf("id", "kind", "planned_seconds"),
        AttendoData.TODOS_FILE to setOf("id", "title", "date"),
        AttendoData.HABITS_FILE to setOf("id", "title", "days"),
        AttendoData.HABIT_LOG_FILE to setOf("habit_id", "date"),
        // Cards before columns: a cards file also has id, title and position.
        AttendoData.KANBAN_CARDS_FILE to setOf("id", "column_id", "title"),
        AttendoData.KANBAN_COLUMNS_FILE to setOf("id", "title", "position"),
        AttendoData.SETTINGS_FILE to setOf("key", "value"),
    )

    fun label(file: String): String = when (file) {
        AttendoData.SUBJECTS_FILE -> "Subjects"
        AttendoData.ATTENDANCE_FILE -> "Attendance marks"
        AttendoData.GRADES_FILE -> "Grades"
        AttendoData.TIMETABLE_FILE -> "Timetable"
        AttendoData.EVENTS_FILE -> "Your calendar events"
        AttendoData.POMODORO_FILE -> "Pomodoro sessions"
        AttendoData.TODOS_FILE -> "Reminders"
        AttendoData.HABITS_FILE -> "Checklist tasks"
        AttendoData.HABIT_LOG_FILE -> "Checklist ticks"
        AttendoData.KANBAN_CARDS_FILE -> "Board cards"
        AttendoData.KANBAN_COLUMNS_FILE -> "Board columns"
        AttendoData.SETTINGS_FILE -> "Settings"
        else -> file
    }

    /** Parses [text] into header-keyed rows; null when the header matches no table. */
    fun read(text: String): Pair<String, List<Map<String, String>>>? {
        val rows = Csv.parse(text.removePrefix("﻿"))
        val header = rows.firstOrNull()?.map { it.trim().lowercase() } ?: return null
        val file = required.entries.firstOrNull { (_, cols) -> header.containsAll(cols) }?.key ?: return null
        val body = rows.drop(1).map { row ->
            header.indices.associate { i -> header[i] to Csv.restoreFormula(row.getOrElse(i) { "" }.trim()) }
        }
        return file to body
    }

    fun plan(files: List<Pair<String, String>>): CsvImportPlan {
        val tables = LinkedHashMap<String, CsvImportPlan.Table>()
        val unrecognised = mutableListOf<String>()
        files.forEach { (name, text) ->
            val parsed = read(text)
            // A second file for the same table is ignored rather than silently mixed in.
            if (parsed == null || parsed.first in tables) {
                unrecognised += name
            } else {
                tables[parsed.first] = CsvImportPlan.Table(parsed.first, name, parsed.second)
            }
        }
        return CsvImportPlan(tables.values.toList(), unrecognised)
    }
}
