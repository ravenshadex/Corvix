package com.corvix.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.SeekBarStyle
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.ui.theme.liquidGlassLens
import com.corvix.player.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CorvixSeekSlider(
    valueFraction: Float,
    onValueFractionChange: (Float) -> Unit,
    seekStyle: SeekBarStyle,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val pct = valueFraction.coerceIn(0f, 1f)
    var widthPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    val latestOnFractionChange by rememberUpdatedState(onValueFractionChange)

    val transition = rememberInfiniteTransition(label = "seek_anim")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "seek_wave"
    )

    val thumbWidthDp = when (seekStyle) {
        SeekBarStyle.MINIMAL -> 28.dp
        SeekBarStyle.GROOVE -> 30.dp
        else -> 32.dp
    }
    val thumbHeightDp = when (seekStyle) {
        SeekBarStyle.MINIMAL -> 17.dp
        SeekBarStyle.GROOVE -> 19.dp
        else -> 20.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    latestOnFractionChange((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    latestOnFractionChange((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (seekStyle == SeekBarStyle.WAVEFORM || seekStyle == SeekBarStyle.BLOCKS) {
            val heights = CorvixSampleData.seekWaveHeights
            val litCount = (pct * heights.size).roundToInt()
            Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                val gap = 2.5.dp.toPx()
                val barW = ((size.width - gap * (heights.size - 1)) / heights.size).coerceAtLeast(2f)
                heights.forEachIndexed { idx, baseFrac ->
                    val animated = if (seekStyle == SeekBarStyle.WAVEFORM && isPlaying) {
                        (baseFrac * (0.86f + 0.14f * sin(wavePhase + idx * 0.45f))).coerceIn(0.22f, 1f)
                    } else if (seekStyle == SeekBarStyle.BLOCKS) {
                        0.58f
                    } else {
                        baseFrac
                    }
                    val bh = size.height * animated
                    val x = idx * (barW + gap)
                    val y = (size.height - bh) / 2f
                    val isLit = idx < litCount
                    drawRoundRect(
                        brush = if (isLit) {
                            Brush.verticalGradient(listOf(palette.accent, palette.accentSecondary))
                        } else {
                            Brush.verticalGradient(listOf(palette.sliderTrack, palette.sliderTrack))
                        },
                        topLeft = Offset(x, y),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        } else {
            val trackHeightDp = when (seekStyle) {
                SeekBarStyle.MINIMAL -> 4.dp
                SeekBarStyle.GROOVE -> 7.dp
                else -> 6.5.dp
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(trackHeightDp)) {
                val r = size.height / 2f
                drawRoundRect(
                    color = palette.sliderTrack,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(r, r)
                )
                if (seekStyle == SeekBarStyle.GROOVE) {
                    var gx = 4f
                    while (gx < size.width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.20f),
                            start = Offset(gx, 1f),
                            end = Offset(gx, size.height - 1f),
                            strokeWidth = 1.5f
                        )
                        gx += 10f
                    }
                }
                val fillW = (size.width * pct).coerceAtLeast(r * 2f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(palette.accent, palette.accentSecondary)),
                    topLeft = Offset.Zero,
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }

            val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
            val thumbCenterPx = (pct * widthPx).coerceIn(thumbWidthPx / 2f, (widthPx - thumbWidthPx / 2f).coerceAtLeast(thumbWidthPx / 2f))
            val thumbX = (thumbCenterPx - thumbWidthPx / 2f).roundToInt()

            LiquidGlassSliderThumb(
                modifier = Modifier
                    .offset { IntOffset(thumbX, 0) }
                    .width(thumbWidthDp)
                    .height(thumbHeightDp)
            )
        }
    }
}

@Composable
fun CorvixHorizontalSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val frac = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    CorvixSeekSlider(
        valueFraction = frac,
        onValueFractionChange = { newFrac ->
            onValueChange(valueRange.start + span * newFrac)
        },
        seekStyle = SeekBarStyle.DROPLET,
        isPlaying = false,
        modifier = modifier
    )
}

@Composable
fun CorvixVerticalEqSlider(
    valueDb: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val metrics = LocalCorvixMetrics.current
    val frac = ((valueDb + 12f) / 24f).coerceIn(0f, 1f)
    var heightPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val trackHeightDp = (108f * metrics.scale).coerceIn(82f, 126f).dp
    val thumbHeightDp = 17.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    Box(
        modifier = modifier
            .width(32.dp)
            .height(trackHeightDp)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFrac = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                    latestOnValueChange((-12 + (newFrac * 24f).roundToInt()).coerceIn(-12, 12))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newFrac = (1f - (change.position.y / heightPx)).coerceIn(0f, 1f)
                    latestOnValueChange((-12 + (newFrac * 24f).roundToInt()).coerceIn(-12, 12))
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.width(6.5.dp).height(trackHeightDp)) {
            val r = size.width / 2f
            drawRoundRect(
                color = palette.sliderTrack,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(r, r)
            )
            val fillH = (size.height * frac).coerceAtLeast(r * 2f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(palette.accent, palette.accentSecondary)),
                topLeft = Offset(0f, size.height - fillH),
                size = Size(size.width, fillH),
                cornerRadius = CornerRadius(r, r)
            )
        }

        val yCenter = (1f - frac) * heightPx
        val clampedTop = (yCenter - thumbHeightPx / 2f)
            .coerceIn(0f, (heightPx - thumbHeightPx).coerceAtLeast(0f))
            .roundToInt()

        LiquidGlassSliderThumb(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, clampedTop) }
                .width(24.dp)
                .height(thumbHeightDp)
        )
    }
}

@Composable
private fun LiquidGlassSliderThumb(modifier: Modifier = Modifier) {
    val palette = LocalCorvixPalette.current
    Box(
        modifier = modifier
            .liquidGlassSurface(
                shape = CircleShape,
                isDark = palette.isDark,
                isAmoled = palette.isAmoled,
                strong = false,
                elevation = 4.dp,
                borderWidth = 0.85.dp,
                brightRim = true,
                preset = palette.preset,
                accent = palette.accent,
                accentSecondary = palette.accentSecondary
            )
            .liquidGlassLens(intensity = 2.0f, cornerRadius = 999.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (palette.isDark) 0.24f else 0.88f),
                        Color.White.copy(alpha = if (palette.isDark) 0.04f else 0.56f),
                        Color.White.copy(alpha = if (palette.isDark) 0.14f else 0.74f)
                    )
                ),
                CircleShape
            )
    )
}
