package com.corvix.player.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import com.corvix.player.model.ArtStyle
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.FolderDirectory
import com.corvix.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

data class LibraryScanResult(
    val tracks: List<Track>,
    val folders: List<FolderDirectory>,
    val totalCount: Int,
    val fromDeviceCount: Int
)

object LocalLibraryScanner {

    private val artPalettePool = listOf(
        Triple(ArtStyle.MIDNIGHT, Color(0xFF38BDF8), Color(0xFF6366F1)),
        Triple(ArtStyle.EMERALD, Color(0xFF34D399), Color(0xFF0EA5E9)),
        Triple(ArtStyle.AMBER, Color(0xFFFB923C), Color(0xFFF43F5E)),
        Triple(ArtStyle.GOLD, Color(0xFFFACC15), Color(0xFF14B8A6)),
        Triple(ArtStyle.ROSE, Color(0xFFFB7185), Color(0xFFC026D3)),
        Triple(ArtStyle.VIOLET, Color(0xFFC084FC), Color(0xFF38BDF8))
    )

    suspend fun scanOfflineLibrary(context: Context): LibraryScanResult = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<Track>()
        val folderCounts = linkedMapOf<String, MutableList<Track>>()

        runCatching {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                var idx = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val title = cursor.getString(titleCol).orEmpty().ifBlank { "Track $id" }
                    val artist = cursor.getString(artistCol).orEmpty().let {
                        if (it.isBlank() || it == "<unknown>") "Unknown Artist" else it
                    }
                    val album = cursor.getString(albumCol).orEmpty().ifBlank { "Offline Album" }
                    val durSec = ((cursor.getLong(durCol)) / 1000L).toInt().coerceAtLeast(30)
                    val path = cursor.getString(dataCol).orEmpty()
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val sizeBytes = cursor.getLong(sizeCol)
                    val sizeMb = sizeBytes / (1024.0 * 1024.0)
                    val estKbps = ((sizeBytes * 8L) / durSec.coerceAtLeast(1) / 1000L).toInt().coerceIn(32, 9999)
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                    val ext = path.substringAfterLast('.', "").uppercase()
                    val shortFmt = when {
                        ext.isNotBlank() && ext.length <= 5 -> ext
                        mime.contains("flac", true) -> "FLAC"
                        mime.contains("opus", true) -> "OPUS"
                        mime.contains("ogg", true) -> "OGG"
                        mime.contains("wav", true) -> "WAV"
                        mime.contains("mp4", true) || mime.contains("aac", true) -> "M4A"
                        else -> "MP3"
                    }
                    val formatGroup = when (shortFmt) {
                        "FLAC" -> "FLAC"
                        "WAV", "DSD", "DSF", "DFF" -> "DSD"
                        "ALAC", "M4A", "AAC", "AIFF" -> "ALAC"
                        else -> "MP3"
                    }
                    val folder = File(path).parent ?: "/Music/Device"
                    val palette = artPalettePool[idx % artPalettePool.size]
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val track = Track(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        genre = "Offline",
                        bpm = 84 + (idx * 7) % 44,
                        energy = 0.35f + ((idx % 5) * 0.11f),
                        formatLabel = "$shortFmt · Offline",
                        shortFormat = shortFmt,
                        formatGroup = formatGroup,
                        bitrateInfo = "$estKbps kbps · ${"%.1f".format(sizeMb)} MB",
                        durationSec = durSec,
                        folderPath = folder,
                        artStyle = palette.first,
                        primaryColor = palette.second,
                        secondaryColor = palette.third,
                        orbColors = listOf(palette.second, palette.third, palette.second.copy(alpha = 0.8f)),
                        contentUri = uri,
                        albumId = albumId,
                        dateAddedSec = dateAdded,
                        isDeviceFile = true
                    )
                    scanned += track
                    folderCounts.getOrPut(folder) { mutableListOf() }.add(track)
                    idx++
                }
            }
        }

        val folders = if (scanned.isNotEmpty()) {
            folderCounts.entries.map { (dir, items) ->
                val codecs = items.map { it.shortFormat }.distinct().take(3).joinToString(", ")
                FolderDirectory(
                    name = File(dir).name.ifBlank { "Music" },
                    displayPath = dir,
                    trackCount = items.size,
                    codecSummary = codecs,
                    sizeLabel = "${items.size} files"
                )
            }
        } else {
            emptyList()
        }

        LibraryScanResult(
            tracks = scanned,
            folders = folders,
            totalCount = scanned.size,
            fromDeviceCount = scanned.size
        )
    }

    /**
     * Generates a soft offline ambient WAV clip for showcase tracks without a MediaStore file
     * so Media3 playback, seeking, EQ, and background service controls function cleanly everywhere.
     */
    suspend fun ensureShowcaseAudioUri(context: Context, track: Track): Uri = withContext(Dispatchers.IO) {
        track.contentUri?.let { return@withContext it }

        val dir = File(context.cacheDir, "corvix_tones").apply { mkdirs() }
        val file = File(dir, "track_${track.id}.wav")
        if (file.exists() && file.length() > 1024L) {
            return@withContext Uri.fromFile(file)
        }

        val sampleRate = 22050
        val seconds = 18
        val numSamples = sampleRate * seconds
        val baseFreq = when ((track.id % 6L).toInt()) {
            0 -> 196.0
            1 -> 220.0
            2 -> 246.94
            3 -> 261.63
            4 -> 293.66
            else -> 174.61
        }

        val pcmBytes = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = 0.72 + 0.28 * sin(2.0 * PI * (track.bpm / 120.0) * t)
            val pad = (
                sin(2.0 * PI * baseFreq * t) * 0.45 +
                    sin(2.0 * PI * (baseFreq * 1.25) * t) * 0.28 +
                    sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.22
                ) * env * 0.18
            val sample = (pad.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
            pcmBytes[i * 2] = (sample.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavHeaderAndPcm(file, sampleRate, pcmBytes)
        Uri.fromFile(file)
    }

    private fun writeWavHeaderAndPcm(file: File, sampleRate: Int, pcm: ByteArray) {
        val totalDataLen = pcm.size + 36
        val byteRate = sampleRate * 2
        val header = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            (totalDataLen and 0xff).toByte(),
            ((totalDataLen shr 8) and 0xff).toByte(),
            ((totalDataLen shr 16) and 0xff).toByte(),
            ((totalDataLen shr 24) and 0xff).toByte(),
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            16, 0, 0, 0,
            1, 0,
            1, 0,
            (sampleRate and 0xff).toByte(),
            ((sampleRate shr 8) and 0xff).toByte(),
            ((sampleRate shr 16) and 0xff).toByte(),
            ((sampleRate shr 24) and 0xff).toByte(),
            (byteRate and 0xff).toByte(),
            ((byteRate shr 8) and 0xff).toByte(),
            ((byteRate shr 16) and 0xff).toByte(),
            ((byteRate shr 24) and 0xff).toByte(),
            2, 0,
            16, 0,
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            (pcm.size and 0xff).toByte(),
            ((pcm.size shr 8) and 0xff).toByte(),
            ((pcm.size shr 16) and 0xff).toByte(),
            ((pcm.size shr 24) and 0xff).toByte()
        )
        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcm)
        }
    }
}
