package com.corvix.player.audio

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches accurate high-resolution artist portrait photos online
 * (track-verified Deezer lookup -> exact-name Deezer artist -> Wikipedia music bio fallback)
 * and caches decoded ImageBitmaps in memory.
 */
object ArtistPhotoRepository {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    suspend fun fetchArtistPhoto(
        rawArtistName: String,
        sampleTrackTitle: String = ""
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val primary = cleanPrimaryArtist(rawArtistName)
        if (primary.isBlank() || primary.equals("Unknown Artist", ignoreCase = true)) return@withContext null

        val key = primary.lowercase()
        cache[key]?.let { return@withContext it }
        if (misses.contains(key)) return@withContext null

        val url = queryDeezerByTrackAndArtist(primary, sampleTrackTitle)
            ?: queryDeezerExactArtist(primary)
            ?: queryWikipediaMusician(primary)

        if (url.isNullOrBlank()) {
            misses.add(key)
            return@withContext null
        }

        val bmp = downloadImageBitmap(url)
        if (bmp != null) {
            cache[key] = bmp
            bmp
        } else {
            misses.add(key)
            null
        }
    }

    private fun cleanPrimaryArtist(raw: String): String {
        return raw
            .split(",", "&", "feat.", "Feat.", "ft.", "Ft.", " x ", " X ", "/")
            .firstOrNull()
            ?.trim()
            ?: raw.trim()
    }

    private fun queryDeezerByTrackAndArtist(artist: String, trackTitle: String): String? {
        if (trackTitle.isBlank()) return null
        val cleanTitle = trackTitle.substringBefore("(").substringBefore("-").trim()
        if (cleanTitle.length < 2) return null
        return runCatching {
            val q = URLEncoder.encode("artist:\"$artist\" track:\"$cleanTitle\"", "UTF-8")
            val jsonText = httpGet("https://api.deezer.com/search?q=$q&limit=3") ?: return null
            val data = JSONObject(jsonText).optJSONArray("data") ?: return null
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val artistObj = item.optJSONObject("artist") ?: continue
                val name = artistObj.optString("name", "")
                if (name.equals(artist, ignoreCase = true) ||
                    name.contains(artist, ignoreCase = true) ||
                    artist.contains(name, ignoreCase = true)
                ) {
                    val pic = artistObj.optString("picture_xl").takeIf { it.startsWith("http") }
                        ?: artistObj.optString("picture_big").takeIf { it.startsWith("http") }
                    if (pic != null && !pic.contains("/images/artist//")) return pic
                }
            }
            null
        }.getOrNull()
    }

    private fun queryDeezerExactArtist(artist: String): String? = runCatching {
        val q = URLEncoder.encode(artist, "UTF-8")
        val jsonText = httpGet("https://api.deezer.com/search/artist?q=$q&limit=5") ?: return null
        val data = JSONObject(jsonText).optJSONArray("data") ?: return null
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val name = item.optString("name", "")
            if (name.equals(artist, ignoreCase = true)) {
                val pic = item.optString("picture_xl").takeIf { it.startsWith("http") }
                    ?: item.optString("picture_big").takeIf { it.startsWith("http") }
                if (pic != null && !pic.contains("/images/artist//")) return pic
            }
        }
        null
    }.getOrNull()

    private fun queryWikipediaMusician(artist: String): String? = runCatching {
        val slug = URLEncoder.encode(artist.replace(' ', '_'), "UTF-8")
        val jsonText = httpGet("https://en.wikipedia.org/api/rest_v1/page/summary/$slug") ?: return null
        val root = JSONObject(jsonText)
        if (root.optString("type") != "standard") return null
        val desc = (root.optString("description") + " " + root.optString("extract")).lowercase()
        val isMusicBio = listOf("singer", "musician", "composer", "rapper", "songwriter", "band", "dj", "producer", "playback")
            .any { it in desc }
        if (!isMusicBio) return null
        root.optJSONObject("originalimage")?.optString("source")?.takeIf { it.startsWith("http") }
            ?: root.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.startsWith("http") }
    }.getOrNull()

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "CorvixAudioPlayer/1.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadImageBitmap(urlStr: String): ImageBitmap? {
        var conn: HttpURLConnection? = null
        return runCatching {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "CorvixAudioPlayer/1.0")
            }
            if (conn!!.responseCode in 200..299) {
                conn!!.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } else {
                null
            }
        }.getOrNull().also {
            conn?.disconnect()
        }
    }
}
