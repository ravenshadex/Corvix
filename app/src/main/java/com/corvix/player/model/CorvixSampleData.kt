package com.corvix.player.model

import androidx.compose.ui.graphics.Color

object CorvixSampleData {

    val defaultSyncedLyrics: List<LyricLine> = listOf(
        LyricLine(0, "00:18 — Warm analog bias settles in"),
        LyricLine(45, "00:45 — Static hums along the midnight wire"),
        LyricLine(72, "01:12 — Spinning copper reels in lossless glow"),
        LyricLine(115, "01:55 — Distant chords echo across the room"),
        LyricLine(155, "02:35 — No signal needed, only local memory...")
    )

    val seedTracks: List<Track> = listOf(
        Track(
            id = 1001L,
            title = "Midnight Tape",
            artist = "Kaelen Voss",
            album = "Echoes on Tape",
            genre = "Ambient",
            bpm = 82,
            energy = 0.31f,
            formatLabel = "FLAC · 24bit/96kHz",
            shortFormat = "FLAC",
            formatGroup = "FLAC",
            bitrateInfo = "2,418 kbps · 48.2 MB",
            durationSec = 248,
            folderPath = "/Music/English/Hi-Res",
            artStyle = ArtStyle.MIDNIGHT,
            primaryColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFF6366F1),
            orbColors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFA855F7)),
            lyrics = defaultSyncedLyrics
        ),
        Track(
            id = 1002L,
            title = "നീല നിലാവ് (Neela Nilavu)",
            artist = "K. S. Harisankar",
            album = "Monsoon Kochi",
            genre = "Melody",
            bpm = 78,
            energy = 0.34f,
            formatLabel = "FLAC · 24bit/96kHz",
            shortFormat = "FLAC",
            formatGroup = "FLAC",
            bitrateInfo = "2,180 kbps · 44.1 MB",
            durationSec = 256,
            folderPath = "/Music/Malayalam/Lossless",
            artStyle = ArtStyle.EMERALD,
            primaryColor = Color(0xFF34D399),
            secondaryColor = Color(0xFF0EA5E9),
            orbColors = listOf(Color(0xFF34D399), Color(0xFF0EA5E9), Color(0xFF059669)),
            lyrics = listOf(
                LyricLine(0, "00:14 — Golden cassette hum in morning light"),
                LyricLine(58, "00:58 — Monsoon rain across the window pane"),
                LyricLine(112, "01:52 — Drift slow inside the tape grain...")
            )
        ),
        Track(
            id = 1003L,
            title = "மழை குருவி (Mazhai Kuruvi)",
            artist = "A. R. Rahman",
            album = "Chekka Chivantha",
            genre = "Classical",
            bpm = 92,
            energy = 0.42f,
            formatLabel = "ALAC · 24bit/48kHz",
            shortFormat = "ALAC",
            formatGroup = "ALAC",
            bitrateInfo = "1,720 kbps · 39.0 MB",
            durationSec = 302,
            folderPath = "/Music/Tamil/Hi-Res",
            artStyle = ArtStyle.AMBER,
            primaryColor = Color(0xFFFB923C),
            secondaryColor = Color(0xFFF43F5E),
            orbColors = listOf(Color(0xFFFBBF24), Color(0xFFEA580C), Color(0xFFF43F5E)),
            lyrics = null
        ),
        Track(
            id = 1004L,
            title = "कुन फाया (Kun Faya)",
            artist = "Mohit Chauhan",
            album = "Rockstar Acoustic",
            genre = "Sufi",
            bpm = 84,
            energy = 0.39f,
            formatLabel = "WAV · 24bit/96kHz",
            shortFormat = "WAV",
            formatGroup = "DSD",
            bitrateInfo = "4,608 kbps · 98.4 MB",
            durationSec = 285,
            folderPath = "/Music/Hindi/Masters",
            artStyle = ArtStyle.GOLD,
            primaryColor = Color(0xFFFACC15),
            secondaryColor = Color(0xFF14B8A6),
            orbColors = listOf(Color(0xFFFDE047), Color(0xFF14B8A6), Color(0xFF0D9488)),
            lyrics = defaultSyncedLyrics
        ),
        Track(
            id = 1005L,
            title = "సామజవరగమన (Samajavaragamana)",
            artist = "Sid Sriram",
            album = "Ala Vaikunthapurramuloo",
            genre = "Melody",
            bpm = 96,
            energy = 0.48f,
            formatLabel = "FLAC · 24bit/48kHz",
            shortFormat = "FLAC",
            formatGroup = "FLAC",
            bitrateInfo = "1,890 kbps · 41.2 MB",
            durationSec = 219,
            folderPath = "/Music/Telugu/Lossless",
            artStyle = ArtStyle.ROSE,
            primaryColor = Color(0xFFFB7185),
            secondaryColor = Color(0xFFC026D3),
            orbColors = listOf(Color(0xFFFB7185), Color(0xFFC026D3), Color(0xFF9333EA)),
            lyrics = null
        ),
        Track(
            id = 1006L,
            title = "夜明けの電車 (Night Train)",
            artist = "Kaito",
            album = "Tokyo City Pop",
            genre = "City Pop",
            bpm = 118,
            energy = 0.78f,
            formatLabel = "DSD64 · 2.8 MHz",
            shortFormat = "DSD",
            formatGroup = "DSD",
            bitrateInfo = "5,644 kbps · 112 MB",
            durationSec = 234,
            folderPath = "/Music/Japanese/Vinyl",
            artStyle = ArtStyle.VIOLET,
            primaryColor = Color(0xFFC084FC),
            secondaryColor = Color(0xFF38BDF8),
            orbColors = listOf(Color(0xFFC084FC), Color(0xFF7C3AED), Color(0xFF38BDF8)),
            lyrics = defaultSyncedLyrics
        ),
        Track(
            id = 1007L,
            title = "밤하늘 (Starry Seoul)",
            artist = "Minji",
            album = "Hangul Nights",
            genre = "K-Indie",
            bpm = 106,
            energy = 0.62f,
            formatLabel = "OPUS · 256 kbps",
            shortFormat = "OPUS",
            formatGroup = "MP3",
            bitrateInfo = "256 kbps · 6.4 MB",
            durationSec = 207,
            folderPath = "/Music/Korean/Indie",
            artStyle = ArtStyle.MIDNIGHT,
            primaryColor = Color(0xFF60A5FA),
            secondaryColor = Color(0xFF22D3EE),
            orbColors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE), Color(0xFF3B82F6)),
            lyrics = null
        ),
        Track(
            id = 1008L,
            title = "Analog Sun",
            artist = "Mira Sol",
            album = "Warm Cassette",
            genre = "Lo-Fi",
            bpm = 88,
            energy = 0.36f,
            formatLabel = "MP3 · 320 kbps",
            shortFormat = "MP3",
            formatGroup = "MP3",
            bitrateInfo = "320 kbps · 8.4 MB",
            durationSec = 212,
            folderPath = "/Music/English/Lo-Fi",
            artStyle = ArtStyle.AMBER,
            primaryColor = Color(0xFFF87171),
            secondaryColor = Color(0xFFFBBF24),
            orbColors = listOf(Color(0xFFF87171), Color(0xFFFBBF24), Color(0xFFEA580C)),
            lyrics = defaultSyncedLyrics
        )
    )

    val seedFolders: List<FolderDirectory> = listOf(
        FolderDirectory(
            name = "Hi-Res Masters",
            displayPath = "/Music/Hi-Res Masters",
            trackCount = 312,
            codecSummary = "FLAC, DSD, WAV",
            sizeLabel = "9.8 GB"
        ),
        FolderDirectory(
            name = "Focus & Lo-Fi",
            displayPath = "/Music/Focus & Lo-Fi",
            trackCount = 184,
            codecSummary = "MP3, OPUS, OGG",
            sizeLabel = "2.1 GB"
        ),
        FolderDirectory(
            name = "ALAC & M4A",
            displayPath = "/SDCard/ALAC & M4A",
            trackCount = 92,
            codecSummary = "ALAC, AAC, AIFF",
            sizeLabel = "2.9 GB"
        ),
        FolderDirectory(
            name = "Downloads",
            displayPath = "/Download/Audio",
            trackCount = 49,
            codecSummary = "Mixed offline files",
            sizeLabel = "540 MB"
        )
    )

    val seedPlaylists: List<PlaylistSummary> = listOf(
        PlaylistSummary(
            id = "favorites",
            title = "Favorites",
            subtitle = "0 liked tracks",
            artStyle = ArtStyle.ROSE,
            queueIndices = emptyList(),
            isSmart = true
        )
    )

    val outputSinks: List<AudioSinkInfo> = listOf(
        AudioSinkInfo(
            name = "USB DAC Direct (Exclusive)",
            shortName = "USB DAC",
            detail = "32-bit/384kHz Bit-Perfect",
            rateLabel = "32/384",
            autoEqProfile = AutoEqProfile.MANUAL
        ),
        AudioSinkInfo(
            name = "Sony WH-1000XM5 (LDAC)",
            shortName = "Sony WH-1000XM5",
            detail = "LDAC 990kbps · AutoEQ Matched",
            rateLabel = "AutoEQ",
            autoEqProfile = AutoEqProfile.XM5
        ),
        AudioSinkInfo(
            name = "Phone Internal Speaker",
            shortName = "Internal Speaker",
            detail = "Android Mixer · 48kHz",
            rateLabel = "48kHz",
            autoEqProfile = AutoEqProfile.MANUAL
        )
    )

    val seekWaveHeights: List<Float> = listOf(
        0.35f, 0.55f, 0.80f, 0.48f, 0.92f, 0.65f, 0.40f, 0.76f,
        1.00f, 0.62f, 0.44f, 0.88f, 0.70f, 0.52f, 0.95f, 0.60f,
        0.38f, 0.82f, 0.68f, 0.46f, 0.86f, 0.58f, 0.42f, 0.30f
    )

    val visualizerHeights: List<Float> = listOf(
        0.38f, 0.66f, 0.48f, 0.86f, 0.62f, 0.92f,
        0.52f, 0.82f, 0.58f, 0.88f, 0.44f, 0.68f
    )
}
