package com.corvix.player.audio

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.corvix.player.model.AdaptiveGlassPalette
import com.corvix.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

@Immutable
data class LoadedArtwork(
    val imageBitmap: ImageBitmap,
    val palette: AdaptiveGlassPalette
) {
    val primaryColor: Color get() = palette.primary
    val secondaryColor: Color get() = palette.secondary
}

/**
 * Lock-free two-tier memory cache storing pre-converted Compose ImageBitmap + precomputed
 * AdaptiveGlassPalette off the main thread so tab switches never run ContentResolver,
 * asImageBitmap(), or extractVibrantColors() on the UI thread.
 */
object AlbumArtLoader {

    private val thumbCache = object : LruCache<String, LoadedArtwork>(128) {}
    private val highResCache = object : LruCache<String, LoadedArtwork>(16) {}
    private val missingKeys: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun getCached(track: Track, highRes: Boolean): LoadedArtwork? {
        val key = cacheKey(track, highRes) ?: return null
        return if (highRes) {
            highResCache.get(key) ?: cacheKey(track, false)?.let { thumbCache.get(it) }
        } else {
            thumbCache.get(key)
        }
    }

    fun getCachedPalette(track: Track): AdaptiveGlassPalette? =
        cacheKey(track, true)?.let { highResCache.get(it)?.palette }
            ?: cacheKey(track, false)?.let { thumbCache.get(it)?.palette }

    private fun diskThumbFile(context: Context, track: Track): File {
        val dir = File(context.cacheDir, "art_thumbs").apply { if (!exists()) mkdirs() }
        return File(dir, "${track.albumId}_${track.id}.jpg")
    }

    fun invalidate(track: Track, context: Context? = null) {
        cacheKey(track, true)?.let { highResCache.remove(it) }
        cacheKey(track, false)?.let {
            thumbCache.remove(it)
            missingKeys.remove(it)
        }
        if (context != null) {
            runCatching { diskThumbFile(context, track).delete() }
        }
    }

    suspend fun load(
        context: Context,
        track: Track,
        highRes: Boolean = false
    ): LoadedArtwork? {
        val key = cacheKey(track, highRes) ?: return null
        getCached(track, highRes)?.let { cached ->
            if (!highRes || highResCache.get(key) != null) return cached
        }
        val customFile = ArtworkSearchRepository.customArtFile(context, track.id)
        val hasCustomArt = customFile.exists() && customFile.length() > 64L
        val baseMissKey = cacheKey(track, false)
        if (!hasCustomArt && baseMissKey in missingKeys) return null

        return withContext(Dispatchers.IO) {
            val targetPx = if (highRes) 640 else 200
            val diskFile = if (!hasCustomArt && !highRes) diskThumbFile(context, track) else null
            val bmp = when {
                hasCustomArt -> runCatching { decodeSampled(customFile.readBytes(), targetPx) }.getOrNull()
                diskFile != null && diskFile.exists() && diskFile.length() > 64L ->
                    runCatching { BitmapFactory.decodeFile(diskFile.absolutePath) }.getOrNull()
                        ?: decodeAlbumBitmap(context, track, targetPx)?.also { saveThumb(diskFile, it) }
                else -> decodeAlbumBitmap(context, track, targetPx)?.also {
                    if (diskFile != null) saveThumb(diskFile, it)
                }
            } ?: run {
                if (baseMissKey != null) missingKeys.add(baseMissKey)
                return@withContext null
            }
            // Convert ImageBitmap & compute AdaptiveGlassPalette once on Dispatchers.IO
            val imageBitmap = bmp.asImageBitmap()
            val adaptivePalette = extractVibrantColors(bmp, track)
            val result = LoadedArtwork(
                imageBitmap = imageBitmap,
                palette = adaptivePalette
            )
            if (highRes) {
                highResCache.put(key, result)
            } else {
                thumbCache.put(key, result)
            }
            result
        }
    }

    private fun saveThumb(file: File, bmp: Bitmap) {
        runCatching {
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 86, out)
            }
        }
    }

    private fun cacheKey(track: Track, highRes: Boolean): String? {
        if (!track.isDeviceFile && track.contentUri == null && track.albumId <= 0L) return null
        val tier = if (highRes) "hi" else "lo"
        return "${track.albumId}_${track.id}_$tier"
    }

    private fun decodeAlbumBitmap(context: Context, track: Track, targetPx: Int): Bitmap? {
        val resolver = context.contentResolver

        // 1. Direct MediaStore track URI thumbnail (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.contentUri != null) {
            runCatching {
                return resolver.loadThumbnail(track.contentUri, Size(targetPx, targetPx), null)
            }
        }

        // 2. MediaStore album art URI
        if (track.albumId > 0L) {
            val albumUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                track.albumId
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    return resolver.loadThumbnail(albumUri, Size(targetPx, targetPx), null)
                }
            }
            runCatching {
                resolver.openInputStream(albumUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    decodeSampled(bytes, targetPx)?.let { return it }
                }
            }
        }

        // 3. Fallback: embedded ID3 / FLAC / M4A picture inside audio file
        val uri = track.contentUri ?: return null
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return@runCatching null
            decodeSampled(bytes, targetPx)
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, reqSize: Int): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= reqSize &&
            bounds.outHeight / (sampleSize * 2) >= reqSize
        ) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun extractVibrantColors(bmp: Bitmap, track: Track): AdaptiveGlassPalette {
        val w = bmp.width
        val h = bmp.height
        if (w < 8 || h < 8) {
            return AdaptiveGlassPalette(
                primary = track.primaryColor,
                secondary = track.secondaryColor,
                orbs = track.orbColors
            )
        }
        val samples = listOf(
            bmp.getPixel(w / 4, h / 4),
            bmp.getPixel((w * 3) / 4, h / 4),
            bmp.getPixel(w / 2, h / 2),
            bmp.getPixel(w / 4, (h * 3) / 4),
            bmp.getPixel((w * 3) / 4, (h * 3) / 4)
        )
        val vibrant = samples.sortedByDescending { px ->
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            val sat = maxC - minC
            val lum = (r + g + b) / 3
            if (lum < 28 || lum > 240) sat / 4 else sat
        }
        val c1 = ensureReadableAccent(vibrant.first())
        val c2 = ensureReadableAccent(vibrant.getOrElse(1) { vibrant.first() })
        return AdaptiveGlassPalette(
            primary = c1,
            secondary = c2,
            orbs = listOf(c1, c2, c1.copy(alpha = 0.85f))
        )
    }

    private fun ensureReadableAccent(argb: Int): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = hsv[1].coerceIn(0.42f, 0.92f)
        hsv[2] = hsv[2].coerceIn(0.68f, 0.98f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
