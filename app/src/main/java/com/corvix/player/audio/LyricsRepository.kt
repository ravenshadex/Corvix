package com.corvix.player.audio

import android.content.Context
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import com.corvix.player.model.LyricLine
import com.corvix.player.model.ScriptTransliterator
import com.corvix.player.model.Track
import com.corvix.player.model.parseRawLrcLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Immutable
data class LyricsCandidate(
    val id: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSec: Int,
    val rawText: String,
    val isSynced: Boolean,
    val hasNonLatinScript: Boolean,
    val previewText: String,
    val sourceLabel: String
)

data class LyricsLookupResult(
    val lines: List<LyricLine>,
    val sourceLabel: String,
    val errorMessage: String? = null
)

/**
 * Resolves local .lrc files, offline cached lyrics, and searchable multi-version
 * LRCLIB / Lyrics.ovh candidates with English/Romanized transliteration support.
 */
object LyricsRepository {

    private val parensSuffixRegex = Regex("\\s*[\\(\\[](From|Feat\\.|Ft\\.|Original|Deluxe|Remastered|Official|Version)[^\\)\\]]*[\\)\\]]", RegexOption.IGNORE_CASE)
    private val dashSuffixRegex = Regex("\\s*-\\s*(From|Red Bull|Original|Single|Remix|Version).*$", RegexOption.IGNORE_CASE)

    fun parseWithLanguageOption(rawText: String, englishOnly: Boolean): List<LyricLine> {
        val normalized = if (englishOnly && ScriptTransliterator.hasIndicNonLatin(rawText)) {
            rawText.lineSequence()
                .map { ScriptTransliterator.transliterateToEnglish(it) }
                .joinToString("\n")
        } else {
            rawText
        }
        return parseRawLrcLyrics(normalized)
    }

    suspend fun findLocalOrCachedLyrics(
        context: Context,
        track: Track,
        englishOnly: Boolean = false
    ): Pair<List<LyricLine>, String>? = withContext(Dispatchers.IO) {
        val cacheFile = File(File(context.filesDir, "lyrics_cache"), "track_${track.id}.lrc")
        if (cacheFile.exists() && cacheFile.length() > 8L) {
            val raw = cacheFile.readText()
            val parsed = parseWithLanguageOption(raw, englishOnly)
            if (parsed.isNotEmpty()) return@withContext parsed to "Offline Cache"
        }

        if (track.isDeviceFile) {
            resolveMediaFilePath(context, track.id)?.let { audioPath ->
                val audioFile = File(audioPath)
                val base = audioFile.nameWithoutExtension
                val parent = audioFile.parentFile
                if (parent != null) {
                    val found = listOf(File(parent, "$base.lrc"), File(parent, "$base.txt"))
                        .firstOrNull { it.exists() && it.canRead() && it.length() > 8L }
                    if (found != null) {
                        val parsed = parseWithLanguageOption(found.readText(), englishOnly)
                        if (parsed.isNotEmpty()) return@withContext parsed to "Local .lrc"
                    }
                }
            }
        }
        null
    }

    fun cacheLyricsLocally(context: Context, trackId: Long, rawOrFormatted: String) {
        runCatching {
            val dir = File(context.filesDir, "lyrics_cache").apply { mkdirs() }
            File(dir, "track_${trackId}.lrc").writeText(rawOrFormatted)
        }
    }

    fun clearCachedLyrics(context: Context, trackId: Long) {
        runCatching {
            File(File(context.filesDir, "lyrics_cache"), "track_${trackId}.lrc").delete()
        }
    }

    suspend fun searchCandidates(
        track: Track,
        customQuery: String = "",
        preferEnglishScript: Boolean = true
    ): List<LyricsCandidate> = withContext(Dispatchers.IO) {
        val cleanTitle = if (customQuery.isNotBlank()) customQuery.trim() else cleanTrackQuery(track.title)
        val primaryArtist = cleanArtistQuery(track.artist)
        val results = mutableListOf<LyricsCandidate>()
        val seenRaw = HashSet<String>()

        fun addCandidateList(list: List<LyricsCandidate>) {
            for (c in list) {
                val sig = "${c.trackName.lowercase()}|${c.artistName.lowercase()}|${c.isSynced}|${c.hasNonLatinScript}"
                if (sig !in seenRaw) {
                    seenRaw.add(sig)
                    results += c
                }
            }
        }

        if (customQuery.isNotBlank()) {
            addCandidateList(queryLrclibCandidates(cleanTitle, ""))
        } else {
            queryLrclibExactCandidate(cleanTitle, primaryArtist, cleanTrackQuery(track.album), track.durationSec)?.let {
                addCandidateList(listOf(it))
            }
            addCandidateList(queryLrclibCandidates(cleanTitle, primaryArtist))
            if (results.size < 3 && primaryArtist.isNotBlank()) {
                addCandidateList(queryLrclibCandidates(cleanTitle, ""))
            }
            if (results.isEmpty()) {
                queryLyricsOvh(primaryArtist, cleanTitle)?.let { plain ->
                    val nonLatin = ScriptTransliterator.hasIndicNonLatin(plain)
                    addCandidateList(
                        listOf(
                            LyricsCandidate(
                                id = "ovh_${track.id}",
                                trackName = cleanTitle,
                                artistName = primaryArtist,
                                albumName = track.album,
                                durationSec = track.durationSec,
                                rawText = plain,
                                isSynced = false,
                                hasNonLatinScript = nonLatin,
                                previewText = plain.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                                sourceLabel = "Lyrics.ovh"
                            )
                        )
                    )
                }
            }
        }

        results.sortedWith(
            compareByDescending<LyricsCandidate> {
                if (preferEnglishScript) !it.hasNonLatinScript else true
            }.thenByDescending { it.isSynced }
        )
    }

    suspend fun fetchLiveLyrics(
        context: Context,
        track: Track,
        preferredSite: String,
        englishOnly: Boolean = true
    ): LyricsLookupResult = withContext(Dispatchers.IO) {
        val candidates = searchCandidates(track, customQuery = "", preferEnglishScript = englishOnly)
        val best = candidates.firstOrNull()
        if (best != null) {
            cacheLyricsLocally(context, track.id, best.rawText)
            val lines = parseWithLanguageOption(best.rawText, englishOnly)
            val srcTag = if (englishOnly && best.hasNonLatinScript) "${best.sourceLabel} (Romanized)" else best.sourceLabel
            return@withContext LyricsLookupResult(lines = lines, sourceLabel = srcTag)
        }

        LyricsLookupResult(
            lines = emptyList(),
            sourceLabel = preferredSite,
            errorMessage = "No online lyrics match found. Try editing search text or selecting manually."
        )
    }

    private fun resolveMediaFilePath(context: Context, id: Long): String? = runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    fun cleanTrackQuery(raw: String): String {
        var s = parensSuffixRegex.replace(raw, " ")
        s = dashSuffixRegex.replace(s, " ")
        return s.replace(Regex("\\s+"), " ").trim().ifEmpty { raw.trim() }
    }

    fun cleanArtistQuery(raw: String): String =
        raw.split(',', '&', ';', '/')
            .firstOrNull()
            ?.replace(Regex("\\(.*?\\)"), "")
            ?.trim()
            .orEmpty()
            .ifEmpty { raw.trim() }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun httpGet(urlStr: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6500
            readTimeout = 7000
            setRequestProperty("User-Agent", "CorvixPlayer/1.0 (Android)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun parseCandidateObject(obj: JSONObject, indexHint: Int): LyricsCandidate? {
        val synced = obj.optString("syncedLyrics", "").trim().takeIf { it.isNotEmpty() && it != "null" }
        val plain = obj.optString("plainLyrics", "").trim().takeIf { it.isNotEmpty() && it != "null" }
        val raw = synced ?: plain ?: return null
        val isSynced = synced != null
        val nonLatin = ScriptTransliterator.hasIndicNonLatin(raw)
        val firstLine = raw.lineSequence()
            .map { it.replace(Regex("^\\[\\d+:\\d+(?:\\.\\d+)?\\]\\s*"), "").trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return LyricsCandidate(
            id = "${obj.optInt("id", indexHint)}_$isSynced",
            trackName = obj.optString("trackName", obj.optString("name", "Unknown")),
            artistName = obj.optString("artistName", "Unknown Artist"),
            albumName = obj.optString("albumName", ""),
            durationSec = obj.optDouble("duration", 0.0).toInt(),
            rawText = raw,
            isSynced = isSynced,
            hasNonLatinScript = nonLatin,
            previewText = firstLine,
            sourceLabel = if (isSynced) "LRCLIB Synced" else "LRCLIB"
        )
    }

    private fun queryLrclibExactCandidate(title: String, artist: String, album: String, durationSec: Int): LyricsCandidate? {
        if (title.isBlank() || artist.isBlank()) return null
        val url = "https://lrclib.net/api/get?artist_name=${enc(artist)}&track_name=${enc(title)}&album_name=${enc(album)}&duration=$durationSec"
        val body = httpGet(url) ?: return null
        return runCatching { parseCandidateObject(JSONObject(body), 0) }.getOrNull()
    }

    private fun queryLrclibCandidates(title: String, artist: String): List<LyricsCandidate> {
        if (title.isBlank()) return emptyList()
        val query = if (artist.isNotBlank()) "$title $artist" else title
        val url = "https://lrclib.net/api/search?q=${enc(query)}"
        val body = httpGet(url) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(body)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parseCandidateObject(obj, i)?.let { add(it) }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun queryLyricsOvh(artist: String, title: String): String? {
        if (artist.isBlank() || title.isBlank()) return null
        val pathArtist = enc(artist).replace("+", "%20")
        val pathTitle = enc(title).replace("+", "%20")
        val body = httpGet("https://api.lyrics.ovh/v1/$pathArtist/$pathTitle") ?: return null
        return runCatching {
            val lyr = JSONObject(body).optString("lyrics", "").trim()
            if (lyr.isNotEmpty() && lyr != "null") lyr else null
        }.getOrNull()
    }
}
