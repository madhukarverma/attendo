package com.attendo.core.data

import com.attendo.core.data.csv.Csv
import com.attendo.core.data.csv.CsvTable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CsvTest {

    @Test
    fun roundTripsSpecialCharacters() {
        val row = listOf("plain", "with,comma", "with \"quote\"", "multi\nline", "")
        val parsed = Csv.parse(Csv.encodeRow(row) + "\n")
        assertEquals(listOf(row), parsed)
    }

    @Test
    fun tableReadsByColumnName() {
        val dir = Files.createTempDirectory("csv").toFile()
        val table = CsvTable(File(dir, "t.csv"), listOf("id", "name"))
        table.write(listOf(mapOf("id" to "1", "name" to "Maths, II")))
        assertEquals(listOf(mapOf("id" to "1", "name" to "Maths, II")), table.read())

        // A newer schema with an extra column still reads old files.
        val newer = CsvTable(File(dir, "t.csv"), listOf("id", "name", "code"))
        assertEquals("", newer.read().single()["code"].orEmpty())
        dir.deleteRecursively()
    }
}
