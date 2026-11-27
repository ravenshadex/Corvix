package com.corvix.player.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corvix.player.BuildConfig
import com.corvix.player.R
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixPalette
import java.security.MessageDigest

enum class SealTrustState(val badgeLabel: String) {
    VERIFIED_RELEASE("Key Verified"),
    DEBUG_DEV_BUILD("Dev Build"),
    UNVERIFIED_TAMPERED("Unverified Key")
}

data class CreatorSealInspection(
    val trustState: SealTrustState,
    val activeSha256Hex: String,
    val shortFingerprint: String
)

/**
 * Cryptographic identity constants and verification helpers bound to ravenshadex's
 * private release keystore (corvix-release.jks, CN=Corvix, O=Corvix Audio, C=IN).
 */
object CreatorSealCrypto {
    const val CREATOR_HANDLE = "ravenshadex"
    const val RELEASE_CERT_SHA256 =
        "EAAE1247DBC523B48BB51CED718847811BCFDECB03CC104417AD757AB5C528B0"
    const val RELEASE_CERT_SERIAL = "3312BEB623B13AA3"

    fun releaseByteWeights(): List<Float> = hexToNormalizedWeights(RELEASE_CERT_SHA256)

    fun hexToNormalizedWeights(hex: String): List<Float> {
        val clean = hex.filter { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
        if (clean.length < 2) return List(32) { 0.5f }
        val count = (clean.length / 2).coerceAtMost(32)
        return List(count) { idx ->
            val byteVal = clean.substring(idx * 2, idx * 2 + 2).toInt(16)
            byteVal / 255f
        }
    }

    fun formatShortFingerprint(hex: String): String {
        val clean = hex.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        return if (clean.length >= 16) {
            "${clean.substring(0, 4)}·${clean.substring(4, 8)}···${clean.substring(clean.length - 8, clean.length - 4)}·${clean.substring(clean.length - 4)}"
        } else {
            RELEASE_CERT_SHA256.substring(0, 8)
        }
    }

    fun verifySigningDigest(actualSha256Hex: String?, isDebugBuild: Boolean): CreatorSealInspection {
        val normalized = actualSha256Hex?.uppercase()?.replace(":", "")?.trim().orEmpty()
        val matchesRelease = normalized == RELEASE_CERT_SHA256
        val trust = when {
            matchesRelease -> SealTrustState.VERIFIED_RELEASE
            isDebugBuild -> SealTrustState.DEBUG_DEV_BUILD
            else -> SealTrustState.UNVERIFIED_TAMPERED
        }
        val displayHex = if (normalized.length == 64) normalized else RELEASE_CERT_SHA256
        return CreatorSealInspection(
            trustState = trust,
            activeSha256Hex = displayHex,
            shortFingerprint = formatShortFingerprint(displayHex)
        )
    }

    @Suppress("DEPRECATION")
    fun inspectAppCertificate(context: Context): CreatorSealInspection {
        val digestHex = runCatching {
            val pm = context.packageManager
            val pkgName = context.packageName
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                val signers = if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory
                }
                signers?.firstOrNull()?.toByteArray()
            } else {
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            }
            certBytes?.let { bytes ->
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { b -> "%02X".format(b) }
            }
        }.getOrNull()
        return verifySigningDigest(digestHex, BuildConfig.DEBUG)
    }
}

/**
 * Displays the user's bespoke crimson-obsidian signature emblem with a subtle
 * Liquid Glass spotlight backing and animated entrance/replay shimmer.
 */
@Composable
fun RavenshadexSignatureEmblem(
    replayKey: Int = 0,
    animateOnAppear: Boolean = true,
    showBackdropGlow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val reveal = remember { Animatable(if (animateOnAppear) 0.84f else 1f) }
    val alphaAnim = remember { Animatable(if (animateOnAppear) 0f else 1f) }

    LaunchedEffect(replayKey, animateOnAppear) {
        if (animateOnAppear || replayKey > 0) {
            reveal.snapTo(0.84f)
            alphaAnim.snapTo(0f)
            alphaAnim.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
            reveal.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        } else {
            reveal.snapTo(1f)
            alphaAnim.snapTo(1f)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showBackdropGlow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFEF4444).copy(alpha = 0.24f),
                            Color(0xFF991B1B).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    radius = size.maxDimension * 0.52f
                )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.ravenshadex_signature),
            contentDescription = "ravenshadex personal signature",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = reveal.value
                    scaleY = reveal.value
                    alpha = alphaAnim.value
                }
        )
    }
}

/**
 * 32-bar deterministic acoustic waveform derived directly from the 256-bit
 * release signing certificate SHA-256 bytes.
 */
@Composable
fun CryptographicKeyWaveform(
    primary: Color,
    secondary: Color,
    weights: List<Float>,
    replayKey: Int = 0,
    animateOnAppear: Boolean = true,
    modifier: Modifier = Modifier
) {
    val reveal = remember { Animatable(if (animateOnAppear) 0f else 1f) }
    LaunchedEffect(replayKey, animateOnAppear) {
        if (animateOnAppear || replayKey > 0) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        } else {
            reveal.snapTo(1f)
        }
    }

    val p = reveal.value.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = weights.size.coerceAtLeast(1)
        val step = w / count.toFloat()
        val barWidth = (step * 0.56f).coerceIn(1.5f, 4.5f)
        val midY = h / 2f

        for (i in 0 until count) {
            val barThreshold = i.toFloat() / count.toFloat()
            val barScale = ((p - barThreshold * 0.65f) / 0.35f).coerceIn(0f, 1f)
            if (barScale <= 0f) continue
            val amp = (0.20f + weights[i] * 0.80f) * barScale
            val halfH = (h * 0.46f * amp).coerceAtLeast(1.5f)
            val x = i * step + (step - barWidth) / 2f
            val frac = i.toFloat() / (count - 1).coerceAtLeast(1)
            val barColor = Color(
                red = primary.red + (secondary.red - primary.red) * frac,
                green = primary.green + (secondary.green - primary.green) * frac,
                blue = primary.blue + (secondary.blue - primary.blue) * frac,
                alpha = 0.48f + weights[i] * 0.48f
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, midY - halfH),
                size = Size(barWidth, halfH * 2f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Liquid Glass Personal Signature + Cryptographic Release-Key Seal card
 * for the bottom of SettingsScreen.
 */
@Composable
fun CreatorSignatureSettingsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var replayKey by remember { mutableIntStateOf(0) }

    val inspection = remember(context) { CreatorSealCrypto.inspectAppCertificate(context) }
    val weights = remember(inspection.activeSha256Hex) {
        CreatorSealCrypto.hexToNormalizedWeights(inspection.activeSha256Hex)
    }

    val trustColor = when (inspection.trustState) {
        SealTrustState.VERIFIED_RELEASE -> Color(0xFF34D399)
        SealTrustState.DEBUG_DEV_BUILD -> Color(0xFFFBBF24)
        SealTrustState.UNVERIFIED_TAMPERED -> Color(0xFFF87171)
    }
    val trustIcon = when (inspection.trustState) {
        SealTrustState.VERIFIED_RELEASE -> Icons.Rounded.Verified
        SealTrustState.DEBUG_DEV_BUILD -> Icons.Rounded.Shield
        SealTrustState.UNVERIFIED_TAMPERED -> Icons.Rounded.WarningAmber
    }

    val infinite = rememberInfiniteTransition(label = "card_wordmark_sheen")
    val sheen by infinite.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart),
        label = "wordmark_sheen"
    )
    val wordBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to palette.textPrimary,
            (sheen - 0.16f).coerceIn(0f, 1f) to Color(0xFFEF4444),
            sheen.coerceIn(0f, 1f) to Color.White,
            (sheen + 0.16f).coerceIn(0f, 1f) to palette.accent,
            1f to palette.textPrimary
        )
    )

    val plateBg = if (palette.isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF1A0B10).copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.04f),
                palette.accent.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF151821).copy(alpha = 0.88f),
                Color(0xFF1F1218).copy(alpha = 0.84f),
                Color(0xFF151821).copy(alpha = 0.88f)
            )
        )
    }
    val plateBorder = if (palette.isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val plateMuted = Color(0xFFCBD5E1)

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Crafted by ${CreatorSealCrypto.CREATOR_HANDLE}",
                    style = font.style(13.5f, FontWeight.Bold, palette.textPrimary),
                    modifier = Modifier.weight(1f)
                )

                GlassPill(
                    onClick = { replayKey++ },
                    active = false,
                    padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = trustIcon,
                            contentDescription = null,
                            tint = trustColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = inspection.trustState.badgeLabel,
                            style = font.style(10.5f, FontWeight.SemiBold, trustColor)
                        )
                    }
                }
            }

            // Recessed Holographic Liquid Glass Signature Showcase
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(plateBg)
                    .border(1.dp, plateBorder, RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { replayKey++ }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RavenshadexSignatureEmblem(
                    replayKey = replayKey,
                    showBackdropGlow = true,
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "RAVENSHADEX",
                        maxLines = 1,
                        style = TextStyle(
                            brush = if (palette.isDark) wordBrush else Brush.linearGradient(
                                listOf(Color.White, Color(0xFFFCA5A5), Color.White)
                            ),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 2.2.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )

                    CryptographicKeyWaveform(
                        primary = Color(0xFFEF4444),
                        secondary = palette.accent,
                        weights = weights,
                        replayKey = replayKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )

                    Text(
                        text = "SHA-256 · ${inspection.shortFingerprint}",
                        maxLines = 1,
                        style = font.style(9.5f, FontWeight.Medium, plateMuted.copy(alpha = 0.85f), mono = true)
                    )
                }
            }
        }
    }
}

/**
 * Compact personal signature emblem & SHA-256 key seal footer for WelcomeScreen.
 */
@Composable
fun OnboardingCreatorSignatureFooter(
    amberStart: Color,
    amberEnd: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var replayKey by remember { mutableIntStateOf(0) }
    val inspection = remember(context) { CreatorSealCrypto.inspectAppCertificate(context) }
    val weights = remember(inspection.activeSha256Hex) {
        CreatorSealCrypto.hexToNormalizedWeights(inspection.activeSha256Hex)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.24f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { replayKey++ }
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RavenshadexSignatureEmblem(
            replayKey = replayKey,
            showBackdropGlow = false,
            modifier = Modifier
                .width(44.dp)
                .height(25.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "RAVENSHADEX",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.6.sp,
                    color = Color(0xFFF8FAFC),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Text(
                text = "KEY ${inspection.shortFingerprint}",
                maxLines = 1,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 8.5.sp,
                    color = mutedColor.copy(alpha = 0.72f),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        CryptographicKeyWaveform(
            primary = Color(0xFFEF4444),
            secondary = amberEnd,
            weights = weights.take(16),
            replayKey = replayKey,
            modifier = Modifier
                .width(54.dp)
                .height(14.dp)
        )
    }
}
