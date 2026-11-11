package com.attendo.core.data.csv

/** Minimal RFC 4180 CSV encoder/parser: quoted fields, escaped quotes, newlines inside quotes. */
object Csv {

    fun encodeRow(fields: List<String>): String = fields.joinToString(",") { escape(it) }

    private fun escape(field: String): String {
        val needsQuotes = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + field.replace("\"", "\"\"") + "\"" else field
    }

    /** First characters that make spreadsheet apps treat a cell as a formula (CSV injection). */
    private val FORMULA_START = charArrayOf('=', '+', '-', '@', '\t', '\r')

    /**
     * For files meant to be opened in Excel / Sheets: a cell starting with a formula character gets
     * a leading apostrophe so it shows as text instead of running (OWASP "CSV injection").
     */
    fun neutralizeFormula(field: String): String =
        if (field.isNotEmpty() && field[0] in FORMULA_START) "'$field" else field

    /** Undoes [neutralizeFormula] when such a file is restored. */
    fun restoreFormula(field: String): String =
        if (field.length >= 2 && field[0] == '\'' && field[1] in FORMULA_START) field.substring(1) else field

    /** Re-encodes CSV [text] with every cell passed through [neutralizeFormula]. */
    fun neutralizeFormulas(text: String): String = buildString {
        parse(text).forEach { row -> append(encodeRow(row.map(::neutralizeFormula))).append('\n') }
    }

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString()); field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString()); field.clear()
                        rows.add(row); row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filterNot { it.size == 1 && it[0].isEmpty() }
    }
}
