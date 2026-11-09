package com.attendo.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.attendo.core.data.AttendoData
import java.io.File

/** Copies the CSV files to a shareable cache folder and opens the Android share sheet. */
object CsvExporter {

    fun share(context: Context, data: AttendoData) {
        val files = data.csvFiles()
        if (files.isEmpty()) {
            Toast.makeText(context, "Nothing to export yet", Toast.LENGTH_SHORT).show()
            return
        }
        val exportDir = File(context.cacheDir, "export").apply {
            deleteRecursively()
            mkdirs()
        }
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>(
            files.map { source ->
                val copy = source.copyTo(File(exportDir, source.name), overwrite = true)
                FileProvider.getUriForFile(context, authority, copy)
            },
        )
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "Attendo attendance export")
            clipData = ClipData.newRawUri("", uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Export CSV files"))
    }
}
