package com.corvix.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvix.player.model.AppSkin
import com.corvix.player.model.AppTypography
import com.corvix.player.model.ArtStyle
import com.corvix.player.model.GlassThemePreset
import com.corvix.player.model.GlassTone

@Immutable
data class CorvixPalette(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val isIpod: Boolean,
    val isWinamp: Boolean = false,
    val preset: GlassThemePreset,
    val tone: GlassTone,
    val accent: Color,
    val accentSecondary: Color,
    val orbColors: List<Color>,
    val orbAlpha: Float,
    val backgroundBrush: Brush,
    val viewportBackground: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accentText: Color,
    val divider: Color,
    val surfaceBrush: Brush,
    val surfaceBorder: Color,
    val surfaceBorderWidth: Dp,
    val surfaceHighlight: Color,
    val surfaceStrongBrush: Brush,
    val surfaceStrongBorder: Color,
    val pillBrush: Brush,
    val pillBorder: Color,
    val activePillBrush: Brush,
    val activePillBorder: Color,
    val activePillContent: Color,
    val rowActiveBrush: Brush,
    val dockCapsuleBrush: Brush,
    val dockCapsuleBorder: Color,
    val dockIndicatorBrush: Brush,
    val dockIndicatorBorder: Color,
    val dockTabInactive: Color,
    val dockTabActive: Color,
    val sliderTrack: Color,
    val eqBarColor: Color,
    val cornerCard: Dp,
    val blurDp: Dp
)

val LocalCorvixPalette = staticCompositionLocalOf<CorvixPalette> {
    error("LocalCorvixPalette not provided")
}

val LocalCorvixFont = staticCompositionLocalOf<CorvixTypographyTokens> {
    CorvixTypographyTokens.default()
}

val LocalCorvixMetrics = staticCompositionLocalOf {
    CorvixMetrics.fromViewport(390.dp, 844.dp)
}

@Immutable
data class CorvixMetrics(
    val widthDp: Dp,
    val heightDp: Dp,
    val scale: Float,
    val textScale: Float,
    val isCompactWidth: Boolean,
    val isWideOrLandscape: Boolean,
    val isShortHeight: Boolean,
    val horizontalPadding: Dp,
    val maxContentWidth: Dp,
    val dockMaxWidth: Dp,
    val dockBottomClearance: Dp,
    val artThumbSize: Dp,
    val ipodWheelSize: Dp
) {
    companion object {
        fun fromViewport(widthDp: Dp, heightDp: Dp): CorvixMetrics {
            val w = widthDp.value.coerceAtLeast(280f)
            val h = heightDp.value.coerceAtLeast(360f)
            val scale = (w / 355f).coerceIn(0.94f, 1.24f)
            val textScale = (w / 310f).coerceIn(1.12f, 1.32f)
            val isCompact = w < 340f
            val isWide = w >= 600f || w > h
            val isShort = h < 660f
            val hPad = when {
                w < 340f -> 12.dp
                w >= 600f -> 24.dp
                else -> 14.dp
            }
            val wheelDp = minOf(w * 0.74f, h * 0.36f).coerceIn(200f, 300f).dp
            return CorvixMetrics(
                widthDp = widthDp,
                heightDp = heightDp,
                scale = scale,
                textScale = textScale,
                isCompactWidth = isCompact,
                isWideOrLandscape = isWide,
                isShortHeight = isShort,
                horizontalPadding = hPad,
                maxContentWidth = 680.dp,
                dockMaxWidth = 560.dp,
                dockBottomClearance = (134f * scale + 12f).dp,
                artThumbSize = (46f * scale).coerceIn(40f, 54f).dp,
                ipodWheelSize = wheelDp
            )
        }
    }
}

@Immutable
data class CorvixTypographyTokens(
    val family: FontFamily,
    val weightBoost: FontWeight,
    val letterSpacingScale: Float,
    val sizeScale: Float = 1f
) {
    fun style(
        sizeSp: Float,
        weight: FontWeight = FontWeight.Normal,
        color: Color = Color.Unspecified,
        mono: Boolean = false
    ): TextStyle {
        val resolvedFamily = if (mono) FontFamily.Monospace else family
        val resolvedWeight = if (weightBoost == FontWeight.Light && weight == FontWeight.Normal) {
            FontWeight.Light
        } else {
            weight
        }
        return TextStyle(
            fontFamily = resolvedFamily,
            fontWeight = resolvedWeight,
            fontSize = (sizeSp * sizeScale).sp,
            color = color,
            letterSpacing = (0.25f * letterSpacingScale).sp
        )
    }

    companion object {
        fun default() = CorvixTypographyTokens(
            family = FontFamily.SansSerif,
            weightBoost = FontWeight.Normal,
            letterSpacingScale = 1f,
            sizeScale = 1f
        )
    }
}

fun resolveTypographyTokens(typography: AppTypography): CorvixTypographyTokens = when (typography) {
    AppTypography.SANS -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Normal, 1f)
    AppTypography.ROUNDED -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Medium, 1.15f)
    AppTypography.HUMANIST -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Normal, 1.3f)
    AppTypography.SERIF -> CorvixTypographyTokens(FontFamily.Serif, FontWeight.Normal, 1.0f)
    AppTypography.BOOK -> CorvixTypographyTokens(FontFamily.Serif, FontWeight.Medium, 1.15f)
    AppTypography.MONO -> CorvixTypographyTokens(FontFamily.Monospace, FontWeight.Normal, 0.85f)
    AppTypography.CONDENSED -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.SemiBold, -0.45f)
    AppTypography.IPOD -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Medium, -0.25f)
    AppTypography.SLAB -> CorvixTypographyTokens(FontFamily.Serif, FontWeight.SemiBold, 1.4f)
    AppTypography.WIDE -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Medium, 1.85f)
    AppTypography.MINIMAL -> CorvixTypographyTokens(FontFamily.SansSerif, FontWeight.Light, 1.1f)
}

fun buildCorvixPalette(
    tone: GlassTone,
    preset: GlassThemePreset,
    skin: AppSkin = AppSkin.LIQUID_SHELL,
    adaptiveAlbumColor: Boolean,
    trackPrimary: Color,
    trackSecondary: Color,
    trackOrbs: List<Color>,
    blurIntensity: Float
): CorvixPalette {
    val isIpod = skin == AppSkin.CLASSIC_IPOD
    val isWinamp = skin == AppSkin.WINAMP_CLASSIC
    val isDark = if (isIpod) false else if (isWinamp) true else tone != GlassTone.LIGHT
    val isAmoled = !isIpod && !isWinamp && tone == GlassTone.AMOLED

    val accent = if (isWinamp) Color(0xFF00FF00) else if (adaptiveAlbumColor) trackPrimary else Color(0xFF94A3B8)
    val accent2 = if (isWinamp) Color(0xFFD8B95B) else if (adaptiveAlbumColor) trackSecondary else Color(0xFF64748B)
    val orbs = if (adaptiveAlbumColor) trackOrbs else listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF94A3B8))

    if (isWinamp) {
        return CorvixPalette(
            isDark = true,
            isAmoled = false,
            isIpod = false,
            isWinamp = true,
            preset = preset,
            tone = tone,
            accent = Color(0xFF00FF00),
            accentSecondary = Color(0xFFD8B95B),
            orbColors = orbs,
            orbAlpha = 0f,
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF090B12), Color(0xFF090B12))),
            viewportBackground = Color(0xFF1D1F30),
            textPrimary = Color(0xFFFFFFFF),
            textMuted = Color(0xFFA4ABC2),
            accentText = Color(0xFF00E800),
            divider = Color(0xFF3E4459),
            surfaceBrush = Brush.verticalGradient(listOf(Color(0xFF282A3E), Color(0xFF1D1F30))),
            surfaceBorder = Color(0xFF5B627B),
            surfaceBorderWidth = 1.dp,
            surfaceHighlight = Color(0xFF636A84),
            surfaceStrongBrush = Brush.verticalGradient(listOf(Color(0xFF2D3047), Color(0xFF1C1E2E))),
            surfaceStrongBorder = Color(0xFFD8B95B),
            pillBrush = Brush.verticalGradient(listOf(Color(0xFFE5E9F2), Color(0xFFAEB6C9))),
            pillBorder = Color(0xFF3C4254),
            activePillBrush = Brush.verticalGradient(listOf(Color(0xFF0000C0), Color(0xFF000090))),
            activePillBorder = Color(0xFFD8B95B),
            activePillContent = Color.White,
            rowActiveBrush = Brush.horizontalGradient(listOf(Color(0xFF0000C0), Color(0xFF0000C0))),
            dockCapsuleBrush = Brush.verticalGradient(listOf(Color(0xFF282A3E), Color(0xFF1D1F30))),
            dockCapsuleBorder = Color(0xFF5B627B),
            dockIndicatorBrush = Brush.verticalGradient(listOf(Color(0xFF0000C0), Color(0xFF000090))),
            dockIndicatorBorder = Color(0xFFD8B95B),
            dockTabInactive = Color(0xFFA4ABC2),
            dockTabActive = Color(0xFF00FF00),
            sliderTrack = Color(0xFF0D0F18),
            eqBarColor = Color(0xFFE6D615),
            cornerCard = 2.dp,
            blurDp = 0.dp
        )
    }

    if (isIpod) {
        val ipodBlue = Color(0xFF1D4ED8)
        val ipodBlueTop = Color(0xFF3B82F6)
        return CorvixPalette(
            isDark = false,
            isAmoled = false,
            isIpod = true,
            preset = preset,
            tone = tone,
            accent = ipodBlueTop,
            accentSecondary = ipodBlue,
            orbColors = orbs,
            orbAlpha = 0f,
            backgroundBrush = Brush.linearGradient(
                listOf(Color(0xFFE7EAEF), Color(0xFFCDD3DD), Color(0xFFAEB6C4))
            ),
            viewportBackground = Color(0xFFF4F8FC),
            textPrimary = Color(0xFF0F172A),
            textMuted = Color(0xFF475569),
            accentText = ipodBlue,
            divider = Color(0xFFCBD5E1),
            surfaceBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
            surfaceBorder = Color(0xFFCBD5E1),
            surfaceBorderWidth = 1.dp,
            surfaceHighlight = Color.White,
            surfaceStrongBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC))),
            surfaceStrongBorder = Color(0xFF94A3B8),
            pillBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
            pillBorder = Color(0xFFCBD5E1),
            activePillBrush = Brush.verticalGradient(listOf(ipodBlueTop, ipodBlue)),
            activePillBorder = Color(0xFF1E40AF),
            activePillContent = Color.White,
            rowActiveBrush = Brush.verticalGradient(listOf(ipodBlueTop, ipodBlue)),
            dockCapsuleBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
            dockCapsuleBorder = Color(0xFF94A3B8),
            dockIndicatorBrush = Brush.verticalGradient(listOf(ipodBlueTop, ipodBlue)),
            dockIndicatorBorder = Color(0xFF1E40AF),
            dockTabInactive = Color(0xFF475569),
            dockTabActive = Color.White,
            sliderTrack = Color(0xFFCBD5E1),
            eqBarColor = ipodBlue,
            cornerCard = 8.dp,
            blurDp = 0.dp
        )
    }

    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFFCBD5E1) else Color(0xA60F172A)
    val accentText = if (isDark) accent else accent2
    val divider = if (isDark) Color(0x24FFFFFF) else Color(0x180F172A)

    val bgBrush = when {
        isAmoled -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        preset == GlassThemePreset.SMOKE && isDark -> Brush.verticalGradient(listOf(Color(0xFF090C12), Color(0xFF090C12)))
        preset == GlassThemePreset.SAKURA && isDark -> Brush.verticalGradient(listOf(Color(0xFF331428), Color(0xFF160A14)))
        preset == GlassThemePreset.SAKURA && !isDark -> Brush.verticalGradient(listOf(Color(0xFFFCE7F3), Color(0xFFFFF7FA)))
        preset == GlassThemePreset.VELVET && isDark -> Brush.verticalGradient(listOf(Color(0xFF17163B), Color(0xFF08071A)))
        isDark -> Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.22f), Color(0xFF090C12)),
            center = Offset(500f, 0f),
            radius = 1250f
        )
        else -> Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.14f), Color(0xFFF8FBFF)),
            center = Offset(500f, 0f),
            radius = 1200f
        )
    }

    val surfaceBrush = when {
        isAmoled -> Brush.linearGradient(
            listOf(Color(0xFFE2E8F0).copy(alpha = 0.08f), Color(0xFF060609).copy(alpha = 0.85f))
        )
        preset == GlassThemePreset.FROST && isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.24f), accent.copy(alpha = 0.16f))
        )
        preset == GlassThemePreset.FROST && !isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.90f), accent.copy(alpha = 0.14f))
        )
        preset == GlassThemePreset.MERCURY && isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.28f), Color(0xFF94A3B8).copy(alpha = 0.12f), accent.copy(alpha = 0.22f))
        )
        preset == GlassThemePreset.MERCURY && !isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.94f), Color(0xFFCBD5E1).copy(alpha = 0.55f), accent.copy(alpha = 0.22f))
        )
        preset == GlassThemePreset.AURORA && isDark -> Brush.linearGradient(
            listOf(accent.copy(alpha = 0.26f), accent2.copy(alpha = 0.22f), Color(0xFFEC4899).copy(alpha = 0.18f))
        )
        preset == GlassThemePreset.AURORA && !isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.86f), accent.copy(alpha = 0.14f), Color(0xFFF472B6).copy(alpha = 0.12f))
        )
        preset == GlassThemePreset.PRISM && isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.24f), accent.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f), accent2.copy(alpha = 0.18f))
        )
        preset == GlassThemePreset.PRISM && !isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.94f), accent.copy(alpha = 0.12f), Color.White.copy(alpha = 0.82f))
        )
        preset == GlassThemePreset.SMOKE && isDark -> Brush.linearGradient(
            listOf(Color(0xFF1E222A).copy(alpha = 0.78f), Color(0xFF0F1117).copy(alpha = 0.68f))
        )
        preset == GlassThemePreset.SMOKE && !isDark -> Brush.linearGradient(
            listOf(Color(0xFFF1F5F9).copy(alpha = 0.88f), Color(0xFFE2E8F0).copy(alpha = 0.78f))
        )
        preset == GlassThemePreset.SAKURA && isDark -> Brush.linearGradient(
            listOf(Color(0xFFFBCFE8).copy(alpha = 0.16f), Color(0xFFF472B6).copy(alpha = 0.08f))
        )
        preset == GlassThemePreset.SAKURA && !isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.90f), Color(0xFFFCE7F3).copy(alpha = 0.68f))
        )
        preset == GlassThemePreset.VELVET && isDark -> Brush.linearGradient(
            listOf(Color(0xFF818CF8).copy(alpha = 0.18f), Color(0xFF1E1B4B).copy(alpha = 0.55f))
        )
        isDark -> Brush.linearGradient(
            listOf(accent.copy(alpha = 0.17f), Color.White.copy(alpha = 0.07f), accent2.copy(alpha = 0.12f))
        )
        else -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.88f), Color.White.copy(alpha = 0.68f), accent.copy(alpha = 0.09f))
        )
    }

    val surfaceStrongBrush = if (isAmoled) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.14f), Color(0xFF05070E).copy(alpha = 0.94f)))
    } else if (isDark) {
        Brush.linearGradient(
            listOf(accent.copy(alpha = 0.24f), Color(0xFF0C1220).copy(alpha = 0.78f), accent2.copy(alpha = 0.20f))
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.96f), Color.White.copy(alpha = 0.84f), accent.copy(alpha = 0.12f))
        )
    }

    val surfaceBorder = when {
        preset == GlassThemePreset.CYBER -> accent.copy(alpha = 0.72f)
        preset == GlassThemePreset.MERCURY && isDark -> Color.White.copy(alpha = 0.45f)
        preset == GlassThemePreset.MERCURY && !isDark -> Color.White
        preset == GlassThemePreset.SMOKE && isDark -> Color.White.copy(alpha = 0.14f)
        preset == GlassThemePreset.SMOKE && !isDark -> Color(0x1A0F172A)
        preset == GlassThemePreset.SAKURA && isDark -> Color(0xFFFBCFE8).copy(alpha = 0.34f)
        preset == GlassThemePreset.SAKURA && !isDark -> Color(0xFFF472B6).copy(alpha = 0.22f)
        preset == GlassThemePreset.VELVET && isDark -> Color(0xFFC7D2FE).copy(alpha = 0.28f)
        isDark -> accent.copy(alpha = 0.30f)
        else -> Color.White.copy(alpha = 0.96f)
    }

    val borderW = if (preset == GlassThemePreset.CYBER || preset == GlassThemePreset.MERCURY) 1.5.dp else 1.dp

    val orbAlpha = when {
        isAmoled -> 0.08f
        preset == GlassThemePreset.AURORA && isDark -> 0.25f
        preset == GlassThemePreset.AURORA && !isDark -> 0.20f
        isDark -> 0.22f
        else -> 0.14f
    }

    return CorvixPalette(
        isDark = isDark,
        isAmoled = isAmoled,
        isIpod = false,
        preset = preset,
        tone = tone,
        accent = accent,
        accentSecondary = accent2,
        orbColors = orbs,
        orbAlpha = orbAlpha,
        backgroundBrush = bgBrush,
        viewportBackground = Color.Transparent,
        textPrimary = textPrimary,
        textMuted = textMuted,
        accentText = accentText,
        divider = divider,
        surfaceBrush = surfaceBrush,
        surfaceBorder = surfaceBorder,
        surfaceBorderWidth = borderW,
        surfaceHighlight = if (isDark) Color.White.copy(alpha = 0.40f) else Color.White,
        surfaceStrongBrush = surfaceStrongBrush,
        surfaceStrongBorder = if (isDark) accent.copy(alpha = 0.45f) else Color.White,
        pillBrush = if (isDark) {
            Brush.linearGradient(listOf(accent.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.72f)))
        },
        pillBorder = if (isDark) accent.copy(alpha = 0.32f) else Color(0x140F172A),
        activePillBrush = Brush.linearGradient(listOf(accent.copy(alpha = 0.90f), accent2.copy(alpha = 0.90f))),
        activePillBorder = Color.White.copy(alpha = 0.60f),
        activePillContent = Color.White,
        rowActiveBrush = if (isDark) {
            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), accent2.copy(alpha = 0.08f), Color.Transparent))
        } else {
            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.14f), Color.White.copy(alpha = 0.72f), Color.Transparent))
        },
        dockCapsuleBrush = if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xE8141D32),
                    Color(0xEB0B101D),
                    Color(0xE8131B2E)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xF0FFFFFF),
                    Color(0xEBF4F8FF),
                    Color(0xEBEDF4FF)
                )
            )
        },
        dockCapsuleBorder = if (isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.94f),
        dockIndicatorBrush = if (isDark) {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), accent.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.96f), accent.copy(alpha = 0.18f)))
        },
        dockIndicatorBorder = if (isDark) Color.White.copy(alpha = 0.36f) else Color.White,
        dockTabInactive = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xD60F172A),
        dockTabActive = if (isDark) accent else accent2,
        sliderTrack = if (isDark) Color.White.copy(alpha = 0.22f) else Color(0x3D787880),
        eqBarColor = accent,
        cornerCard = 24.dp,
        blurDp = blurIntensity.dp
    )
}

fun artBrushFor(style: ArtStyle): Brush = when (style) {
    ArtStyle.MIDNIGHT -> Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF4338CA), Color(0xFF0F172A)))
    ArtStyle.AMBER -> Brush.radialGradient(listOf(Color(0xFFFBBF24), Color(0xFFEA580C), Color(0xFF29150B)))
    ArtStyle.EMERALD -> Brush.radialGradient(listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF022C22)))
    ArtStyle.ROSE -> Brush.radialGradient(listOf(Color(0xFFFB7185), Color(0xFFBE185D), Color(0xFF240B18)))
    ArtStyle.VIOLET -> Brush.radialGradient(listOf(Color(0xFFC084FC), Color(0xFF6D28D9), Color(0xFF1E1B4B)))
    ArtStyle.GOLD -> Brush.radialGradient(listOf(Color(0xFFFDE047), Color(0xFF0D9488), Color(0xFF042F2E)))
}

@Composable
fun ProvideCorvixTheme(
    palette: CorvixPalette,
    typography: AppTypography,
    metrics: CorvixMetrics = CorvixMetrics.fromViewport(390.dp, 844.dp),
    liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    content: @Composable () -> Unit
) {
    val tokens = resolveTypographyTokens(typography).copy(sizeScale = metrics.textScale)
    CompositionLocalProvider(
        LocalCorvixPalette provides palette,
        LocalCorvixFont provides tokens,
        LocalCorvixMetrics provides metrics,
        LocalLiquidGlassConfig provides liquidGlassConfig,
        content = content
    )
}
