package com.attendo.core.data.csv

import java.io.File

/**
 * One CSV file with a header row. Rows are exposed as column-name maps so new columns can be
 * added later without breaking old files (missing columns read as "").
 *
 * Writes are atomic: data goes to a temp file which then replaces the original.
 */
class CsvTable(val file: File, val columns: List<String>) {

    fun read(): List<Map<String, String>> {
        if (!file.exists()) return emptyList()
        val rows = Csv.parse(file.readText(Charsets.UTF_8))
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1).map { row ->
            header.indices.associate { i -> header[i] to row.getOrElse(i) { "" } }
        }
    }

    fun write(rows: List<Map<String, String>>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(
            buildString {
                append(Csv.encodeRow(columns)).append('\n')
                rows.forEach { r -> append(Csv.encodeRow(columns.map { r[it].orEmpty() })).append('\n') }
            },
            Charsets.UTF_8,
        )
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not replace ${file.name}" }
        }
    }
}
