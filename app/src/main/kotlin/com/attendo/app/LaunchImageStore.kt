package com.attendo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The launch-screen image the user picks from their gallery. It's downscaled and kept in app-private
 * storage (outside the exported CSV folder). The colour of its edges is sampled so the launch screen
 * background can match it exactly.
 */
object LaunchImageStore {

    private const val MAX_SIDE = 1600

    const val FILE_NAME = "launch_image.jpg"

    fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** Copies [uri] into app storage; returns the image's edge colour as `#RRGGBB`, or null on failure. */
    suspend fun save(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            file(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            edgeColor(bitmap).also { bitmap.recycle() }
        }.getOrNull()
    }

    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        val f = file(context)
        if (!f.exists()) null else BitmapFactory.decodeFile(f.path)?.asImageBitmap()
    }

    fun delete(context: Context) {
        file(context).delete()
    }

    /** Average colour of the outermost pixels (all four edges). */
    private fun edgeColor(b: Bitmap): String {
        var r = 0L
        var g = 0L
        var bl = 0L
        var n = 0
        fun add(x: Int, y: Int) {
            val c = b.getPixel(x, y)
            r += Color.red(c); g += Color.green(c); bl += Color.blue(c); n++
        }
        val stepX = maxOf(1, b.width / 200)
        val stepY = maxOf(1, b.height / 200)
        for (x in 0 until b.width step stepX) { add(x, 0); add(x, b.height - 1) }
        for (y in 0 until b.height step stepY) { add(0, y); add(b.width - 1, y) }
        return "#%02X%02X%02X".format(r / n, g / n, bl / n)
    }
}
