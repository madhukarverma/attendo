package com.attendo.core.backup

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/** A file in the app's hidden Drive folder. */
internal data class RemoteFile(val id: String, val modifiedAt: Long, val size: Long)

internal class DriveHttpException(val code: Int, body: String) : IOException("Google Drive error $code: ${body.take(200)}")

/**
 * Minimal Google Drive v3 REST client for the hidden app-data folder (scope `drive.appdata`). The
 * user can't see or edit these files in Drive, and other apps can't read them. Blocking: call
 * from an IO dispatcher.
 */
internal class DriveAppDataClient(private val accessToken: String) {

    /** The signed-in user's email and display name. */
    fun about(): Pair<String, String?> {
        val json = JSONObject(get("$API/about?fields=user(displayName,emailAddress)").toString(Charsets.UTF_8))
        val user = json.getJSONObject("user")
        return user.getString("emailAddress") to user.optString("displayName").takeIf { it.isNotBlank() }
    }

    /** Backups named [name], newest first. */
    fun list(name: String): List<RemoteFile> {
        val q = URLEncoder.encode("name = '$name' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$q&orderBy=${URLEncoder.encode("modifiedTime desc", "UTF-8")}" +
            "&fields=${URLEncoder.encode("files(id,modifiedTime,size)", "UTF-8")}"
        val files = JSONObject(get(url).toString(Charsets.UTF_8)).optJSONArray("files") ?: return emptyList()
        return (0 until files.length()).map { i -> files.getJSONObject(i).toRemoteFile() }
    }

    fun download(id: String): ByteArray = get("$API/files/$id?alt=media")

    /** Creates a new file in the app-data folder (multipart upload). */
    fun create(name: String, bytes: ByteArray, mimeType: String): RemoteFile {
        val boundary = "attendo-${System.nanoTime()}"
        val metadata = JSONObject().put("name", name).put("parents", org.json.JSONArray().put("appDataFolder")).toString()
        val body = java.io.ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n".toByteArray())
            write("--$boundary\r\nContent-Type: $mimeType\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val response = request(
            "POST",
            "$UPLOAD/files?uploadType=multipart&fields=${URLEncoder.encode("id,modifiedTime,size", "UTF-8")}",
            body,
            "multipart/related; boundary=$boundary",
        )
        return JSONObject(response.toString(Charsets.UTF_8)).toRemoteFile()
    }

    fun delete(id: String) {
        request("DELETE", "$API/files/$id")
    }

    private fun get(url: String): ByteArray = request("GET", url)

    private fun request(method: String, url: String, body: ByteArray? = null, contentType: String? = null): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                setFixedLengthStreamingMode(body.size)
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) throw DriveHttpException(code, bytes.toString(Charsets.UTF_8))
            return bytes
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toRemoteFile() = RemoteFile(
        id = getString("id"),
        modifiedAt = optString("modifiedTime").takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L,
        size = optString("size").toLongOrNull() ?: 0L,
    )

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
