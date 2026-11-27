package com.corvix.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.corvix.player.audio.AlbumArtLoader
import com.corvix.player.model.AppScreen
import com.corvix.player.model.ArtStyle
import com.corvix.player.model.Track
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.corvix.player.ui.theme.LiquidGlassTokens
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.ui.theme.LocalLiquidGlassConfig
import com.corvix.player.ui.theme.artBrushFor
import com.corvix.player.ui.theme.liquidGlassLens
import com.corvix.player.ui.theme.liquidGlassPressScale
import com.corvix.player.ui.theme.liquidGlassSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CorvixAmbientOrbs(modifier: Modifier = Modifier) {
    val palette = LocalCorvixPalette.current
    if (palette.orbAlpha <= 0.01f) return

    val transition = rememberInfiniteTransition(label = "corvix_orbs")
    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbA"
    )
    val phaseB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbB"
    )
    val phaseC by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbC"
    )

    val c1 by animateColorAsState(palette.orbColors.getOrElse(0) { palette.accent }, tween(450), label = "c1")
    val c2 by animateColorAsState(palette.orbColors.getOrElse(1) { palette.accentSecondary }, tween(450), label = "c2")
    val c3 by animateColorAsState(palette.orbColors.getOrElse(2) { palette.accent }, tween(450), label = "c3")
    val alpha = palette.orbAlpha

    val brush1 = remember(c1, alpha) {
        Brush.radialGradient(listOf(c1.copy(alpha = alpha), c1.copy(alpha = alpha * 0.4f), Color.Transparent))
    }
    val brush2 = remember(c2, alpha) {
        Brush.radialGradient(listOf(c2.copy(alpha = alpha), c2.copy(alpha = alpha * 0.4f), Color.Transparent))
    }
    val brush3 = remember(c3, alpha) {
        Brush.radialGradient(listOf(c3.copy(alpha = alpha * 0.9f), c3.copy(alpha = alpha * 0.35f), Color.Transparent))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Modifier.blur(palette.blurDp)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = -60f + cos(phaseA) * 32f
                    translationY = -40f + sin(phaseA) * 28f
                }
                .background(brush1, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(310.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = 90f - cos(phaseB) * 36f
                    translationY = -30f - sin(phaseB) * 32f
                }
                .background(brush2, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(270.dp)
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    translationX = 20f + cos(phaseC) * 30f
                    translationY = 60f - sin(phaseC) * 28f
                }
                .background(brush3, CircleShape)
        )
        // Soft dark specular glass spheres for liquid glass refraction
        if (palette.isDark && !palette.isAmoled) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        translationX = 50f - cos(phaseA) * 18f
                        translationY = 140f + sin(phaseB) * 16f
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color(0xFF0B0D10).copy(alpha = 0.35f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        translationX = -40f + sin(phaseC) * 22f
                        translationY = -120f + cos(phaseA) * 18f
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color(0xFF0B0D10).copy(alpha = 0.30f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    cornerRadius: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalCorvixPalette.current
    val radius = cornerRadius ?: palette.cornerCard
    val shape = RoundedCornerShape(radius)
    val frostBoost = (palette.blurDp.value / 100f).coerceIn(0f, 1f)

    val containerModifier = if (!palette.isIpod && !palette.isWinamp) {
        modifier
            .liquidGlassSurface(
                shape = shape,
                isDark = palette.isDark,
                isAmoled = palette.isAmoled,
                strong = strong,
                frostBoost = frostBoost,
                elevation = if (strong) 16.dp else 10.dp,
                brightRim = strong,
                preset = palette.preset,
                accent = palette.accent,
                accentSecondary = palette.accentSecondary
            )
            .liquidGlassLens(cornerRadius = radius)
            .padding(contentPadding)
    } else {
        val bgBrush = if (strong) palette.surfaceStrongBrush else palette.surfaceBrush
        val borderColor = if (strong) palette.surfaceStrongBorder else palette.surfaceBorder
        modifier
            .clip(shape)
            .background(bgBrush, shape)
            .border(palette.surfaceBorderWidth, borderColor, shape)
            .padding(contentPadding)
    }

    Box(
        modifier = containerModifier,
        content = content
    )
}

@Composable
fun GlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    shape: Shape = RoundedCornerShape(999.dp),
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalCorvixPalette.current
    val effectiveShape = if (palette.isIpod && shape == RoundedCornerShape(999.dp)) {
        RoundedCornerShape(if (active) 6.dp else 8.dp)
    } else {
        shape
    }
    val interactionSource = remember { MutableInteractionSource() }

    if (!palette.isIpod && !palette.isWinamp) {
        val cfg = LocalLiquidGlassConfig.current
        val fillBrush = if (active) {
            if (palette.isDark) {
                val c = cfg.contrastBoost
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = (0.24f * c).coerceIn(0.08f, 0.50f)),
                        palette.accent.copy(alpha = (0.26f * c).coerceIn(0.10f, 0.55f)),
                        Color.White.copy(alpha = (0.14f * c).coerceIn(0.04f, 0.35f))
                    )
                )
            } else {
                palette.activePillBrush
            }
        } else {
            LiquidGlassTokens.smokedCrystalBrush(
                isDark = palette.isDark,
                isAmoled = palette.isAmoled,
                strong = false,
                contrastBoost = cfg.contrastBoost,
                preset = palette.preset,
                accent = palette.accent,
                accentSecondary = palette.accentSecondary
            )
        }
        val rimBrush = LiquidGlassTokens.specularRimBrush(
            isDark = palette.isDark,
            bright = active,
            rimStrength = cfg.rimLightStrength,
            rotationDeg = cfg.rimLightRotationDeg,
            preset = palette.preset
        )
        val tintAlpha = (cfg.colorOpacity * 0.65f).coerceIn(0f, 0.85f)
        Row(
            modifier = modifier
                .liquidGlassPressScale(interactionSource)
                .clip(effectiveShape)
                .background(if (palette.isDark) LiquidGlassTokens.SmokedObsidian.copy(alpha = tintAlpha) else Color.Transparent, effectiveShape)
                .background(fillBrush, effectiveShape)
                .border(cfg.softRimWidthDp.dp, rimBrush, effectiveShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    } else {
        val brush = if (active) palette.activePillBrush else palette.pillBrush
        val border = if (active) palette.activePillBorder else palette.pillBorder

        Row(
            modifier = modifier
                .clip(effectiveShape)
                .background(brush, effectiveShape)
                .border(1.dp, border, effectiveShape)
                .clickable(onClick = onClick)
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun GlassToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "switch_thumb"
    )
    val trackBrush = when {
        palette.isIpod && checked -> Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
        palette.isIpod -> Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1)))
        checked -> Brush.horizontalGradient(
            listOf(
                palette.accent.copy(alpha = 0.62f),
                palette.accentSecondary.copy(alpha = 0.72f)
            )
        )
        palette.isDark -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f)
            )
        )
        else -> Brush.verticalGradient(listOf(Color(0x28787880), Color(0x18787880)))
    }
    val trackRimBrush = when {
        palette.isIpod -> SolidColor(if (checked) Color(0xFF1E40AF) else Color(0xFF94A3B8))
        checked -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.14f),
                palette.accent.copy(alpha = 0.42f)
            )
        )
        palette.isDark -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.22f)
            )
        )
        else -> SolidColor(Color.White.copy(alpha = 0.75f))
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.42f)
            .width(48.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackBrush, CircleShape)
            .border(0.9.dp, trackRimBrush, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .width(24.dp)
                .height(20.dp)
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.32f else 0.92f),
                            Color.White.copy(alpha = if (palette.isDark) 0.08f else 0.65f),
                            Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.82f)
                        )
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
fun IosHeartIcon(
    liked: Boolean,
    modifier: Modifier = Modifier,
    unlikedTint: Color = Color.White,
    likedTint: Color = Color(0xFFFF375F)
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.86f)
            cubicTo(w * 0.14f, h * 0.62f, w * 0.04f, h * 0.36f, w * 0.08f, h * 0.24f)
            cubicTo(w * 0.12f, h * 0.10f, w * 0.28f, h * 0.07f, w * 0.40f, h * 0.15f)
            cubicTo(w * 0.45f, h * 0.18f, w * 0.48f, h * 0.23f, w * 0.50f, h * 0.27f)
            cubicTo(w * 0.52f, h * 0.23f, w * 0.55f, h * 0.18f, w * 0.60f, h * 0.15f)
            cubicTo(w * 0.72f, h * 0.07f, w * 0.88f, h * 0.10f, w * 0.92f, h * 0.24f)
            cubicTo(w * 0.96f, h * 0.36f, w * 0.86f, h * 0.62f, w * 0.50f, h * 0.86f)
            close()
        }
        if (liked) {
            drawPath(path = path, color = likedTint)
        } else {
            drawPath(
                path = path,
                color = unlikedTint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.8.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

val LocalArtworkVersion = compositionLocalOf { 0 }

@Composable
fun ArtworkBox(
    artStyle: ArtStyle,
    modifier: Modifier = Modifier,
    track: Track? = null,
    highRes: Boolean = false,
    cornerRadius: Dp = 14.dp,
    showPlayIcon: Boolean = false,
    showFallbackDisc: Boolean = false,
    showBorder: Boolean = true,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val context = LocalContext.current
    val artworkVersion = LocalArtworkVersion.current
    var loadedBitmap by remember(track?.id, track?.albumId, highRes, artworkVersion) {
        mutableStateOf(track?.let { AlbumArtLoader.getCached(it, highRes)?.imageBitmap })
    }

    LaunchedEffect(track?.id, track?.albumId, highRes, artworkVersion) {
        if (track != null && (loadedBitmap == null || highRes)) {
            AlbumArtLoader.load(context, track, highRes)?.let { art ->
                loadedBitmap = art.imageBitmap
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(artBrushFor(artStyle), shape)
            .then(
                if (showBorder) Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), shape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = loadedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = track?.album ?: "Album Art",
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else if (showFallbackDisc) {
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(46.dp)
            )
        }
        if (showPlayIcon) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PlayingEqBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color? = null
) {
    val palette = LocalCorvixPalette.current
    val resolvedColor = barColor ?: palette.eqBarColor
    val fractions = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "eq_bars")
        val bar0 by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
            label = "b0"
        )
        val bar1 by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(520, delayMillis = 120), RepeatMode.Reverse),
            label = "b1"
        )
        val bar2 by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(460, delayMillis = 240), RepeatMode.Reverse),
            label = "b2"
        )
        listOf(bar0, bar1, bar2)
    } else {
        listOf(0.35f, 0.55f, 0.35f)
    }

    Row(
        modifier = modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        fractions.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((12f * frac).dp)
                    .clip(CircleShape)
                    .background(resolvedColor)
            )
        }
    }
}

@Composable
fun CorvixStatusBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current

    Column(modifier = modifier.fillMaxWidth()) {
        val statusBarModifier = if (palette.isIpod) {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1))))
                .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.horizontalPadding + 4.dp, vertical = 6.dp)
        }

        Row(
            modifier = statusBarModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "11:59",
                style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted, mono = true)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!palette.isIpod) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
                GlassPill(
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OFFLINE",
                        style = font.style(10f, FontWeight.SemiBold, palette.textPrimary)
                    )
                }
                Text(
                    text = "84%",
                    style = font.style(11.5f, FontWeight.Medium, palette.textMuted, mono = true)
                )
            }
        }

        if (palette.isIpod) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
                    .border(1.5.dp, Color(0xFF1E293B))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    AppScreen.IPOD_MENU to "Menu",
                    AppScreen.LIBRARY to "Music",
                    AppScreen.FOLDERS to "Folders",
                    AppScreen.NOW_PLAYING to "Playing",
                    AppScreen.SETTINGS to "Settings"
                )
                tabs.forEach { (screen, label) ->
                    val active = currentScreen == screen
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { onNavigate(screen) }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            style = font.style(
                                sizeSp = 11f,
                                weight = FontWeight.SemiBold,
                                color = if (active) Color.White else Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSectionDivider(modifier: Modifier = Modifier) {
    val palette = LocalCorvixPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.divider)
    )
}
