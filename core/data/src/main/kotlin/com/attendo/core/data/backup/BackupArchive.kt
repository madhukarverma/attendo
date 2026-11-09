package com.attendo.core.data.backup

import com.attendo.core.data.csv.Csv
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The account backup file: a zip holding the app's CSV files unchanged (plus optional extras such
 * as the launch image) and a small `manifest.csv` (format version, app version, time).
 */
object BackupArchive {
    const val FILE_NAME = "attendo-backup.zip"
    const val FORMAT_VERSION = 1
    private const val MANIFEST = "manifest.csv"
    private const val MAX_ENTRY_BYTES = 20 * 1024 * 1024

    data class Contents(val manifest: Map<String, String>, val files: Map<String, ByteArray>) {
        val createdAt: Long? get() = manifest["created_at"]?.toLongOrNull()
    }

    fun pack(files: Map<String, ByteArray>, appVersion: String, createdAt: Long): ByteArray {
        val manifest = buildString {
            append(Csv.encodeRow(listOf("key", "value"))).append('\n')
            listOf(
                "format" to FORMAT_VERSION.toString(),
                "app_version" to appVersion,
                "created_at" to createdAt.toString(),
            ).forEach { (k, v) -> append(Csv.encodeRow(listOf(k, v))).append('\n') }
        }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            (mapOf(MANIFEST to manifest.toByteArray()) + files).forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** Reads a backup. Only flat entry names are kept; anything with a path is ignored. */
    fun unpack(bytes: ByteArray): Contents {
        val files = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (!entry.isDirectory && '/' !in name && '\\' !in name && !name.startsWith(".")) {
                    files[name] = readLimited(zip)
                }
                zip.closeEntry()
            }
        }
        val manifestText = files.remove(MANIFEST)?.toString(Charsets.UTF_8).orEmpty()
        val manifest = Csv.parse(manifestText).drop(1).filter { it.size >= 2 }.associate { it[0] to it[1] }
        require(manifest.isNotEmpty() || files.isNotEmpty()) { "Not an Attendo backup" }
        return Contents(manifest, files)
    }

    private fun readLimited(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            require(out.size() <= MAX_ENTRY_BYTES) { "Backup entry too large" }
        }
        return out.toByteArray()
    }
}
