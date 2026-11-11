package com.attendo.app

import com.attendo.core.data.csv.Csv
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.attendo.core.data.AttendoData
import java.io.File

/**
 * Copies the CSV files to a shareable cache folder and opens the Android share sheet. The copies
 * have spreadsheet formulas neutralised (cells starting with = + - @ get a leading apostrophe), so
 * opening an export in Excel can't run anything; restoring the file removes the apostrophes again.
 */
object CsvExporter {

    fun share(context: Context, data: AttendoData, only: List<String> = AttendoData.ALL_FILES) {
        val files = data.csvFiles(only)
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
                val copy = File(exportDir, source.name)
                copy.writeText(Csv.neutralizeFormulas(source.readText(Charsets.UTF_8)), Charsets.UTF_8)
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
