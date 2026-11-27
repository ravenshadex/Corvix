package com.corvix.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.SheetType
import com.corvix.player.model.Track
import com.corvix.player.model.formatSeconds
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class IpodListEntry(
    val title: String,
    val trailingValue: String = "",
    val showChevron: Boolean = true,
    val previewTrack: Track? = null,
    val onClick: () -> Unit
)

enum class IpodRightGraphic {
    MUSIC_NOTE,
    GEAR,
    TRACK_ART
}

/**
 * Brushed silver 7th-Gen iPod LCD header bar (no simulated time or battery telemetry).
 */
@Composable
fun IpodTopLcdHeader(
    title: String,
    isPlaying: Boolean,
    centeredTitle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val font = LocalCorvixFont.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF6F8FA), Color(0xFFDDE2E9), Color(0xFFC5CCD6))
                    )
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = font.style(13.5f, FontWeight.Bold, Color(0xFF111827)),
                modifier = if (centeredTitle) Modifier.align(Alignment.Center) else Modifier.align(Alignment.CenterStart)
            )
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = null,
                tint = Color(0xFF1D6CE0),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF94A3B8))
        )
    }
}

/**
 * Authentic iPod Classic 7th Gen Split-Screen Layout:
 * Left 52% crisp white scrollable list with glossy blue selection highlight + scrollbar.
 * Right 48% dark metallic slate panel with 3D Music Note, 3D Gear, or reflected Album Art.
 */
@Composable
fun IpodSplitScreenScaffold(
    headerTitle: String,
    isPlaying: Boolean,
    entries: List<IpodListEntry>,
    focusedIndex: Int,
    rightGraphic: IpodRightGraphic,
    fallbackTrack: Track,
    rightCaptionTitle: String? = null,
    rightCaptionSub: String? = null,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val font = LocalCorvixFont.current
    val safeFocus = if (entries.isEmpty()) 0 else focusedIndex.coerceIn(0, entries.lastIndex)
    val previewTrack = entries.getOrNull(safeFocus)?.previewTrack ?: fallbackTrack

    LaunchedEffect(safeFocus, entries.size) {
        if (entries.isNotEmpty() && safeFocus in entries.indices) {
            listState.animateScrollToItem(safeFocus)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        IpodTopLcdHeader(title = headerTitle, isPlaying = isPlaying, centeredTitle = false)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left white pane + thin right scrollbar
            Box(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items",
                            style = font.style(13f, FontWeight.Medium, Color(0xFF64748B))
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 9.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(entries) { idx, entry ->
                            val selected = idx == safeFocus
                            IpodSplitListRow(
                                entry = entry,
                                selected = selected
                            )
                        }
                    }

                    // Recessed classic vertical scrollbar indicator
                    val total = entries.size.coerceAtLeast(1)
                    val firstVisible = listState.firstVisibleItemIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
                    val thumbFraction = (7f / total.toFloat()).coerceIn(0.18f, 0.85f)
                    val topOffsetFraction = if (total > 1) {
                        (firstVisible.toFloat() / (total - 1).toFloat()) * (1f - thumbFraction)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(9.dp)
                            .background(Color(0xFFE5E7EB))
                            .border(0.5.dp, Color(0xFFCBD5E1))
                            .padding(horizontal = 1.5.dp, vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (topOffsetFraction > 0.005f) {
                                Spacer(modifier = Modifier.weight(topOffsetFraction))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(thumbFraction)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                        )
                                    )
                            )
                            val bottomRemain = (1f - thumbFraction - topOffsetFraction).coerceAtLeast(0f)
                            if (bottomRemain > 0.005f) {
                                Spacer(modifier = Modifier.weight(bottomRemain))
                            }
                        }
                    }
                }
            }

            // Right 48% dark glossy metallic showcase panel
            IpodMetallicRightPane(
                rightGraphic = rightGraphic,
                track = previewTrack,
                captionTitle = rightCaptionTitle,
                captionSub = rightCaptionSub,
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun IpodSplitListRow(
    entry: IpodListEntry,
    selected: Boolean
) {
    val font = LocalCorvixFont.current
    val bgBrush = if (selected) {
        Brush.verticalGradient(listOf(Color(0xFF56ACFB), Color(0xFF1A6EE0), Color(0xFF1057C4)))
    } else {
        Brush.verticalGradient(listOf(Color.White, Color.White))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgBrush)
            .clickable { entry.onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = font.style(
                sizeSp = 14.5f,
                weight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else Color(0xFF0F172A)
            ),
            modifier = Modifier.weight(1f).padding(end = 6.dp)
        )
        if (entry.trailingValue.isNotEmpty()) {
            Text(
                text = entry.trailingValue,
                maxLines = 1,
                style = font.style(
                    sizeSp = 13f,
                    weight = FontWeight.Medium,
                    color = if (selected) Color.White.copy(alpha = 0.92f) else Color(0xFF475569)
                )
            )
        } else if (entry.showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IpodMetallicRightPane(
    rightGraphic: IpodRightGraphic,
    track: Track,
    captionTitle: String?,
    captionSub: String?,
    modifier: Modifier = Modifier
) {
    val font = LocalCorvixFont.current
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF566374), Color(0xFF2B3442), Color(0xFF141A22))
                )
            )
    ) {
        // Subtle glossy diagonal sheen across upper right showcase pane
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (rightGraphic) {
                IpodRightGraphic.MUSIC_NOTE -> {
                    Ipod3DMusicNoteGraphic(modifier = Modifier.size(120.dp))
                }
                IpodRightGraphic.GEAR -> {
                    Ipod3DGearGraphic(modifier = Modifier.size(124.dp))
                }
                IpodRightGraphic.TRACK_ART -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                rotationY = -10f
                                cameraDistance = 12f * density
                            }
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(4.dp))
                    ) {
                        ArtworkBox(
                            artStyle = track.artStyle,
                            track = track,
                            highRes = true,
                            cornerRadius = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Smoothly faded floor reflection underneath artwork (zero sharp bottom cutoff)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(34.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                rotationY = -10f
                                cameraDistance = 12f * density
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.42f),
                                        0.55f to Color.Black.copy(alpha = 0.14f),
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleY = -1f }
                        ) {
                            ArtworkBox(
                                artStyle = track.artStyle,
                                track = track,
                                highRes = false,
                                cornerRadius = 2.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (!captionTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = captionTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = font.style(13f, FontWeight.Bold, Color.White)
                        )
                    }
                    if (!captionSub.isNullOrBlank()) {
                        Text(
                            text = captionSub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = font.style(11.5f, FontWeight.Normal, Color(0xFFCBD5E1))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Ipod3DMusicNoteGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val noteBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        // Top tilted beam connecting two stems
        val beamPath = Path().apply {
            moveTo(w * 0.34f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.11f)
            lineTo(w * 0.80f, h * 0.25f)
            lineTo(w * 0.34f, h * 0.34f)
            close()
        }
        drawPath(beamPath, brush = noteBrush)
        // Left & right stems
        drawLine(
            brush = noteBrush,
            start = Offset(w * 0.37f, h * 0.26f),
            end = Offset(w * 0.37f, h * 0.68f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = noteBrush,
            start = Offset(w * 0.76f, h * 0.17f),
            end = Offset(w * 0.76f, h * 0.60f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        // Two noteheads
        drawOval(
            brush = noteBrush,
            topLeft = Offset(w * 0.14f, h * 0.58f),
            size = androidx.compose.ui.geometry.Size(w * 0.27f, h * 0.20f)
        )
        drawOval(
            brush = noteBrush,
            topLeft = Offset(w * 0.53f, h * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.27f, h * 0.20f)
        )
        // Gloss floor reflection under noteheads
        drawOval(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(w * 0.14f, h * 0.80f),
            size = androidx.compose.ui.geometry.Size(w * 0.27f, h * 0.10f)
        )
        drawOval(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(w * 0.53f, h * 0.74f),
            size = androidx.compose.ui.geometry.Size(w * 0.27f, h * 0.10f)
        )
    }
}

@Composable
private fun Ipod3DGearGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension * 0.40f
        val innerR = size.minDimension * 0.25f
        val gearBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF8896A6)),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        val teeth = 16
        for (i in 0 until teeth) {
            val angleRad = (i * 360.0 / teeth) * PI / 180.0
            val p1 = Offset(cx + cos(angleRad).toFloat() * innerR, cy + sin(angleRad).toFloat() * innerR)
            val p2 = Offset(cx + cos(angleRad).toFloat() * (outerR + size.minDimension * 0.07f), cy + sin(angleRad).toFloat() * (outerR + size.minDimension * 0.07f))
            drawLine(brush = gearBrush, start = p1, end = p2, strokeWidth = size.minDimension * 0.065f, cap = StrokeCap.Butt)
        }
        drawCircle(brush = gearBrush, radius = outerR * 0.88f, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF27303E), radius = innerR * 0.96f, center = Offset(cx, cy))
        for (i in 0 until 3) {
            val spokeAngle = (i * 120.0 - 90.0) * PI / 180.0
            drawLine(
                brush = gearBrush,
                start = Offset(cx, cy),
                end = Offset(cx + cos(spokeAngle).toFloat() * innerR, cy + sin(spokeAngle).toFloat() * innerR),
                strokeWidth = size.minDimension * 0.08f
            )
        }
        drawCircle(brush = gearBrush, radius = innerR * 0.34f, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF27303E), radius = innerR * 0.16f, center = Offset(cx, cy))
        drawCircle(
            color = Color.White.copy(alpha = 0.20f),
            radius = outerR * 0.90f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * Authentic iPod Classic 7th Gen Now Playing screen matching user reference image 2:
 * Silver LCD header, "X of N" track position, 3D tilted album art with mirror reflection on left,
 * bold title/artist/album on right, glossy blue pill progress bar with elapsed & -remaining times.
 */
@Composable
fun IpodSeventhGenNowPlaying(
    state: CorvixUiState,
    elapsedSec: Int,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val font = LocalCorvixFont.current
    val track = state.currentTrack
    val dur = track.durationSec.coerceAtLeast(1)
    val clampedElapsed = elapsedSec.coerceIn(0, dur)
    val remaining = (dur - clampedElapsed).coerceAtLeast(0)
    val progressFrac = (clampedElapsed.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    val trackPosition = (state.currentTrackIndex + 1).coerceAtLeast(1)
    val trackTotal = state.tracks.size.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFCFDFF), Color(0xFFF3F6FA), Color(0xFFE7ECF2))
                )
            )
    ) {
        IpodTopLcdHeader(
            title = "Now Playing",
            isPlaying = state.isPlaying,
            centeredTitle = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top micro info bar: "1 of 12" + Shuffle glyph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$trackPosition of $trackTotal",
                    style = font.style(12.5f, FontWeight.Bold, Color(0xFF334155))
                )
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.isShuffle) Color(0xFF1D6CE0) else Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { viewModel.toggleShuffle() }
                )
            }

            // Center hero row: 3D tilted album art with mirror reflection + Title/Artist/Album
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.46f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.lyricsDrawerOpen) {
                        val lyrics = track.lyrics
                        val lyricListState = rememberLazyListState()
                        val activeIdx = lyrics?.indexOfLast { clampedElapsed >= it.sec }?.coerceAtLeast(0) ?: 0
                        LaunchedEffect(activeIdx, lyrics?.size) {
                            if (!lyrics.isNullOrEmpty() && activeIdx in lyrics.indices) {
                                lyricListState.animateScrollToItem(activeIdx)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.95f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LYRICS", style = font.style(10.5f, FontWeight.Bold, Color(0xFF1D6CE0)))
                                Text(
                                    text = "Art",
                                    style = font.style(10.5f, FontWeight.SemiBold, Color(0xFF475569)),
                                    modifier = Modifier.clickable { viewModel.toggleLyricsDrawer() }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!lyrics.isNullOrEmpty()) {
                                LazyColumn(
                                    state = lyricListState,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    itemsIndexed(lyrics) { idx, line ->
                                        Text(
                                            text = line.text,
                                            style = font.style(
                                                sizeSp = if (idx == activeIdx) 12f else 11f,
                                                weight = if (idx == activeIdx) FontWeight.Bold else FontWeight.Normal,
                                                color = if (idx == activeIdx) Color(0xFF1D6CE0) else Color(0xFF475569)
                                            ),
                                            modifier = Modifier.clickable { viewModel.seekTo(line.sec) }
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (state.isFetchingLyrics) "Fetching..." else "No lyrics yet",
                                        style = font.style(11.5f, FontWeight.Medium, Color(0xFF64748B))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Fetch Lyrics",
                                        style = font.style(11.5f, FontWeight.Bold, Color.White),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1D6CE0))
                                            .clickable { viewModel.fetchLyricsFromSite("LRCLIB (Synced .lrc)") }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    rotationY = 14f
                                    cameraDistance = 12f * density
                                }
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                        ) {
                            ArtworkBox(
                                artStyle = track.artStyle,
                                track = track,
                                highRes = true,
                                showFallbackDisc = true,
                                cornerRadius = 4.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Smoothly faded cover reflection (zero sharp bottom edge cutoff)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    rotationY = 14f
                                    cameraDistance = 12f * density
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.0f to Color.Black.copy(alpha = 0.42f),
                                            0.55f to Color.Black.copy(alpha = 0.14f),
                                            1.0f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { scaleY = -1f }
                            ) {
                                ArtworkBox(
                                    artStyle = track.artStyle,
                                    track = track,
                                    highRes = false,
                                    cornerRadius = 2.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.54f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = font.style(18.5f, FontWeight.Bold, Color(0xFF0F172A))
                    )
                    Text(
                        text = track.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = font.style(14.5f, FontWeight.SemiBold, Color(0xFF334155))
                    )
                    Text(
                        text = track.album,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = font.style(13.5f, FontWeight.Normal, Color(0xFF475569))
                    )
                }
            }

            // Bottom progress scrubber + Shuffle / Repeat + optional Lyrics/Options row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                var barWidthPx by remember { mutableStateOf(1f) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatSeconds(clampedElapsed),
                        style = font.style(12.5f, FontWeight.Bold, Color(0xFF1E293B), mono = true)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFD5DCE4), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
                                )
                            )
                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(3.dp))
                            .pointerInput(dur) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                    viewModel.seekTo((frac * dur).roundToInt())
                                }
                            }
                            .pointerInput(dur) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                    viewModel.seekTo((frac * dur).roundToInt())
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFrac.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF81C7FF), Color(0xFF338CF0), Color(0xFF1A65CC))
                                    )
                                )
                        )
                    }
                    Text(
                        text = "-${formatSeconds(remaining)}",
                        style = font.style(12.5f, FontWeight.Bold, Color(0xFF1E293B), mono = true)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.toggleShuffle() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.isShuffle) Color(0xFF1D6CE0) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = Color(0xFF475569),
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { viewModel.openLyricsSheet(false) }
                        )
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Options",
                            tint = Color(0xFF475569),
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { viewModel.openSheet(SheetType.NP_MENU) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.cycleRepeat() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) Color(0xFF1D6CE0) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
