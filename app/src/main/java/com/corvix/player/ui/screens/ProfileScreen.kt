package com.corvix.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corvix.player.model.ArtistRecapEntry
import com.corvix.player.model.RecapStats
import com.corvix.player.model.Track
import com.corvix.player.ui.components.ArtworkBox
import com.corvix.player.ui.components.GlassPill
import com.corvix.player.ui.components.GlassSurface
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current

    val recap = state.recapStats
    val trackById = remember(state.tracks) { state.tracks.associateBy { it.id } }
    var selectedRing by remember { mutableStateOf(-1) }
    var carouselPage by remember { mutableStateOf(0) }

    val safePage = carouselPage.coerceIn(0, (recap.topArtists.size - 1).coerceAtLeast(0))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = metrics.horizontalPadding),
        contentPadding = PaddingValues(bottom = metrics.dockBottomClearance, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Export Pill
        item(key = "header") {
            RecapHeader(
                onExport = { viewModel.shareSongCard(state.currentTrack.id) }
            )
        }

        // 2. Swipeable "Most Played" Artist Carousel
        if (recap.topArtists.isNotEmpty()) {
            item(key = "artist_carousel") {
                Text(
                    "Most Played Artists",
                    style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                ArtistCarousel(
                    artists = recap.topArtists,
                    page = safePage,
                    onPageChange = { carouselPage = it },
                    tracks = state.tracks,
                    playCounts = state.playCounts
                )
            }
        }

        // 3. 2x2 "What It Adds Up To" Metric Grid
        item(key = "metric_grid") {
            Text(
                "What It Adds Up To",
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            MetricGrid(recap)
        }

        // 4. Interactive "How You Listen" Habit Rings
        item(key = "habit_rings") {
            Text(
                "How You Listen",
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            HabitRingsGrid(
                recap = recap,
                selectedRing = selectedRing,
                onRingTap = { idx -> selectedRing = if (selectedRing == idx) -1 else idx }
            )
            if (selectedRing >= 0) {
                Spacer(Modifier.height(8.dp))
                HabitInsightBanner(ringIdx = selectedRing)
            }
        }

        // 5. "Your Hours" 24-Hour Radial Clock & Weekly Bar Chart
        item(key = "hours_section") {
            Text(
                "Your Hours",
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            HoursSection(recap)
        }

        // 6. "What You Listen To" Language & Category Breakdown
        item(key = "language_breakdown") {
            Text(
                "What You Listen To",
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            LanguageBreakdownCard(state, recap) { track ->
                val idx = state.tracks.indexOfFirst { it.id == track.id }
                if (idx >= 0) viewModel.playTrack(idx)
            }
        }

        // 7. Scrollable "Top Songs" List
        if (recap.topTracks.isNotEmpty()) {
            item(key = "top_songs_header") {
                Text(
                    "Top Songs",
                    style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            itemsIndexed(recap.topTracks, key = { _, p -> "top_${p.first}" }) { idx, (id, cnt) ->
                val track = trackById[id] ?: return@itemsIndexed
                TopSongRow(
                    rank = idx + 1,
                    track = track,
                    playCount = cnt,
                    onPlay = {
                        val idxInTracks = state.tracks.indexOfFirst { it.id == track.id }
                        if (idxInTracks >= 0) viewModel.playTrack(idxInTracks)
                    }
                )
            }
        }
    }
}

@Composable
private fun RecapHeader(onExport: () -> Unit) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Your Recap",
                style = font.style(26f, FontWeight.Bold, palette.textPrimary)
            )
            Text(
                "Listening lore · Local Library",
                style = font.style(12f, FontWeight.Normal, palette.textMuted)
            )
        }
        GlassPill(
            onClick = onExport,
            padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = palette.accentText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Export Card",
                    style = font.style(12f, FontWeight.SemiBold, palette.accentText)
                )
            }
        }
    }
}

@Composable
private fun ArtistCarousel(
    artists: List<ArtistRecapEntry>,
    page: Int,
    onPageChange: (Int) -> Unit,
    tracks: List<Track>,
    playCounts: Map<Long, Int>
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val artist = artists.getOrNull(page) ?: return

    val repTrack = remember(artist, tracks, playCounts) {
        tracks.filter { it.artist == artist.artistName }
            .maxByOrNull { playCounts[it.id] ?: 0 }
    }

    var dragDelta by remember { mutableStateOf(0f) }

    GlassSurface(
        strong = false,
        cornerRadius = palette.cornerCard,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .pointerInput(artists.size, page) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragDelta < -40f && page < artists.size - 1) onPageChange(page + 1)
                            if (dragDelta > 40f && page > 0) onPageChange(page - 1)
                            dragDelta = 0f
                        }
                    ) { _, delta -> dragDelta += delta }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (repTrack != null) {
                    ArtworkBox(
                        artStyle = repTrack.artStyle,
                        track = repTrack,
                        highRes = true,
                        cornerRadius = palette.cornerCard,
                        showBorder = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x33000000), Color(0xEB0A0A0F))
                            )
                        )
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { if (page > 0) onPageChange(page - 1) }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { if (page < artists.size - 1) onPageChange(page + 1) }
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        artist.artistName,
                        style = font.style(20f, FontWeight.Black, Color.White).copy(fontStyle = FontStyle.Italic),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${artist.playCount} plays · ${artist.totalMinutes} min",
                        style = font.style(12f, FontWeight.Medium, Color.White.copy(alpha = 0.82f))
                    )
                    if (artist.topTrackTitle.isNotBlank()) {
                        Text(
                            "On repeat: ${artist.topTrackTitle}",
                            style = font.style(11f, FontWeight.SemiBold, palette.accentSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(artists.size) { i ->
                        Box(
                            modifier = Modifier
                                .width(if (i == page) 14.dp else 6.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(if (i == page) palette.accent else Color.White.copy(alpha = 0.42f))
                        )
                    }
                }
            }
        }
}

@Composable
private fun MetricGrid(recap: RecapStats) {
    val hours = recap.totalMinutes / 60f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Hours",
                value = if (hours >= 100f) hours.toInt().toString() else "%.1f".format(hours),
                icon = { SparklineChart(recap.hourlyPlays) }
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Plays",
                value = recap.totalPlays.toString(),
                icon = { MiniEqBars() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Artists",
                value = recap.uniqueArtists.toString(),
                icon = { DotMatrixMeter(recap.uniqueArtists.coerceAtMost(100)) }
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Streak",
                value = "${recap.streakDays}d",
                icon = { StreakTicks(recap.streakDays.coerceAtMost(14)) }
            )
        }
    }
}

@Composable
private fun MetricTile(modifier: Modifier, label: String, value: String, icon: @Composable () -> Unit) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    GlassSurface(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                value,
                style = font.style(28f, FontWeight.Black, palette.textPrimary),
                maxLines = 1
            )
            icon()
            Text(
                label,
                style = font.style(11f, FontWeight.SemiBold, palette.textMuted)
            )
        }
    }
}

@Composable
private fun SparklineChart(values: List<Int>) {
    val palette = LocalCorvixPalette.current
    val accent = palette.accent
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * step, size.height - (v / max) * size.height)
        }
        if (pts.size < 2) return@Canvas
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val cx = (pts[i - 1].x + pts[i].x) / 2f
                cubicTo(cx, pts[i - 1].y, cx, pts[i].y, pts[i].x, pts[i].y)
            }
        }
        drawPath(path, accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val fill = Path().apply {
            addPath(path)
            lineTo(pts.last().x, size.height)
            lineTo(pts.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent)))
    }
}

@Composable
private fun MiniEqBars() {
    val palette = LocalCorvixPalette.current
    val accent = palette.accent
    val inf = rememberInfiniteTransition(label = "profile_eq")
    val h1 by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "h1")
    val h2 by inf.animateFloat(0.7f, 0.3f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "h2")
    val h3 by inf.animateFloat(0.5f, 0.9f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "h3")
    val h4 by inf.animateFloat(0.6f, 0.2f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "h4")
    val h5 by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "h5")
    val h6 by inf.animateFloat(0.8f, 0.4f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "h6")
    val h7 by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "h7")
    val heights = listOf(h1, h2, h3, h4, h5, h6, h7)
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * 6) / 7f
        heights.forEachIndexed { i, h ->
            val barH = h * size.height
            drawRoundRect(
                color = accent,
                topLeft = Offset(i * (barW + gap), size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

@Composable
private fun DotMatrixMeter(count: Int) {
    val palette = LocalCorvixPalette.current
    val accent = palette.accent
    val track = palette.sliderTrack
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val dots = 16
        val filled = (count * dots / 60f).toInt().coerceIn(1, dots)
        val r = 3.dp.toPx()
        val spacing = (size.width - r * 2 * dots) / (dots - 1).coerceAtLeast(1)
        val cy = size.height / 2f
        for (i in 0 until dots) {
            val cx = r + i * (r * 2 + spacing)
            drawCircle(if (i < filled) accent else track, radius = r, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun StreakTicks(days: Int) {
    val palette = LocalCorvixPalette.current
    val accent = palette.accent
    val track = palette.sliderTrack
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val barCount = 14
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val active = i < days
            val h = if (active) size.height * 0.78f else size.height * 0.34f
            drawRoundRect(
                color = if (active) accent else track,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

private data class RingDef(val label: String, val pct: Float, val insight: String)

@Composable
private fun HabitRingsGrid(
    recap: RecapStats,
    selectedRing: Int,
    onRingTap: (Int) -> Unit
) {
    val rings = remember(recap) {
        listOf(
            RingDef("On Repeat", recap.repeatRate, "You replay favourites more than 2× before moving on."),
            RingDef("Loyalty", recap.loyaltyRate, "Sessions where you stick to one artist all the way through."),
            RingDef("After Dark", recap.afterDarkRate, "Tracks played between 10 PM and 2 AM."),
            RingDef("Discovery", recap.discoveryRate, "Songs you've played 3 times or fewer — fresh picks!")
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0 to 1, 2 to 3).forEach { (a, b) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitRingCard(rings[a], a, selectedRing, onRingTap, Modifier.weight(1f))
                HabitRingCard(rings[b], b, selectedRing, onRingTap, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HabitRingCard(
    ring: RingDef,
    idx: Int,
    selected: Int,
    onTap: (Int) -> Unit,
    modifier: Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val isActive = selected == idx
    val animPct by animateFloatAsState(ring.pct.coerceIn(0f, 1f), tween(900), label = "ring_$idx")
    val trackColor = palette.sliderTrack
    val activeColor = palette.accent

    GlassSurface(
        strong = isActive,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(12.dp),
        modifier = modifier
            .then(
                if (isActive) Modifier.border(1.5.dp, palette.accent.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap(idx) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                    drawArc(activeColor, startAngle = -90f, sweepAngle = animPct * 360f, useCenter = false, style = stroke)
                }
                Text(
                    "${(ring.pct * 100).toInt()}%",
                    style = font.style(13f, FontWeight.Bold, palette.textPrimary)
                )
            }
            Text(
                ring.label,
                style = font.style(11f, FontWeight.SemiBold, if (isActive) palette.accentText else palette.textMuted),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HabitInsightBanner(ringIdx: Int) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val insights = listOf(
        "You replay favourites more than 2× before moving on.",
        "Sessions where you stick to one artist all the way through.",
        "Tracks played between 10 PM and 2 AM.",
        "Songs you've played 3 times or fewer — fresh picks!"
    )
    GlassSurface(
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lightbulb, null, tint = palette.accentText, modifier = Modifier.size(16.dp).padding(top = 1.dp))
            Text(
                insights.getOrElse(ringIdx) { "" },
                style = font.style(12f, FontWeight.Normal, palette.textPrimary)
            )
        }
    }
}

@Composable
private fun HoursSection(recap: RecapStats) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var hoveredSpoke by remember { mutableStateOf(-1) }
    GlassSurface(
        cornerRadius = palette.cornerCard,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RadialClockDial(
                hourlyPlays = recap.hourlyPlays,
                hoveredSpoke = hoveredSpoke,
                onSpokeHover = { hoveredSpoke = it },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
            if (hoveredSpoke >= 0) {
                val hour = hoveredSpoke
                val label = when (hour) {
                    0 -> "Midnight"
                    12 -> "Noon"
                    else -> if (hour < 12) "$hour AM" else "${hour - 12} PM"
                }
                Text(
                    "$label — ${recap.hourlyPlays.getOrElse(hour) { 0 }} plays",
                    style = font.style(12f, FontWeight.SemiBold, palette.accentText),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            WeeklyBarChart(recap.weeklyPlays)
        }
    }
}

@Composable
private fun RadialClockDial(
    hourlyPlays: List<Int>,
    hoveredSpoke: Int,
    onSpokeHover: (Int) -> Unit,
    modifier: Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val max = (hourlyPlays.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val accent = palette.accent
    val highlight = palette.accentSecondary
    val track = palette.sliderTrack
    val ringColor = palette.divider

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val angle = atan2(pos.y - cy, pos.x - cx) * 180.0 / PI + 90.0
                        val spoke = ((angle.toFloat() + 360f) % 360f / 15f).toInt() % 24
                        onSpokeHover(spoke)
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = minOf(cx, cy) * 0.80f
            val innerR = outerR * 0.38f
            val spokeW = 5.5.dp.toPx()

            drawCircle(ringColor, radius = outerR, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
            drawCircle(track.copy(alpha = 0.25f), radius = innerR, center = Offset(cx, cy))

            for (h in 0 until 24) {
                val angleDeg = h * 15f - 90f
                val rad = (angleDeg * PI / 180.0).toFloat()
                val frac = hourlyPlays.getOrElse(h) { 0 } / max
                val spokeLen = innerR + (outerR - innerR) * frac.coerceAtLeast(0.10f)
                val isHovered = h == hoveredSpoke
                val color = when {
                    isHovered -> highlight
                    frac > 0.5f -> accent.copy(alpha = 0.92f)
                    frac > 0.2f -> accent.copy(alpha = 0.55f)
                    else -> track
                }

                drawLine(
                    color = color,
                    start = Offset(cx + innerR * cos(rad), cy + innerR * sin(rad)),
                    end = Offset(cx + spokeLen * cos(rad), cy + spokeLen * sin(rad)),
                    strokeWidth = if (isHovered) spokeW * 1.45f else spokeW,
                    cap = StrokeCap.Round
                )
            }
        }

        listOf(
            "Midnight" to Alignment.TopCenter,
            "6 AM" to Alignment.CenterEnd,
            "Noon" to Alignment.BottomCenter,
            "6 PM" to Alignment.CenterStart
        ).forEach { (label, align) ->
            Text(
                label,
                style = font.style(9.5f, FontWeight.Medium, palette.textMuted),
                modifier = Modifier.align(align).padding(2.dp)
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(weekly: List<Int>) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxVal = (weekly.maxOrNull() ?: 1).coerceAtLeast(1)
    val max = maxVal.toFloat()
    val peak = weekly.indexOfFirst { it == maxVal }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { i, label ->
            val frac = weekly.getOrElse(i) { 0 } / max
            val isPeak = i == peak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(60.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(frac.coerceAtLeast(0.08f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(if (isPeak) palette.accent else palette.sliderTrack)
                    )
                }
                Text(
                    label,
                    style = font.style(
                        10f,
                        if (isPeak) FontWeight.Bold else FontWeight.Medium,
                        if (isPeak) palette.accentText else palette.textMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun LanguageBreakdownCard(
    state: CorvixUiState,
    recap: RecapStats,
    onPlayTrack: (Track) -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    val categoryData = remember(state.tracks) {
        val total = state.tracks.size.coerceAtLeast(1).toFloat()
        val soundtrackList = state.tracks.filter {
            it.album.contains("OST", ignoreCase = true) ||
                it.album.contains("Soundtrack", ignoreCase = true) ||
                it.genre.contains("Soundtrack", ignoreCase = true) ||
                it.genre.contains("Score", ignoreCase = true)
        }
        val soundtrackIds = soundtrackList.map { it.id }.toSet()
        val instrumentalList = state.tracks.filter {
            it.id !in soundtrackIds && (
                it.genre.contains("Instrumental", ignoreCase = true) ||
                    it.genre.contains("Ambient", ignoreCase = true) ||
                    it.title.contains("Instrumental", ignoreCase = true)
                )
        }
        val instrumentalIds = instrumentalList.map { it.id }.toSet()
        val studioList = state.tracks.filter { it.id !in soundtrackIds && it.id !in instrumentalIds }
        listOf(
            Triple("Studio Originals", studioList.size / total, studioList),
            Triple("Film & Soundtrack", soundtrackList.size / total, soundtrackList),
            Triple("Instrumental", instrumentalList.size / total, instrumentalList)
        ).filter { it.third.isNotEmpty() }
    }

    GlassSurface(
        cornerRadius = palette.cornerCard,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (categoryData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categories", style = font.style(13f, FontWeight.SemiBold, palette.textPrimary))
                    Text("Tap to inspect", style = font.style(10.5f, FontWeight.Normal, palette.textMuted))
                }
                categoryData.forEach { (cat, frac, catTracks) ->
                    val isExpanded = expandedCategory == cat
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { expandedCategory = if (isExpanded) null else cat }
                                .padding(vertical = 3.dp)
                        ) {
                            ProgressRow("$cat (${catTracks.size})", frac)
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (palette.isDark) Color.White.copy(alpha = 0.05f)
                                        else Color(0xFF0F172A).copy(alpha = 0.04f)
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                catTracks.take(25).forEach { trk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onPlayTrack(trk) }
                                            .padding(horizontal = 4.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ArtworkBox(
                                            artStyle = trk.artStyle,
                                            track = trk,
                                            cornerRadius = 6.dp,
                                            showBorder = false,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = trk.title,
                                                style = font.style(12f, FontWeight.SemiBold, palette.textPrimary),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${trk.artist} · ${trk.genre}",
                                                style = font.style(10.5f, FontWeight.Normal, palette.textMuted),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = trk.durationFormatted,
                                            style = font.style(11f, FontWeight.Normal, palette.textMuted, mono = true)
                                        )
                                    }
                                }
                                if (catTracks.size > 25) {
                                    Text(
                                        text = "+${catTracks.size - 25} more tracks",
                                        style = font.style(10.5f, FontWeight.Medium, palette.textMuted),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Library Summary", style = font.style(13f, FontWeight.SemiBold, palette.textPrimary))
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val avgMin = recap.avgSongLengthSec / 60
                val avgSec = recap.avgSongLengthSec % 60
                SummaryStat("Avg Length", "%d:%02d".format(avgMin, avgSec))
                SummaryStat("Lossless", "${recap.losslessPercent}%")
                SummaryStat("Total Tracks", state.tracks.size.toString())
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, fraction: Float) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val animFrac by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "lang_$label")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            Text(
                "${(fraction * 100).toInt()}%",
                style = font.style(11f, FontWeight.SemiBold, palette.accentText, mono = true)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(palette.sliderTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFrac)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(palette.accent, palette.accentSecondary)),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = font.style(14f, FontWeight.Bold, palette.textPrimary), maxLines = 1)
        Text(label, style = font.style(9.5f, FontWeight.Normal, palette.textMuted), maxLines = 1)
    }
}

@Composable
private fun TopSongRow(rank: Int, track: Track, playCount: Int, onPlay: () -> Unit) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    GlassSurface(
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "%02d".format(rank),
                style = font.style(13f, FontWeight.Bold, palette.accentText, mono = true),
                modifier = Modifier.width(26.dp)
            )
            ArtworkBox(
                artStyle = track.artStyle,
                track = track,
                cornerRadius = 10.dp,
                modifier = Modifier.size(44.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    track.title,
                    style = font.style(13f, FontWeight.SemiBold, palette.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        track.artist,
                        style = font.style(11f, FontWeight.Normal, palette.textMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(track.shortFormat, style = font.style(9f, FontWeight.Bold, palette.accentText))
                    }
                }
            }
            Text(
                "${playCount}×",
                style = font.style(11f, FontWeight.SemiBold, palette.textMuted, mono = true)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.pillBrush)
                    .border(1.dp, palette.pillBorder, CircleShape)
                    .clickable { onPlay() }
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
