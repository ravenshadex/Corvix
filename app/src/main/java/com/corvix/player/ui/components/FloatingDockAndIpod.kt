package com.corvix.player.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.Track
import androidx.compose.foundation.layout.widthIn
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.ui.theme.liquidGlassLens
import com.corvix.player.ui.theme.liquidGlassSurface
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

private data class DockTab(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun FloatingGlassBottomDock(
    currentScreen: AppScreen,
    currentTrack: Track,
    isPlaying: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    modifier: Modifier = Modifier,
    elapsedSecFlow: StateFlow<Int>? = null
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val elapsedSec = elapsedSecFlow?.collectAsState()?.value ?: 0
    val progressFrac = (elapsedSec.toFloat() / currentTrack.durationSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val tabs = remember {
        listOf(
            DockTab(AppScreen.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
            DockTab(AppScreen.PLAYLISTS, "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
            DockTab(AppScreen.PROFILE, "Profile", Icons.Rounded.Person),
            DockTab(AppScreen.SETTINGS, "Settings", Icons.Rounded.Settings)
        )
    }
    val activeIdx = tabs.indexOfFirst { it.screen == currentScreen }.let { if (it >= 0) it else 0 }
    val frostBoost = ((palette.blurDp.value - 10f) / 38f).coerceIn(0f, 1f)

    val dockBackdrop = Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = metrics.dockMaxWidth)
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (metrics.isShortHeight) 4.dp else 8.dp,
                bottom = if (metrics.isShortHeight) 16.dp else 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Floating 24dp Liquid Glass Mini-Player Capsule with integrated bottom progress bar
        if (currentTrack.id != 0L) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (palette.isDark) Color(0xF00B0E14) else Color(0xF5F4F7FC),
                    shape = RoundedCornerShape(24.dp)
                )
                .liquidGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    isDark = palette.isDark,
                    isAmoled = palette.isAmoled,
                    strong = true,
                    frostBoost = frostBoost,
                    elevation = 16.dp,
                    brightRim = true,
                    preset = palette.preset,
                    accent = palette.accent,
                    accentSecondary = palette.accentSecondary
                )
                .liquidGlassLens(cornerRadius = 24.dp)
                .clickable { onNavigate(AppScreen.NOW_PLAYING) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArtworkBox(
                        artStyle = currentTrack.artStyle,
                        track = currentTrack,
                        cornerRadius = 12.dp,
                        modifier = Modifier.size(metrics.artThumbSize)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            style = font.style(14f, FontWeight.Bold, palette.textPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentTrack.artist} · ${currentTrack.shortFormat}",
                            style = font.style(12f, FontWeight.Normal, palette.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (palette.isDark) Color.White.copy(alpha = 0.16f)
                                    else Color(0xFF0F172A).copy(alpha = 0.14f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFrac)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        if (palette.isDark) Color.White.copy(alpha = 0.90f)
                                        else Color(0xFF0F172A)
                                    )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = palette.textPrimary,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onTogglePlay() }
                            .padding(3.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Next track",
                        tint = palette.textPrimary,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onNextTrack() }
                            .padding(3.dp)
                    )
                }
            }
        }
        }

        // Translucent 64dp 4-Tab Liquid Glass Dock with Glossy Sliding Lens Pill
        val dockHeight = (64f * metrics.scale).coerceIn(58f, 68f).dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(dockHeight)
                .background(
                    color = if (palette.isDark) Color(0xF00B0E14) else Color(0xF5F4F7FC),
                    shape = CircleShape
                )
                .liquidGlassSurface(
                    shape = CircleShape,
                    isDark = palette.isDark,
                    isAmoled = palette.isAmoled,
                    strong = true,
                    frostBoost = frostBoost,
                    elevation = 16.dp,
                    brightRim = true,
                    preset = palette.preset,
                    accent = palette.accent,
                    accentSecondary = palette.accentSecondary
                )
                .liquidGlassLens(cornerRadius = 999.dp)
                .padding(5.dp)
        ) {
            val tabWidth = maxWidth / 4f
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * activeIdx,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                label = "dock_lens"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (palette.isDark) 0.24f else 0.96f),
                                Color.White.copy(alpha = if (palette.isDark) 0.12f else 0.80f)
                            )
                        ),
                        CircleShape
                    )
                    .border(
                        1.1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.16f))
                        ),
                        CircleShape
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val selected = tab.screen == currentScreen
                    val tint = if (selected) {
                        if (palette.isDark) Color.White else Color(0xFF0F172A)
                    } else {
                        if (palette.isDark) Color(0xFFCBD5E1) else Color(0xA60F172A)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(tab.screen) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            softWrap = false,
                            style = font.style(
                                sizeSp = 10.5f,
                                weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = tint
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassicIpodWheel(
    wheelDegrees: Float,
    onStepWheel: (delta: Int, degreesDelta: Float) -> Unit,
    onMenuClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val wheelDp = metrics.ipodWheelSize
    val centerDp = wheelDp * 0.353f
    var wheelSizePx by remember { mutableStateOf(1f) }
    var lastAngle by remember { mutableStateOf(0f) }
    var accum by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(wheelDp)
                .onSizeChanged { wheelSizePx = it.width.toFloat().coerceAtLeast(1f) }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFF6F7F9), Color(0xFFEBECEE))
                    ),
                    CircleShape
                )
                .border(1.dp, Color(0xFFB6BFC9), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val c = wheelSizePx / 2f
                            lastAngle = (atan2(start.y - c, start.x - c) * 180.0 / PI).toFloat()
                            accum = 0f
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val c = wheelSizePx / 2f
                            val angle = (atan2(change.position.y - c, change.position.x - c) * 180.0 / PI).toFloat()
                            var delta = angle - lastAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            lastAngle = angle
                            accum += delta
                            while (accum >= 18f) {
                                onStepWheel(1, 18f)
                                accum -= 18f
                            }
                            while (accum <= -18f) {
                                onStepWheel(-1, -18f)
                                accum += 18f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Rotating subtle ring texture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((wheelDp.value * 0.05f).dp)
                    .rotate(wheelDegrees)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x180F172A), CircleShape)
            )

            Text(
                text = "MENU",
                style = font.style(14f, FontWeight.Bold, Color(0xFF475569)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (wheelDp.value * 0.07f).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMenuClick() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Icon(
                imageVector = Icons.Rounded.FastRewind,
                contentDescription = "Previous",
                tint = Color(0xFF475569),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (wheelDp.value * 0.075f).dp)
                    .size((wheelDp.value * 0.115f).coerceIn(24f, 34f).dp)
                    .clip(CircleShape)
                    .clickable { onPrevClick() }
            )

            Icon(
                imageVector = Icons.Rounded.FastForward,
                contentDescription = "Next",
                tint = Color(0xFF475569),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = (wheelDp.value * 0.075f).dp)
                    .size((wheelDp.value * 0.115f).coerceIn(24f, 34f).dp)
                    .clip(CircleShape)
                    .clickable { onNextClick() }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (wheelDp.value * 0.065f).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onPlayPauseClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF475569), modifier = Modifier.size((wheelDp.value * 0.085f).coerceIn(18f, 26f).dp))
                Icon(Icons.Rounded.Pause, null, tint = Color(0xFF475569), modifier = Modifier.size((wheelDp.value * 0.08f).coerceIn(17f, 24f).dp))
            }

            // Center Select Button
            Box(
                modifier = Modifier
                    .size(centerDp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                        ),
                        CircleShape
                    )
                    .border(1.dp, Color(0xFF94A3B8), CircleShape)
                    .clickable { onCenterClick() }
            )
        }
    }
}
