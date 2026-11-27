package com.corvix.player

import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppSkin
import com.corvix.player.model.AppTypography
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.GlassThemePreset
import com.corvix.player.model.LibrarySortOrder
import com.corvix.player.model.SeekBarStyle
import com.corvix.player.model.TrackLanguage
import com.corvix.player.model.VisualizerStyle
import com.corvix.player.model.detectTrackLanguage
import com.corvix.player.model.parseRawLrcLyrics
import com.corvix.player.ui.theme.CorvixMetrics
import com.corvix.player.ui.theme.buildCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorvixModelsTest {

    @Test
    fun detectTrackLanguageRecognizesAllSevenLanguages() {
        val detected = CorvixSampleData.seedTracks.map { it.language }.toSet()
        assertEquals(
            setOf(
                TrackLanguage.ENGLISH,
                TrackLanguage.MALAYALAM,
                TrackLanguage.TAMIL,
                TrackLanguage.HINDI,
                TrackLanguage.TELUGU,
                TrackLanguage.JAPANESE,
                TrackLanguage.KOREAN
            ),
            detected
        )
        assertEquals(TrackLanguage.MALAYALAM, detectTrackLanguage("Rain Song", "Artist", "Album", "/Music/Malayalam"))
        assertEquals(TrackLanguage.JAPANESE, detectTrackLanguage("City Lights", "Kaito", "Tokyo Pop", "/Music"))
        assertEquals(TrackLanguage.KOREAN, detectTrackLanguage("Moonlight", "Minji", "Seoul", "/Music"))
    }

    @Test
    fun presetCountsMatchDesignSpecification() {
        assertEquals(9, GlassThemePreset.entries.size)
        assertEquals(3, AppSkin.entries.size)
        assertEquals(11, AppTypography.entries.size)
        assertEquals(5, SeekBarStyle.entries.size)
        assertEquals(11, VisualizerStyle.entries.size)

        val winampPalette = buildCorvixPalette(
            tone = com.corvix.player.model.GlassTone.DARK,
            preset = GlassThemePreset.LIQUID,
            skin = AppSkin.WINAMP_CLASSIC,
            adaptiveAlbumColor = true,
            trackPrimary = androidx.compose.ui.graphics.Color.Cyan,
            trackSecondary = androidx.compose.ui.graphics.Color.Blue,
            trackOrbs = listOf(androidx.compose.ui.graphics.Color.Cyan),
            blurIntensity = 28f
        )
        assertTrue(winampPalette.isWinamp)
    }

    @Test
    fun uiStateLanguageFilterAndSortWorkCleanly() {
        val sample = CorvixSampleData.seedTracks
        val az = CorvixUiState(tracks = sample, librarySortOrder = LibrarySortOrder.TITLE_AZ).filteredTracks
        assertEquals(8, az.size)
        val titles = az.map { it.title.trim().lowercase() }
        assertEquals(titles.sorted(), titles)

        val byArtist = CorvixUiState(tracks = sample, librarySortOrder = LibrarySortOrder.ARTIST).filteredTracks
        val artists = byArtist.map { it.artist.trim().lowercase() }
        assertEquals(artists.sorted(), artists)

        val byDuration = CorvixUiState(tracks = sample, librarySortOrder = LibrarySortOrder.DURATION).filteredTracks
        val durations = byDuration.map { it.durationSec }
        assertEquals(durations.sortedDescending(), durations)

        val firstSample = sample.first()
        val firstWordTitle = firstSample.title.trim().split(Regex("\\s+")).first()
        val firstWordArtist = firstSample.artist.trim().split(Regex("\\s+")).first()
        val multiToken = CorvixUiState(
            tracks = sample,
            searchQuery = "$firstWordArtist $firstWordTitle"
        ).filteredTracks
        assertTrue(multiToken.any { it.id == firstSample.id })

        val prefixMatches = CorvixUiState(
            tracks = sample,
            searchQuery = firstWordTitle
        ).filteredTracks
        assertTrue(prefixMatches.isNotEmpty())
        assertTrue(prefixMatches.first().title.lowercase().contains(firstWordTitle.lowercase()))
    }

    @Test
    fun lrcParserExtractsSyncedTimestamps() {
        val parsed = parseRawLrcLyrics("[01:12.50] Copper reels\nPlain line")
        assertEquals(2, parsed.size)
        assertEquals(72, parsed[0].sec)
        assertTrue(parsed[0].text.contains("Copper reels"))
        assertEquals(25, parsed[1].sec)
    }

    @Test
    fun metricsAutoScaleAcrossPhoneAndTabletSizes() {
        val small = CorvixMetrics.fromViewport(320.dp, 568.dp)
        val normal = CorvixMetrics.fromViewport(360.dp, 800.dp)
        val landscape = CorvixMetrics.fromViewport(844.dp, 390.dp)
        val tablet = CorvixMetrics.fromViewport(800.dp, 1280.dp)

        assertTrue(small.isCompactWidth)
        assertTrue(small.isShortHeight)
        assertTrue(small.scale < normal.scale)
        assertTrue(small.ipodWheelSize < normal.ipodWheelSize)
        assertTrue(landscape.isWideOrLandscape)
        assertTrue(tablet.isWideOrLandscape)
        assertTrue(tablet.textScale >= normal.textScale)
    }
}
