package com.attendo.core.data

import com.attendo.core.data.backup.BackupArchive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {

    @Test
    fun packAndUnpackKeepFilesAndManifest() {
        val files = mapOf(
            "subjects.csv" to "id,name\n1,\"Maths, II\"\n".toByteArray(),
            "launch_image.jpg" to byteArrayOf(1, 2, 3, 0, -1),
        )
        val contents = BackupArchive.unpack(BackupArchive.pack(files, appVersion = "0.21.0", createdAt = 42L))
        assertEquals(setOf("subjects.csv", "launch_image.jpg"), contents.files.keys)
        assertArrayEquals(files.getValue("launch_image.jpg"), contents.files.getValue("launch_image.jpg"))
        assertEquals("0.21.0", contents.manifest["app_version"])
        assertEquals(42L, contents.createdAt)
    }

    @Test
    fun entriesWithPathsAreIgnored() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            listOf("../evil.csv", "dir/x.csv", "todos.csv").forEach {
                zip.putNextEntry(ZipEntry(it))
                zip.write("id,title,date\n".toByteArray())
                zip.closeEntry()
            }
        }
        assertEquals(setOf("todos.csv"), BackupArchive.unpack(out.toByteArray()).files.keys)
    }
}
