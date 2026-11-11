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
    fun formulasAreNeutralisedOnExportAndRestoredOnImport() {
        // As the app writes it: a cell containing quotes is quoted, with the quotes doubled.
        val exported = Csv.neutralizeFormulas("id,title,notes\n1,\"=HYPERLINK(\"\"x\"\")\",- list item\n2,Plain,@me\n")
        assertEquals(
            listOf(listOf("id", "title", "notes"), listOf("1", "'=HYPERLINK(\"x\")", "'- list item"), listOf("2", "Plain", "'@me")),
            Csv.parse(exported),
        )
        assertEquals("=HYPERLINK(\"x\")", Csv.restoreFormula("'=HYPERLINK(\"x\")"))
        assertEquals("'quoted on purpose", Csv.restoreFormula("'quoted on purpose"))
        assertEquals("2026-11-11", Csv.neutralizeFormula("2026-11-11"))
    }

    @Test
    fun interruptedWriteIsRecovered() {
        val dir = Files.createTempDirectory("csv").toFile()
        val table = CsvTable(File(dir, "t.csv"), listOf("id"))
        // A crash between deleting the old file and renaming the new one leaves only the temp file.
        File(dir, "t.csv.tmp").writeText("id\n7\n")
        assertEquals(listOf(mapOf("id" to "7")), table.read())
        assertEquals(true, File(dir, "t.csv").exists())
        dir.deleteRecursively()
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
