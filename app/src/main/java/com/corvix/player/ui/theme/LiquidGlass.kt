package com.corvix.player.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.corvix.player.model.GlassThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class LiquidGlassConfig(
    val colorOpacity: Float = 0.14f,
    val contrastBoost: Float = 1.0f,
    val rimLightStrength: Float = 0.58f,
    val rimLightRotationDeg: Float = 315f,
    val softRimStrength: Float = 0.22f,
    val softRimWidthDp: Float = 1.1f,
    val glassBlurPx: Float = 28f,
    val refractionStrength: Float = 2.4f,
    val refractionWidth: Float = 0.14f,
    val chromaticAberration: Float = 0.0f,
    val shadowStrength: Float = 0.50f
)

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }

/**
 * True "Real Liquid Glass" (Apple iOS 26 / VisionOS-inspired) material system.
 * Crystal-clear transparent glass with preset-aware tinting, API 33+ AGSL refraction,
 * rotatable directional specular rims, soft inner bevel sheen, and spring press physics.
 */
object LiquidGlassTokens {
    val ObsidianCanvas = Color(0xFF07090E)
    val SmokedObsidian = Color(0xFF0E131D)
    val SmokedObsidianStrong = Color(0xFF121826)
    val TextScrim = Color(0x7306080B)

    fun smokedCrystalBrush(
        isDark: Boolean,
        isAmoled: Boolean,
        strong: Boolean,
        frostBoost: Float = 0f,
        contrastBoost: Float = 1.0f,
        preset: GlassThemePreset = GlassThemePreset.LIQUID,
        accent: Color = Color.White,
        accentSecondary: Color = Color.White
    ): Brush {
        if (!isDark) {
            val top = if (strong) 0.90f else 0.78f
            val mid = if (strong) 0.72f else 0.58f
            val bot = if (strong) 0.84f else 0.68f
            return Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = top),
                    Color(0xFFF3F6FC).copy(alpha = mid),
                    Color.White.copy(alpha = bot)
                )
            )
        }
        if (isAmoled) {
            return Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = ((if (strong) 0.11f else 0.06f) * contrastBoost).coerceIn(0f, 0.45f)),
                    Color(0xFF050608).copy(alpha = 0.65f),
                    Color.White.copy(alpha = ((if (strong) 0.07f else 0.03f) * contrastBoost).coerceIn(0f, 0.35f))
                )
            )
        }

        val tl = (((if (strong) 0.18f else 0.13f) + frostBoost * 0.28f) * contrastBoost).coerceIn(0.01f, 0.72f)
        val mid = (((if (strong) 0.04f else 0.02f) + frostBoost * 0.18f) / contrastBoost.coerceAtLeast(0.5f)).coerceIn(0.01f, 0.45f)
        val br = (((if (strong) 0.11f else 0.07f) + frostBoost * 0.22f) * contrastBoost).coerceIn(0.01f, 0.58f)

        return when (preset) {
            GlassThemePreset.FROST -> Brush.linearGradient(
                listOf(Color(0xFFE0F2FE).copy(alpha = tl + 0.08f), Color(0xFF93C5FD).copy(alpha = mid + 0.05f), Color.White.copy(alpha = br + 0.06f))
            )
            GlassThemePreset.MERCURY -> Brush.linearGradient(
                listOf(Color(0xFFF8FAFC).copy(alpha = tl + 0.10f), Color(0xFF64748B).copy(alpha = mid + 0.08f), Color(0xFFE2E8F0).copy(alpha = br + 0.08f))
            )
            GlassThemePreset.AURORA -> Brush.linearGradient(
                listOf(Color(0xFF22D3EE).copy(alpha = 0.22f), Color(0xFF34D399).copy(alpha = 0.14f), Color(0xFFEC4899).copy(alpha = 0.20f))
            )
            GlassThemePreset.PRISM -> Brush.linearGradient(
                listOf(Color(0xFF38BDF8).copy(alpha = 0.20f), Color(0xFFA855F7).copy(alpha = 0.14f), Color(0xFFFB7185).copy(alpha = 0.18f))
            )
            GlassThemePreset.SMOKE -> Brush.linearGradient(
                listOf(Color(0xFF334155).copy(alpha = 0.28f), Color(0xFF0F172A).copy(alpha = 0.35f), Color(0xFF1E293B).copy(alpha = 0.25f))
            )
            GlassThemePreset.SAKURA -> Brush.linearGradient(
                listOf(Color(0xFFFBCFE8).copy(alpha = 0.22f), Color(0xFFF472B6).copy(alpha = 0.12f), Color(0xFFFCE7F3).copy(alpha = 0.18f))
            )
            GlassThemePreset.VELVET -> Brush.linearGradient(
                listOf(Color(0xFF818CF8).copy(alpha = 0.22f), Color(0xFF312E81).copy(alpha = 0.20f), Color(0xFFC084FC).copy(alpha = 0.18f))
            )
            GlassThemePreset.CYBER -> Brush.linearGradient(
                listOf(Color(0xFF00F5D4).copy(alpha = 0.20f), Color(0xFF0F172A).copy(alpha = 0.18f), Color(0xFFF72585).copy(alpha = 0.20f))
            )
            GlassThemePreset.LIQUID -> Brush.linearGradient(
                listOf(Color.White.copy(alpha = tl), Color.White.copy(alpha = mid), accent.copy(alpha = br * 0.7f))
            )
        }
    }

    fun specularRimBrush(
        isDark: Boolean,
        bright: Boolean = false,
        rimStrength: Float = 0.58f,
        rotationDeg: Float = 315f,
        preset: GlassThemePreset = GlassThemePreset.LIQUID
    ): Brush {
        val rad = rotationDeg * PI / 180.0
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val start = Offset(500f - dx * 500f, 500f - dy * 500f)
        val end = Offset(500f + dx * 500f, 500f + dy * 500f)

        if (!isDark) {
            return Brush.linearGradient(
                colors = listOf(Color.White, Color(0x330F172A), Color.White.copy(alpha = 0.85f)),
                start = start,
                end = end
            )
        }
        val tl = (if (bright) rimStrength * 1.15f else rimStrength).coerceIn(0f, 1f)
        val mid = (rimStrength * 0.18f).coerceIn(0f, 0.4f)
        val br = (rimStrength * 0.55f).coerceIn(0f, 0.85f)

        val colors = when (preset) {
            GlassThemePreset.AURORA -> listOf(Color(0xFF22D3EE).copy(alpha = tl), Color(0xFF34D399).copy(alpha = mid + 0.12f), Color(0xFFEC4899).copy(alpha = br))
            GlassThemePreset.PRISM -> listOf(Color(0xFF38BDF8).copy(alpha = tl), Color(0xFFFACC15).copy(alpha = mid + 0.15f), Color(0xFFFB7185).copy(alpha = br))
            GlassThemePreset.SAKURA -> listOf(Color(0xFFFBCFE8).copy(alpha = tl), Color.White.copy(alpha = mid), Color(0xFFF472B6).copy(alpha = br))
            GlassThemePreset.VELVET -> listOf(Color(0xFFC7D2FE).copy(alpha = tl), Color(0xFF6366F1).copy(alpha = mid + 0.10f), Color(0xFFA855F7).copy(alpha = br))
            GlassThemePreset.CYBER -> listOf(Color(0xFF00F5D4).copy(alpha = tl), Color.White.copy(alpha = mid), Color(0xFFF72585).copy(alpha = br))
            GlassThemePreset.FROST -> listOf(Color(0xFFE0F2FE).copy(alpha = tl), Color.White.copy(alpha = mid + 0.08f), Color(0xFF93C5FD).copy(alpha = br))
            GlassThemePreset.MERCURY -> listOf(Color.White.copy(alpha = (tl * 1.15f).coerceAtMost(1f)), Color(0xFF94A3B8).copy(alpha = mid + 0.12f), Color.White.copy(alpha = br))
            else -> listOf(Color.White.copy(alpha = tl), Color.White.copy(alpha = mid), Color.White.copy(alpha = br))
        }
        return Brush.linearGradient(colors = colors, start = start, end = end)
    }
}

private const val LIQUID_GLASS_LENS_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float cornerRadiusPx;
    uniform float intensity;
    uniform float rimBandWidth;
    uniform float aberration;

    half4 main(float2 fragCoord) {
        if (size.x <= 2.0 || size.y <= 2.0) {
            return content.eval(fragCoord);
        }
        float2 halfSize = size * 0.5;
        float r = clamp(cornerRadiusPx, 0.0, min(halfSize.x, halfSize.y));
        float2 p = abs(fragCoord - halfSize) - (halfSize - float2(r));
        float sdf = length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - r;
        float distFromEdge = max(-sdf, 0.0);
        float bandPx = max(min(size.x, size.y) * rimBandWidth * 0.85, 4.0);
        float t = smoothstep(bandPx, 0.0, distFromEdge);
        float rimBand = t * t;
        float2 centerDir = normalize(fragCoord - halfSize + float2(0.0001));
        float shift = rimBand * intensity * 2.6;

        float2 cMin = float2(0.5, 0.5);
        float2 cMax = size - float2(0.5, 0.5);
        half red = content.eval(clamp(fragCoord + centerDir * shift * (1.0 + 0.85 * aberration), cMin, cMax)).r;
        half green = content.eval(clamp(fragCoord + centerDir * shift * (1.0 - 0.15 * aberration), cMin, cMax)).g;
        half blue = content.eval(clamp(fragCoord + centerDir * shift * (1.0 - 0.95 * aberration), cMin, cMax)).b;
        half alpha = content.eval(fragCoord).a;
        return half4(red, green, blue, alpha);
    }
"""

/**
 * Applies subtle AGSL optical edge-refraction & optional chromatic aberration on API 33+.
 */
fun Modifier.liquidGlassLens(
    intensity: Float? = null,
    enabled: Boolean = true,
    cornerRadius: Dp = 20.dp
): Modifier = composed {
    val cfg = LocalLiquidGlassConfig.current
    val resolvedIntensity = intensity ?: cfg.refractionStrength
    if (!enabled || resolvedIntensity <= 0.05f || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }
    val shader = remember { RuntimeShader(LIQUID_GLASS_LENS_AGSL) }
    this.graphicsLayer {
        if (size.width > 2f && size.height > 2f) {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("cornerRadiusPx", cornerRadius.toPx())
            shader.setFloatUniform("intensity", resolvedIntensity)
            shader.setFloatUniform("rimBandWidth", cfg.refractionWidth.coerceIn(0.02f, 0.40f))
            shader.setFloatUniform("aberration", cfg.chromaticAberration.coerceIn(0f, 3f))
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    }
}

/**
 * Full Liquid Glass container modifier driven by LocalLiquidGlassConfig & active GlassThemePreset.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape,
    isDark: Boolean,
    isAmoled: Boolean = false,
    strong: Boolean = false,
    frostBoost: Float = 0f,
    elevation: Dp = 14.dp,
    borderWidth: Dp? = null,
    brightRim: Boolean = false,
    preset: GlassThemePreset = GlassThemePreset.LIQUID,
    accent: Color = Color.White,
    accentSecondary: Color = Color.White
): Modifier = composed {
    val cfg = LocalLiquidGlassConfig.current
    val effectiveShadowAlpha = cfg.shadowStrength.coerceIn(0f, 1f)
    val effectiveBorderW = borderWidth ?: cfg.softRimWidthDp.dp

    val shadowMod = if (elevation > 0.dp && isDark && effectiveShadowAlpha > 0.02f) {
        Modifier.drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val blurRadius = (elevation.toPx() * 0.65f).coerceAtLeast(1f)
            val shadowColor = Color.Black.copy(alpha = (effectiveShadowAlpha * 0.55f).coerceIn(0f, 0.80f))
            drawContext.canvas.save()
            drawContext.canvas.clipPath(path, ClipOp.Difference)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = shadowColor.toArgb()
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }
                translate(0f, elevation.toPx() * 0.22f)
                drawPath(path.asAndroidPath(), paint)
            }
            drawContext.canvas.restore()
        }
    } else {
        Modifier
    }

    val baseAlpha = if (strong) (cfg.colorOpacity + 0.08f).coerceIn(0f, 0.96f) else cfg.colorOpacity.coerceIn(0f, 0.95f)
    val baseTint = when {
        !isDark -> Color.Transparent
        isAmoled -> Color(0xFF030406).copy(alpha = (baseAlpha + 0.18f).coerceIn(0f, 0.96f))
        strong -> LiquidGlassTokens.SmokedObsidianStrong.copy(alpha = baseAlpha)
        else -> LiquidGlassTokens.SmokedObsidian.copy(alpha = baseAlpha)
    }

    val rimBrush = LiquidGlassTokens.specularRimBrush(
        isDark = isDark,
        bright = brightRim,
        rimStrength = cfg.rimLightStrength,
        rotationDeg = cfg.rimLightRotationDeg,
        preset = preset
    )
    val softAlpha = (if (strong) cfg.softRimStrength * 1.15f else cfg.softRimStrength).coerceIn(0f, 0.70f)

    this
        .then(shadowMod)
        .clip(shape)
        .background(baseTint, shape)
        .background(
            LiquidGlassTokens.smokedCrystalBrush(
                isDark = isDark,
                isAmoled = isAmoled,
                strong = strong,
                frostBoost = frostBoost,
                contrastBoost = cfg.contrastBoost,
                preset = preset,
                accent = accent,
                accentSecondary = accentSecondary
            ),
            shape
        )
        .border(effectiveBorderW, rimBrush, shape)
        .drawWithContent {
            drawContent()
            if (isDark && size.height > 4f && softAlpha > 0.005f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = softAlpha),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.30f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height * 0.30f)
                )
            }
        }
}

/**
 * Smooth spring press physics (scale = 0.97f) for liquid-glass pills and buttons.
 */
fun Modifier.liquidGlassPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
