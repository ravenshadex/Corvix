package com.corvix.player.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.corvix.player.R
import com.corvix.player.model.AppSkin

private object OnboardingPalette {
    val Obsidian = Color(0xFF07090E)
    val CarbonCard = Color(0xFF12151E)
    val CarbonSelected = Color(0xFF1A1C22)
    val BorderHairline = Color(0xFF252A3A)
    val AmberStart = Color(0xFFFFD400)
    val AmberEnd = Color(0xFFFFA800)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val ReadyEmerald = Color(0xFF10B981)
}

private fun onboardingTextStyle(
    sizeSp: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = OnboardingPalette.TextPrimary,
    mono: Boolean = false,
    lineHeightSp: Float? = null
): TextStyle = TextStyle(
    fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = (lineHeightSp ?: (sizeSp * 1.28f)).sp,
    color = color,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
fun WelcomeScreen(
    initialSkin: AppSkin = AppSkin.WINAMP_CLASSIC,
    onScanAndStart: (AppSkin) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableIntStateOf(0) }
    var selectedSkin by remember { mutableStateOf(initialSkin) }

    BackHandler(enabled = page == 1) {
        page = 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingPalette.Obsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut(tween(150)))
                }
            },
            label = "corvix_onboarding_2page",
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxSize()
        ) { currentPage ->
            when (currentPage) {
                0 -> OnboardingWelcomePage(
                    onSkip = { onScanAndStart(AppSkin.LIQUID_SHELL) },
                    onNext = { page = 1 }
                )
                else -> OnboardingSkinAndAccessPage(
                    selectedSkin = selectedSkin,
                    onSelectSkin = { selectedSkin = it },
                    onBack = { page = 0 },
                    onLaunch = { onScanAndStart(selectedSkin) }
                )
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage(
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: CORVIX // AUDIO + Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CORVIX // AUDIO",
                style = onboardingTextStyle(11.5f, FontWeight.Bold, OnboardingPalette.AmberStart, mono = true)
                    .copy(letterSpacing = 0.8.sp)
            )
            Text(
                text = "Skip",
                style = onboardingTextStyle(13f, FontWeight.SemiBold, OnboardingPalette.TextMuted),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Hero Brand Stage (Concentric Acoustic Ripples + Sent Corvix Logo)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            CorvixHeroAcousticLogo()

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(OnboardingPalette.AmberStart.copy(alpha = 0.14f))
                    .border(1.dp, OnboardingPalette.AmberStart.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = OnboardingPalette.AmberStart,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "100% OFFLINE HI-RES",
                    style = onboardingTextStyle(11f, FontWeight.Bold, OnboardingPalette.AmberStart, mono = true)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pure Sound.\nZero Signal.",
                style = onboardingTextStyle(28f, FontWeight.ExtraBold, OnboardingPalette.TextPrimary, lineHeightSp = 34f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Built for local music collectors — lossless playback, hardware DSP, and classic hardware skins.",
                style = onboardingTextStyle(13f, FontWeight.Normal, OnboardingPalette.TextMuted, lineHeightSp = 19f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        // 3 Scannable Capability Rows with crisp Vector Icons
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            OnboardingValueCard(
                icon = Icons.Rounded.GraphicEq,
                title = "Bit-Perfect Offline Engine",
                subtitle = "FLAC, ALAC, WAV & gapless playback"
            )
            OnboardingValueCard(
                icon = Icons.Rounded.Tune,
                title = "10-Band EQ & Hardware DSP",
                subtitle = "AutoEQ curves, Bass Boost & Reverb"
            )
            OnboardingValueCard(
                icon = Icons.Rounded.Bolt,
                title = "3 Iconic Player Skins",
                subtitle = "Liquid Shell, Classic iPod & Winamp 2.91"
            )
        }

        // Bottom Stepper + Primary Electric Amber CTA
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            OnboardingStepStepper(activePage = 0)
            CorvixAmberPrimaryButton(
                label = "Choose Your Skin",
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun CorvixHeroAcousticLogo() {
    val transition = rememberInfiniteTransition(label = "corvix_acoustic_rings")
    val p1 by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring1"
    )
    val p2 by transition.animateFloat(
        initialValue = 1.06f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring2"
    )

    Box(
        modifier = Modifier.size(204.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        OnboardingPalette.AmberStart.copy(alpha = 0.22f),
                        OnboardingPalette.AmberEnd.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.48f
            )
        }

        Box(
            modifier = Modifier
                .size(196.dp)
                .graphicsLayer { scaleX = p2; scaleY = p2 }
                .border(1.dp, OnboardingPalette.AmberStart.copy(alpha = 0.24f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(170.dp)
                .graphicsLayer { scaleX = p1; scaleY = p1 }
                .border(1.dp, OnboardingPalette.AmberStart.copy(alpha = 0.34f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer { scaleX = p2; scaleY = p2 }
                .border(1.dp, OnboardingPalette.AmberStart.copy(alpha = 0.44f), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(128.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = OnboardingPalette.AmberStart,
                    spotColor = OnboardingPalette.AmberStart
                )
                .clip(RoundedCornerShape(28.dp))
                .background(OnboardingPalette.AmberStart)
                .border(2.dp, OnboardingPalette.AmberStart.copy(alpha = 0.72f), RoundedCornerShape(28.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.corvix_welcome_logo),
                contentDescription = "Corvix App Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun OnboardingValueCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OnboardingPalette.CarbonCard)
            .border(1.dp, OnboardingPalette.BorderHairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OnboardingPalette.AmberStart.copy(alpha = 0.14f))
                .border(1.dp, OnboardingPalette.AmberStart.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnboardingPalette.AmberStart,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = onboardingTextStyle(13f, FontWeight.Bold, OnboardingPalette.TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = onboardingTextStyle(11.5f, FontWeight.Normal, OnboardingPalette.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OnboardingSkinAndAccessPage(
    selectedSkin: AppSkin,
    onSelectSkin: (AppSkin) -> Unit,
    onBack: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    val notificationsPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    fun isAudioGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    var audioGranted by remember { mutableStateOf(isAudioGranted()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                audioGranted = isAudioGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        audioGranted = isAudioGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Navigation & Stepper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = OnboardingPalette.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Back",
                    style = onboardingTextStyle(13f, FontWeight.SemiBold, OnboardingPalette.TextMuted)
                )
            }
            OnboardingStepStepper(activePage = 1)
            Text(
                text = "2 / 2",
                style = onboardingTextStyle(12f, FontWeight.Medium, OnboardingPalette.TextMuted, mono = true)
            )
        }

        // Header
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
            Text(
                text = "Make Corvix Yours",
                style = onboardingTextStyle(23f, FontWeight.ExtraBold, OnboardingPalette.TextPrimary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick your starting player interface — switch skins anytime in Settings.",
                style = onboardingTextStyle(12.5f, FontWeight.Normal, OnboardingPalette.TextMuted, lineHeightSp = 18f)
            )
        }

        // Interactive 3-Skin Picker
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SkinOptionCard(
                skin = AppSkin.LIQUID_SHELL,
                badge = "DEFAULT",
                description = "Translucent dark glass, adaptive album orbs & floating capsule dock.",
                selected = selectedSkin == AppSkin.LIQUID_SHELL,
                onClick = { onSelectSkin(AppSkin.LIQUID_SHELL) }
            )
            SkinOptionCard(
                skin = AppSkin.CLASSIC_IPOD,
                badge = "RETRO",
                description = "Metallic chassis, crisp split menu display & tactile rotary Click Wheel.",
                selected = selectedSkin == AppSkin.CLASSIC_IPOD,
                onClick = { onSelectSkin(AppSkin.CLASSIC_IPOD) }
            )
            SkinOptionCard(
                skin = AppSkin.WINAMP_CLASSIC,
                badge = "2.91 PORT",
                description = "3-window brushed slate rack, 7-seg green LCD, 10-band EQ & playlist.",
                selected = selectedSkin == AppSkin.WINAMP_CLASSIC,
                onClick = { onSelectSkin(AppSkin.WINAMP_CLASSIC) }
            )
        }

        // Offline Music Access Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OnboardingPalette.CarbonCard)
                .border(1.dp, OnboardingPalette.BorderHairline, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Offline Music Access",
                    style = onboardingTextStyle(13f, FontWeight.Bold, OnboardingPalette.TextPrimary)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (audioGranted) {
                        "Local FLAC, MP3 & ALAC folders ready"
                    } else {
                        "Scan device folders locally. Zero cloud sync."
                    },
                    style = onboardingTextStyle(11.5f, FontWeight.Normal, OnboardingPalette.TextMuted, lineHeightSp = 15.5f)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (audioGranted) OnboardingPalette.ReadyEmerald
                        else OnboardingPalette.AmberStart.copy(alpha = 0.15f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (audioGranted) OnboardingPalette.ReadyEmerald
                        else OnboardingPalette.AmberStart.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (!audioGranted) {
                            val req = buildList {
                                add(audioPermission)
                                if (notificationsPermission != null) add(notificationsPermission)
                            }
                            permLauncher.launch(req.toTypedArray())
                        }
                    }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (audioGranted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF052E16),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (audioGranted) "Ready" else "Allow",
                    style = onboardingTextStyle(
                        12f,
                        FontWeight.Bold,
                        if (audioGranted) Color(0xFF052E16) else OnboardingPalette.AmberStart
                    )
                )
            }
        }

        // Dynamic Launch CTA
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            CorvixAmberPrimaryButton(
                label = "Start with ${selectedSkin.title}",
                trailingIcon = Icons.Rounded.Bolt,
                onClick = onLaunch
            )
        }
    }
}

@Composable
private fun SkinOptionCard(
    skin: AppSkin,
    badge: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) OnboardingPalette.CarbonSelected else OnboardingPalette.CarbonCard
    val borderColor = if (selected) OnboardingPalette.AmberStart else OnboardingPalette.BorderHairline
    val borderWidth = if (selected) 1.6.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkinMiniThumbnail(skin = skin)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = skin.title,
                    style = onboardingTextStyle(13.5f, FontWeight.Bold, OnboardingPalette.TextPrimary)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) OnboardingPalette.AmberStart
                            else Color.Transparent
                        )
                        .border(
                            width = 0.8.dp,
                            color = if (selected) OnboardingPalette.AmberStart else OnboardingPalette.BorderHairline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = onboardingTextStyle(
                            9.5f,
                            FontWeight.Bold,
                            if (selected) OnboardingPalette.Obsidian else OnboardingPalette.TextMuted,
                            mono = true
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = onboardingTextStyle(11.5f, FontWeight.Normal, OnboardingPalette.TextMuted, lineHeightSp = 15.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) OnboardingPalette.AmberStart else Color.Transparent)
                .border(
                    width = 1.4.dp,
                    color = if (selected) OnboardingPalette.AmberStart else OnboardingPalette.BorderHairline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = OnboardingPalette.Obsidian,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SkinMiniThumbnail(skin: AppSkin) {
    when (skin) {
        AppSkin.LIQUID_SHELL -> {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF102A43), Color(0xFF090D16))))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                        radius = size.width * 0.34f,
                        center = Offset(size.width * 0.5f, size.height * 0.28f)
                    )
                    drawCircle(
                        color = Color(0xFF7DD3FC).copy(alpha = 0.7f),
                        radius = size.width * 0.34f,
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                        style = Stroke(1.5f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(0f, size.height * 0.68f),
                        size = Size(size.width, size.height * 0.08f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(0f, size.height * 0.82f),
                        size = Size(size.width, size.height * 0.18f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }
        }

        AppSkin.CLASSIC_IPOD -> {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFE5E7EB), Color(0xFF9CA3AF))))
                    .border(1.dp, Color(0xFF64748B), RoundedCornerShape(8.dp))
                    .padding(5.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color.White, Offset.Zero, Size(size.width, size.height * 0.42f))
                    drawRect(Color(0xFF1D4ED8), Offset(2f, 4f), Size(size.width - 4f, size.height * 0.11f))
                    drawRect(Color(0xFFCBD5E1), Offset(2f, size.height * 0.20f), Size(size.width * 0.7f, 3f))
                    val wheelCenter = Offset(size.width * 0.5f, size.height * 0.72f)
                    val r = size.width * 0.38f
                    drawCircle(Color.White, radius = r, center = wheelCenter)
                    drawCircle(Color(0xFF94A3B8), radius = r, center = wheelCenter, style = Stroke(1.2f))
                    drawCircle(Color(0xFFCBD5E1), radius = r * 0.36f, center = wheelCenter)
                }
            }
        }

        AppSkin.WINAMP_CLASSIC -> {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF282A3E), Color(0xFF161826))))
                    .border(1.dp, Color(0xFF090B12), RoundedCornerShape(3.dp))
                    .padding(3.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFD8B95B).copy(alpha = 0.75f), Offset.Zero, Size(size.width, 4f))
                    drawRect(Color.Black, Offset(0f, 6f), Size(size.width, 13f))
                    drawRect(Color(0xFF00FF00), Offset(3f, 10f), Size(size.width * 0.45f, 4f))
                    drawRect(Color(0xFF25293E), Offset(0f, 21f), Size(size.width, 14f))
                    val barXs = listOf(4f, 11f, 18f, 25f, 32f)
                    val barHs = listOf(8f, 5f, 10f, 6f, 9f)
                    barXs.forEachIndexed { idx, bx ->
                        drawRect(
                            color = if (idx % 2 == 0) Color(0xFFE6D615) else Color(0xFF74AE23),
                            topLeft = Offset(bx, 33f - barHs[idx]),
                            size = Size(4f, barHs[idx])
                        )
                    }
                    drawRect(Color.Black, Offset(0f, 37f), Size(size.width, size.height - 37f))
                    drawRect(Color(0xFF00E800), Offset(3f, 41f), Size(size.width * 0.72f, 2.5f))
                    drawRect(Color.White, Offset(3f, 47f), Size(size.width * 0.56f, 2.5f))
                    drawRect(Color(0xFF00E800), Offset(3f, 53f), Size(size.width * 0.65f, 2.5f))
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepStepper(activePage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { index ->
            val isActive = index == activePage
            Box(
                modifier = Modifier
                    .width(if (isActive) 24.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) OnboardingPalette.AmberStart
                        else Color.White.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

@Composable
private fun CorvixAmberPrimaryButton(
    label: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = OnboardingPalette.AmberStart,
                spotColor = OnboardingPalette.AmberEnd
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(OnboardingPalette.AmberStart, OnboardingPalette.AmberEnd))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = onboardingTextStyle(15f, FontWeight.ExtraBold, OnboardingPalette.Obsidian)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = OnboardingPalette.Obsidian,
            modifier = Modifier.size(18.dp)
        )
    }
}
