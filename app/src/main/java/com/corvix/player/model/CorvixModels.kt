package com.corvix.player.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppScreen {
    IPOD_MENU,
    LIBRARY,
    FOLDERS,
    PLAYLISTS,
    PROFILE,
    SETTINGS,
    NOW_PLAYING,
    WIDGET
}

enum class LibraryView(val label: String, val heading: String) {
    SONGS("Songs", "All Songs"),
    ARTISTS("Artists", "Artists (ID3)"),
    ALBUMS("Albums", "Albums (ID3)"),
    GENRES("Genres", "Genres")
}

enum class LibrarySortOrder(val label: String) {
    TITLE_AZ("Title A–Z"),
    DATE_ADDED("Date Added"),
    MOST_PLAYED("Most Played"),
    ARTIST("Artist"),
    DURATION("Duration")
}

enum class TrackLanguage(val label: String) {
    ALL("All"),
    MALAYALAM("Malayalam"),
    TAMIL("Tamil"),
    HINDI("Hindi"),
    TELUGU("Telugu"),
    ENGLISH("English"),
    JAPANESE("Japanese"),
    KOREAN("Korean")
}

enum class ArtStyle {
    MIDNIGHT,
    AMBER,
    EMERALD,
    ROSE,
    VIOLET,
    GOLD
}

enum class GlassTone(val label: String) {
    DARK("Dark Glass"),
    AMOLED("AMOLED"),
    LIGHT("Light Frost")
}

enum class GlassThemePreset(
    val title: String,
    val subtitle: String,
    val defaultBlur: Float
) {
    LIQUID("Pure Liquid Glass", "Wet specular", 28f),
    FROST("Frost Liquid Glass", "44px icy frost", 44f),
    MERCURY("Mercury Liquid", "Molten chrome", 32f),
    AURORA("Aurora Gel", "Iridescent gel", 34f),
    PRISM("Prism Crystal", "Faceted refraction", 20f),
    SMOKE("Obsidian Smoke", "Matte smoked glass", 38f),
    CYBER("Neon Cyber Glass", "Glowing edge rims", 22f),
    SAKURA("Sakura Silk", "Rose pearl satin", 30f),
    VELVET("Midnight Velvet", "Deep indigo frost", 34f)
}

enum class AppSkin(
    val title: String,
    val subtitle: String
) {
    LIQUID_SHELL("Liquid Shell", "Floating dock & full touch glass"),
    CLASSIC_IPOD("Classic iPod", "Metallic bezel & Click Wheel UI"),
    WINAMP_CLASSIC("Winamp Classic", "2.91 3-window rack, 10-band EQ & terminal playlist")
}

enum class AppTypography(val badgeLabel: String, val menuLabel: String) {
    SANS("SF System Sans", "SF / System Sans"),
    ROUNDED("SF Rounded", "SF Rounded"),
    HUMANIST("Geometric", "Geometric Humanist"),
    SERIF("Editorial Serif", "Editorial Serif"),
    BOOK("Classic Book", "Classic Book Serif"),
    MONO("Studio Mono", "Studio Mono"),
    CONDENSED("Condensed", "Condensed Display"),
    IPOD("iPod Swiss", "iPod Classic (Swiss)"),
    SLAB("Typewriter Slab", "Typewriter Slab"),
    WIDE("Modern Wide", "Modern Wide"),
    MINIMAL("Clean Thin", "Clean Thin")
}

enum class SeekBarStyle(val title: String, val subtitle: String) {
    DROPLET("Liquid Droplet", "Glossy frosted capsule"),
    WAVEFORM("Audio Waveform", "Dynamic bar spectrum"),
    MINIMAL("Minimal Line", "Slim 3.5px glow"),
    BLOCKS("Segmented Blocks", "24-step glass bricks"),
    GROOVE("Vinyl Groove", "Recessed needle track")
}

enum class VisualizerStyle(val title: String, val subtitle: String) {
    BARS("Spectrum Bars", "Classic crystal peaks"),
    MIRROR("Mirror Pulse", "Center symmetric"),
    WAVE("Sine Waveform", "Fluid analog wave"),
    RING("Orbit Ring", "Concentric halo rings"),
    PARTICLES("Particle Drift", "Floating glow dust"),
    MATRIX("LED Matrix", "Segmented retro steps"),
    AURORA("Aurora Ribbon", "Soft blurred veil"),
    NEEDLE("VU Meters", "Dual peak needles"),
    RIPPLE("Water Ripples", "Liquid shockwaves"),
    DOTS("Dotted Equalizer", "Floating pearl nodes"),
    HORIZON("Neon Horizon", "Horizon synth grid")
}

enum class RepeatMode(val badge: String) {
    OFF("OFF"),
    ALL("ALL"),
    ONE("1")
}

enum class EqPreset(val label: String, val bands: List<Int>) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    BASS("Warm Bass", listOf(5, 3, 0, 1, 2)),
    VOCAL("Vocal", listOf(-2, 1, 4, 3, 1)),
    STUDIO("Studio", listOf(2, 1, 0, 2, 3))
}

enum class ReverbSpace(val chipLabel: String, val fullName: String, val presetId: Short) {
    OFF("Off", "Off", 0),
    ROOM("Room", "Small Room", 1),
    HALL("Hall", "Concert Hall", 3),
    CATHEDRAL("Cathedral", "Cathedral", 4),
    PLATE("Plate", "Plate", 6)
}

enum class AutoEqProfile(val chipLabel: String, val fullName: String, val bands: List<Int>) {
    MANUAL("Manual", "Manual", listOf(4, 2, 0, 3, 2, 3, 1, -1, 2, 4)),
    XM5("Sony XM5", "Sony WH-1000XM5 (AutoEQ)", listOf(-3, -4, 1, 3, 2, -2, -5, 1, 4, 3)),
    HD650("HD 650", "Sennheiser HD 650 (Harman)", listOf(3, 1, -1, 2, 1, 5, 2, 0, 2, 1)),
    BLESSING3("Blessing 3", "Moondrop Blessing 3", listOf(2, 0, 1, -1, 2, 3, 1, 0, -1, 2)),
    AIRPODS("AirPods Pro 2", "AirPods Pro 2 (AutoEQ)", listOf(-1, 1, 0, 2, -1, 2, -1, 1, 2, 0))
}

enum class BtCodec(val chipLabel: String, val statusLabel: String) {
    LDAC("LDAC", "LDAC 990kbps"),
    APTX_HD("aptX HD", "aptX HD 576kbps"),
    AAC("AAC", "AAC 256kbps"),
    SBC("SBC", "SBC 328kbps")
}

enum class MoodType(val label: String, val rangeLabel: String) {
    CHILL("Chill", "<95 BPM"),
    FOCUS("Focus", "95–115"),
    PEAK("Peak", ">115 BPM")
}

enum class SheetType {
    NONE,
    TAGS,
    RINGTONE,
    SLEEP,
    AUTO,
    SIGNAL,
    LYRICS,
    NP_MENU,
    TRACK_ACTIONS,
    ARTWORK_SEARCH,
    PLAYLIST_PICKER,
    CREATE_PLAYLIST,
    RENAME_PLAYLIST,
    SORT_ORDER
}

@Immutable
data class AdaptiveGlassPalette(
    val primary: Color,
    val secondary: Color,
    val orbs: List<Color>
)

@Immutable
data class LyricLine(
    val sec: Int,
    val text: String
)

@Immutable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val bpm: Int,
    val energy: Float,
    val formatLabel: String,
    val shortFormat: String,
    val formatGroup: String,
    val bitrateInfo: String,
    val durationSec: Int,
    val folderPath: String,
    val artStyle: ArtStyle,
    val primaryColor: Color,
    val secondaryColor: Color,
    val orbColors: List<Color>,
    val contentUri: Uri? = null,
    val albumId: Long = -1L,
    val dateAddedSec: Long = 0L,
    val isDeviceFile: Boolean = false,
    val lyrics: List<LyricLine>? = null,
    val lyricsSource: String? = null,
    val detectedLanguage: TrackLanguage = detectTrackLanguage(
        title = title,
        artist = artist,
        album = album,
        folder = folderPath,
        lyrics = lyrics
    )
) {
    val language: TrackLanguage
        get() = detectedLanguage

    val durationFormatted: String = formatSeconds(durationSec)
}

@Immutable
data class FolderDirectory(
    val name: String,
    val displayPath: String,
    val trackCount: Int,
    val codecSummary: String,
    val sizeLabel: String
)

// ── Recap / Profile stats ─────────────────────────────────────────────────────
@Immutable
data class ArtistRecapEntry(
    val artistName: String,
    val playCount: Int,
    val totalMinutes: Int,
    val topTrackTitle: String
)

@Immutable
data class RecapStats(
    val totalPlays: Int = 0,
    val totalMinutes: Int = 0,
    val uniqueArtists: Int = 0,
    val streakDays: Int = 0,
    /** hourly play distribution, indices 0-23 */
    val hourlyPlays: List<Int> = List(24) { 0 },
    /** weekly play distribution, indices 0 = Mon … 6 = Sun */
    val weeklyPlays: List<Int> = List(7) { 0 },
    val topArtists: List<ArtistRecapEntry> = emptyList(),
    val topTracks: List<Pair<Long, Int>> = emptyList(), // trackId → playCount
    /** language label → fraction 0..1 */
    val languageFractions: Map<String, Float> = emptyMap(),
    /** repeat rate 0..1 */
    val repeatRate: Float = 0f,
    /** loyalty (single-artist session pct) 0..1 */
    val loyaltyRate: Float = 0f,
    /** fraction of plays after 10 PM 0..1 */
    val afterDarkRate: Float = 0f,
    /** fraction of plays on tracks played < 3 times (discovery) 0..1 */
    val discoveryRate: Float = 0f,
    val losslessPercent: Int = 0,
    val avgSongLengthSec: Int = 0
)
// ──────────────────────────────────────────────────────────────────────────────

@Immutable
data class PlaylistSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val artStyle: ArtStyle,
    val queueIndices: List<Int>,
    val isSmart: Boolean = false
)

@Immutable
data class PlaybackQueue(
    val key: String,
    val label: String,
    val trackIds: List<Long>
)

@Immutable
data class AudioSinkInfo(
    val name: String,
    val shortName: String,
    val detail: String,
    val rateLabel: String,
    val autoEqProfile: AutoEqProfile
)

fun formatSeconds(totalSec: Int): String {
    val clamped = totalSec.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return "%02d:%02d".format(mins, secs)
}

private val MALAYALAM_REGEX = Regex("[\\u0D00-\\u0D7F]|\\bmalayalam\\b", RegexOption.IGNORE_CASE)
private val TAMIL_REGEX = Regex("[\\u0B80-\\u0BFF]|\\btamil\\b", RegexOption.IGNORE_CASE)
private val HINDI_REGEX = Regex("[\\u0900-\\u097F]|\\bhindi\\b", RegexOption.IGNORE_CASE)
private val TELUGU_REGEX = Regex("[\\u0C00-\\u0C7F]|\\btelugu\\b", RegexOption.IGNORE_CASE)
private val JAPANESE_REGEX = Regex("[\\u3040-\\u30FF\\u4E00-\\u9FAF]|\\bjapanese\\b|\\btokyo\\b", RegexOption.IGNORE_CASE)
private val KOREAN_REGEX = Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF]|\\bkorean\\b|\\bseoul\\b|\\bhangul\\b", RegexOption.IGNORE_CASE)

/**
 * Recognizes track language from Unicode script ranges, ID3 metadata, folder path, and lyrics.
 */
fun detectTrackLanguage(
    title: String,
    artist: String,
    album: String,
    folder: String,
    lyrics: List<LyricLine>? = null
): TrackLanguage {
    val lyricsText = lyrics?.joinToString(" ") { it.text }.orEmpty()
    val sample = "$title $artist $album $folder $lyricsText"
    return when {
        MALAYALAM_REGEX.containsMatchIn(sample) -> TrackLanguage.MALAYALAM
        TAMIL_REGEX.containsMatchIn(sample) -> TrackLanguage.TAMIL
        HINDI_REGEX.containsMatchIn(sample) -> TrackLanguage.HINDI
        TELUGU_REGEX.containsMatchIn(sample) -> TrackLanguage.TELUGU
        JAPANESE_REGEX.containsMatchIn(sample) -> TrackLanguage.JAPANESE
        KOREAN_REGEX.containsMatchIn(sample) -> TrackLanguage.KOREAN
        else -> TrackLanguage.ENGLISH
    }
}

fun parseRawLrcLyrics(rawText: String): List<LyricLine> {
    val timestampPattern = Regex("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]\\s*(.*)$")
    return rawText.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { idx, line ->
            val match = timestampPattern.matchEntire(line)
            if (match != null) {
                val mins = match.groupValues[1].toIntOrNull() ?: 0
                val secs = match.groupValues[2].toDoubleOrNull()?.toInt() ?: 0
                val text = match.groupValues[3].ifBlank { "..." }
                LyricLine(
                    sec = mins * 60 + secs,
                    text = "%02d:%02d — %s".format(mins, secs, text)
                )
            } else {
                LyricLine(sec = idx * 25, text = line)
            }
        }
        .toList()
}

