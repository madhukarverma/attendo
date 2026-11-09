package com.attendo.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads CSV files the user picked (system file picker) as display name + text, for restore. */
object CsvImporter {

    /** Larger than any real export; protects against picking a huge unrelated file. */
    private const val MAX_BYTES = 10 * 1024 * 1024

    suspend fun read(context: Context, uris: List<Uri>): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            val name = displayName(context, uri) ?: uri.lastPathSegment ?: "file"
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = input.readNBytesCompat(MAX_BYTES + 1)
                if (buffer.size > MAX_BYTES) null else buffer
            }
            bytes?.let { name to it.toString(Charsets.UTF_8) }
        }
    }

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val n = read(chunk, 0, minOf(chunk.size, limit - out.size()))
            if (n < 0) break
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }
}
