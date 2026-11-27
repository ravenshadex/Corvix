package com.corvix.player.audio

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.corvix.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Immutable
data class ArtworkCandidate(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String
)

object ArtworkSearchRepository {

    fun customArtFile(context: Context, trackId: Long): File =
        File(File(context.filesDir, "custom_art").apply { mkdirs() }, "track_${trackId}.jpg")

    suspend fun searchCovers(query: String): List<ArtworkCandidate> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val urlStr = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=15"
        val body = runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 7000
                setRequestProperty("User-Agent", "CorvixPlayer/1.0 (Android)")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext emptyList()

        runCatching {
            val arr = JSONObject(body).optJSONArray("results") ?: return@runCatching emptyList()
            val seen = HashSet<String>()
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val raw100 = obj.optString("artworkUrl100", "").trim()
                    if (raw100.isEmpty()) continue
                    val hiResUrl = raw100
                        .replace("100x100bb", "600x600bb")
                        .replace("100x100", "600x600")
                    val album = obj.optString("collectionName", obj.optString("trackName", "Album")).trim()
                    if (hiResUrl in seen) continue
                    seen.add(hiResUrl)
                    add(
                        ArtworkCandidate(
                            id = "${obj.optLong("collectionId", i.toLong())}_$i",
                            title = obj.optString("trackName", "").trim(),
                            artist = obj.optString("artistName", "").trim(),
                            album = album,
                            artworkUrl = hiResUrl
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    suspend fun downloadCustomArt(context: Context, track: Track, imageUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "CorvixPlayer/1.0 (Android)")
                }
                val bytes = try {
                    if (conn.responseCode in 200..299) conn.inputStream.use { it.readBytes() } else null
                } finally {
                    conn.disconnect()
                } ?: return@runCatching false

                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching false
                decoded.recycle()
                customArtFile(context, track.id).writeBytes(bytes)
                AlbumArtLoader.invalidate(track)
                true
            }.getOrDefault(false)
        }

    suspend fun saveLocalCustomArt(context: Context, track: Track, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    if (bytes.isEmpty()) return@runCatching false
                    FileOutputStream(customArtFile(context, track.id)).use { out ->
                        out.write(bytes)
                    }
                    AlbumArtLoader.invalidate(track)
                    true
                } ?: false
            }.getOrDefault(false)
        }
}
