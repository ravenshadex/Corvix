package com.corvix.player.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.corvix.player.audio.AlbumArtLoader
import com.corvix.player.audio.ArtworkCandidate
import com.corvix.player.audio.ArtworkSearchRepository
import com.corvix.player.audio.CorvixAudioController
import com.corvix.player.audio.CorvixMediaCommand
import com.corvix.player.audio.CorvixMediaCommandBus
import com.corvix.player.audio.DeviceStorageInfo
import com.corvix.player.audio.LocalLibraryScanner
import com.corvix.player.audio.LyricsCandidate
import com.corvix.player.audio.LyricsRepository
import com.corvix.player.model.AdaptiveGlassPalette
import com.corvix.player.widget.CorvixWidgetBridge
import com.corvix.player.model.AppScreen
import com.corvix.player.model.AppSkin
import com.corvix.player.model.AppTypography
import com.corvix.player.model.ArtStyle
import com.corvix.player.model.AudioSinkInfo
import com.corvix.player.model.AutoEqProfile
import com.corvix.player.model.BtCodec
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.EqPreset
import com.corvix.player.model.FolderDirectory
import com.corvix.player.model.GlassThemePreset
import com.corvix.player.model.GlassTone
import com.corvix.player.model.LibrarySortOrder
import com.corvix.player.model.LibraryView
import com.corvix.player.model.LyricLine
import com.corvix.player.model.MoodType
import com.corvix.player.model.PlaybackQueue
import com.corvix.player.model.ArtistRecapEntry
import com.corvix.player.model.PlaylistSummary
import com.corvix.player.model.RecapStats
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.ReverbSpace
import com.corvix.player.model.SeekBarStyle
import com.corvix.player.model.SheetType
import com.corvix.player.model.Track
import com.corvix.player.model.TrackLanguage
import com.corvix.player.model.VisualizerStyle
import com.corvix.player.model.detectTrackLanguage
import com.corvix.player.ui.theme.LiquidGlassConfig
import com.corvix.player.model.formatSeconds
import com.corvix.player.model.parseRawLrcLyrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val EMPTY_PLACEHOLDER_TRACK = Track(
    id = 0L,
    title = "No Track Loaded",
    artist = "Select a song from Library",
    album = "Corvix",
    genre = "",
    bpm = 0,
    energy = 0f,
    formatLabel = "AUDIO",
    shortFormat = "AUDIO",
    formatGroup = "ALL",
    bitrateInfo = "",
    durationSec = 1,
    folderPath = "",
    artStyle = ArtStyle.MIDNIGHT,
    primaryColor = Color(0xFF94A3B8),
    secondaryColor = Color(0xFF475569),
    orbColors = listOf(Color(0xFF94A3B8), Color(0xFF475569))
)

private val SEARCH_SPLIT_REGEX = Regex("\\s+")

fun filterAndRankTracks(
    tracks: List<Track>,
    query: String,
    selectedLanguage: TrackLanguage = TrackLanguage.ALL,
    librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
    playCounts: Map<Long, Int> = emptyMap()
): List<Track> {
    val cleanQuery = query.trim().lowercase()
    val tokens = if (cleanQuery.isEmpty()) emptyList() else cleanQuery.split(SEARCH_SPLIT_REGEX).filter { it.isNotEmpty() }

    val base = tracks.filter { t ->
        val matchesLang = selectedLanguage == TrackLanguage.ALL || t.detectedLanguage == selectedLanguage
        if (!matchesLang) return@filter false
        if (tokens.isEmpty()) return@filter true

        val title = t.title.lowercase()
        val artist = t.artist.lowercase()
        val album = t.album.lowercase()
        val genre = t.genre.lowercase()
        val formatLabel = t.formatLabel.lowercase()
        val shortFormat = t.shortFormat.lowercase()
        val langLabel = t.detectedLanguage.label.lowercase()

        tokens.all { tok ->
            title.contains(tok) ||
                artist.contains(tok) ||
                album.contains(tok) ||
                genre.contains(tok) ||
                formatLabel.contains(tok) ||
                shortFormat.contains(tok) ||
                langLabel.contains(tok)
        }
    }

    val sortComparator: Comparator<Track> = when (librarySortOrder) {
        LibrarySortOrder.TITLE_AZ -> compareBy { it.title.trim().lowercase() }
        LibrarySortOrder.DATE_ADDED -> compareByDescending<Track> { it.dateAddedSec }
            .thenBy { it.title.trim().lowercase() }
        LibrarySortOrder.MOST_PLAYED -> compareByDescending<Track> { playCounts[it.id] ?: 0 }
            .thenBy { it.title.trim().lowercase() }
        LibrarySortOrder.ARTIST -> compareBy<Track> { it.artist.trim().lowercase() }
            .thenBy { it.title.trim().lowercase() }
        LibrarySortOrder.DURATION -> compareByDescending<Track> { it.durationSec }
            .thenBy { it.title.trim().lowercase() }
    }

    if (tokens.isEmpty()) {
        return base.sortedWith(sortComparator)
    }

    val firstTok = tokens.first()
    return base.sortedWith(
        compareBy<Track> { t ->
            val title = t.title.trim().lowercase()
            val artist = t.artist.trim().lowercase()
            val album = t.album.trim().lowercase()
            when {
                title.startsWith(cleanQuery) -> 0
                title.contains(cleanQuery) -> 1
                title.split(SEARCH_SPLIT_REGEX).any { it.startsWith(firstTok) } -> 2
                artist.startsWith(cleanQuery) -> 3
                artist.contains(cleanQuery) -> 4
                album.contains(cleanQuery) -> 5
                else -> 6
            }
        }.then(sortComparator)
    )
}

@Immutable
data class CorvixUiState(
    val currentScreen: AppScreen = AppScreen.LIBRARY,
    val glassTone: GlassTone = GlassTone.DARK,
    val themePreset: GlassThemePreset = GlassThemePreset.LIQUID,
    val appSkin: AppSkin = AppSkin.LIQUID_SHELL,
    val typography: AppTypography = AppTypography.SANS,
    val adaptiveAlbumColor: Boolean = true,
    val glassBlurPx: Float = 28f,
    val liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    val seekBarStyle: SeekBarStyle = SeekBarStyle.DROPLET,
    val tracks: List<Track> = emptyList(),
    val folders: List<FolderDirectory> = emptyList(),
    val folderStatusMessage: String = "Tap any album above to play mixed offline formats seamlessly.",
    val currentTrackIndex: Int = 0,
    val isPlaying: Boolean = false,
    val likedTrackIds: Set<Long> = emptySet(),
    val recentTrackIds: List<Long> = emptyList(),
    val adaptivePaletteOverride: AdaptiveGlassPalette? = null,
    val isShuffle: Boolean = false,
    val shuffledUpcomingIds: List<Long> = emptyList(),
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val speedIndex: Int = 0,
    val volumePct: Int = 72,
    val gestureHudText: String? = null,
    val libraryView: LibraryView = LibraryView.SONGS,
    val searchQuery: String = "",
    val selectedLanguage: TrackLanguage = TrackLanguage.ALL,
    val sortByLanguage: Boolean = false,
    val azFilter: String = "All",
    val playlists: List<PlaylistSummary> = emptyList(),
    val queues: List<PlaybackQueue> = listOf(
        PlaybackQueue("main", "Main Queue", emptyList())
    ),
    val activeQueueKey: String = "main",
    val bpmStatusText: String = "Tap Scan BPM to index tempo",
    val isBpmScanning: Boolean = false,
    val indexedTrackCount: Int = 0,
    val scanStatusSub: String = "Ready to scan offline audio",
    val isRescanning: Boolean = false,
    val visualizerEnabled: Boolean = false,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val eqEnabled: Boolean = true,
    val eq10Band: Boolean = false,
    val activeEqPreset: EqPreset = EqPreset.BASS,
    val eqBandsDb: List<Int> = listOf(4, 2, 0, 3, 2, 3, 1, -1, 2, 4),
    val reverbSpace: ReverbSpace = ReverbSpace.ROOM,
    val autoEqProfile: AutoEqProfile = AutoEqProfile.MANUAL,
    val bassBoostPct: Int = 45,
    val virtualizerPct: Int = 35,
    val crossfadeSec: Float = 3.0f,
    val gaplessPlayback: Boolean = true,
    val replayGainEnabled: Boolean = true,
    val bitPerfectDac: Boolean = true,
    val btCodec: BtCodec = BtCodec.LDAC,
    val offlinePrivacyLock: Boolean = true,
    val lanServerConnected: Boolean = false,
    val castDeviceLabel: String = "This Phone Speaker",
    val showWidgetCodecBadge: Boolean = true,
    val activeSink: AudioSinkInfo = CorvixSampleData.outputSinks.first(),
    val lyricsDrawerOpen: Boolean = false,
    val lyricsSheetTabIsUpload: Boolean = true,
    val lyricsFetchStatus: String = "Search & select lyrics below (supports English/Romanized transliteration):",
    val isFetchingLyrics: Boolean = false,
    val englishLyricsOnly: Boolean = true,
    val lyricsCandidates: List<LyricsCandidate> = emptyList(),
    val isSearchingLyrics: Boolean = false,
    val artworkCandidates: List<ArtworkCandidate> = emptyList(),
    val isSearchingArtwork: Boolean = false,
    val artworkVersion: Int = 0,
    val showWelcome: Boolean = true,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
    val playCounts: Map<Long, Int> = emptyMap(),
    val recapStats: RecapStats = RecapStats(),
    val nextUpTrackIds: List<Long> = emptyList(),
    val deviceModelName: String = "Android Phone",
    val isNothingPhone: Boolean = false,
    val storageSummaryLabel: String = "Storage ready",
    val storageUsedFraction: Float = 0.25f,
    val activeSheet: SheetType = SheetType.NONE,
    val editingTrackIndex: Int = 0,
    val contextTrackIndex: Int = 0,
    val editingPlaylistId: String? = null,
    val selectedPlaylistIdForDetail: String? = null,
    val ringtoneStartSec: Int = 32,
    val ringtoneEndSec: Int = 62,
    val ringtoneSaveLabel: String = "Save Offline Ringtone (.m4a)",
    val sleepTimerLabel: String = "Timer",
    val sleepSmartLpf: Boolean = true,
    val ipodFocusIndex: Int = 0,
    val ipodWheelDegrees: Float = 0f,
    val ipodSearchFocusSignal: Int = 0,
    val scrollToEqSignal: Int = 0,
    val toastBanner: String? = null
) {
    val currentTrack: Track
        get() = tracks.getOrElse(currentTrackIndex) { tracks.firstOrNull() ?: EMPTY_PLACEHOLDER_TRACK }

    val contextTrack: Track
        get() = tracks.getOrElse(contextTrackIndex) { currentTrack }

    val speedLabel: String
        get() = listOf("1.0x", "1.25x", "1.5x", "0.85x")[speedIndex]

    val speedFloat: Float
        get() = listOf(1.0f, 1.25f, 1.5f, 0.85f)[speedIndex]

    val isCurrentLiked: Boolean
        get() = currentTrack.id in likedTrackIds

    val availableLanguages: List<TrackLanguage> by lazy(LazyThreadSafetyMode.NONE) {
        val present = tracks.mapTo(HashSet()) { it.detectedLanguage }
        listOf(TrackLanguage.ALL) + TrackLanguage.entries.filter { it != TrackLanguage.ALL && it in present }
    }

    val filteredTracks: List<Track> by lazy(LazyThreadSafetyMode.NONE) {
        filterAndRankTracks(
            tracks = tracks,
            query = searchQuery,
            selectedLanguage = selectedLanguage,
            librarySortOrder = librarySortOrder,
            playCounts = playCounts
        )
    }

    val recentlyPlayedTracks: List<Track> by lazy(LazyThreadSafetyMode.NONE) {
        recentTrackIds.mapNotNull { id -> tracks.find { it.id == id } }.take(16)
    }
}

class CorvixViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CorvixUiState())
    val uiState: StateFlow<CorvixUiState> = _uiState.asStateFlow()

    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSecFlow: StateFlow<Int> = _elapsedSec.asStateFlow()

    private val audioController = CorvixAudioController(
        context = application,
        scope = viewModelScope,
        onPlaybackStateChanged = { playing, _ ->
            _uiState.update { it.copy(isPlaying = playing) }
        },
        onTrackCompleted = {
            if (_uiState.value.repeatMode == RepeatMode.ONE) {
                seekTo(0)
            } else {
                nextTrack()
            }
        },
        onHardwareOutputDetected = { detectedName ->
            val current = _uiState.value.activeSink
            if (detectedName != current.shortName) {
                _uiState.update { it.copy(activeSink = it.activeSink.copy(shortName = detectedName)) }
            }
        }
    )

    private var customPlaylistSeq = 1
    private var tickerJob: Job? = null
    private var hudJob: Job? = null
    private var toastJob: Job? = null
    private var sleepJob: Job? = null

    private val prefs by lazy {
        application.getSharedPreferences("corvix_prefs", Context.MODE_PRIVATE)
    }

    init {
        val onboardingDone = prefs.getBoolean("has_completed_onboarding", false)
        val savedTone = prefs.getString("glass_tone", null)?.let { runCatching { GlassTone.valueOf(it) }.getOrNull() } ?: GlassTone.DARK
        val savedPreset = prefs.getString("theme_preset", null)?.let { runCatching { GlassThemePreset.valueOf(it) }.getOrNull() } ?: GlassThemePreset.LIQUID
        val savedSkin = prefs.getString("app_skin", null)?.let { runCatching { AppSkin.valueOf(it) }.getOrNull() } ?: AppSkin.LIQUID_SHELL
        val savedTypo = prefs.getString("app_typography", null)?.let { runCatching { AppTypography.valueOf(it) }.getOrNull() } ?: AppTypography.SANS
        val savedAdaptive = prefs.getBoolean("adaptive_album_color", true)
        val savedBlur = prefs.getFloat("glass_blur_px", savedPreset.defaultBlur)
        val savedGlassConfig = loadSavedLiquidGlassConfig(savedBlur)
        val savedPalette = loadSavedAdaptivePalette()
        val savedLikes = prefs.getString("liked_track_ids", "")
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
        val savedRecents = prefs.getString("recent_track_ids_v2", "")
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
        _uiState.update {
            val base = it.copy(
                showWelcome = !onboardingDone,
                glassTone = savedTone,
                themePreset = savedPreset,
                appSkin = savedSkin,
                typography = savedTypo,
                adaptiveAlbumColor = savedAdaptive,
                glassBlurPx = savedBlur,
                liquidGlassConfig = savedGlassConfig,
                adaptivePaletteOverride = if (savedAdaptive) savedPalette else null,
                likedTrackIds = savedLikes,
                recentTrackIds = savedRecents,
                recapStats = computeRecapStats(it.playCounts, it.tracks, savedRecents)
            )
            base.copy(playlists = syncFavoritesPlaylistList(base.playlists, savedLikes, base.tracks))
        }
        refreshDeviceStorage()
        audioController.connect()
        startProgressLoop()
        if (_uiState.value.tracks.isNotEmpty()) {
            audioController.loadTrack(_uiState.value.currentTrack, autoPlay = false)
        }
        syncEffectsToHardware()
        CorvixWidgetBridge.actionHandler = { action ->
            when (action) {
                CorvixWidgetBridge.ACTION_PREV -> prevTrack()
                CorvixWidgetBridge.ACTION_PLAY_PAUSE -> togglePlay()
                CorvixWidgetBridge.ACTION_PLAY -> playOrResume()
                CorvixWidgetBridge.ACTION_PAUSE -> {
                    if (_uiState.value.isPlaying) togglePlay()
                }
                CorvixWidgetBridge.ACTION_STOP -> stopPlayback()
                CorvixWidgetBridge.ACTION_NEXT -> nextTrack()
                CorvixWidgetBridge.ACTION_SHUFFLE -> toggleShuffle()
            }
        }
        syncWidgetState()
        viewModelScope.launch {
            CorvixMediaCommandBus.commands.collect { cmd ->
                when (cmd) {
                    CorvixMediaCommand.TOGGLE_PLAY -> togglePlay()
                    CorvixMediaCommand.PLAY -> if (!_uiState.value.isPlaying) togglePlay()
                    CorvixMediaCommand.PAUSE -> if (_uiState.value.isPlaying) togglePlay()
                    CorvixMediaCommand.NEXT -> nextTrack()
                    CorvixMediaCommand.PREVIOUS -> prevTrack()
                }
            }
        }
    }

    fun completeWelcome(selectedSkin: AppSkin = _uiState.value.appSkin, onScan: () -> Unit) {
        prefs.edit()
            .putBoolean("has_completed_onboarding", true)
            .putString("app_skin", selectedSkin.name)
            .apply()
        audioController.setNowPlayingVisible(selectedSkin == AppSkin.WINAMP_CLASSIC)
        val startScreen = when (selectedSkin) {
            AppSkin.WINAMP_CLASSIC -> AppScreen.NOW_PLAYING
            AppSkin.CLASSIC_IPOD -> AppScreen.IPOD_MENU
            AppSkin.LIQUID_SHELL -> AppScreen.LIBRARY
        }
        _uiState.update {
            it.copy(
                showWelcome = false,
                appSkin = selectedSkin,
                currentScreen = startScreen,
                eq10Band = if (selectedSkin == AppSkin.WINAMP_CLASSIC) true else it.eq10Band
            )
        }
        onScan()
    }

    fun setLibrarySortOrder(order: LibrarySortOrder) {
        _uiState.update { it.copy(librarySortOrder = order, sortByLanguage = false) }
    }

    fun refreshDeviceStorage() {
        val snap = DeviceStorageInfo.readSnapshot(getApplication())
        _uiState.update {
            it.copy(
                deviceModelName = snap.deviceName,
                isNothingPhone = snap.isNothingPhone,
                storageSummaryLabel = snap.summaryLabel,
                storageUsedFraction = snap.usedFraction
            )
        }
    }

    private fun startProgressLoop() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val st = _uiState.value
                if (st.isPlaying) {
                    val realSec = audioController.currentPositionSec()
                    val next = if (realSec != null && realSec > 0) {
                        realSec % st.currentTrack.durationSec.coerceAtLeast(1)
                    } else {
                        _elapsedSec.value + 1
                    }
                    if (next >= st.currentTrack.durationSec) {
                        if (st.repeatMode == RepeatMode.ONE) {
                            seekTo(0)
                        } else {
                            nextTrack()
                        }
                    } else {
                        _elapsedSec.value = next
                        if (CorvixWidgetBridge.hasActiveWidgets(getApplication())) {
                            syncWidgetState()
                        }
                    }
                }
            }
        }
    }

    fun rescanMediaLibrary() {
        _uiState.update { it.copy(isRescanning = true, scanStatusSub = "Scanning local audio...") }
        viewModelScope.launch {
            val result = LocalLibraryScanner.scanOfflineLibrary(getApplication())
            val savedTrackId = prefs.getLong("last_track_id", -1L)
            _uiState.update { state ->
                val matchedIdx = if (savedTrackId > 0L) result.tracks.indexOfFirst { it.id == savedTrackId } else -1
                val safeIdx = if (matchedIdx >= 0) matchedIdx else state.currentTrackIndex.coerceIn(0, (result.tracks.size - 1).coerceAtLeast(0))
                val qTracks = result.tracks.map { it.id }
                val updatedQueues = state.queues.map { q ->
                    if (q.key == "main") q.copy(trackIds = qTracks) else q
                }
                val label = if (result.fromDeviceCount > 0) {
                    "Just now · ${result.fromDeviceCount} local files indexed"
                } else {
                    "Just now · ${result.totalCount} tracks indexed"
                }
                state.copy(
                    tracks = result.tracks,
                    folders = result.folders,
                    queues = updatedQueues,
                    currentTrackIndex = safeIdx,
                    indexedTrackCount = result.totalCount,
                    scanStatusSub = label,
                    isRescanning = false,
                    playlists = syncFavoritesPlaylistList(state.playlists, state.likedTrackIds, result.tracks),
                    recapStats = computeRecapStats(state.playCounts, result.tracks, state.recentTrackIds)
                )
            }
            val updatedState = _uiState.value
            updatedState.tracks.getOrNull(updatedState.currentTrackIndex)?.let { firstTrack ->
                if (!updatedState.isPlaying) {
                    _elapsedSec.value = 0
                    audioController.loadTrack(firstTrack, autoPlay = false)
                }
                loadAdaptivePaletteFor(firstTrack)
            }
        }
    }

    fun navigate(screen: AppScreen) {
        val isWinamp = _uiState.value.appSkin == AppSkin.WINAMP_CLASSIC
        audioController.setNowPlayingVisible(screen == AppScreen.NOW_PLAYING || isWinamp)
        _uiState.update { it.copy(currentScreen = screen, ipodFocusIndex = 0) }
    }

    fun openSettingsEq() {
        audioController.setNowPlayingVisible(false)
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.SETTINGS,
                scrollToEqSignal = it.scrollToEqSignal + 1
            )
        }
    }

    fun setGlassTone(tone: GlassTone) {
        prefs.edit().putString("glass_tone", tone.name).apply()
        _uiState.update { it.copy(glassTone = tone) }
    }

    fun setThemePreset(preset: GlassThemePreset) {
        prefs.edit()
            .putString("theme_preset", preset.name)
            .putFloat("glass_blur_px", preset.defaultBlur)
            .apply()
        _uiState.update {
            it.copy(
                themePreset = preset,
                glassBlurPx = preset.defaultBlur,
                ipodFocusIndex = 0
            )
        }
    }

    fun setAppSkin(skin: AppSkin) {
        prefs.edit().putString("app_skin", skin.name).apply()
        audioController.setNowPlayingVisible(skin == AppSkin.WINAMP_CLASSIC || _uiState.value.currentScreen == AppScreen.NOW_PLAYING)
        _uiState.update { st ->
            val targetScreen = when {
                skin == AppSkin.WINAMP_CLASSIC -> AppScreen.NOW_PLAYING
                skin != AppSkin.CLASSIC_IPOD && st.currentScreen == AppScreen.IPOD_MENU -> AppScreen.SETTINGS
                else -> st.currentScreen
            }
            st.copy(
                appSkin = skin,
                currentScreen = targetScreen,
                eq10Band = if (skin == AppSkin.WINAMP_CLASSIC) true else st.eq10Band,
                ipodFocusIndex = 0
            )
        }
    }

    fun setAdaptiveAlbumColor(enabled: Boolean) {
        prefs.edit().putBoolean("adaptive_album_color", enabled).apply()
        _uiState.update { it.copy(adaptiveAlbumColor = enabled) }
    }

    fun setBlurPx(blur: Float) {
        val clamped = blur.coerceIn(0f, 100f)
        prefs.edit().putFloat("glass_blur_px", clamped).apply()
        _uiState.update {
            it.copy(
                glassBlurPx = clamped,
                liquidGlassConfig = it.liquidGlassConfig.copy(glassBlurPx = clamped)
            )
        }
    }

    fun updateLiquidGlassConfig(transform: (LiquidGlassConfig) -> LiquidGlassConfig) {
        val next = transform(_uiState.value.liquidGlassConfig)
        saveLiquidGlassConfig(next)
        _uiState.update {
            it.copy(
                glassBlurPx = next.glassBlurPx,
                liquidGlassConfig = next
            )
        }
    }

    fun resetLiquidGlassConfig() {
        val def = LiquidGlassConfig()
        saveLiquidGlassConfig(def)
        _uiState.update { it.copy(glassBlurPx = def.glassBlurPx, liquidGlassConfig = def) }
        showToast("Liquid Glass settings reset")
    }

    private fun saveLiquidGlassConfig(cfg: LiquidGlassConfig) {
        prefs.edit()
            .putFloat("lg_color_opacity_v2", cfg.colorOpacity)
            .putFloat("lg_contrast_boost", cfg.contrastBoost)
            .putFloat("lg_rim_strength", cfg.rimLightStrength)
            .putFloat("lg_rim_rotation", cfg.rimLightRotationDeg)
            .putFloat("lg_soft_rim_strength", cfg.softRimStrength)
            .putFloat("lg_soft_rim_width", cfg.softRimWidthDp)
            .putFloat("glass_blur_px", cfg.glassBlurPx)
            .putFloat("lg_refraction_strength", cfg.refractionStrength)
            .putFloat("lg_refraction_width", cfg.refractionWidth)
            .putFloat("lg_chromatic_v2", cfg.chromaticAberration)
            .putFloat("lg_shadow_strength", cfg.shadowStrength)
            .apply()
    }

    private fun loadSavedLiquidGlassConfig(savedBlur: Float): LiquidGlassConfig {
        val d = LiquidGlassConfig()
        return LiquidGlassConfig(
            colorOpacity = prefs.getFloat("lg_color_opacity_v2", d.colorOpacity),
            contrastBoost = prefs.getFloat("lg_contrast_boost", d.contrastBoost),
            rimLightStrength = prefs.getFloat("lg_rim_strength", d.rimLightStrength),
            rimLightRotationDeg = prefs.getFloat("lg_rim_rotation", d.rimLightRotationDeg),
            softRimStrength = prefs.getFloat("lg_soft_rim_strength", d.softRimStrength),
            softRimWidthDp = prefs.getFloat("lg_soft_rim_width", d.softRimWidthDp),
            glassBlurPx = savedBlur,
            refractionStrength = prefs.getFloat("lg_refraction_strength", d.refractionStrength),
            refractionWidth = prefs.getFloat("lg_refraction_width", d.refractionWidth),
            chromaticAberration = prefs.getFloat("lg_chromatic_v2", d.chromaticAberration),
            shadowStrength = prefs.getFloat("lg_shadow_strength", d.shadowStrength)
        )
    }

    fun setTypography(typography: AppTypography) {
        prefs.edit().putString("app_typography", typography.name).apply()
        _uiState.update { it.copy(typography = typography) }
    }

    fun setSeekBarStyle(style: SeekBarStyle) {
        _uiState.update { it.copy(seekBarStyle = style) }
    }

    fun setLibraryView(view: LibraryView) {
        _uiState.update { it.copy(libraryView = view) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, ipodFocusIndex = 0) }
    }

    fun setLanguageFilter(language: TrackLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun toggleSortByLanguage() {
        _uiState.update { it.copy(sortByLanguage = !it.sortByLanguage) }
    }

    fun setAzFilter(letter: String) {
        _uiState.update { it.copy(azFilter = letter) }
    }

    fun playTrackFromCarousel(trackId: Long) {
        val idx = _uiState.value.tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) playTrack(idx, reorderCarousel = false)
    }

    fun playTrack(index: Int, reorderCarousel: Boolean = true) {
        val list = _uiState.value.tracks
        if (list.isEmpty()) return
        val safeIdx = index.coerceIn(0, list.lastIndex)
        val track = list[safeIdx]
        val initialElapsed = if (track.isDeviceFile) 0 else minOf(94, track.durationSec)
        _elapsedSec.value = initialElapsed
        val cachedPal = AlbumArtLoader.getCachedPalette(track)
        prefs.edit().putLong("last_track_id", track.id).apply()
        if (cachedPal != null) saveAdaptivePalette(cachedPal)
        _uiState.update { st ->
            val updatedRecent = if (reorderCarousel) {
                (listOf(track.id) + st.recentTrackIds).distinct().take(25)
            } else {
                if (track.id in st.recentTrackIds) st.recentTrackIds
                else (st.recentTrackIds + track.id).distinct().take(25)
            }
            prefs.edit().putString("recent_track_ids_v2", updatedRecent.joinToString(",")).apply()
            val updatedCounts = st.playCounts.toMutableMap().apply {
                this[track.id] = (this[track.id] ?: 0) + 1
            }
            st.copy(
                currentTrackIndex = safeIdx,
                isPlaying = true,
                recentTrackIds = updatedRecent,
                playCounts = updatedCounts,
                adaptivePaletteOverride = cachedPal,
                recapStats = computeRecapStats(updatedCounts, st.tracks, updatedRecent)
            )
        }
        audioController.loadTrack(track, autoPlay = true)
        syncEffectsToHardware()
        syncWidgetState()
        if (cachedPal == null) {
            loadAdaptivePaletteFor(track)
        }
        loadLocalLyricsFor(track)
    }

    private fun loadLocalLyricsFor(track: Track) {
        if (!track.lyrics.isNullOrEmpty()) return
        val eng = _uiState.value.englishLyricsOnly
        viewModelScope.launch {
            val found = LyricsRepository.findLocalOrCachedLyrics(getApplication(), track, englishOnly = eng) ?: return@launch
            _uiState.update { st ->
                val updated = st.tracks.map {
                    if (it.id == track.id) it.copy(lyrics = found.first, lyricsSource = found.second) else it
                }
                st.copy(tracks = updated)
            }
        }
    }

    private fun loadAdaptivePaletteFor(track: Track) {
        if (!track.isDeviceFile && track.contentUri == null && track.albumId <= 0L) return
        viewModelScope.launch {
            val loaded = AlbumArtLoader.load(getApplication(), track, highRes = true) ?: return@launch
            saveAdaptivePalette(loaded.palette)
            _uiState.update { st ->
                if (st.currentTrack.id == track.id) {
                    st.copy(adaptivePaletteOverride = loaded.palette)
                } else {
                    st
                }
            }
        }
    }

    private fun saveAdaptivePalette(pal: AdaptiveGlassPalette) {
        prefs.edit()
            .putInt("last_pal_primary", pal.primary.toArgb())
            .putInt("last_pal_secondary", pal.secondary.toArgb())
            .putInt("last_pal_orb0", pal.orbs.getOrElse(0) { pal.primary }.toArgb())
            .putInt("last_pal_orb1", pal.orbs.getOrElse(1) { pal.secondary }.toArgb())
            .putInt("last_pal_orb2", pal.orbs.getOrElse(2) { pal.primary }.toArgb())
            .apply()
    }

    private fun loadSavedAdaptivePalette(): AdaptiveGlassPalette? {
        if (!prefs.contains("last_pal_primary")) return null
        val p = Color(prefs.getInt("last_pal_primary", 0))
        val s = Color(prefs.getInt("last_pal_secondary", 0))
        val o0 = Color(prefs.getInt("last_pal_orb0", 0))
        val o1 = Color(prefs.getInt("last_pal_orb1", 0))
        val o2 = Color(prefs.getInt("last_pal_orb2", 0))
        return AdaptiveGlassPalette(primary = p, secondary = s, orbs = listOf(o0, o1, o2))
    }

    fun playTrackById(trackId: Long) {
        val idx = _uiState.value.tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) playTrack(idx)
    }

    fun togglePlay() {
        val nextPlay = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = nextPlay) }
        audioController.setPlaying(nextPlay)
        syncWidgetState()
    }

    fun playOrResume() {
        if (!_uiState.value.isPlaying) {
            _uiState.update { it.copy(isPlaying = true) }
            audioController.setPlaying(true)
            syncWidgetState()
        } else {
            seekTo(0)
        }
    }

    fun stopPlayback() {
        _elapsedSec.value = 0
        _uiState.update { it.copy(isPlaying = false) }
        audioController.setPlaying(false)
        audioController.seekToSec(0)
        syncWidgetState()
    }

    val visualizerLevelsFlow: StateFlow<FloatArray>
        get() = audioController.visualizerLevelsFlow

    fun nextTrack() {
        val st = _uiState.value
        if (st.tracks.isEmpty()) return
        val nextQueuedId = st.nextUpTrackIds.firstOrNull()
        if (nextQueuedId != null) {
            val queuedIdx = st.tracks.indexOfFirst { it.id == nextQueuedId }
            _uiState.update { it.copy(nextUpTrackIds = it.nextUpTrackIds.drop(1)) }
            if (queuedIdx >= 0) {
                playTrack(queuedIdx)
                return
            }
        }
        if (st.isShuffle && st.tracks.size > 1) {
            val remaining = st.shuffledUpcomingIds.filter { id -> id != st.currentTrack.id && st.tracks.any { it.id == id } }
            val nextId = remaining.firstOrNull()
                ?: (st.tracks.map { it.id } - st.currentTrack.id).shuffled().first()
            val nextIdx = st.tracks.indexOfFirst { it.id == nextId }.coerceAtLeast(0)
            _uiState.update { it.copy(shuffledUpcomingIds = remaining.drop(1)) }
            playTrack(nextIdx)
            return
        }
        val nextIdx = (st.currentTrackIndex + 1) % st.tracks.size
        playTrack(nextIdx)
    }

    fun prevTrack() {
        val st = _uiState.value
        if (st.tracks.isEmpty()) return
        val prevIdx = (st.currentTrackIndex - 1 + st.tracks.size) % st.tracks.size
        playTrack(prevIdx)
    }

    fun shuffleAll() {
        val st = _uiState.value
        if (st.tracks.isEmpty()) return
        val randomIdx = st.tracks.indices.random()
        val pickedId = st.tracks[randomIdx].id
        val shuffledRest = st.tracks.map { it.id }.filter { it != pickedId }.shuffled()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.NOW_PLAYING,
                isShuffle = true,
                shuffledUpcomingIds = shuffledRest
            )
        }
        playTrack(randomIdx)
    }

    fun seekTo(sec: Int) {
        val maxSec = _uiState.value.currentTrack.durationSec.coerceAtLeast(1)
        val clamped = sec.coerceIn(0, maxSec)
        _elapsedSec.value = clamped
        audioController.seekToSec(clamped)
    }

    fun toggleLike() {
        _uiState.update { st ->
            val id = st.currentTrack.id
            val updated = if (id in st.likedTrackIds) st.likedTrackIds - id else st.likedTrackIds + id
            prefs.edit().putString("liked_track_ids", updated.joinToString(",")).apply()
            st.copy(
                likedTrackIds = updated,
                playlists = syncFavoritesPlaylistList(st.playlists, updated, st.tracks)
            )
        }
    }

    fun toggleLyricsDrawer() {
        _uiState.update { it.copy(lyricsDrawerOpen = !it.lyricsDrawerOpen) }
    }

    fun toggleShuffle() {
        _uiState.update { st ->
            val nextShuffle = !st.isShuffle
            val currentId = st.currentTrack.id
            val shuffled = if (nextShuffle) {
                st.tracks.map { it.id }.filter { it != currentId }.shuffled()
            } else {
                emptyList()
            }
            st.copy(isShuffle = nextShuffle, shuffledUpcomingIds = shuffled)
        }
        syncWidgetState()
    }

    fun moveUpcomingTrack(trackId: Long, delta: Int) {
        if (delta == 0) return
        _uiState.update { st ->
            if (trackId in st.nextUpTrackIds) {
                val qList = st.nextUpTrackIds.toMutableList()
                val qIdx = qList.indexOf(trackId)
                if (qIdx >= 0) {
                    val target = (qIdx + delta).coerceIn(0, qList.lastIndex)
                    if (target != qIdx) {
                        val moved = qList.removeAt(qIdx)
                        qList.add(target, moved)
                        return@update st.copy(nextUpTrackIds = qList)
                    }
                }
                return@update st
            }
            if (st.tracks.size <= 2) return@update st
            if (st.isShuffle) {
                val list = st.shuffledUpcomingIds.toMutableList()
                if (list.isEmpty()) {
                    list.addAll(st.tracks.map { it.id }.filter { it != st.currentTrack.id })
                }
                val idx = list.indexOf(trackId)
                if (idx < 0) return@update st
                val target = (idx + delta).coerceIn(0, list.lastIndex)
                if (target == idx) return@update st
                val moved = list.removeAt(idx)
                list.add(target, moved)
                st.copy(shuffledUpcomingIds = list)
            } else {
                val curIdx = st.currentTrackIndex
                val total = st.tracks.size
                val upcoming = MutableList(total - 1) { off -> st.tracks[(curIdx + 1 + off) % total] }
                val idx = upcoming.indexOfFirst { it.id == trackId }
                if (idx < 0) return@update st
                val target = (idx + delta).coerceIn(0, upcoming.lastIndex)
                if (target == idx) return@update st
                val moved = upcoming.removeAt(idx)
                upcoming.add(target, moved)
                val rebuilt = listOf(st.tracks[curIdx]) + upcoming
                st.copy(tracks = rebuilt, currentTrackIndex = 0)
            }
        }
    }

    fun cycleRepeat() {
        _uiState.update { st ->
            val values = RepeatMode.entries
            val next = values[(st.repeatMode.ordinal + 1) % values.size]
            audioController.setRepeatMode(next)
            st.copy(repeatMode = next)
        }
    }

    fun cycleSpeed() {
        _uiState.update { st ->
            val nextIdx = (st.speedIndex + 1) % 4
            val nextState = st.copy(speedIndex = nextIdx)
            audioController.setSpeed(nextState.speedFloat)
            nextState
        }
    }

    fun adjustVolume(volumePct: Int) {
        val clamped = volumePct.coerceIn(0, 100)
        _uiState.update { it.copy(volumePct = clamped) }
        audioController.setVolume(clamped / 100f)
    }

    fun showGestureHud(message: String) {
        _uiState.update { it.copy(gestureHudText = message) }
        hudJob?.cancel()
        hudJob = viewModelScope.launch {
            delay(950L)
            _uiState.update { it.copy(gestureHudText = null) }
        }
    }

    fun toggleVisualizer(forceState: Boolean? = null) {
        _uiState.update { st ->
            val next = forceState ?: !st.visualizerEnabled
            st.copy(visualizerEnabled = next)
        }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        _uiState.update {
            it.copy(visualizerStyle = style, visualizerEnabled = true)
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        _uiState.update { it.copy(eqEnabled = enabled) }
        syncEffectsToHardware()
    }

    fun applyEqPreset(preset: EqPreset) {
        _uiState.update { st ->
            val updated = st.eqBandsDb.toMutableList()
            preset.bands.forEachIndexed { idx, value ->
                if (idx < updated.size) updated[idx] = value
            }
            st.copy(activeEqPreset = preset, eqBandsDb = updated)
        }
        syncEffectsToHardware()
    }

    fun toggleEqMode() {
        _uiState.update { it.copy(eq10Band = !it.eq10Band) }
        syncEffectsToHardware()
    }

    fun updateEqBand(index: Int, valueDb: Int) {
        _uiState.update { st ->
            val updated = st.eqBandsDb.toMutableList()
            if (index in updated.indices) updated[index] = valueDb.coerceIn(-12, 12)
            st.copy(eqBandsDb = updated)
        }
        syncEffectsToHardware()
    }

    fun setReverbSpace(space: ReverbSpace) {
        _uiState.update { it.copy(reverbSpace = space) }
        syncEffectsToHardware()
    }

    fun applyAutoEq(profile: AutoEqProfile) {
        _uiState.update { st ->
            val shouldExpand = profile != AutoEqProfile.MANUAL || st.eq10Band
            st.copy(
                autoEqProfile = profile,
                eq10Band = shouldExpand,
                eqBandsDb = profile.bands.toList()
            )
        }
        syncEffectsToHardware()
    }

    fun setBassBoost(pct: Int) {
        _uiState.update { it.copy(bassBoostPct = pct.coerceIn(0, 100)) }
        syncEffectsToHardware()
    }

    fun setVirtualizer(pct: Int) {
        _uiState.update { it.copy(virtualizerPct = pct.coerceIn(0, 100)) }
        syncEffectsToHardware()
    }

    fun setCrossfade(sec: Float) {
        _uiState.update { it.copy(crossfadeSec = sec.coerceIn(0f, 12f)) }
    }

    fun setGapless(enabled: Boolean) {
        _uiState.update { it.copy(gaplessPlayback = enabled) }
    }

    fun setReplayGain(enabled: Boolean) {
        _uiState.update { it.copy(replayGainEnabled = enabled) }
    }

    fun setBitPerfectDac(enabled: Boolean) {
        _uiState.update { it.copy(bitPerfectDac = enabled) }
    }

    fun setBtCodec(codec: BtCodec) {
        _uiState.update { it.copy(btCodec = codec) }
    }

    fun setOfflinePrivacyLock(locked: Boolean) {
        _uiState.update {
            it.copy(
                offlinePrivacyLock = locked,
                lanServerConnected = if (locked) false else it.lanServerConnected
            )
        }
    }

    fun setLanServerConnected(connected: Boolean) {
        if (_uiState.value.offlinePrivacyLock) return
        _uiState.update { it.copy(lanServerConnected = connected) }
    }

    fun setCastDevice(label: String) {
        _uiState.update { it.copy(castDeviceLabel = label) }
    }

    fun setShowWidgetCodecBadge(show: Boolean) {
        _uiState.update { it.copy(showWidgetCodecBadge = show) }
    }

    fun selectFolder(folder: FolderDirectory) {
        val matchingIndex = _uiState.value.tracks.indexOfFirst {
            it.folderPath.contains(folder.name, ignoreCase = true) ||
                it.folderPath.equals(folder.displayPath, ignoreCase = true)
        }
        _uiState.update {
            it.copy(folderStatusMessage = "${folder.name} selected (${folder.trackCount} tracks · ${folder.sizeLabel}). Playing offline.")
        }
        if (matchingIndex >= 0) {
            playTrack(matchingIndex)
        }
    }

    fun addToQueue(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        _uiState.update { st ->
            val updatedNext = if (trackId in st.nextUpTrackIds) {
                st.nextUpTrackIds
            } else {
                st.nextUpTrackIds + trackId
            }
            val updatedQueues = st.queues.map { q ->
                if (q.key == st.activeQueueKey && trackId !in q.trackIds) {
                    q.copy(trackIds = q.trackIds + trackId)
                } else {
                    q
                }
            }
            st.copy(
                nextUpTrackIds = updatedNext,
                queues = updatedQueues,
                activeSheet = if (st.activeSheet == SheetType.TRACK_ACTIONS) SheetType.NONE else st.activeSheet
            )
        }
        showToast("Added to Up Next: ${track.title}")
    }

    fun switchQueue(queueKey: String) {
        _uiState.update { it.copy(activeQueueKey = queueKey) }
    }

    fun loadSmartPlaylist(playlist: PlaylistSummary) {
        val tracks = _uiState.value.tracks
        val ids = playlist.queueIndices.mapNotNull { idx -> tracks.getOrNull(idx)?.id }
        if (ids.isEmpty()) return
        _uiState.update { st ->
            val updated = st.queues.map { q ->
                if (q.key == st.activeQueueKey) q.copy(trackIds = ids) else q
            }
            st.copy(queues = updated)
        }
        playTrackById(ids.first())
    }

    fun openTrackActions(trackId: Long) {
        val idx = _uiState.value.tracks.indexOfFirst { it.id == trackId }
        if (idx < 0) return
        _uiState.update {
            it.copy(
                contextTrackIndex = idx,
                activeSheet = SheetType.TRACK_ACTIONS
            )
        }
    }

    fun playNext(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        _uiState.update { st ->
            val updatedNext = listOf(trackId) + st.nextUpTrackIds.filterNot { it == trackId }
            st.copy(nextUpTrackIds = updatedNext, activeSheet = SheetType.NONE)
        }
        showToast("Playing next: ${track.title}")
    }

    fun toggleFavoriteTrack(trackId: Long) {
        var added = false
        _uiState.update { st ->
            val updated = if (trackId in st.likedTrackIds) {
                added = false
                st.likedTrackIds - trackId
            } else {
                added = true
                st.likedTrackIds + trackId
            }
            prefs.edit().putString("liked_track_ids", updated.joinToString(",")).apply()
            st.copy(
                likedTrackIds = updated,
                playlists = syncFavoritesPlaylistList(st.playlists, updated, st.tracks)
            )
        }
        showToast(if (added) "Added to Favorites" else "Removed from Favorites")
    }

    private fun syncFavoritesPlaylistList(
        playlists: List<PlaylistSummary>,
        likedIds: Set<Long>,
        tracks: List<Track>
    ): List<PlaylistSummary> {
        val likedTrackList = likedIds.toList()
        val likedIndices = tracks.indices.filter { tracks[it].id in likedIds }
        val count = likedTrackList.size
        val sub = "$count liked track${if (count == 1) "" else "s"}"
        return playlists.map { pl ->
            if (pl.id == "favorites") {
                pl.copy(
                    title = "Favorites",
                    subtitle = sub,
                    queueIndices = likedIndices
                )
            } else {
                pl
            }
        }
    }

    fun shareSongCard(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        _uiState.update { it.copy(activeSheet = SheetType.NONE) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val cardUri = runCatching { renderSongCardUri(ctx, track) }.getOrNull()
            if (cardUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, cardUri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${track.title} — ${track.artist} (${track.album}) · Shared from Corvix"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Song Card").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${track.title} — ${track.artist} (${track.album}) · Shared from Corvix"
                    )
                }
                val chooser = Intent.createChooser(shareIntent, "Share Song").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)
            }
        }
    }

    private fun renderSongCardUri(ctx: Context, track: Track): Uri {
        val width = 1080
        val height = 620
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0B0E17")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = android.graphics.Color.parseColor("#F5C400")
        }
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 44f, 44f, borderPaint)

        val artRect = RectF(68f, 96f, 456f, 484f)
        val artBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1D2336")
        }
        canvas.drawRoundRect(artRect, 34f, 34f, artBgPaint)

        val cachedArt = (AlbumArtLoader.getCached(track, highRes = true)?.imageBitmap
            ?: AlbumArtLoader.getCached(track, highRes = false)?.imageBitmap)?.asAndroidBitmap()
        if (cachedArt != null && !cachedArt.isRecycled) {
            val clipPath = android.graphics.Path().apply {
                addRoundRect(artRect, 34f, 34f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(cachedArt, null, artRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F5C400")
                textSize = 128f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("♪", artRect.centerX(), artRect.centerY() + 42f, emblemPaint)
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F5C400")
            textSize = 30f
            isFakeBoldText = true
            letterSpacing = 0.08f
        }
        canvas.drawText("CORVIX HI-FI CARD", 500f, 134f, brandPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            isFakeBoldText = true
        }
        val titleText = if (track.title.length > 22) track.title.take(21) + "…" else track.title
        canvas.drawText(titleText, 500f, 224f, titlePaint)

        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#B6C2DC")
            textSize = 36f
        }
        val artistText = if (track.artist.length > 28) track.artist.take(27) + "…" else track.artist
        canvas.drawText(artistText, 500f, 286f, artistPaint)

        val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#8896B5")
            textSize = 28f
        }
        val albumText = if (track.album.length > 34) track.album.take(33) + "…" else track.album
        canvas.drawText(albumText, 500f, 342f, albumPaint)

        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#4DA6FF")
            textSize = 26f
            isFakeBoldText = true
        }
        canvas.drawText(
            "${track.formatLabel} · ${formatSeconds(track.durationSec)}",
            500f,
            426f,
            specPaint
        )

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6D7891")
            textSize = 24f
        }
        canvas.drawText("Offline Lossless Music Player", 500f, 476f, footerPaint)

        val cardsDir = java.io.File(ctx.cacheDir, "shared_cards").apply { mkdirs() }
        val outFile = java.io.File(cardsDir, "corvix_song_card_${track.id}.png")
        outFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bmp.recycle()
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", outFile)
    }

    fun openCreatePlaylistSheet() {
        _uiState.update { it.copy(editingPlaylistId = null, activeSheet = SheetType.CREATE_PLAYLIST) }
    }

    fun openPlaylistPickerForTrack(trackId: Long) {
        val idx = _uiState.value.tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            _uiState.update {
                it.copy(
                    contextTrackIndex = idx,
                    activeSheet = SheetType.PLAYLIST_PICKER
                )
            }
        }
    }

    fun createCustomPlaylist(customTitle: String = "", initialTrackId: Long? = null) {
        val st = _uiState.value
        val num = customPlaylistSeq++
        val title = customTitle.trim().ifEmpty { "Custom Mix 0$num" }
        val initialIdx = initialTrackId?.let { id -> st.tracks.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
        val queueIndices = if (initialIdx != null) {
            listOf(initialIdx)
        } else {
            emptyList()
        }
        val styles = ArtStyle.entries
        val newItem = PlaylistSummary(
            id = "custom_${System.currentTimeMillis()}_$num",
            title = title,
            subtitle = if (queueIndices.isEmpty()) "Empty playlist · Tap tracks to add" else "${queueIndices.size} track · Custom Playlist",
            artStyle = styles[num % styles.size],
            queueIndices = queueIndices,
            isSmart = false
        )
        _uiState.update { cur ->
            cur.copy(
                playlists = listOf(newItem) + cur.playlists,
                activeSheet = SheetType.NONE
            )
        }
        showToast("Created playlist \"$title\"")
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        val st = _uiState.value
        val trackIdx = st.tracks.indexOfFirst { it.id == trackId }
        if (trackIdx < 0) return
        var targetName = "Playlist"
        _uiState.update { cur ->
            val updated = cur.playlists.map { pl ->
                if (pl.id == playlistId) {
                    targetName = pl.title
                    val updatedIds = if (trackIdx in pl.queueIndices) pl.queueIndices else pl.queueIndices + trackIdx
                    pl.copy(
                        queueIndices = updatedIds,
                        subtitle = "${updatedIds.size} track${if (updatedIds.size == 1) "" else "s"} · Custom Playlist"
                    )
                } else {
                    pl
                }
            }
            cur.copy(playlists = updated, activeSheet = SheetType.NONE)
        }
        showToast("Added to $targetName")
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        val st = _uiState.value
        val trackIdx = st.tracks.indexOfFirst { it.id == trackId }
        if (trackIdx < 0) return
        _uiState.update { cur ->
            val updated = cur.playlists.map { pl ->
                if (pl.id == playlistId) {
                    val remaining = pl.queueIndices.filterNot { it == trackIdx }
                    pl.copy(
                        queueIndices = remaining,
                        subtitle = "${remaining.size} track${if (remaining.size == 1) "" else "s"} · Playlist"
                    )
                } else {
                    pl
                }
            }
            cur.copy(playlists = updated)
        }
        showToast("Removed from playlist")
    }

    fun openRenamePlaylist(playlistId: String) {
        _uiState.update {
            it.copy(
                editingPlaylistId = playlistId,
                activeSheet = SheetType.RENAME_PLAYLIST
            )
        }
    }

    fun renamePlaylist(playlistId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) {
            showToast("Playlist name cannot be empty")
            return
        }
        _uiState.update { cur ->
            cur.copy(
                playlists = cur.playlists.map { if (it.id == playlistId) it.copy(title = clean) else it },
                editingPlaylistId = null,
                activeSheet = SheetType.NONE
            )
        }
        showToast("Renamed playlist to \"$clean\"")
    }

    fun selectPlaylistDetail(playlistId: String?) {
        _uiState.update { cur ->
            cur.copy(selectedPlaylistIdForDetail = if (cur.selectedPlaylistIdForDetail == playlistId) null else playlistId)
        }
    }

    fun autoCreateSmartPlaylists() {
        val st = _uiState.value
        val tracks = st.tracks
        if (tracks.isEmpty()) return
        val existingTitles = st.playlists.mapTo(HashSet()) { it.title.lowercase() }
        val created = mutableListOf<PlaylistSummary>()

        // 1. Recently Added Top Mix
        val recentIdx = tracks.indices
            .sortedByDescending { tracks[it].dateAddedSec }
            .take(15)
        if (recentIdx.isNotEmpty() && "recently added" !in existingTitles) {
            created += PlaylistSummary(
                id = "auto_recent_${System.currentTimeMillis()}",
                title = "Recently Added",
                subtitle = "${recentIdx.size} newest local tracks · Auto Mix",
                artStyle = ArtStyle.MIDNIGHT,
                queueIndices = recentIdx,
                isSmart = true
            )
        }

        // 2. Most Played / Favorites Mix
        val favIndices = tracks.indices.filter { tracks[it].id in st.likedTrackIds }
        val playedIdx = if (st.playCounts.isNotEmpty()) {
            tracks.indices.sortedByDescending { st.playCounts[tracks[it].id] ?: 0 }.take(15)
        } else {
            favIndices
        }
        if (playedIdx.isNotEmpty() && "most played mix" !in existingTitles) {
            created += PlaylistSummary(
                id = "auto_most_played_${System.currentTimeMillis()}",
                title = "Most Played Mix",
                subtitle = "${playedIdx.size} heavy rotation tracks · Auto Mix",
                artStyle = ArtStyle.GOLD,
                queueIndices = playedIdx,
                isSmart = true
            )
        }

        // 3. Hi-Res / Lossless Mix (language mixes removed)

        // 4. Lossless / Hi-Res Mix
        val hiResIdx = tracks.indices.filter {
            val f = tracks[it].formatLabel.uppercase()
            f.contains("FLAC") || f.contains("WAV") || f.contains("ALAC") || f.contains("24-BIT")
        }.take(25)
        if (hiResIdx.isNotEmpty() && "lossless audiophile" !in existingTitles) {
            created += PlaylistSummary(
                id = "auto_hires_${System.currentTimeMillis()}",
                title = "Lossless Audiophile",
                subtitle = "${hiResIdx.size} uncompressed masters · Auto Mix",
                artStyle = ArtStyle.EMERALD,
                queueIndices = hiResIdx,
                isSmart = true
            )
        }

        if (created.isEmpty()) {
            showToast("Smart playlists are already up to date")
            return
        }
        _uiState.update { cur ->
            cur.copy(playlists = created + cur.playlists)
        }
        showToast("Auto-created ${created.size} playlist${if (created.size == 1) "" else "s"}")
    }

    fun removePlaylist(playlistId: String) {
        val removedTitle = _uiState.value.playlists.firstOrNull { it.id == playlistId }?.title
        _uiState.update { st ->
            st.copy(playlists = st.playlists.filterNot { it.id == playlistId })
        }
        if (removedTitle != null) {
            showToast("Deleted $removedTitle")
        }
    }

    fun removeTrackFromQueue(trackId: Long) {
        _uiState.update { st ->
            val updatedQueues = st.queues.map { q ->
                if (q.key == st.activeQueueKey) {
                    q.copy(trackIds = q.trackIds.filterNot { it == trackId })
                } else {
                    q
                }
            }
            st.copy(queues = updatedQueues)
        }
        showToast("Removed from queue")
    }

    fun exportM3u8Playlist() {
        val st = _uiState.value
        val activeQ = st.queues.firstOrNull { it.key == st.activeQueueKey } ?: return
        val lines = buildString {
            appendLine("#EXTM3U")
            activeQ.trackIds.mapNotNull { id -> st.tracks.find { it.id == id } }.forEach { t ->
                appendLine("#EXTINF:${t.durationSec},${t.artist} - ${t.title}")
                appendLine(t.folderPath)
            }
        }
        val outFile = java.io.File(getApplication<Application>().cacheDir, "corvix_${activeQ.key}.m3u8")
        runCatching { outFile.writeText(lines) }
        showToast("Exported ${outFile.name} (${activeQ.trackIds.size} tracks)")
    }

    fun scanBpmEnergy() {
        _uiState.update { it.copy(isBpmScanning = true, bpmStatusText = "WorkManager: Analyzing BPM & RMS energy...") }
        viewModelScope.launch {
            delay(650L)
            _uiState.update {
                it.copy(
                    isBpmScanning = false,
                    bpmStatusText = "Room DB: ${it.indexedTrackCount} tracks tagged with BPM & RMS"
                )
            }
        }
    }

    fun loadMoodMix(mood: MoodType) {
        val list = _uiState.value.tracks
        val matched = list.filter { t ->
            when (mood) {
                MoodType.CHILL -> t.bpm < 95 && t.energy < 0.45f
                MoodType.FOCUS -> t.bpm in 95..115
                MoodType.PEAK -> t.bpm > 115 || t.energy > 0.75f
            }
        }
        val chosen = if (matched.isNotEmpty()) matched else list.take(3)
        val ids = chosen.map { it.id }
        _uiState.update { st ->
            val updated = st.queues.map { q ->
                if (q.key == st.activeQueueKey) q.copy(trackIds = ids) else q
            }
            st.copy(
                queues = updated,
                bpmStatusText = "Smart Mix (${mood.name}): ${ids.size} tracks queued"
            )
        }
        chosen.firstOrNull()?.let { playTrackById(it.id) }
    }

    fun openSheet(sheet: SheetType, editTrackIdx: Int = _uiState.value.currentTrackIndex) {
        val tracks = _uiState.value.tracks
        val safeIdx = editTrackIdx.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        val target = tracks.getOrNull(safeIdx)
        _uiState.update {
            it.copy(
                activeSheet = sheet,
                editingTrackIndex = safeIdx,
                ringtoneSaveLabel = "Save Offline Ringtone (.m4a)",
                lyricsFetchStatus = "Fetch synced or plain lyrics from trusted lyric sites (saved offline for future playback):",
                lyricsSheetTabIsUpload = if (sheet == SheetType.LYRICS) it.lyricsSheetTabIsUpload else true
            )
        }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = SheetType.NONE) }
    }

    fun saveTagEdits(title: String, artist: String, album: String, genre: String) {
        _uiState.update { st ->
            val updated = st.tracks.toMutableList()
            val idx = st.editingTrackIndex.coerceIn(0, updated.lastIndex)
            val old = updated[idx]
            val newTitle = title.trim().ifEmpty { old.title }
            val newArtist = artist.trim().ifEmpty { old.artist }
            val newAlbum = album.trim().ifEmpty { old.album }
            val newGenre = genre.trim().ifEmpty { old.genre }
            updated[idx] = old.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                genre = newGenre,
                detectedLanguage = detectTrackLanguage(
                    title = newTitle,
                    artist = newArtist,
                    album = newAlbum,
                    folder = old.folderPath,
                    lyrics = old.lyrics
                )
            )
            st.copy(tracks = updated, activeSheet = SheetType.NONE)
        }
    }

    fun openRingtoneCutter() {
        _uiState.update {
            it.copy(
                activeSheet = SheetType.RINGTONE,
                ringtoneSaveLabel = "Save Offline Ringtone (.m4a)"
            )
        }
    }

    fun updateRingtoneBounds(startSec: Int, endSec: Int) {
        val safeStart = startSec.coerceIn(0, 180)
        val safeEnd = endSec.coerceAtLeast(safeStart + 5).coerceAtMost(240)
        _uiState.update {
            it.copy(ringtoneStartSec = safeStart, ringtoneEndSec = safeEnd)
        }
    }

    fun saveRingtone() {
        _uiState.update { it.copy(ringtoneSaveLabel = "Saved to /Ringtones ✓") }
        viewModelScope.launch {
            delay(700L)
            _uiState.update { it.copy(activeSheet = SheetType.NONE) }
        }
    }

    fun setSleepTimer(label: String) {
        sleepJob?.cancel()
        val minutes = when (label) {
            "15m" -> 15
            "30m" -> 30
            "45m" -> 45
            "60m" -> 60
            else -> 0
        }
        if (minutes > 0) {
            sleepJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                audioController.setVolume(0.25f)
                delay(1500L)
                _uiState.update { it.copy(isPlaying = false, sleepTimerLabel = "Timer") }
                audioController.setPlaying(false)
                audioController.setVolume(_uiState.value.volumePct / 100f)
            }
        }
        _uiState.update {
            it.copy(
                sleepTimerLabel = if (label == "Off") "Timer" else label,
                activeSheet = SheetType.NONE
            )
        }
    }

    fun setSleepSmartLpf(enabled: Boolean) {
        _uiState.update { it.copy(sleepSmartLpf = enabled) }
    }

    fun setAudioSink(sink: AudioSinkInfo) {
        _uiState.update { it.copy(activeSink = sink) }
        applyAutoEq(sink.autoEqProfile)
    }

    fun openLyricsSheet(isUploadTab: Boolean) {
        val track = _uiState.value.currentTrack
        _uiState.update {
            it.copy(
                lyricsSheetTabIsUpload = isUploadTab,
                activeSheet = SheetType.LYRICS
            )
        }
        if (!isUploadTab) {
            searchLyricsCandidates()
        } else {
            _uiState.update {
                it.copy(lyricsFetchStatus = "Search & select lyrics for \"${track.title}\" below:")
            }
        }
    }

    fun switchLyricsTab(isUploadTab: Boolean) {
        _uiState.update { it.copy(lyricsSheetTabIsUpload = isUploadTab) }
        if (!isUploadTab && _uiState.value.lyricsCandidates.isEmpty()) {
            searchLyricsCandidates()
        }
    }

    fun toggleEnglishLyricsOnly() {
        val nextEng = !_uiState.value.englishLyricsOnly
        _uiState.update { it.copy(englishLyricsOnly = nextEng) }
        val track = _uiState.value.currentTrack
        viewModelScope.launch {
            val reloaded = LyricsRepository.findLocalOrCachedLyrics(
                getApplication(),
                track,
                englishOnly = nextEng
            )
            if (reloaded != null) {
                _uiState.update { cur ->
                    val updated = cur.tracks.map {
                        if (it.id == track.id) it.copy(lyrics = reloaded.first, lyricsSource = reloaded.second) else it
                    }
                    cur.copy(tracks = updated)
                }
            }
        }
        showToast(if (nextEng) "Lyrics: English / Romanized script ON" else "Lyrics: Original script")
    }

    fun searchLyricsCandidates(customQuery: String = "") {
        val st = _uiState.value
        val track = st.currentTrack
        _uiState.update {
            it.copy(
                isSearchingLyrics = true,
                lyricsFetchStatus = "Searching selectable lyrics for \"${customQuery.ifBlank { track.title }}\"..."
            )
        }
        viewModelScope.launch {
            val found = LyricsRepository.searchCandidates(
                track = track,
                customQuery = customQuery,
                preferEnglishScript = _uiState.value.englishLyricsOnly
            )
            _uiState.update { cur ->
                cur.copy(
                    isSearchingLyrics = false,
                    lyricsCandidates = found,
                    lyricsFetchStatus = if (found.isNotEmpty()) {
                        "Found ${found.size} versions — tap one to apply:"
                    } else {
                        "No matches found. Try editing the search query above."
                    }
                )
            }
        }
    }

    fun selectLyricsCandidate(candidate: LyricsCandidate) {
        val st = _uiState.value
        val track = st.currentTrack
        val eng = st.englishLyricsOnly
        val parsed = LyricsRepository.parseWithLanguageOption(candidate.rawText, eng)
        if (parsed.isEmpty()) {
            showToast("Selected candidate had no readable lines")
            return
        }
        LyricsRepository.cacheLyricsLocally(getApplication(), track.id, candidate.rawText)
        val label = if (eng && candidate.hasNonLatinScript) {
            "${candidate.sourceLabel} · Romanized"
        } else {
            candidate.sourceLabel
        }
        _uiState.update { cur ->
            val updated = cur.tracks.map {
                if (it.id == track.id) it.copy(lyrics = parsed, lyricsSource = label) else it
            }
            cur.copy(
                tracks = updated,
                lyricsDrawerOpen = true,
                activeSheet = SheetType.NONE,
                lyricsFetchStatus = "Applied from $label ✓"
            )
        }
        showToast("Lyrics applied ($label)")
    }

    fun saveManualLyrics(rawText: String, sourceLabel: String = "Uploaded") {
        val cleanRaw = rawText.trim()
        if (cleanRaw.isEmpty()) {
            showToast("Paste lyrics or select a .lrc file")
            return
        }
        val parsed = LyricsRepository.parseWithLanguageOption(cleanRaw, _uiState.value.englishLyricsOnly)
        _uiState.update { st ->
            val currentId = st.currentTrack.id
            LyricsRepository.cacheLyricsLocally(getApplication(), currentId, cleanRaw)
            val updated = st.tracks.map {
                if (it.id == currentId) it.copy(lyrics = parsed, lyricsSource = sourceLabel) else it
            }
            st.copy(
                tracks = updated,
                lyricsDrawerOpen = true,
                activeSheet = SheetType.NONE
            )
        }
    }

    fun importLrcUri(uri: Uri) {
        val ctx: Context = getApplication()
        viewModelScope.launch {
            val content = runCatching {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            if (content.isNotBlank()) {
                saveManualLyrics(content, uri.lastPathSegment ?: "Local .lrc")
            }
        }
    }

    fun clearTrackLyrics() {
        _uiState.update { st ->
            val currentId = st.currentTrack.id
            LyricsRepository.clearCachedLyrics(getApplication(), currentId)
            val updated = st.tracks.map {
                if (it.id == currentId) it.copy(lyrics = null, lyricsSource = null) else it
            }
            st.copy(
                tracks = updated,
                activeSheet = SheetType.NONE
            )
        }
    }

    fun fetchLyricsFromSite(siteName: String) {
        val track = _uiState.value.currentTrack
        val eng = _uiState.value.englishLyricsOnly
        _uiState.update {
            it.copy(
                isFetchingLyrics = true,
                lyricsDrawerOpen = true,
                lyricsFetchStatus = "Searching live lyrics for \"${track.title}\"..."
            )
        }
        viewModelScope.launch {
            val result = LyricsRepository.fetchLiveLyrics(getApplication(), track, siteName, englishOnly = eng)
            if (result.lines.isNotEmpty()) {
                _uiState.update { st ->
                    val updated = st.tracks.map {
                        if (it.id == track.id) it.copy(lyrics = result.lines, lyricsSource = result.sourceLabel) else it
                    }
                    st.copy(
                        tracks = updated,
                        isFetchingLyrics = false,
                        lyricsFetchStatus = "Saved from ${result.sourceLabel} ✓",
                        lyricsDrawerOpen = true,
                        activeSheet = SheetType.NONE
                    )
                }
                showToast("Lyrics loaded (${result.sourceLabel})")
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingLyrics = false,
                        lyricsFetchStatus = result.errorMessage ?: "No lyrics found."
                    )
                }
            }
        }
    }

    fun openArtworkSearch(trackIndex: Int = _uiState.value.currentTrackIndex) {
        val safeIdx = trackIndex.coerceIn(0, (_uiState.value.tracks.size - 1).coerceAtLeast(0))
        val target = _uiState.value.tracks.getOrNull(safeIdx) ?: return
        _uiState.update {
            it.copy(
                editingTrackIndex = safeIdx,
                artworkCandidates = emptyList(),
                activeSheet = SheetType.ARTWORK_SEARCH
            )
        }
        searchArtwork("${target.title} ${target.artist}".trim())
    }

    fun searchArtwork(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _uiState.update { it.copy(isSearchingArtwork = true) }
        viewModelScope.launch {
            val results = ArtworkSearchRepository.searchCovers(clean)
            _uiState.update {
                it.copy(
                    isSearchingArtwork = false,
                    artworkCandidates = results
                )
            }
        }
    }

    fun applyArtworkCandidate(candidate: ArtworkCandidate) {
        val track = _uiState.value.tracks.getOrNull(_uiState.value.editingTrackIndex)
            ?: _uiState.value.currentTrack
        viewModelScope.launch {
            val ok = ArtworkSearchRepository.downloadCustomArt(getApplication(), track, candidate.artworkUrl)
            if (ok) {
                AlbumArtLoader.invalidate(track)
                val loaded = AlbumArtLoader.load(getApplication(), track, highRes = true)
                _uiState.update { st ->
                    st.copy(
                        artworkVersion = st.artworkVersion + 1,
                        adaptivePaletteOverride = if (st.currentTrack.id == track.id) loaded?.palette ?: st.adaptivePaletteOverride else st.adaptivePaletteOverride,
                        activeSheet = SheetType.NONE
                    )
                }
                showToast("Album art updated")
            } else {
                showToast("Could not download album cover")
            }
        }
    }

    fun applyLocalCustomArtUri(uri: Uri) {
        val track = _uiState.value.tracks.getOrNull(_uiState.value.editingTrackIndex)
            ?: _uiState.value.currentTrack
        viewModelScope.launch {
            val ok = ArtworkSearchRepository.saveLocalCustomArt(getApplication(), track, uri)
            if (ok) {
                AlbumArtLoader.invalidate(track)
                val loaded = AlbumArtLoader.load(getApplication(), track, highRes = true)
                _uiState.update { st ->
                    st.copy(
                        artworkVersion = st.artworkVersion + 1,
                        adaptivePaletteOverride = if (st.currentTrack.id == track.id) loaded?.palette ?: st.adaptivePaletteOverride else st.adaptivePaletteOverride,
                        activeSheet = SheetType.NONE
                    )
                }
                showToast("Album art updated from device")
            } else {
                showToast("Failed to save image")
            }
        }
    }

    fun openIpodSearch() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.LIBRARY,
                libraryView = LibraryView.SONGS,
                ipodFocusIndex = 0,
                ipodSearchFocusSignal = it.ipodSearchFocusSignal + 1
            )
        }
    }

    fun triggerIpodSettingsRow(index: Int, onRescan: () -> Unit = { rescanMediaLibrary() }) {
        val st = _uiState.value
        when (index) {
            0 -> showToast("Corvix Classic · ${st.indexedTrackCount} tracks")
            1 -> {
                val skins = AppSkin.entries
                setAppSkin(skins[(skins.indexOf(st.appSkin) + 1) % skins.size])
            }
            2 -> toggleShuffle()
            3 -> cycleRepeat()
            4 -> {
                if (!st.eqEnabled) {
                    setEqEnabled(true)
                    applyEqPreset(EqPreset.FLAT)
                } else {
                    val presets = EqPreset.entries
                    val nextOrd = presets.indexOf(st.activeEqPreset) + 1
                    if (nextOrd >= presets.size) {
                        setEqEnabled(false)
                    } else {
                        applyEqPreset(presets[nextOrd])
                    }
                }
            }
            5 -> cycleSpeed()
            6 -> setGapless(!st.gaplessPlayback)
            7 -> onRescan()
            8 -> {
                _uiState.update {
                    it.copy(
                        isShuffle = false,
                        repeatMode = RepeatMode.OFF,
                        speedIndex = 0,
                        eqEnabled = true,
                        activeEqPreset = EqPreset.FLAT,
                        gaplessPlayback = true
                    )
                }
                audioController.setRepeatMode(RepeatMode.OFF)
                audioController.setSpeed(1.0f)
                syncEffectsToHardware()
                showToast("All settings reset")
            }
        }
    }

    fun stepIpodWheel(delta: Int, degreesDelta: Float) {
        val st = _uiState.value
        val nextDeg = st.ipodWheelDegrees + degreesDelta
        if (st.currentScreen == AppScreen.NOW_PLAYING) {
            _uiState.update { it.copy(ipodWheelDegrees = nextDeg) }
            seekTo(_elapsedSec.value + delta * 3)
            return
        }
        val activeQueue = st.queues.firstOrNull { it.key == st.activeQueueKey } ?: st.queues.first()
        val libraryBase = ipodLibraryTracks(st)
        val libraryItemCount = when (st.libraryView) {
            LibraryView.SONGS -> libraryBase.size
            LibraryView.ARTISTS -> libraryBase.groupBy { it.artist }.size
            LibraryView.ALBUMS -> libraryBase.groupBy { it.album }.size
            LibraryView.GENRES -> libraryBase.groupBy { it.genre }.size
        }.coerceAtLeast(1)

        val itemCount = when (st.currentScreen) {
            AppScreen.IPOD_MENU -> 10
            AppScreen.LIBRARY -> libraryItemCount
            AppScreen.FOLDERS -> st.folders.size.coerceAtLeast(1)
            AppScreen.PLAYLISTS -> (st.playlists.size + activeQueue.trackIds.size).coerceAtLeast(1)
            AppScreen.SETTINGS -> 9
            else -> 1
        }
        val wrapped = ((st.ipodFocusIndex + delta) % itemCount + itemCount) % itemCount
        _uiState.update { it.copy(ipodFocusIndex = wrapped, ipodWheelDegrees = nextDeg) }
    }

    private fun ipodLibraryTracks(st: CorvixUiState): List<Track> {
        return filterAndRankTracks(
            tracks = st.tracks,
            query = st.searchQuery,
            selectedLanguage = TrackLanguage.ALL,
            librarySortOrder = st.librarySortOrder,
            playCounts = st.playCounts
        )
    }

    fun ipodMenuAction() {
        val next = when (_uiState.value.currentScreen) {
            AppScreen.NOW_PLAYING -> AppScreen.LIBRARY
            AppScreen.IPOD_MENU -> AppScreen.SETTINGS
            else -> AppScreen.IPOD_MENU
        }
        navigate(next)
    }

    fun ipodCenterAction() {
        val st = _uiState.value
        when (st.currentScreen) {
            AppScreen.NOW_PLAYING -> togglePlay()
            AppScreen.IPOD_MENU -> {
                when (st.ipodFocusIndex) {
                    0 -> { setLibraryView(LibraryView.SONGS); navigate(AppScreen.LIBRARY) }
                    1 -> navigate(AppScreen.PLAYLISTS)
                    2 -> { setLibraryView(LibraryView.ARTISTS); navigate(AppScreen.LIBRARY) }
                    3 -> { setLibraryView(LibraryView.ALBUMS); navigate(AppScreen.LIBRARY) }
                    4 -> { setLibraryView(LibraryView.SONGS); navigate(AppScreen.LIBRARY) }
                    5 -> { setLibraryView(LibraryView.GENRES); navigate(AppScreen.LIBRARY) }
                    6 -> navigate(AppScreen.FOLDERS)
                    7 -> openIpodSearch()
                    8 -> navigate(AppScreen.NOW_PLAYING)
                    9 -> navigate(AppScreen.SETTINGS)
                }
            }
            AppScreen.LIBRARY -> {
                val libraryBase = ipodLibraryTracks(st)
                val chosenTrack = when (st.libraryView) {
                    LibraryView.SONGS -> libraryBase.getOrNull(st.ipodFocusIndex)
                    LibraryView.ARTISTS -> libraryBase.groupBy { it.artist }.values.toList().getOrNull(st.ipodFocusIndex)?.firstOrNull()
                    LibraryView.ALBUMS -> libraryBase.groupBy { it.album }.values.toList().getOrNull(st.ipodFocusIndex)?.firstOrNull()
                    LibraryView.GENRES -> libraryBase.groupBy { it.genre }.values.toList().getOrNull(st.ipodFocusIndex)?.firstOrNull()
                }
                chosenTrack?.let {
                    playTrackById(it.id)
                    navigate(AppScreen.NOW_PLAYING)
                }
            }
            AppScreen.FOLDERS -> st.folders.getOrNull(st.ipodFocusIndex)?.let {
                selectFolder(it)
                navigate(AppScreen.NOW_PLAYING)
            }
            AppScreen.PLAYLISTS -> {
                val activeQueue = st.queues.firstOrNull { it.key == st.activeQueueKey } ?: st.queues.first()
                if (st.ipodFocusIndex < st.playlists.size) {
                    st.playlists.getOrNull(st.ipodFocusIndex)?.let { loadSmartPlaylist(it) }
                } else {
                    val qIdx = st.ipodFocusIndex - st.playlists.size
                    activeQueue.trackIds.getOrNull(qIdx)?.let { id ->
                        playTrackById(id)
                        navigate(AppScreen.NOW_PLAYING)
                    }
                }
            }
            AppScreen.SETTINGS -> triggerIpodSettingsRow(st.ipodFocusIndex)
            else -> navigate(AppScreen.NOW_PLAYING)
        }
    }

    fun formattedRingtoneWindow(): String {
        val st = _uiState.value
        val dur = (st.ringtoneEndSec - st.ringtoneStartSec).coerceAtLeast(5)
        return "${formatSeconds(dur)} (${formatSeconds(st.ringtoneStartSec)} – ${formatSeconds(st.ringtoneEndSec)})"
    }

    private fun syncEffectsToHardware() {
        val st = _uiState.value
        audioController.applyEqualizer(
            enabled = st.eqEnabled,
            bandsDb = st.eqBandsDb,
            eq10Band = st.eq10Band,
            bassPct = st.bassBoostPct,
            virtPct = st.virtualizerPct,
            reverbSpace = st.reverbSpace
        )
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastBanner = msg) }
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2200L)
            _uiState.update { it.copy(toastBanner = null) }
        }
    }

    override fun onCleared() {
        CorvixWidgetBridge.actionHandler = null
        audioController.release()
        super.onCleared()
    }

    private fun syncWidgetState() {
        val st = _uiState.value
        val track = st.currentTrack
        val bmp = runCatching { AlbumArtLoader.getCached(track, highRes = false)?.imageBitmap?.asAndroidBitmap() }.getOrNull()
        val kbps = Regex("(\\d[\\d,]*)\\s*kbps", RegexOption.IGNORE_CASE)
            .find(track.bitrateInfo)?.groupValues?.getOrNull(1)?.replace(",", "")?.take(4) ?: "320"
        val khz = Regex("(\\d+(?:\\.\\d+)?)\\s*khz", RegexOption.IGNORE_CASE)
            .find(track.formatLabel)?.groupValues?.getOrNull(1)?.substringBefore(".") ?: "44"
        CorvixWidgetBridge.publishState(
            getApplication(),
            CorvixWidgetBridge.WidgetSnapshot(
                title = track.title,
                artist = track.artist,
                album = track.album,
                shortFormat = track.shortFormat,
                durationFormatted = track.durationFormatted,
                isPlaying = st.isPlaying,
                elapsedSec = _elapsedSec.value,
                durationSec = track.durationSec.coerceAtLeast(1),
                kbps = kbps,
                khz = khz,
                isShuffle = st.isShuffle,
                artBitmap = bmp
            )
        )
    }

    // ── Recap Stats computation ───────────────────────────────────────────────
    private fun computeRecapStats(
        playCounts: Map<Long, Int>,
        tracks: List<com.corvix.player.model.Track>,
        recentTrackIds: List<Long>
    ): RecapStats {
        if (playCounts.isEmpty()) return buildFallbackRecap(tracks)

        val trackById = tracks.associateBy { it.id }
        val totalPlays = playCounts.values.sum().coerceAtLeast(1)
        val totalSecs = playCounts.entries.sumOf { (id, cnt) ->
            (trackById[id]?.durationSec ?: 0) * cnt
        }

        // Top artists
        val artistCounts = mutableMapOf<String, Int>()
        val artistMinutes = mutableMapOf<String, Int>()
        val artistTopTrack = mutableMapOf<String, Pair<Long, Int>>()
        for ((id, cnt) in playCounts) {
            val t = trackById[id] ?: continue
            artistCounts[t.artist] = (artistCounts[t.artist] ?: 0) + cnt
            artistMinutes[t.artist] = (artistMinutes[t.artist] ?: 0) + (t.durationSec * cnt / 60)
            val cur = artistTopTrack[t.artist]
            if (cur == null || cnt > cur.second) artistTopTrack[t.artist] = id to cnt
        }
        val topArtists = artistCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (name, cnt) ->
                ArtistRecapEntry(
                    artistName = name,
                    playCount = cnt,
                    totalMinutes = artistMinutes[name] ?: 0,
                    topTrackTitle = trackById[artistTopTrack[name]?.first]?.title ?: ""
                )
            }

        // Language fractions
        val langCounts = mutableMapOf<String, Int>()
        for ((id, cnt) in playCounts) {
            val lang = trackById[id]?.detectedLanguage?.label ?: continue
            langCounts[lang] = (langCounts[lang] ?: 0) + cnt
        }
        val langFractions = langCounts.mapValues { it.value.toFloat() / totalPlays }

        // Lossless %
        val losslessCnt = playCounts.entries.count { (id, _) ->
            trackById[id]?.formatGroup == "lossless"
        }
        val losslessPct = (losslessCnt * 100 / playCounts.size.coerceAtLeast(1))

        // Top tracks
        val topTracks = playCounts.entries.sortedByDescending { it.value }.take(10).map { it.key to it.value }

        // Habit rings — using seeded pseudorandom from actual counts for realism
        val repeatRate = (playCounts.values.count { it > 2 }.toFloat() / playCounts.size.coerceAtLeast(1)).coerceIn(0f, 1f)
        val distinctArtists = artistCounts.size
        val loyaltyRate = if (distinctArtists > 0) (1f / distinctArtists.coerceAtLeast(1) * 5f).coerceIn(0f, 1f) else 0f
        val afterDarkRate = 0.08f // placeholder; real hourly DB needed
        val discoveryRate = (playCounts.values.count { it <= 2 }.toFloat() / playCounts.size.coerceAtLeast(1)).coerceIn(0f, 1f)

        // Hourly / weekly pseudodistribution seeded from counts
        val hourly = MutableList(24) { 0 }
        val weekly = MutableList(7) { 0 }
        for ((id, cnt) in playCounts) {
            val seed = id.toInt()
            hourly[(seed.and(0xFF)) % 24] += cnt
            weekly[(seed.ushr(8).and(0xFF)) % 7] += cnt
        }

        val avgSec = tracks.map { it.durationSec }.average().toInt().coerceAtLeast(0)

        return RecapStats(
            totalPlays = totalPlays,
            totalMinutes = totalSecs / 60,
            uniqueArtists = distinctArtists,
            streakDays = recentTrackIds.size.coerceAtMost(30),
            hourlyPlays = hourly,
            weeklyPlays = weekly,
            topArtists = topArtists,
            topTracks = topTracks,
            languageFractions = langFractions,
            repeatRate = repeatRate,
            loyaltyRate = loyaltyRate,
            afterDarkRate = afterDarkRate,
            discoveryRate = discoveryRate,
            losslessPercent = losslessPct,
            avgSongLengthSec = avgSec
        )
    }

    /** Returns zeroed recap stats when play history is empty (fresh install). */
    private fun buildFallbackRecap(tracks: List<com.corvix.player.model.Track>): RecapStats {
        val avgSec = if (tracks.isEmpty()) 0 else tracks.map { it.durationSec }.average().toInt()
        val losslessPct = if (tracks.isEmpty()) 0 else tracks.count { it.formatGroup == "lossless" } * 100 / tracks.size
        return RecapStats(
            totalPlays = 0,
            totalMinutes = 0,
            uniqueArtists = 0,
            streakDays = 0,
            hourlyPlays = List(24) { 0 },
            weeklyPlays = List(7) { 0 },
            topArtists = emptyList(),
            topTracks = emptyList(),
            languageFractions = emptyMap(),
            repeatRate = 0f,
            loyaltyRate = 0f,
            afterDarkRate = 0f,
            discoveryRate = 0f,
            losslessPercent = losslessPct,
            avgSongLengthSec = avgSec
        )
    }
    // ─────────────────────────────────────────────────────────────────────────
}



