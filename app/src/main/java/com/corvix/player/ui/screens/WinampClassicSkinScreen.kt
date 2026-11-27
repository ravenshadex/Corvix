package com.corvix.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.AutoEqProfile
import com.corvix.player.model.EqPreset
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.SheetType
import com.corvix.player.model.Track
import com.corvix.player.model.formatSeconds
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class WinampThemePreset(
    val label: String,
    val chassisTop: Color,
    val chassisMid: Color,
    val chassisBot: Color,
    val frameOuter: Color,
    val titleTop: Color,
    val titleBot: Color,
    val goldTop: Color,
    val lcdGreen: Color,
    val trackGreen: Color,
    val selectedBlue: Color
) {
    CLASSIC("Classic", Color(0xFF282A3E), Color(0xFF1D1F30), Color(0xFF24273A), Color(0xFF090B12), Color(0xFF2D3047), Color(0xFF1C1E2E), Color(0xFFD8B95B), Color(0xFF00FF00), Color(0xFF00E800), Color(0xFF0000C0)),
    AMBER("Amber", Color(0xFF2B2116), Color(0xFF1C150D), Color(0xFF241B11), Color(0xFF0D0905), Color(0xFF332718), Color(0xFF1F170E), Color(0xFFF2DC8E), Color(0xFFFFB000), Color(0xFFFFC233), Color(0xFF8C5A00)),
    CYAN("Cyber", Color(0xFF142434), Color(0xFF0D1824), Color(0xFF12202E), Color(0xFF060B10), Color(0xFF192D40), Color(0xFF0E1B28), Color(0xFF7DD3FC), Color(0xFF00F0FF), Color(0xFF38BDF8), Color(0xFF0369A1)),
    CRIMSON("Crimson", Color(0xFF2E161F), Color(0xFF1C0C12), Color(0xFF261119), Color(0xFF0D0508), Color(0xFF381B26), Color(0xFF210E15), Color(0xFFFDA4AF), Color(0xFFFF3B5C), Color(0xFFFB7185), Color(0xFF9F1239)),
    MONO("Silver", Color(0xFF272A30), Color(0xFF1A1C21), Color(0xFF212429), Color(0xFF0B0C0F), Color(0xFF30343B), Color(0xFF1D2026), Color(0xFFE2E8F0), Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF475569))
}

private object WinampColors {
    var ChassisTop by mutableStateOf(Color(0xFF282A3E))
    var ChassisMid by mutableStateOf(Color(0xFF1D1F30))
    var ChassisBot by mutableStateOf(Color(0xFF24273A))
    var FrameOuter by mutableStateOf(Color(0xFF090B12))
    val BevelLight = Color(0xFF636A84)
    val BevelDark = Color(0xFF141622)
    var TitleTop by mutableStateOf(Color(0xFF2D3047))
    var TitleBot by mutableStateOf(Color(0xFF1C1E2E))
    var GoldTop by mutableStateOf(Color(0xFFD8B95B))
    val GoldHighlight = Color(0xFFF2DC8E)
    val GoldShadow = Color(0xFF9A8033)
    val LcdBlack = Color(0xFF000000)
    val LcdDotGrid = Color(0xFF181B24)
    var LcdGreen by mutableStateOf(Color(0xFF00FF00))
    var TrackGreen by mutableStateOf(Color(0xFF00E800))
    val DimText = Color(0xFF7A8198)
    val SpecLabel = Color(0xFFA4ABC2)
    val ClutterText = Color(0xFF8E95AD)
    val VolOrangeStart = Color(0xFFDE7814)
    val VolOrangeEnd = Color(0xFFF29B2E)
    val BalGreen = Color(0xFF00C800)
    val SeekGroove = Color(0xFF0D0F18)
    val SeekGoldLight = Color(0xFFF3DC93)
    val SeekGoldMid = Color(0xFFD5B257)
    val SeekGoldDark = Color(0xFF9A792D)
    val BtnTop = Color(0xFFE5E9F2)
    val BtnMid = Color(0xFFCFD5E3)
    val BtnBot = Color(0xFFAEB6C9)
    val BtnShadow = Color(0xFF3C4254)
    val BtnGlyph = Color(0xFF252938)
    val LedGreen = Color(0xFF00D800)
    val EqGraphBg = Color(0xFF25293E)
    val EqGraphGrid = Color(0xFF414866)
    val EqGraphYellow = Color(0xFFFFE600)
    val EqScaleAmber = Color(0xFFE0B030)
    val EqTroughTop = Color(0xFFE6D615)
    val EqTroughMid = Color(0xFFB5CE1E)
    val EqTroughBot = Color(0xFF74AE23)
    var PlaylistSelectedBlue by mutableStateOf(Color(0xFF0000C0))
    val ScrollGoldLight = Color(0xFFF0DC98)
    val ScrollGoldDark = Color(0xFF947629)

    fun applyTheme(preset: WinampThemePreset) {
        ChassisTop = preset.chassisTop
        ChassisMid = preset.chassisMid
        ChassisBot = preset.chassisBot
        FrameOuter = preset.frameOuter
        TitleTop = preset.titleTop
        TitleBot = preset.titleBot
        GoldTop = preset.goldTop
        LcdGreen = preset.lcdGreen
        TrackGreen = preset.trackGreen
        PlaylistSelectedBlue = preset.selectedBlue
    }
}

private val WinampEqLabels = listOf("70", "180", "320", "600", "1K", "3K", "6K", "12K", "14K", "16K")

@Composable
fun WinampClassicSkinScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
    val visualizerLevels by viewModel.visualizerLevelsFlow.collectAsState()

    var mainShade by remember { mutableStateOf(false) }
    var eqOpen by remember { mutableStateOf(true) }
    var eqShade by remember { mutableStateOf(false) }
    var plOpen by remember { mutableStateOf(true) }
    var plShade by remember { mutableStateOf(false) }
    var remainingMode by remember { mutableStateOf(false) }
    var oscMode by remember { mutableStateOf(false) }
    var balanceFraction by remember { mutableFloatStateOf(0.5f) }
    var preampDb by remember { mutableIntStateOf(0) }
    var selectedPlaylistIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    val winampPrefs = remember { context.getSharedPreferences("corvix_prefs", android.content.Context.MODE_PRIVATE) }
    var winampTheme by remember {
        val saved = winampPrefs.getString("winamp_theme", null)
        val initial = WinampThemePreset.entries.firstOrNull { it.name == saved } ?: WinampThemePreset.CLASSIC
        WinampColors.applyTheme(initial)
        mutableStateOf(initial)
    }

    val activeQueue = remember(state.queues, state.activeQueueKey) {
        state.queues.firstOrNull { it.key == state.activeQueueKey } ?: state.queues.firstOrNull()
    }
    val queueTracks: List<Track> = remember(state.tracks, activeQueue) {
        val ids = activeQueue?.trackIds.orEmpty()
        val resolved = ids.mapNotNull { id -> state.tracks.find { it.id == id } }
        resolved.ifEmpty { state.tracks }
    }

    // Peak hold state for 19 spectrum columns
    val peakLevels = remember { FloatArray(19) { 0.15f } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(WinampColors.FrameOuter)
    ) {
        val density = LocalDensity.current
        // Classic Winamp virtual 275px coordinate unit scaled uniformly to phone width
        val u: Dp = maxWidth / 275f
        val uPx: Float = with(density) { u.toPx() }
        fun spU(v: Float): TextUnit = with(density) { (v * uPx).toSp() }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // ================= WINDOW 1: WINAMP MAIN =================
            WinampWindowBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (mainShade) Modifier.height(u * 26f)
                        else Modifier.height(u * 116f)
                    )
            ) {
                WinampTitleBar(
                    title = "WINAMP · ${winampTheme.label.uppercase()}",
                    u = u,
                    spU = ::spU,
                    onMenuClick = {
                        val next = WinampThemePreset.entries[(winampTheme.ordinal + 1) % WinampThemePreset.entries.size]
                        winampTheme = next
                        WinampColors.applyTheme(next)
                        winampPrefs.edit().putString("winamp_theme", next.name).apply()
                    },
                    onShadeClick = { mainShade = !mainShade },
                    onMinClick = { viewModel.navigate(AppScreen.WIDGET) },
                    onCloseClick = { viewModel.navigate(AppScreen.SETTINGS) }
                )

                if (mainShade) {
                    WinampMainShadeContent(
                        track = state.currentTrack,
                        trackNumber = (queueTracks.indexOfFirst { it.id == state.currentTrack.id }.takeIf { it >= 0 } ?: state.currentTrackIndex) + 1,
                        elapsedSec = elapsedSec,
                        isPlaying = state.isPlaying,
                        u = u,
                        spU = ::spU,
                        onPrev = { viewModel.prevTrack() },
                        onPlay = { viewModel.playOrResume() },
                        onPause = { viewModel.togglePlay() },
                        onStop = { viewModel.stopPlayback() },
                        onNext = { viewModel.nextTrack() }
                    )
                } else {
                    WinampMainContent(
                        state = state,
                        track = state.currentTrack,
                        trackNumber = (queueTracks.indexOfFirst { it.id == state.currentTrack.id }.takeIf { it >= 0 } ?: state.currentTrackIndex) + 1,
                        elapsedSec = elapsedSec,
                        remainingMode = remainingMode,
                        oscMode = oscMode,
                        balanceFraction = balanceFraction,
                        eqOpen = eqOpen,
                        plOpen = plOpen,
                        visualizerLevels = visualizerLevels,
                        peakLevels = peakLevels,
                        u = u,
                        spU = ::spU,
                        onToggleTimerMode = { remainingMode = !remainingMode },
                        onToggleVisMode = { oscMode = !oscMode },
                        onClutterO = { viewModel.navigate(AppScreen.SETTINGS) },
                        onClutterA = { viewModel.navigate(AppScreen.WIDGET) },
                        onClutterI = { viewModel.openSheet(SheetType.TAGS) },
                        onClutterD = { mainShade = !mainShade },
                        onClutterV = { oscMode = !oscMode },
                        onVolumeChange = { viewModel.adjustVolume((it * 100f).roundToInt()) },
                        onBalanceChange = { balanceFraction = it.coerceIn(0f, 1f) },
                        onToggleEq = { eqOpen = !eqOpen },
                        onTogglePl = { plOpen = !plOpen },
                        onSeek = { sec -> viewModel.seekTo(sec) },
                        onPrev = { viewModel.prevTrack() },
                        onPlay = { viewModel.playOrResume() },
                        onPause = { viewModel.togglePlay() },
                        onStop = { viewModel.stopPlayback() },
                        onNext = { viewModel.nextTrack() },
                        onEject = { viewModel.navigate(AppScreen.SETTINGS) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onCycleRepeat = { viewModel.cycleRepeat() },
                        onBoltClick = { viewModel.navigate(AppScreen.SETTINGS) }
                    )
                }
            }

            // ================= WINDOW 2: WINAMP EQUALIZER =================
            if (eqOpen) {
                WinampWindowBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (eqShade) Modifier.height(u * 15f)
                            else Modifier.height(u * 116f)
                        )
                ) {
                    WinampTitleBar(
                        title = "WINAMP EQUALIZER",
                        u = u,
                        spU = ::spU,
                        onMenuClick = { viewModel.openSettingsEq() },
                        onShadeClick = { eqShade = !eqShade },
                        onMinClick = null,
                        onCloseClick = { eqOpen = false }
                    )
                    if (!eqShade) {
                        WinampEqualizerContent(
                            state = state,
                            preampDb = preampDb,
                            u = u,
                            spU = ::spU,
                            onToggleOn = { viewModel.setEqEnabled(!state.eqEnabled) },
                            onToggleAuto = {
                                val profiles = AutoEqProfile.entries
                                val next = profiles[(profiles.indexOf(state.autoEqProfile) + 1) % profiles.size]
                                viewModel.applyAutoEq(next)
                                viewModel.showToast("AUTO EQ: ${next.chipLabel}")
                            },
                            onCyclePreset = {
                                val presets = EqPreset.entries
                                val next = presets[(presets.indexOf(state.activeEqPreset) + 1) % presets.size]
                                viewModel.applyEqPreset(next)
                                viewModel.showToast("EQ Preset: ${next.label}")
                            },
                            onPreampChange = { preampDb = it.coerceIn(-12, 12) },
                            onBandChange = { idx, db -> viewModel.updateEqBand(idx, db) }
                        )
                    }
                }
            }

            // ================= WINDOW 3: WINAMP PLAYLIST =================
            if (plOpen) {
                WinampWindowBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (plShade) Modifier.height(u * 15f)
                            else Modifier.weight(1f)
                        )
                ) {
                    WinampTitleBar(
                        title = "WINAMP PLAYLIST",
                        u = u,
                        spU = ::spU,
                        onMenuClick = { viewModel.navigate(AppScreen.PLAYLISTS) },
                        onShadeClick = { plShade = !plShade },
                        onMinClick = { eqOpen = !eqOpen },
                        onCloseClick = { plOpen = false }
                    )
                    if (!plShade) {
                        WinampPlaylistContent(
                            tracks = queueTracks,
                            currentTrackId = state.currentTrack.id,
                            selectedIndex = selectedPlaylistIndex,
                            elapsedSec = elapsedSec,
                            u = u,
                            spU = ::spU,
                            onPlayTrack = { tr -> viewModel.playTrackById(tr.id) },
                            onAddClick = { viewModel.navigate(AppScreen.FOLDERS) },
                            onRemClick = {
                                val target = queueTracks.getOrNull(selectedPlaylistIndex) ?: state.currentTrack
                                viewModel.removeTrackFromQueue(target.id)
                                selectedPlaylistIndex = -1
                            },
                            onSelClick = {
                                if (queueTracks.isNotEmpty()) {
                                    selectedPlaylistIndex = (selectedPlaylistIndex + 1) % queueTracks.size
                                }
                            },
                            onMiscClick = { viewModel.openSheet(SheetType.NP_MENU) },
                            onListOptsClick = { viewModel.navigate(AppScreen.PLAYLISTS) },
                            onPrev = { viewModel.prevTrack() },
                            onPlay = { viewModel.playOrResume() },
                            onPause = { viewModel.togglePlay() },
                            onStop = { viewModel.stopPlayback() },
                            onNext = { viewModel.nextTrack() },
                            onEject = { viewModel.navigate(AppScreen.SETTINGS) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WinampWindowBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(WinampColors.ChassisTop, WinampColors.ChassisMid, WinampColors.ChassisBot)
                )
            )
            .border(1.dp, WinampColors.FrameOuter)
            .padding(1.dp)
            .border(1.dp, WinampColors.BevelLight.copy(alpha = 0.55f))
    ) {
        content()
    }
}

@Composable
private fun WinampTitleBar(
    title: String,
    u: Dp,
    spU: (Float) -> TextUnit,
    onMenuClick: () -> Unit,
    onShadeClick: (() -> Unit)?,
    onMinClick: (() -> Unit)?,
    onCloseClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(u * 14f)
            .background(Brush.verticalGradient(listOf(WinampColors.TitleTop, WinampColors.TitleBot)))
            .padding(horizontal = u * 3f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u * 3f)
    ) {
        // Left menu corner bolt/triangle
        Box(
            modifier = Modifier
                .size(u * 10f)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(u * 7f)) {
                val p = Path().apply {
                    moveTo(0f, size.height * 0.2f)
                    lineTo(size.width, size.height * 0.2f)
                    lineTo(size.width * 0.5f, size.height * 0.85f)
                    close()
                }
                drawPath(p, WinampColors.GoldTop)
            }
        }

        GoldDoubleBars(modifier = Modifier.weight(1f).height(u * 6f))

        Text(
            text = title,
            color = Color.White,
            fontSize = spU(7.2f),
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = spU(0.6f),
            maxLines = 1
        )

        GoldDoubleBars(modifier = Modifier.weight(1f).height(u * 6f))

        Row(horizontalArrangement = Arrangement.spacedBy(u * 1.5f)) {
            if (onShadeClick != null) {
                WinampMiniTitleBtn("_", u, spU, onShadeClick)
            }
            if (onMinClick != null) {
                WinampMiniTitleBtn("□", u, spU, onMinClick)
            }
            if (onCloseClick != null) {
                WinampMiniTitleBtn("×", u, spU, onCloseClick)
            }
        }
    }
}

@Composable
private fun GoldDoubleBars(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        drawLine(WinampColors.GoldTop, Offset(0f, h * 0.22f), Offset(w, h * 0.22f), strokeWidth = 1.6f)
        drawLine(WinampColors.GoldShadow, Offset(0f, h * 0.78f), Offset(w, h * 0.78f), strokeWidth = 1.4f)
    }
}

@Composable
private fun WinampMiniTitleBtn(
    symbol: String,
    u: Dp,
    spU: (Float) -> TextUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(u * 9.5f)
            .background(Brush.verticalGradient(listOf(WinampColors.BtnMid, Color(0xFF8F96AB))))
            .border(0.7.dp, WinampColors.BevelDark)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color(0xFF161924),
            fontSize = spU(6.2f),
            fontWeight = FontWeight.Bold,
            lineHeight = spU(6.2f)
        )
    }
}

@Composable
private fun WinampMainShadeContent(
    track: Track,
    trackNumber: Int,
    elapsedSec: Int,
    isPlaying: Boolean,
    u: Dp,
    spU: (Float) -> TextUnit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = u * 4f, vertical = u * 2f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u * 3f)
    ) {
        WinampBlackInset(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = u * 4f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$trackNumber. ${track.artist} - ${track.title}",
                    color = WinampColors.TrackGreen,
                    fontSize = spU(6.8f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${if (isPlaying) "▶" else "❚❚"} ${formatSeconds(elapsedSec)}",
                    color = WinampColors.LcdGreen,
                    fontSize = spU(6.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(u * 1.5f)) {
            WinampTransportButton(WinampGlyph.PREV, u * 16f, u * 10f, onPrev)
            WinampTransportButton(WinampGlyph.PLAY, u * 16f, u * 10f, onPlay)
            WinampTransportButton(WinampGlyph.PAUSE, u * 16f, u * 10f, onPause)
            WinampTransportButton(WinampGlyph.STOP, u * 16f, u * 10f, onStop)
            WinampTransportButton(WinampGlyph.NEXT, u * 16f, u * 10f, onNext)
        }
    }
}

@Composable
private fun WinampMainContent(
    state: CorvixUiState,
    track: Track,
    trackNumber: Int,
    elapsedSec: Int,
    remainingMode: Boolean,
    oscMode: Boolean,
    balanceFraction: Float,
    eqOpen: Boolean,
    plOpen: Boolean,
    visualizerLevels: FloatArray,
    peakLevels: FloatArray,
    u: Dp,
    spU: (Float) -> TextUnit,
    onToggleTimerMode: () -> Unit,
    onToggleVisMode: () -> Unit,
    onClutterO: () -> Unit,
    onClutterA: () -> Unit,
    onClutterI: () -> Unit,
    onClutterD: () -> Unit,
    onClutterV: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onToggleEq: () -> Unit,
    onTogglePl: () -> Unit,
    onSeek: (Int) -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onEject: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onBoltClick: () -> Unit
) {
    val context = LocalContext.current
    var kbpsText by remember(track.id, track.bitrateInfo) { mutableStateOf(parseKbpsBadge(track.bitrateInfo)) }
    var khzText by remember(track.id, track.formatLabel) { mutableStateOf(parseKhzBadge(track.formatLabel)) }
    var isStereo by remember(track.id) { mutableStateOf(true) }

    LaunchedEffect(track.id, track.contentUri) {
        val uri = track.contentUri ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val extractor = android.media.MediaExtractor()
            runCatching {
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val mime = fmt.getString(android.media.MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        if (fmt.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) {
                            val sr = fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) / 1000
                            if (sr > 0) khzText = sr.toString()
                        }
                        if (fmt.containsKey(android.media.MediaFormat.KEY_BIT_RATE)) {
                            val br = fmt.getInteger(android.media.MediaFormat.KEY_BIT_RATE) / 1000
                            if (br > 0) kbpsText = br.toString().take(4)
                        }
                        if (fmt.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)) {
                            isStereo = fmt.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) >= 2
                        }
                        break
                    }
                }
            }
            runCatching { extractor.release() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = u * 5f, vertical = u * 3.5f),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Half: Left Dotted LCD + Right Telemetry/Volume/Balance/EQ/PL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(u * 48f),
            horizontalArrangement = Arrangement.spacedBy(u * 5f)
        ) {
            // Left Dotted-Grid Black LCD
            Box(
                modifier = Modifier
                    .width(u * 102f)
                    .fillMaxHeight()
                    .background(WinampColors.LcdBlack)
                    .border(1.dp, WinampColors.BevelDark)
                    .padding(u * 2f)
            ) {
                // Dotted grid canvas backdrop
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 3.dp.toPx()
                    var x = 2f
                    while (x < size.width) {
                        var y = 2f
                        while (y < size.height) {
                            drawRect(WinampColors.LcdDotGrid, Offset(x, y), Size(1.3f, 1.3f))
                            y += step
                        }
                        x += step
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Timer + Spectrum inside LCD
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val timerText = if (remainingMode) {
                            "-${formatSeconds((track.durationSec - elapsedSec).coerceAtLeast(0))}"
                        } else {
                            formatSeconds(elapsedSec)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onToggleTimerMode() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play indicator + red/green dot
                            Canvas(modifier = Modifier.size(width = u * 15f, height = u * 12f)) {
                                drawRect(
                                    color = if (state.isPlaying) WinampColors.LcdGreen else Color(0xFFCC2200),
                                    topLeft = Offset(0f, size.height * 0.35f),
                                    size = Size(size.width * 0.22f, size.height * 0.28f)
                                )
                                if (state.isPlaying) {
                                    val p = Path().apply {
                                        moveTo(size.width * 0.38f, size.height * 0.12f)
                                        lineTo(size.width * 0.92f, size.height * 0.5f)
                                        lineTo(size.width * 0.38f, size.height * 0.88f)
                                        close()
                                    }
                                    drawPath(p, WinampColors.LcdGreen)
                                } else {
                                    drawRect(
                                        WinampColors.LcdGreen,
                                        topLeft = Offset(size.width * 0.38f, size.height * 0.16f),
                                        size = Size(size.width * 0.2f, size.height * 0.68f)
                                    )
                                    drawRect(
                                        WinampColors.LcdGreen,
                                        topLeft = Offset(size.width * 0.68f, size.height * 0.16f),
                                        size = Size(size.width * 0.2f, size.height * 0.68f)
                                    )
                                }
                            }

                            WinampSevenSegClock(
                                text = timerText,
                                modifier = Modifier
                                    .width(u * 68f)
                                    .height(u * 18f)
                            )
                        }

                        // 19-band Segmented LED Spectrum Analyzer / Oscilloscope
                        WinampSpectrumCanvas(
                            levels = visualizerLevels,
                            peakLevels = peakLevels,
                            isPlaying = state.isPlaying,
                            oscMode = oscMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(u * 21f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onToggleVisMode() }
                        )
                    }
                }
            }

            // Right Side: Ticker + Specs + Volume/Balance/EQ/PL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Green Track Ticker Box
                WinampBlackInset(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(u * 14f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = u * 4f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "$trackNumber. ${track.artist} - ${track.title} (${track.durationFormatted})",
                            color = WinampColors.TrackGreen,
                            fontSize = spU(7.2f),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 128 kbps | 48 kHz | mono stereo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(u * 2.5f)
                    ) {
                        WinampBlackInset(
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = kbpsText,
                                color = WinampColors.TrackGreen,
                                fontSize = spU(6.2f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = u * 3f, vertical = u * 0.8f)
                            )
                        }
                        Text("kbps", color = WinampColors.SpecLabel, fontSize = spU(6.2f), fontFamily = FontFamily.SansSerif)

                        WinampBlackInset {
                            Text(
                                text = khzText,
                                color = WinampColors.TrackGreen,
                                fontSize = spU(6.2f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = u * 3f, vertical = u * 0.8f)
                            )
                        }
                        Text("kHz", color = WinampColors.SpecLabel, fontSize = spU(6.2f), fontFamily = FontFamily.SansSerif)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(u * 4f)) {
                        Text("mono", color = if (!isStereo) WinampColors.LcdGreen else Color(0xFF636980), fontSize = spU(6.2f), fontWeight = if (!isStereo) FontWeight.Bold else FontWeight.SemiBold)
                        Text("stereo", color = if (isStereo) WinampColors.LcdGreen else Color(0xFF636980), fontSize = spU(6.2f), fontWeight = if (isStereo) FontWeight.Bold else FontWeight.SemiBold)
                    }
                }

                // Volume + Balance + EQ / PL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(u * 14f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(u * 4f)
                ) {
                    WinampHorizontalGrooveSlider(
                        fraction = state.volumePct / 100f,
                        fillBrush = Brush.horizontalGradient(listOf(WinampColors.VolOrangeStart, WinampColors.VolOrangeEnd)),
                        centerOut = false,
                        u = u,
                        modifier = Modifier.weight(1.45f).fillMaxHeight(),
                        onFractionChange = onVolumeChange
                    )

                    WinampHorizontalGrooveSlider(
                        fraction = balanceFraction,
                        fillBrush = Brush.horizontalGradient(listOf(WinampColors.BalGreen, WinampColors.BalGreen)),
                        centerOut = true,
                        u = u,
                        modifier = Modifier.weight(0.95f).fillMaxHeight(),
                        onFractionChange = onBalanceChange
                    )

                    WinampLedPill(
                        label = "EQ",
                        active = eqOpen,
                        u = u,
                        spU = spU,
                        modifier = Modifier.width(u * 23f).height(u * 12f),
                        onClick = onToggleEq
                    )
                    WinampLedPill(
                        label = "PL",
                        active = plOpen,
                        u = u,
                        spU = spU,
                        modifier = Modifier.width(u * 23f).height(u * 12f),
                        onClick = onTogglePl
                    )
                }
            }
        }

        // Full-width recessed seek bar with gold shuttle thumb
        WinampGoldSeekBar(
            progress = (elapsedSec.toFloat() / track.durationSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
            u = u,
            modifier = Modifier
                .fillMaxWidth()
                .height(u * 13f),
            onSeekFraction = { frac ->
                onSeek((frac * track.durationSec.coerceAtLeast(1)).roundToInt())
            }
        )

        // Bottom Transport Keys + Eject + Shuffle/Repeat + Winamp Bolt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(u * 22f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u * 1.5f)
            ) {
                WinampTransportButton(WinampGlyph.PREV, u * 23f, u * 18f, onPrev)
                WinampTransportButton(WinampGlyph.PLAY, u * 23f, u * 18f, onPlay)
                WinampTransportButton(WinampGlyph.PAUSE, u * 23f, u * 18f, onPause)
                WinampTransportButton(WinampGlyph.STOP, u * 23f, u * 18f, onStop)
                WinampTransportButton(WinampGlyph.NEXT, u * 23f, u * 18f, onNext)

                Spacer(modifier = Modifier.width(u * 4f))

                WinampTransportButton(WinampGlyph.EJECT, u * 22f, u * 16f, onEject)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u * 2f)
            ) {
                WinampLedPill(
                    label = "SHUFFLE",
                    active = state.isShuffle,
                    u = u,
                    spU = spU,
                    modifier = Modifier.width(u * 45f).height(u * 15f),
                    onClick = onToggleShuffle
                )
                WinampRepeatPill(
                    repeatMode = state.repeatMode,
                    u = u,
                    modifier = Modifier.width(u * 27f).height(u * 15f),
                    onClick = onCycleRepeat
                )

                // Classic Winamp 3D Lightning Bolt
                Canvas(
                    modifier = Modifier
                        .size(u * 18f)
                        .clickable { onBoltClick() }
                ) {
                    val w = size.width
                    val h = size.height
                    val bolt = Path().apply {
                        moveTo(w * 0.58f, h * 0.05f)
                        lineTo(w * 0.14f, h * 0.56f)
                        lineTo(w * 0.44f, h * 0.56f)
                        lineTo(w * 0.34f, h * 0.95f)
                        lineTo(w * 0.86f, h * 0.42f)
                        lineTo(w * 0.55f, h * 0.42f)
                        close()
                    }
                    drawPath(bolt, Brush.verticalGradient(listOf(WinampColors.GoldHighlight, WinampColors.VolOrangeStart)))
                    drawPath(bolt, Color(0xFF94A3B8), style = Stroke(width = 1.4f))
                }
            }
        }
    }
}

@Composable
private fun WinampEqualizerContent(
    state: CorvixUiState,
    preampDb: Int,
    u: Dp,
    spU: (Float) -> TextUnit,
    onToggleOn: () -> Unit,
    onToggleAuto: () -> Unit,
    onCyclePreset: () -> Unit,
    onPreampChange: (Int) -> Unit,
    onBandChange: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = u * 5f, vertical = u * 4f),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top EQ Bar: ON, AUTO, Blue-Slate Grid Spline, PRESETS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(u * 19f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u * 3f)
        ) {
            WinampLedPill(
                label = "ON",
                active = state.eqEnabled,
                u = u,
                spU = spU,
                modifier = Modifier.width(u * 26f).height(u * 13f),
                onClick = onToggleOn
            )
            WinampLedPill(
                label = "AUTO",
                active = state.autoEqProfile != AutoEqProfile.MANUAL,
                u = u,
                spU = spU,
                modifier = Modifier.width(u * 34f).height(u * 13f),
                onClick = onToggleAuto
            )

            // Blue-slate EQ spline monitor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(WinampColors.EqGraphBg)
                    .border(1.dp, WinampColors.BevelDark)
                    .padding(1.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Vertical grid lines
                    val cols = 10
                    for (i in 1 until cols) {
                        val gx = w * i / cols.toFloat()
                        drawLine(WinampColors.EqGraphGrid, Offset(gx, 0f), Offset(gx, h), strokeWidth = 0.8f)
                    }
                    // Center 0 dB line
                    drawLine(WinampColors.EqGraphGrid, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 0.9f)

                    // Crisp yellow response curve
                    val path = Path()
                    val count = 10
                    for (i in 0 until count) {
                        val db = if (state.eqEnabled) state.eqBandsDb.getOrElse(i) { 0 } else 0
                        val x = (i.toFloat() / (count - 1)) * w
                        val y = h * 0.5f - (db / 14f) * (h * 0.42f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = if (state.eqEnabled) WinampColors.EqGraphYellow else WinampColors.DimText,
                        style = Stroke(width = 1.6f)
                    )
                }
            }

            WinampSilverTextButton(
                label = "PRESETS",
                u = u,
                spU = spU,
                modifier = Modifier.width(u * 44f).height(u * 13f),
                onClick = onCyclePreset
            )
        }

        // Preamp + +12db / 0db / -12db Scale + 10 Equalizer Vertical Faders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = u * 4f),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Preamp + Scale Column
            Column(
                modifier = Modifier.width(u * 56f).fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left dash ticks
                    Column(
                        modifier = Modifier.width(u * 6f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("—", color = WinampColors.DimText, fontSize = spU(5.5f))
                        Text("—", color = WinampColors.DimText, fontSize = spU(5.5f))
                        Text("—", color = WinampColors.DimText, fontSize = spU(5.5f))
                    }

                    WinampVerticalEqTroughSlider(
                        dbValue = preampDb,
                        u = u,
                        modifier = Modifier.width(u * 14f).fillMaxHeight(),
                        onDbChange = onPreampChange
                    )

                    // +12 db / • 0 db / -12 db scale labels
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = u * 2f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("+12 db", color = WinampColors.EqScaleAmber, fontSize = spU(5.8f), fontWeight = FontWeight.Bold)
                        Text("• 0 db", color = WinampColors.EqScaleAmber, fontSize = spU(5.8f), fontWeight = FontWeight.Bold)
                        Text("-12 db", color = WinampColors.EqScaleAmber, fontSize = spU(5.8f), fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "PREAMP",
                    color = Color(0xFFD5DBEA),
                    fontSize = spU(5.4f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = u * 4f, top = u * 1.5f)
                )
            }

            // 10 Bands: 70, 180, 320, 600, 1K, 3K, 6K, 12K, 14K, 16K
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WinampEqLabels.forEachIndexed { idx, label ->
                    val bandDb = state.eqBandsDb.getOrElse(idx) { 0 }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WinampVerticalEqTroughSlider(
                            dbValue = bandDb,
                            u = u,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onDbChange = { onBandChange(idx, it) }
                        )
                        Text(
                            text = label,
                            color = Color(0xFFD5DBEA),
                            fontSize = spU(5.6f),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.padding(top = u * 1.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WinampPlaylistContent(
    tracks: List<Track>,
    currentTrackId: Long,
    selectedIndex: Int,
    elapsedSec: Int,
    u: Dp,
    spU: (Float) -> TextUnit,
    onPlayTrack: (Track) -> Unit,
    onAddClick: () -> Unit,
    onRemClick: () -> Unit,
    onSelClick: () -> Unit,
    onMiscClick: () -> Unit,
    onListOptsClick: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onEject: () -> Unit
) {
    val listState = rememberLazyListState()
    val totalSec = remember(tracks) { tracks.sumOf { it.durationSec } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = u * 5f, vertical = u * 4f),
        verticalArrangement = Arrangement.spacedBy(u * 4f)
    ) {
        // Pure Black #000000 Track Terminal + Classic Right Scrollbar
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(u * 2.5f)
        ) {
            WinampBlackInset(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = u * 4f, vertical = u * 3f),
                    verticalArrangement = Arrangement.spacedBy(u * 1.5f)
                ) {
                    itemsIndexed(tracks, key = { idx, tr -> "${tr.id}_$idx" }) { idx, tr ->
                        val isPlayingRow = tr.id == currentTrackId
                        val isSelectedRow = idx == selectedIndex
                        val bgColor = if (isSelectedRow) WinampColors.PlaylistSelectedBlue else Color.Transparent
                        val textColor = when {
                            isSelectedRow -> Color.White
                            isPlayingRow -> Color.White
                            else -> WinampColors.TrackGreen
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .clickable { onPlayTrack(tr) }
                                .padding(horizontal = u * 2f, vertical = u * 2.4f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${tr.artist} - ${tr.title}",
                                color = textColor,
                                fontSize = spU(8.6f),
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = u * 6f)
                            )
                            Text(
                                text = formatShortDuration(tr.durationSec),
                                color = textColor,
                                fontSize = spU(8.6f),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            // Right Metallic Vertical Scrollbar
            Column(
                modifier = Modifier
                    .width(u * 8f)
                    .fillMaxHeight()
                    .background(Color(0xFF141722))
                    .border(0.8.dp, WinampColors.BevelDark),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Up cap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(u * 8f)
                        .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnBot)))
                        .border(0.6.dp, WinampColors.BtnShadow)
                )
                // Gold/silver thumb synced loosely to list scroll
                val scrollFraction = if (tracks.isEmpty()) 0f
                else (listState.firstVisibleItemIndex.toFloat() / tracks.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 0.7f)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = u * 1f)
                ) {
                    val thumbH = (maxHeight * 0.28f).coerceAtLeast(u * 14f)
                    val topOff = (maxHeight - thumbH) * scrollFraction
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, topOff.roundToPx()) }
                            .fillMaxWidth()
                            .height(thumbH)
                            .background(Brush.horizontalGradient(listOf(WinampColors.ScrollGoldLight, WinampColors.ScrollGoldDark)))
                            .border(0.7.dp, Color(0xFF282B3B))
                    )
                }
                // Down cap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(u * 8f)
                        .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnBot)))
                        .border(0.6.dp, WinampColors.BtnShadow)
                )
            }
        }

        // Bottom Playlist Action Strip: ADD REM SEL MISC | [0:00/27:45 + mini transport + 02:12] | LIST OPTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(u * 24f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u * 3f)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(u * 2f)) {
                WinampSilverTextButton("ADD", u, spU, Modifier.width(u * 24f).height(u * 20f), onAddClick)
                WinampSilverTextButton("REM", u, spU, Modifier.width(u * 24f).height(u * 20f), onRemClick)
                WinampSilverTextButton("SEL", u, spU, Modifier.width(u * 24f).height(u * 20f), onSelClick)
                WinampSilverTextButton("MISC", u, spU, Modifier.width(u * 24f).height(u * 20f), onMiscClick)
            }

            // Dual black telemetry LCD + clickable mini transport
            WinampBlackInset(
                modifier = Modifier
                    .weight(1f)
                    .height(u * 20f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = u * 3f, vertical = u * 1.5f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatShortDuration(elapsedSec)}/${formatShortDuration(totalSec)}",
                        color = WinampColors.TrackGreen,
                        fontSize = spU(6.4f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(u * 1.5f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniGoldGlyphButton(WinampGlyph.PREV, u, onPrev)
                            MiniGoldGlyphButton(WinampGlyph.PLAY, u, onPlay)
                            MiniGoldGlyphButton(WinampGlyph.PAUSE, u, onPause)
                            MiniGoldGlyphButton(WinampGlyph.STOP, u, onStop)
                            MiniGoldGlyphButton(WinampGlyph.NEXT, u, onNext)
                            MiniGoldGlyphButton(WinampGlyph.EJECT, u, onEject)
                        }
                        Text(
                            text = formatSeconds(elapsedSec),
                            color = WinampColors.TrackGreen,
                            fontSize = spU(6.2f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // LIST OPTS 2-line silver button
            Box(
                modifier = Modifier
                    .width(u * 28f)
                    .height(u * 20f)
                    .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnMid, WinampColors.BtnBot)))
                    .border(0.9.dp, WinampColors.BtnShadow)
                    .clickable { onListOptsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LIST\nOPTS",
                    color = WinampColors.BtnGlyph,
                    fontSize = spU(5.5f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = spU(6.2f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ================= LOW-LEVEL CLASSIC WINAMP PRIMITIVES =================

@Composable
private fun WinampBlackInset(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(WinampColors.LcdBlack)
            .border(0.8.dp, WinampColors.BevelLight.copy(alpha = 0.65f))
            .padding(0.8.dp)
            .border(0.8.dp, Color(0xFF06080D))
    ) {
        content()
    }
}

@Composable
private fun ClutterLetter(
    letter: String,
    spU: (Float) -> TextUnit,
    onClick: () -> Unit
) {
    Text(
        text = letter,
        color = WinampColors.ClutterText,
        fontSize = spU(5.5f),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        lineHeight = spU(6f),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun MiniGoldGlyphButton(
    glyph: WinampGlyph,
    u: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = u * 8.5f, height = u * 8.5f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(u * 6f, u * 5.6f)) {
            val w = size.width
            val h = size.height
            val col = WinampColors.GoldTop
            when (glyph) {
                WinampGlyph.PREV -> {
                    drawRect(col, Offset(0f, h * 0.1f), Size(w * 0.16f, h * 0.8f))
                    val t1 = Path().apply { moveTo(w * 0.54f, h * 0.1f); lineTo(w * 0.18f, h * 0.5f); lineTo(w * 0.54f, h * 0.9f); close() }
                    val t2 = Path().apply { moveTo(w * 0.95f, h * 0.1f); lineTo(w * 0.56f, h * 0.5f); lineTo(w * 0.95f, h * 0.9f); close() }
                    drawPath(t1, col)
                    drawPath(t2, col)
                }
                WinampGlyph.PLAY -> {
                    val p = Path().apply { moveTo(w * 0.18f, h * 0.1f); lineTo(w * 0.9f, h * 0.5f); lineTo(w * 0.18f, h * 0.9f); close() }
                    drawPath(p, col)
                }
                WinampGlyph.PAUSE -> {
                    drawRect(col, Offset(w * 0.18f, h * 0.12f), Size(w * 0.24f, h * 0.76f))
                    drawRect(col, Offset(w * 0.58f, h * 0.12f), Size(w * 0.24f, h * 0.76f))
                }
                WinampGlyph.STOP -> drawRect(col, Offset(w * 0.18f, h * 0.16f), Size(w * 0.64f, h * 0.68f))
                WinampGlyph.NEXT -> {
                    val t1 = Path().apply { moveTo(w * 0.05f, h * 0.1f); lineTo(w * 0.44f, h * 0.5f); lineTo(w * 0.05f, h * 0.9f); close() }
                    val t2 = Path().apply { moveTo(w * 0.46f, h * 0.1f); lineTo(w * 0.82f, h * 0.5f); lineTo(w * 0.46f, h * 0.9f); close() }
                    drawPath(t1, col)
                    drawPath(t2, col)
                    drawRect(col, Offset(w * 0.84f, h * 0.1f), Size(w * 0.16f, h * 0.8f))
                }
                WinampGlyph.EJECT -> {
                    val p = Path().apply { moveTo(w * 0.5f, h * 0.1f); lineTo(w * 0.92f, h * 0.58f); lineTo(w * 0.08f, h * 0.58f); close() }
                    drawPath(p, col)
                    drawRect(col, Offset(w * 0.08f, h * 0.72f), Size(w * 0.84f, h * 0.18f))
                }
            }
        }
    }
}

@Composable
private fun WinampLedPill(
    label: String,
    active: Boolean,
    u: Dp,
    spU: (Float) -> TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnMid, WinampColors.BtnBot)))
            .border(0.9.dp, WinampColors.BtnShadow)
            .clickable { onClick() }
            .padding(horizontal = u * 2.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(u * 4.2f)
                .background(if (active) WinampColors.LedGreen else Color(0xFF6B7387))
        )
        Spacer(modifier = Modifier.width(u * 2f))
        Text(
            text = label,
            color = WinampColors.BtnGlyph,
            fontSize = spU(5.6f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
    }
}

@Composable
private fun WinampRepeatPill(
    repeatMode: RepeatMode,
    u: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ledColor = when (repeatMode) {
        RepeatMode.OFF -> Color(0xFF6B7387)
        RepeatMode.ALL -> WinampColors.LedGreen
        RepeatMode.ONE -> Color(0xFFF59E0B)
    }
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnMid, WinampColors.BtnBot)))
            .border(0.9.dp, WinampColors.BtnShadow)
            .clickable { onClick() }
            .padding(horizontal = u * 2.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(u * 4.2f).background(ledColor))
        Spacer(modifier = Modifier.width(u * 2.5f))
        Canvas(modifier = Modifier.size(width = u * 11f, height = u * 8f)) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.1f, h * 0.45f)
                lineTo(w * 0.1f, h * 0.22f)
                lineTo(w * 0.86f, h * 0.22f)
                moveTo(w * 0.9f, h * 0.55f)
                lineTo(w * 0.9f, h * 0.78f)
                lineTo(w * 0.14f, h * 0.78f)
            }
            drawPath(p, WinampColors.BtnGlyph, style = Stroke(width = 1.6f, cap = StrokeCap.Square))
        }
    }
}

@Composable
private fun WinampSilverTextButton(
    label: String,
    u: Dp,
    spU: (Float) -> TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnMid, WinampColors.BtnBot)))
            .border(0.9.dp, WinampColors.BtnShadow)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = WinampColors.BtnGlyph,
            fontSize = spU(5.6f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

private enum class WinampGlyph { PREV, PLAY, PAUSE, STOP, NEXT, EJECT }

@Composable
private fun WinampTransportButton(
    glyph: WinampGlyph,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnMid, WinampColors.BtnBot)))
            .border(0.9.dp, WinampColors.BtnShadow)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width * 0.52f, height * 0.54f)) {
            val w = size.width
            val h = size.height
            val col = WinampColors.BtnGlyph
            when (glyph) {
                WinampGlyph.PREV -> {
                    drawRect(col, Offset(0f, h * 0.1f), Size(w * 0.16f, h * 0.8f))
                    val t1 = Path().apply { moveTo(w * 0.54f, h * 0.1f); lineTo(w * 0.18f, h * 0.5f); lineTo(w * 0.54f, h * 0.9f); close() }
                    val t2 = Path().apply { moveTo(w * 0.95f, h * 0.1f); lineTo(w * 0.56f, h * 0.5f); lineTo(w * 0.95f, h * 0.9f); close() }
                    drawPath(t1, col)
                    drawPath(t2, col)
                }
                WinampGlyph.PLAY -> {
                    val p = Path().apply { moveTo(w * 0.18f, h * 0.1f); lineTo(w * 0.9f, h * 0.5f); lineTo(w * 0.18f, h * 0.9f); close() }
                    drawPath(p, col)
                }
                WinampGlyph.PAUSE -> {
                    drawRect(col, Offset(w * 0.18f, h * 0.12f), Size(w * 0.24f, h * 0.76f))
                    drawRect(col, Offset(w * 0.58f, h * 0.12f), Size(w * 0.24f, h * 0.76f))
                }
                WinampGlyph.STOP -> {
                    drawRect(col, Offset(w * 0.18f, h * 0.16f), Size(w * 0.64f, h * 0.68f))
                }
                WinampGlyph.NEXT -> {
                    val t1 = Path().apply { moveTo(w * 0.05f, h * 0.1f); lineTo(w * 0.44f, h * 0.5f); lineTo(w * 0.05f, h * 0.9f); close() }
                    val t2 = Path().apply { moveTo(w * 0.46f, h * 0.1f); lineTo(w * 0.82f, h * 0.5f); lineTo(w * 0.46f, h * 0.9f); close() }
                    drawPath(t1, col)
                    drawPath(t2, col)
                    drawRect(col, Offset(w * 0.84f, h * 0.1f), Size(w * 0.16f, h * 0.8f))
                }
                WinampGlyph.EJECT -> {
                    val p = Path().apply { moveTo(w * 0.5f, h * 0.1f); lineTo(w * 0.92f, h * 0.58f); lineTo(w * 0.08f, h * 0.58f); close() }
                    drawPath(p, col)
                    drawRect(col, Offset(w * 0.08f, h * 0.72f), Size(w * 0.84f, h * 0.18f))
                }
            }
        }
    }
}

@Composable
private fun WinampHorizontalGrooveSlider(
    fraction: Float,
    fillBrush: Brush,
    centerOut: Boolean,
    u: Dp,
    modifier: Modifier = Modifier,
    onFractionChange: (Float) -> Unit
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val latestOnChange by rememberUpdatedState(onFractionChange)
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    latestOnChange((pos.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    latestOnChange((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val trackH = h * 0.44f
        val trackY = (h - trackH) / 2f

        // Groove background
        drawRect(Color(0xFF0D0F18), Offset(0f, trackY), Size(w, trackH))
        if (centerOut) {
            val mid = w * 0.5f
            val targetX = w * clamped
            val startX = minOf(mid, targetX)
            val width = abs(targetX - mid).coerceAtLeast(4f)
            drawRect(fillBrush, Offset(startX, trackY + 1f), Size(width, trackH - 2f))
        } else {
            drawRect(fillBrush, Offset(0f, trackY + 1f), Size(w * clamped, trackH - 2f))
        }

        // Grooved metallic [|||] thumb
        val thumbW = (u * 11f).toPx()
        val thumbH = h * 0.86f
        val thumbX = ((w - thumbW) * clamped).coerceIn(0f, w - thumbW)
        val thumbY = (h - thumbH) / 2f
        drawRect(
            brush = Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnBot)),
            topLeft = Offset(thumbX, thumbY),
            size = Size(thumbW, thumbH)
        )
        drawRect(WinampColors.BtnShadow, Offset(thumbX, thumbY), Size(thumbW, thumbH), style = Stroke(1f))
        for (g in 1..3) {
            val gx = thumbX + thumbW * (g / 4f)
            drawLine(WinampColors.BtnShadow, Offset(gx, thumbY + 2f), Offset(gx, thumbY + thumbH - 2f), strokeWidth = 1f)
        }
    }
}

@Composable
private fun WinampGoldSeekBar(
    progress: Float,
    u: Dp,
    modifier: Modifier = Modifier,
    onSeekFraction: (Float) -> Unit
) {
    val clamped = progress.coerceIn(0f, 1f)
    val latestOnSeek by rememberUpdatedState(onSeekFraction)
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    latestOnSeek((pos.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    latestOnSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
    ) {
        val w = size.width
        val h = size.height
        // Recessed groove
        drawRect(WinampColors.SeekGroove, Offset.Zero, size)
        drawRect(WinampColors.BevelLight.copy(alpha = 0.45f), Offset.Zero, size, style = Stroke(1f))

        // Iconic gold horizontal shuttle thumb
        val thumbW = (u * 29f).toPx()
        val thumbH = h * 0.84f
        val thumbX = ((w - thumbW) * clamped).coerceIn(1f, w - thumbW - 1f)
        val thumbY = (h - thumbH) / 2f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(WinampColors.SeekGoldLight, WinampColors.SeekGoldMid, WinampColors.SeekGoldDark)
            ),
            topLeft = Offset(thumbX, thumbY),
            size = Size(thumbW, thumbH)
        )
        drawRect(Color(0xFF3B2D0B), Offset(thumbX, thumbY), Size(thumbW, thumbH), style = Stroke(1f))
        // Inner horizontal slot line
        drawLine(
            color = WinampColors.SeekGoldLight,
            start = Offset(thumbX + 4f, h * 0.5f),
            end = Offset(thumbX + thumbW - 4f, h * 0.5f),
            strokeWidth = 1.4f
        )
    }
}

@Composable
private fun WinampVerticalEqTroughSlider(
    dbValue: Int,
    u: Dp,
    modifier: Modifier = Modifier,
    onDbChange: (Int) -> Unit
) {
    // dbValue in -12..12 -> fraction 0f (bottom, -12) .. 1f (top, +12)
    val frac = ((dbValue.coerceIn(-12, 12) + 12) / 24f)
    val latestOnDbChange by rememberUpdatedState(onDbChange)
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val f = 1f - (pos.y / size.height.toFloat()).coerceIn(0f, 1f)
                    latestOnDbChange((f * 24f - 12f).roundToInt())
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val f = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    latestOnDbChange((f * 24f - 12f).roundToInt())
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val slotW = (u * 6f).toPx().coerceAtMost(w * 0.52f)
        val slotX = (w - slotW) / 2f
        val padY = (u * 3f).toPx()
        val slotH = (h - padY * 2f).coerceAtLeast(10f)

        // Yellow -> Green recessed trough
        drawRect(
            brush = Brush.verticalGradient(
                listOf(WinampColors.EqTroughTop, WinampColors.EqTroughMid, WinampColors.EqTroughBot)
            ),
            topLeft = Offset(slotX, padY),
            size = Size(slotW, slotH)
        )
        drawRect(WinampColors.BevelDark, Offset(slotX, padY), Size(slotW, slotH), style = Stroke(1f))

        // Metallic grooved thumb
        val thumbW = (u * 10.5f).toPx().coerceAtMost(w * 0.92f)
        val thumbH = (u * 8.5f).toPx()
        val thumbX = (w - thumbW) / 2f
        val thumbY = padY + (slotH - thumbH) * (1f - frac)

        drawRect(
            brush = Brush.verticalGradient(listOf(WinampColors.BtnTop, WinampColors.BtnBot)),
            topLeft = Offset(thumbX, thumbY),
            size = Size(thumbW, thumbH)
        )
        drawRect(Color(0xFF1A1E2B), Offset(thumbX, thumbY), Size(thumbW, thumbH), style = Stroke(1f))
        // Horizontal groove lines
        drawLine(WinampColors.BtnShadow, Offset(thumbX + 2f, thumbY + thumbH * 0.5f), Offset(thumbX + thumbW - 2f, thumbY + thumbH * 0.5f), strokeWidth = 1.2f)
    }
}

@Composable
private fun WinampSevenSegClock(
    text: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val chars = text.takeLast(5)
        val slotW = size.width / chars.length.coerceAtLeast(1)
        val h = size.height
        val stroke = (h * 0.11f).coerceIn(1.5f, 3.2f)

        chars.forEachIndexed { idx, ch ->
            val ox = idx * slotW
            if (ch == ':') {
                val cx = ox + slotW * 0.5f
                drawCircle(WinampColors.LcdGreen, radius = stroke * 0.65f, center = Offset(cx, h * 0.32f))
                drawCircle(WinampColors.LcdGreen, radius = stroke * 0.65f, center = Offset(cx, h * 0.68f))
            } else {
                val mask = sevenSegMask(ch)
                val xL = ox + slotW * 0.18f
                val xR = ox + slotW * 0.82f
                val yT = h * 0.12f
                val yM = h * 0.50f
                val yB = h * 0.88f
                val col = WinampColors.LcdGreen

                if (mask[0]) drawLine(col, Offset(xL, yT), Offset(xR, yT), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[1]) drawLine(col, Offset(xR, yT), Offset(xR, yM), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[2]) drawLine(col, Offset(xR, yM), Offset(xR, yB), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[3]) drawLine(col, Offset(xL, yB), Offset(xR, yB), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[4]) drawLine(col, Offset(xL, yM), Offset(xL, yB), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[5]) drawLine(col, Offset(xL, yT), Offset(xL, yM), strokeWidth = stroke, cap = StrokeCap.Round)
                if (mask[6]) drawLine(col, Offset(xL, yM), Offset(xR, yM), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

private fun sevenSegMask(ch: Char): BooleanArray = when (ch) {
    '0' -> booleanArrayOf(true, true, true, true, true, true, false)
    '1' -> booleanArrayOf(false, true, true, false, false, false, false)
    '2' -> booleanArrayOf(true, true, false, true, true, false, true)
    '3' -> booleanArrayOf(true, true, true, true, false, false, true)
    '4' -> booleanArrayOf(false, true, true, false, false, true, true)
    '5' -> booleanArrayOf(true, false, true, true, false, true, true)
    '6' -> booleanArrayOf(true, false, true, true, true, true, true)
    '7' -> booleanArrayOf(true, true, true, false, false, false, false)
    '8' -> booleanArrayOf(true, true, true, true, true, true, true)
    '9' -> booleanArrayOf(true, true, true, true, false, true, true)
    '-' -> booleanArrayOf(false, false, false, false, false, false, true)
    else -> booleanArrayOf(false, false, false, false, false, false, false)
}

@Composable
private fun WinampSpectrumCanvas(
    levels: FloatArray,
    peakLevels: FloatArray,
    isPlaying: Boolean,
    oscMode: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        if (oscMode) {
            val path = Path()
            val steps = 36
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val lv = levels.getOrElse((t * (levels.size - 1)).roundToInt()) { 0.3f }
                val amp = if (isPlaying) sin(t * 6f * PI.toFloat() + lv * 4f) * (h * 0.36f * lv) else 0f
                val x = t * w
                val y = h * 0.5f + amp
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, WinampColors.LcdGreen, style = Stroke(width = 1.5f))
            return@Canvas
        }

        val cols = 19
        val gapX = 1.5f
        val barW = (w - gapX * (cols - 1)) / cols
        val segH = 2.2f
        val segGap = 1.2f
        val maxSegs = (h / (segH + segGap)).toInt().coerceAtLeast(4)

        for (i in 0 until cols) {
            val srcIdx = (i * levels.size / cols).coerceIn(0, levels.lastIndex)
            val raw = if (isPlaying) {
                val harmonic = 0.65f + 0.35f * cos(i * 0.75f + levels[srcIdx] * 5f)
                (levels[srcIdx] * harmonic).coerceIn(0.08f, 0.96f)
            } else {
                0.04f
            }
            if (raw >= peakLevels[i]) peakLevels[i] = raw
            else peakLevels[i] = (peakLevels[i] - 0.025f).coerceAtLeast(raw)

            val litSegs = (raw * maxSegs).roundToInt().coerceIn(0, maxSegs)
            val x = i * (barW + gapX)

            for (s in 0 until litSegs) {
                val y = h - (s + 1) * (segH + segGap)
                val ratio = s.toFloat() / maxSegs.toFloat()
                val col = when {
                    ratio > 0.78f -> Color(0xFFFF3300)
                    ratio > 0.52f -> Color(0xFFFFD000)
                    ratio > 0.30f -> Color(0xFFA8FF00)
                    else -> WinampColors.LcdGreen
                }
                drawRect(col, Offset(x, y), Size(barW, segH))
            }

            // Floating grey peak tick
            val pSeg = (peakLevels[i] * maxSegs).roundToInt().coerceIn(0, maxSegs - 1)
            val py = h - (pSeg + 1) * (segH + segGap)
            drawRect(Color(0xFF8B93A8), Offset(x, py), Size(barW, 1.6f))
        }
    }
}

private fun formatShortDuration(totalSec: Int): String {
    val clamped = totalSec.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return "%d:%02d".format(mins, secs)
}

private fun parseKbpsBadge(bitrateInfo: String): String {
    val match = Regex("(\\d[\\d,]*)\\s*kbps", RegexOption.IGNORE_CASE).find(bitrateInfo)
    val digits = match?.groupValues?.getOrNull(1)?.replace(",", "") ?: "128"
    return if (digits.length > 4) digits.take(4) else digits
}

private fun parseKhzBadge(formatLabel: String): String {
    val match = Regex("(\\d+(?:\\.\\d+)?)\\s*khz", RegexOption.IGNORE_CASE).find(formatLabel)
    val raw = match?.groupValues?.getOrNull(1) ?: "44"
    return raw.substringBefore(".")
}
