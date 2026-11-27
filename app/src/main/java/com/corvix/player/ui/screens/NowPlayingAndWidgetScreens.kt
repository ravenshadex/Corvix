package com.corvix.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.platform.LocalContext
import com.corvix.player.widget.CorvixWidgetBridge
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.SheetType
import com.corvix.player.model.Track
import com.corvix.player.model.formatSeconds
import com.corvix.player.ui.components.ArtworkBox
import com.corvix.player.ui.components.CorvixSeekSlider
import com.corvix.player.ui.components.GlassPill
import com.corvix.player.ui.components.GlassSectionDivider
import com.corvix.player.ui.components.GlassSurface
import com.corvix.player.ui.components.IosHeartIcon
import com.corvix.player.ui.components.IpodSeventhGenNowPlaying
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.ui.theme.liquidGlassSurface
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val track = state.currentTrack
    val dur = track.durationSec.coerceAtLeast(1)

    var volumeHudVisible by remember { mutableStateOf(false) }

    if (palette.isIpod) {
        val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
        IpodSeventhGenNowPlaying(
            state = state,
            elapsedSec = elapsedSec,
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    val portraitArtCap = minOf(
        metrics.widthDp - metrics.horizontalPadding * 2,
        metrics.heightDp * if (metrics.isShortHeight) 0.34f else 0.38f
    ).coerceIn(185.dp, 380.dp)

    val landscapeArtCap = minOf(
        metrics.heightDp * 0.72f,
        metrics.widthDp * 0.42f
    ).coerceIn(170.dp, 380.dp)

    if (!palette.isIpod && metrics.isWideOrLandscape) {
        // Side-by-side responsive landscape / tablet Now Playing layout
        val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = metrics.horizontalPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NowPlayingTopHeader(state = state, track = track, viewModel = viewModel)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(0.46f),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.lyricsDrawerOpen) {
                        NowPlayingLyricsDrawer(
                            state = state,
                            track = track,
                            elapsedSec = elapsedSec,
                            viewModel = viewModel,
                            cardHeight = landscapeArtCap
                        )
                    } else {
                        NowPlayingArtworkCard(
                            track = track,
                            state = state,
                            viewModel = viewModel,
                            volumeHudVisible = volumeHudVisible,
                            onVolumeHudChange = { volumeHudVisible = it },
                            artSize = landscapeArtCap
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NowPlayingMetaAndActions(state = state, track = track, viewModel = viewModel)
                    NowPlayingSeekCard(track = track, state = state, dur = dur, viewModel = viewModel)
                    NowPlayingTransportDeck(state = state, viewModel = viewModel, isCompact = true)
                    NowPlayingUtilityBar(state = state, viewModel = viewModel)
                    NowPlayingUpNextCard(state = state, viewModel = viewModel, horizontalPadding = 12.dp)
                }
            }
        }
        return
    }

    // Full-screen Portrait Now Playing: 8dp below status bar, balanced upper controls, flush bottom UP NEXT card
    val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = metrics.horizontalPadding,
                    end = metrics.horizontalPadding,
                    top = 8.dp,
                    bottom = 10.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NowPlayingTopHeader(state = state, track = track, viewModel = viewModel)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.lyricsDrawerOpen) {
                    NowPlayingLyricsDrawer(
                        state = state,
                        track = track,
                        elapsedSec = elapsedSec,
                        viewModel = viewModel,
                        cardHeight = portraitArtCap
                    )
                } else {
                    NowPlayingArtworkCard(
                        track = track,
                        state = state,
                        viewModel = viewModel,
                        volumeHudVisible = volumeHudVisible,
                        onVolumeHudChange = { volumeHudVisible = it },
                        artSize = portraitArtCap
                    )
                }
            }

            NowPlayingMetaAndActions(state = state, track = track, viewModel = viewModel)
            NowPlayingSeekCard(track = track, state = state, dur = dur, viewModel = viewModel)
            NowPlayingTransportDeck(state = state, viewModel = viewModel, isCompact = metrics.isShortHeight)
            NowPlayingUtilityBar(state = state, viewModel = viewModel)
        }

        NowPlayingUpNextCard(
            state = state,
            viewModel = viewModel,
            horizontalPadding = metrics.horizontalPadding
        )
    }
}

@Composable
private fun NowPlayingTopHeader(
    state: CorvixUiState,
    track: Track,
    viewModel: CorvixViewModel
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!palette.isIpod) {
            GlassPill(onClick = { viewModel.navigate(AppScreen.LIBRARY) }, padding = PaddingValues(10.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            contentAlignment = if (palette.isIpod) Alignment.CenterStart else Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .border(0.85.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .clickable { viewModel.openSheet(SheetType.SIGNAL) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "NOW PLAYING · ${track.shortFormat} · ${state.activeSink.shortName.uppercase()}",
                    style = font.style(11f, FontWeight.SemiBold, Color(0xFFF5F5F5), mono = true),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        GlassPill(onClick = { viewModel.openSheet(SheetType.NP_MENU) }, padding = PaddingValues(10.dp)) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Track options menu", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NowPlayingArtworkCard(
    track: Track,
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    volumeHudVisible: Boolean,
    onVolumeHudChange: (Boolean) -> Unit,
    artSize: Dp
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var artWidthPx by remember { mutableStateOf(1f) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(artSize)
            .onSizeChanged { artWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .clip(RoundedCornerShape(26.dp))
            .pointerInput(track.id, state.lyricsDrawerOpen) {
                if (state.lyricsDrawerOpen) return@pointerInput
                var mode = 0
                var cumX = 0f
                var cumY = 0f
                var startVol = state.volumePct
                detectDragGestures(
                    onDragStart = { start ->
                        val relX = start.x / artWidthPx
                        mode = 0
                        cumX = 0f
                        cumY = 0f
                        startVol = state.volumePct
                        if (relX < 0.22f || relX > 0.78f) {
                            mode = -1
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumX += dragAmount.x
                        cumY += dragAmount.y
                        if ((mode == 0 || mode == -1) && abs(cumY) > 14f && abs(cumY) > abs(cumX) && mode == -1) {
                            mode = 1
                            onVolumeHudChange(true)
                        } else if ((mode == 0 || mode == -1) && abs(cumX) > 16f) {
                            mode = 2
                        }
                        if (mode == 1) {
                            val nextVol = (startVol - cumY * 0.25f).roundToInt().coerceIn(0, 100)
                            viewModel.adjustVolume(nextVol)
                        } else if (mode == 2) {
                            dragOffsetX = (cumX * 0.35f).coerceIn(-40f, 40f)
                        }
                    },
                    onDragEnd = {
                        onVolumeHudChange(false)
                        if (mode == 2 && abs(cumX) > 70f) {
                            if (cumX < 0f) {
                                viewModel.nextTrack()
                                viewModel.showGestureHud("Next Track ⏭")
                            } else {
                                viewModel.prevTrack()
                                viewModel.showGestureHud("⏮ Previous Track")
                            }
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        onVolumeHudChange(false)
                        dragOffsetX = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ArtworkBox(
            artStyle = track.artStyle,
            track = track,
            highRes = true,
            showFallbackDisc = true,
            showBorder = false,
            cornerRadius = 26.dp,
            modifier = Modifier.fillMaxSize()
        )

        if (state.lyricsDrawerOpen) {
            val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
            val lyrics = track.lyrics
                val lyricListState = rememberLazyListState()
                val activeIdx = lyrics?.indexOfLast { elapsedSec >= it.sec }?.coerceAtLeast(0) ?: 0
                LaunchedEffect(activeIdx, lyrics?.size) {
                    if (!lyrics.isNullOrEmpty() && activeIdx in lyrics.indices) {
                        lyricListState.animateScrollToItem(activeIdx)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xC8090D16))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!lyrics.isNullOrEmpty()) {
                                    "SYNCED LYRICS${track.lyricsSource?.let { " · $it" }.orEmpty()}"
                                } else {
                                    "LYRICS"
                                },
                                style = font.style(10.5f, FontWeight.Bold, palette.accentText),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                GlassPill(
                                    onClick = { viewModel.toggleEnglishLyricsOnly() },
                                    active = state.englishLyricsOnly,
                                    padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (state.englishLyricsOnly) "ENG ON" else "ORIG",
                                        style = font.style(
                                            10f,
                                            FontWeight.Bold,
                                            if (state.englishLyricsOnly) palette.activePillContent else Color.White
                                        )
                                    )
                                }
                                GlassPill(onClick = { viewModel.openLyricsSheet(false) }, padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Change", style = font.style(10f, FontWeight.SemiBold, Color.White))
                                }
                                GlassPill(onClick = { viewModel.toggleLyricsDrawer() }, padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Cover", style = font.style(10f, FontWeight.SemiBold, Color.White))
                                }
                            }
                        }

                        if (!lyrics.isNullOrEmpty()) {
                            LazyColumn(
                                state = lyricListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(lyrics) { idx, line ->
                                    Text(
                                        text = line.text,
                                        style = font.style(
                                            sizeSp = if (idx == activeIdx) 15f else 13f,
                                            weight = if (idx == activeIdx) FontWeight.Bold else FontWeight.Normal,
                                            color = if (idx == activeIdx) Color.White else Color.White.copy(alpha = 0.56f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.seekTo(line.sec) }
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (state.isFetchingLyrics) state.lyricsFetchStatus else "No local lyrics found for \"${track.title}\".",
                                    style = font.style(12.5f, FontWeight.Medium, Color.White.copy(alpha = 0.85f)),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.size(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassPill(
                                        onClick = { viewModel.openLyricsSheet(true) },
                                        padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Upload .lrc", style = font.style(11.5f, FontWeight.SemiBold, Color.White))
                                    }
                                    GlassPill(
                                        onClick = { viewModel.openLyricsSheet(false) },
                                        active = true,
                                        padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Fetch & Select",
                                            style = font.style(11.5f, FontWeight.Bold, palette.activePillContent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Swipe ◀▶ Skip · Edge ↕ Volume",
                    style = font.style(10f, FontWeight.Medium, Color.White.copy(alpha = 0.80f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0x56000000), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            if (state.gestureHudText != null) {
                GlassSurface(
                    strong = true,
                    cornerRadius = 999.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                ) {
                    Text(state.gestureHudText, style = font.style(11f, FontWeight.SemiBold, Color.White))
                }
            }

            if (volumeHudVisible) {
                GlassSurface(
                    strong = true,
                    cornerRadius = 999.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${state.volumePct}%", style = font.style(9.5f, FontWeight.Bold, Color.White, mono = true))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((state.volumePct / 100f).coerceIn(0.05f, 1f))
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun NowPlayingMetaAndActions(
    state: CorvixUiState,
    track: Track,
    viewModel: CorvixViewModel
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(track.title, style = font.style(20f, FontWeight.Bold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = font.style(12.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassPill(onClick = { viewModel.toggleLike() }, padding = PaddingValues(10.dp)) {
            IosHeartIcon(
                liked = state.isCurrentLiked,
                unlikedTint = palette.textPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun NowPlayingLyricsDrawer(
    state: CorvixUiState,
    track: Track,
    elapsedSec: Int,
    viewModel: CorvixViewModel,
    cardHeight: Dp = 280.dp
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    GlassSurface(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        val lyrics = track.lyrics
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val has = !lyrics.isNullOrEmpty()
                Text(
                    text = if (has) {
                        "SYNCED LYRICS${track.lyricsSource?.let { " · $it" }.orEmpty()}"
                    } else {
                        "LYRICS"
                    },
                    style = font.style(12f, FontWeight.SemiBold, palette.textMuted),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                GlassPill(onClick = { viewModel.openLyricsSheet(true) }, padding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("Upload / Fetch", style = font.style(11f, FontWeight.SemiBold, palette.textPrimary))
                }
            }

            if (!lyrics.isNullOrEmpty()) {
                val activeIdx = lyrics.indexOfLast { elapsedSec >= it.sec }.coerceAtLeast(0)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lyrics.forEachIndexed { idx, line ->
                        Text(
                            text = line.text,
                            style = font.style(
                                sizeSp = if (idx == activeIdx) 15.5f else 14f,
                                weight = if (idx == activeIdx) FontWeight.Bold else FontWeight.Normal,
                                color = if (idx == activeIdx) palette.accentText else palette.textMuted
                            )
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Text(
                        "No local lyrics found for \"${track.title}\".",
                        style = font.style(13f, FontWeight.Normal, palette.textMuted)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill(onClick = { viewModel.openLyricsSheet(true) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 10.dp)) {
                            Text("↑ Upload .lrc", style = font.style(12f, FontWeight.SemiBold, palette.textPrimary))
                        }
                        GlassPill(onClick = { viewModel.openLyricsSheet(false) }, active = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 10.dp)) {
                            Text("🌐 Fetch from Sites", style = font.style(12f, FontWeight.SemiBold, palette.activePillContent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingSeekCard(
    track: Track,
    state: CorvixUiState,
    dur: Int,
    viewModel: CorvixViewModel
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
    val progressFrac = (elapsedSec.toFloat() / dur.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        CorvixSeekSlider(
            valueFraction = progressFrac,
            onValueFractionChange = { frac -> viewModel.seekTo((frac * dur).roundToInt()) },
            seekStyle = state.seekBarStyle,
            isPlaying = state.isPlaying
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeconds(elapsedSec), style = font.style(12.5f, FontWeight.Normal, palette.textMuted, mono = true))
            Text(
                text = "${track.shortFormat} · ${track.bitrateInfo}",
                style = font.style(12f, FontWeight.Normal, palette.textMuted, mono = true),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(track.durationFormatted, style = font.style(12.5f, FontWeight.Normal, palette.textMuted, mono = true))
        }
    }
}

@Composable
private fun NowPlayingTransportDeck(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    isCompact: Boolean
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val playBtnSize = if (isCompact) 48.dp else 68.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (isCompact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle",
            tint = if (state.isShuffle) palette.accentText else palette.textMuted,
            modifier = Modifier.size(23.dp).clickable { viewModel.toggleShuffle() }
        )
        GlassPill(onClick = { viewModel.prevTrack() }, padding = PaddingValues(13.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = palette.textPrimary, modifier = Modifier.size(24.dp))
        }
        GlassPill(
            onClick = { viewModel.togglePlay() },
            active = true,
            padding = PaddingValues(0.dp),
            modifier = Modifier
                .size(playBtnSize)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.42f),
                                palette.accent.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            radius = size.maxDimension * 0.85f
                        ),
                        radius = size.maxDimension * 0.85f
                    )
                }
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play/Pause",
                tint = palette.activePillContent,
                modifier = Modifier.size(if (isCompact) 24.dp else 32.dp)
            )
        }
        GlassPill(onClick = { viewModel.nextTrack() }, padding = PaddingValues(13.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = palette.textPrimary, modifier = Modifier.size(24.dp))
        }
        Text(
            text = state.repeatMode.badge,
            style = font.style(12.5f, FontWeight.SemiBold, if (state.repeatMode != RepeatMode.OFF) palette.accentText else palette.textMuted, mono = true),
            modifier = Modifier.clickable { viewModel.cycleRepeat() }.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun NowPlayingUtilityBar(
    state: CorvixUiState,
    viewModel: CorvixViewModel
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassPill(onClick = { viewModel.openSettingsEq() }, modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 9.dp)) {
            Text("EQ", maxLines = 1, style = font.style(12f, FontWeight.SemiBold, palette.textPrimary))
        }
        GlassPill(onClick = { viewModel.cycleSpeed() }, modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 9.dp)) {
            Text(state.speedLabel, maxLines = 1, style = font.style(12f, FontWeight.SemiBold, palette.textPrimary))
        }
        val sleepActive = state.sleepTimerLabel != "Timer"
        GlassPill(
            onClick = { viewModel.openSheet(SheetType.SLEEP) },
            active = sleepActive,
            modifier = Modifier.weight(1.25f),
            padding = PaddingValues(vertical = 9.dp)
        ) {
            Text(
                text = if (sleepActive) state.sleepTimerLabel else "Sleep Timer",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = font.style(12f, FontWeight.SemiBold, if (sleepActive) palette.activePillContent else palette.textPrimary)
            )
        }
        GlassPill(
            onClick = { viewModel.toggleLyricsDrawer() },
            active = state.lyricsDrawerOpen,
            modifier = Modifier.weight(1f),
            padding = PaddingValues(vertical = 9.dp)
        ) {
            Text(
                text = "Lyrics",
                maxLines = 1,
                style = font.style(12f, FontWeight.SemiBold, if (state.lyricsDrawerOpen) palette.activePillContent else palette.textPrimary)
            )
        }
    }
}

@Composable
private fun NowPlayingUpNextCard(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    horizontalPadding: Dp = 16.dp
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val tracks = state.tracks
    if (tracks.size <= 1 && state.nextUpTrackIds.isEmpty()) return

    val collapsedHeight = 152.dp
    val expandedHeight = 430.dp
    var targetHeight by remember { mutableStateOf(collapsedHeight) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetHeight,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.82f,
            stiffness = 380f
        ),
        label = "upnext_height"
    )
    val effectiveHeight = if (isDragging) targetHeight else animatedHeight
    val density = androidx.compose.ui.platform.LocalDensity.current

    val queuedIdsSet = remember(state.nextUpTrackIds) { state.nextUpTrackIds.toSet() }
    val upcoming = remember(tracks, state.currentTrackIndex, state.isShuffle, state.shuffledUpcomingIds, state.nextUpTrackIds) {
        val byId = tracks.associateBy { it.id }
        val queuedTracks = state.nextUpTrackIds.mapNotNull { byId[it] }
        val baseUpcoming = if (state.isShuffle) {
            val ordered = state.shuffledUpcomingIds.mapNotNull { byId[it] }
            if (ordered.isNotEmpty()) ordered
            else tracks.filter { it.id != state.currentTrack.id }
        } else {
            val count = (tracks.size - 1).coerceAtLeast(0)
            List(count) { offset ->
                tracks[(state.currentTrackIndex + 1 + offset) % tracks.size]
            }
        }
        val filteredBase = baseUpcoming.filter { it.id !in queuedIdsSet && it.id != state.currentTrack.id }
        queuedTracks + filteredBase
    }
    val cardShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    fun handleDrag(dragDeltaPx: Float) {
        val deltaDp = with(density) { dragDeltaPx.toDp() }
        targetHeight = (targetHeight - deltaDp).coerceIn(collapsedHeight, expandedHeight)
    }

    fun settleDrag() {
        isDragging = false
        val mid = (collapsedHeight + expandedHeight) / 2f
        targetHeight = if (targetHeight >= mid) expandedHeight else collapsedHeight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = cardShape,
                isDark = palette.isDark,
                isAmoled = palette.isAmoled,
                strong = false,
                elevation = 12.dp,
                preset = palette.preset,
                accent = palette.accent,
                accentSecondary = palette.accentSecondary
            )
            .padding(start = horizontalPadding, end = horizontalPadding, top = 6.dp, bottom = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { settleDrag() },
                        onDragCancel = { settleDrag() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            handleDrag(dragAmount)
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    targetHeight = if (targetHeight > collapsedHeight + 20.dp) collapsedHeight else expandedHeight
                }
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (palette.isDark) Color.White.copy(alpha = 0.34f) else Color(0xFF0F172A).copy(alpha = 0.24f))
            )
            val headerLabel = buildString {
                append("UP NEXT")
                if (state.nextUpTrackIds.isNotEmpty()) {
                    append(" · ${state.nextUpTrackIds.size} QUEUED")
                }
                if (state.isShuffle) {
                    append(" · SHUFFLED")
                }
            }
            Text(
                text = headerLabel,
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted, mono = true)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(effectiveHeight),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(upcoming, key = { _, item -> item.id }) { _, item ->
                UpNextRowItem(
                    item = item,
                    isQueued = item.id in queuedIdsSet,
                    onClick = { viewModel.playTrackById(item.id) },
                    onMove = { delta -> viewModel.moveUpcomingTrack(item.id, delta) }
                )
            }
        }
    }
}

@Composable
private fun UpNextRowItem(
    item: Track,
    isQueued: Boolean = false,
    onClick: () -> Unit,
    onMove: (Int) -> Unit = {}
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stepPx = with(density) { 48.dp.toPx() }
    var dragAccum by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val latestOnMove by androidx.compose.runtime.rememberUpdatedState(onMove)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = dragAccum
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isDragging) {
                    Modifier.background(
                        if (palette.isDark) Color.White.copy(alpha = 0.12f)
                        else Color(0xFF0F172A).copy(alpha = 0.08f)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ArtworkBox(
            artStyle = item.artStyle,
            track = item,
            cornerRadius = 8.dp,
            showBorder = false,
            modifier = Modifier.size(38.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.title,
                    style = font.style(13.5f, FontWeight.SemiBold, palette.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isQueued) {
                    Text(
                        text = "QUEUED",
                        style = font.style(9.5f, FontWeight.Bold, palette.accentText),
                        modifier = Modifier
                            .background(
                                color = palette.accentText.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = item.artist,
                style = font.style(11.5f, FontWeight.Normal, palette.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Reorder track",
            tint = if (isDragging) palette.accentText else palette.textMuted.copy(alpha = 0.8f),
            modifier = Modifier
                .size(32.dp)
                .padding(6.dp)
                .pointerInput(item.id) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccum = 0f
                            isDragging = true
                        },
                        onDragEnd = {
                            dragAccum = 0f
                            isDragging = false
                        },
                        onDragCancel = {
                            dragAccum = 0f
                            isDragging = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccum += dragAmount
                            while (dragAccum <= -stepPx) {
                                latestOnMove(-1)
                                dragAccum += stepPx
                            }
                            while (dragAccum >= stepPx) {
                                latestOnMove(1)
                                dragAccum -= stepPx
                            }
                        }
                    )
                }
        )
        Text(
            text = item.durationFormatted,
            style = font.style(12f, FontWeight.Normal, palette.textMuted, mono = true)
        )
    }
}

@Composable
fun WidgetPreviewScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val context = LocalContext.current
    val track = state.currentTrack
    val elapsedSec by viewModel.elapsedSecFlow.collectAsState()
    val dur = track.durationSec.coerceAtLeast(1)
    val frac = (elapsedSec.toFloat() / dur.toFloat()).coerceIn(0.05f, 1f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.horizontalPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Home Screen Widgets", style = font.style(19f, FontWeight.Bold, palette.textPrimary))
                    Text("Liquid Glass & Winamp Classic", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                }
                GlassPill(onClick = { viewModel.navigate(AppScreen.SETTINGS) }, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Back to App", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
                }
            }

            // 1. Liquid Glass Widget + Add to Home Screen
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4×2 LIQUID GLASS WIDGET", style = font.style(11f, FontWeight.SemiBold, palette.textMuted))
                    GlassPill(
                        onClick = {
                            val ok = CorvixWidgetBridge.requestPinGlassWidget(context)
                            if (!ok) {
                                viewModel.showToast("Long-press home screen → Widgets → Corvix Liquid Glass")
                            }
                        },
                        active = true,
                        padding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("+ Add to Home", style = font.style(11f, FontWeight.SemiBold, palette.activePillContent))
                    }
                }
                GlassSurface(strong = true, cornerRadius = 28.dp, contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.weight(1f).clickable { viewModel.navigate(AppScreen.NOW_PLAYING) },
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkBox(track.artStyle, track = track, cornerRadius = 14.dp, modifier = Modifier.size((48f * metrics.scale).dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = font.style(14f, FontWeight.Bold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${track.artist} · ${track.album}", style = font.style(12f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (state.showWidgetCodecBadge) {
                                GlassPill(onClick = {}, padding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                                    Text(track.shortFormat, style = font.style(11f, FontWeight.Medium, palette.textPrimary, mono = true))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.sliderTrack, CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(palette.activePillBrush, CircleShape)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                GlassPill(onClick = { viewModel.prevTrack() }, padding = PaddingValues(8.dp)) {
                                    Icon(Icons.Rounded.SkipPrevious, null, tint = palette.textPrimary, modifier = Modifier.size(14.dp))
                                }
                                GlassPill(onClick = { viewModel.togglePlay() }, active = true, padding = PaddingValues(8.dp)) {
                                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = palette.activePillContent, modifier = Modifier.size(16.dp))
                                }
                                GlassPill(onClick = { viewModel.nextTrack() }, padding = PaddingValues(8.dp)) {
                                    Icon(Icons.Rounded.SkipNext, null, tint = palette.textPrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Winamp Classic Widget + Add to Home Screen
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4×2 WINAMP CLASSIC WIDGET", style = font.style(11f, FontWeight.SemiBold, palette.textMuted))
                    GlassPill(
                        onClick = {
                            val ok = CorvixWidgetBridge.requestPinWinampWidget(context)
                            if (!ok) {
                                viewModel.showToast("Long-press home screen → Widgets → Corvix Winamp Classic")
                            }
                        },
                        active = true,
                        padding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("+ Add Winamp", style = font.style(11f, FontWeight.SemiBold, palette.activePillContent))
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222436), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFF5B627B), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚡ CORVIX WINAMP",
                            style = font.style(11f, FontWeight.Bold, Color(0xFFD8B95B), mono = true)
                        )
                        Text(
                            "${track.shortFormat} · ${formatSeconds(elapsedSec)}/${track.durationFormatted}",
                            style = font.style(10.5f, FontWeight.Bold, Color(0xFF00FF00), mono = true)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF070A08), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF474E66), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            track.title.uppercase(),
                            style = font.style(13f, FontWeight.Bold, Color(0xFF00FF00), mono = true),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${track.artist.uppercase()} · ${track.album.uppercase()}",
                            style = font.style(11f, FontWeight.Normal, Color(0xFF7CE87C), mono = true),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "|<" to { viewModel.prevTrack() },
                            (if (state.isPlaying) "⏸ PAUSE" else "▶ PLAY") to { viewModel.togglePlay() },
                            ">|" to { viewModel.nextTrack() }
                        ).forEachIndexed { idx, (label, action) ->
                            Box(
                                modifier = Modifier
                                    .weight(if (idx == 1) 1.4f else 1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF3A3F55), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF6D7596), RoundedCornerShape(4.dp))
                                    .clickable { action() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = font.style(
                                        11.5f,
                                        FontWeight.Bold,
                                        if (idx == 1) Color(0xFF00FF00) else Color(0xFFE2E8F0),
                                        mono = true
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeAppIcon("Corvix", Icons.Rounded.MusicNote, active = true) { viewModel.navigate(AppScreen.LIBRARY) }
                HomeAppIcon("Profile", Icons.Rounded.Person, active = false) { viewModel.navigate(AppScreen.PROFILE) }
                HomeAppIcon("Settings", Icons.Rounded.Settings, active = false) { viewModel.navigate(AppScreen.SETTINGS) }
                HomeAppIcon("Clock", Icons.Rounded.Schedule, active = false) { viewModel.openSheet(SheetType.SLEEP) }
            }
        }
    }
}

@Composable
private fun HomeAppIcon(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val iconBoxSize = (48f * metrics.scale).coerceIn(40f, 54f).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (active) {
            GlassPill(onClick = onClick, active = true, shape = RoundedCornerShape(16.dp), padding = PaddingValues(0.dp), modifier = Modifier.size(iconBoxSize)) {
                Icon(icon, label, tint = palette.activePillContent, modifier = Modifier.size(24.dp))
            }
        } else {
            GlassSurface(cornerRadius = 16.dp, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(iconBoxSize).clickable { onClick() }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = palette.textPrimary.copy(alpha = 0.72f), modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(label, style = font.style(11.5f, FontWeight.Normal, palette.textPrimary.copy(alpha = if (active) 1f else 0.72f)))
    }
}
