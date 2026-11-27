package com.corvix.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.VisualizerStyle
import com.corvix.player.ui.theme.LocalCorvixPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CorvixVisualizerCanvas(
    style: VisualizerStyle,
    enabled: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val palette = LocalCorvixPalette.current
    val phase = if (enabled && isPlaying && isVisible) {
        val transition = rememberInfiniteTransition(label = "viz_canvas")
        val animPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
            label = "viz_phase"
        )
        animPhase
    } else {
        0.7f
    }
    val visualAlpha = if (enabled) 1f else 0.35f

    Canvas(modifier = modifier.fillMaxSize().alpha(visualAlpha)) {
        val w = size.width
        val h = size.height
        val cols = CorvixSampleData.visualizerHeights
        val accent = palette.accent

        when (style) {
            VisualizerStyle.BARS -> {
                val sx = w / 240f
                val sy = h / 52f
                cols.forEachIndexed { idx, frac ->
                    val mod = 0.56f + 0.44f * sin(phase * 1.4f + idx * 0.55f)
                    val bh = maxOf(10f, frac * 44f * mod)
                    val bw = 10f * sx
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.88f),
                        topLeft = Offset((8f + idx * 19f) * sx, (48f - bh) * sy),
                        size = Size(bw, bh * sy),
                        cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                    )
                }
            }

            VisualizerStyle.MIRROR -> {
                val sx = w / 240f
                val sy = h / 52f
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(6f * sx, 26f * sy),
                    end = Offset(234f * sx, 26f * sy),
                    strokeWidth = 1.dp.toPx()
                )
                cols.forEachIndexed { idx, frac ->
                    val mod = 0.58f + 0.42f * sin(phase * 1.3f + idx * 0.5f)
                    val bh = maxOf(8f, frac * 42f * mod)
                    val bw = 9f * sx
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.90f),
                        topLeft = Offset((9f + idx * 19f) * sx, (26f - bh / 2f) * sy),
                        size = Size(bw, bh * sy),
                        cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                    )
                }
            }

            VisualizerStyle.WAVE -> {
                val glowPath = Path()
                val corePath = Path()
                val steps = 48
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val ampGlow = h * 0.34f * sin(t * 4f * PI.toFloat() + phase * 1.2f)
                    val ampCore = h * 0.22f * sin(t * 4f * PI.toFloat() + phase * 1.2f)
                    val yg = h * 0.5f + ampGlow
                    val yc = h * 0.5f + ampCore
                    if (i == 0) {
                        glowPath.moveTo(x, yg)
                        corePath.moveTo(x, yc)
                    } else {
                        glowPath.lineTo(x, yg)
                        corePath.lineTo(x, yc)
                    }
                }
                drawPath(glowPath, color = accent.copy(alpha = 0.65f), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(corePath, color = Color.White, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }

            VisualizerStyle.RING -> {
                val center = Offset(w / 2f, h / 2f)
                val unit = minOf(w, h) / 52f
                listOf(12f, 19f, 25f).forEachIndexed { idx, baseR ->
                    val pulse = 0.88f + 0.22f * sin(phase + idx * 0.9f)
                    drawCircle(
                        color = if (idx == 1) accent else Color.White.copy(alpha = 0.88f),
                        radius = baseR * unit * pulse,
                        center = center,
                        style = Stroke(width = (if (idx == 0) 2.dp else 1.35.dp).toPx())
                    )
                }
                drawCircle(color = Color.White, radius = 4.5f * unit, center = center)
            }

            VisualizerStyle.PARTICLES -> {
                val pts = listOf(
                    Triple(24f, 18f, 4f), Triple(46f, 34f, 3f), Triple(72f, 14f, 5f),
                    Triple(98f, 30f, 3.5f), Triple(124f, 16f, 4.5f), Triple(152f, 32f, 3.5f),
                    Triple(178f, 18f, 5f), Triple(204f, 28f, 4f), Triple(224f, 18f, 3f)
                )
                val sx = w / 240f
                val sy = h / 52f
                pts.forEachIndexed { idx, (px, py, r) ->
                    val dy = sin(phase + idx * 0.7f) * 6f * sy
                    drawCircle(
                        color = if (idx % 2 == 1) accent else Color.White,
                        radius = r * minOf(sx, sy),
                        center = Offset(px * sx, py * sy + dy)
                    )
                }
            }

            VisualizerStyle.MATRIX -> {
                val sx = w / 240f
                val sy = h / 52f
                for (c in 0 until 12) {
                    val modulated = (cols[c] * (0.68f + 0.32f * sin(phase + c * 0.55f))).coerceIn(0.15f, 1f)
                    val rowsLit = maxOf(1, (modulated * 4f).roundToInt())
                    for (r in 0 until rowsLit) {
                        drawRoundRect(
                            color = if (r == 3) accent else Color.White.copy(alpha = 0.88f),
                            topLeft = Offset((10f + c * 19f) * sx, (39f - r * 10f) * sy),
                            size = Size(11f * sx, 6f * sy),
                            cornerRadius = CornerRadius(2f * sx, 2f * sx)
                        )
                    }
                }
            }

            VisualizerStyle.AURORA -> {
                val ribbonBack = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.58f)
                    for (i in 1..12) {
                        val t = i / 12f
                        lineTo(t * w, h * (0.45f + 0.22f * sin(t * 6f + phase)))
                    }
                    lineTo(w, h)
                    close()
                }
                val ribbonFront = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.70f)
                    for (i in 1..12) {
                        val t = i / 12f
                        lineTo(t * w, h * (0.62f + 0.18f * cos(t * 5f + phase)))
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(ribbonBack, color = accent.copy(alpha = 0.45f))
                drawPath(ribbonFront, color = Color.White.copy(alpha = 0.28f))
            }

            VisualizerStyle.NEEDLE -> {
                val sx = w / 240f
                val sy = h / 52f
                listOf(68f to 0f, 172f to 0.7f).forEach { (pivotX, offset) ->
                    val px = pivotX * sx
                    val py = 45f * sy
                    val arcR = 38f * minOf(sx, sy)
                    drawArc(
                        color = Color.White.copy(alpha = 0.45f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(px - arcR, py - arcR),
                        size = Size(arcR * 2f, arcR * 2f),
                        style = Stroke(1.5.dp.toPx())
                    )
                    val angle = (-Math.PI / 2.0 + sin(phase * 1.5f + offset) * 0.62).toFloat()
                    val len = 33f * sy
                    drawLine(
                        color = Color.White,
                        start = Offset(px, py),
                        end = Offset(px + cos(angle) * len, py + sin(angle) * len),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = accent, radius = 3.2f * sy, center = Offset(px, py))
                }
            }

            VisualizerStyle.RIPPLE -> {
                val sx = w / 240f
                val sy = h / 52f
                for (i in 0 until 3) {
                    val progress = ((phase / (2f * PI.toFloat())) + i / 3f) % 1f
                    val scale = 0.45f + progress * 0.65f
                    val rx = 68f * sx * scale
                    val ry = 15f * sy * scale
                    val cx = 120f * sx
                    val cy = 28f * sy
                    drawOval(
                        color = (if (i == 1) accent else Color.White).copy(alpha = (1f - progress).coerceIn(0.15f, 0.9f)),
                        topLeft = Offset(cx - rx, cy - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            }

            VisualizerStyle.DOTS -> {
                val sx = w / 240f
                val sy = h / 52f
                cols.forEachIndexed { idx, frac ->
                    val mod = 0.6f + 0.4f * sin(phase + idx * 0.65f)
                    val cx = (15f + idx * 19f) * sx
                    val cy = (44f - frac * 32f * mod) * sy
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(cx, 46f * sy),
                        end = Offset(cx, cy),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = 3.8f * minOf(sx, sy), center = Offset(cx, cy))
                }
            }

            VisualizerStyle.HORIZON -> {
                val sx = w / 240f
                val sy = h / 52f
                val baseY = 44f * sy
                val centerX = 120f * sx
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(10f * sx, baseY),
                    end = Offset(230f * sx, baseY),
                    strokeWidth = 1.5.dp.toPx()
                )
                cols.forEachIndexed { idx, frac ->
                    val mod = 0.62f + 0.38f * sin(phase + idx * 0.5f)
                    val angle = (-60f + idx * 11f) * (PI / 180.0)
                    val len = (14f + frac * 24f * mod)
                    val x2 = centerX + sin(angle).toFloat() * len * 1.9f * sx
                    val y2 = baseY - cos(angle).toFloat() * len * sy
                    drawLine(
                        color = if (idx % 2 == 1) accent else Color.White.copy(alpha = 0.9f),
                        start = Offset(centerX, baseY),
                        end = Offset(x2, y2),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
