package com.corvix.player.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.corvix.player.model.AppScreen
import com.corvix.player.model.AppSkin
import com.corvix.player.model.SheetType
import com.corvix.player.ui.components.ClassicIpodWheel
import com.corvix.player.ui.components.CorvixAmbientOrbs
import com.corvix.player.ui.components.CorvixBottomSheetsHost
import com.corvix.player.ui.components.FloatingGlassBottomDock
import com.corvix.player.ui.components.GlassPill
import com.corvix.player.ui.components.GlassSurface
import com.corvix.player.ui.components.LocalArtworkVersion
import com.corvix.player.ui.screens.FoldersScreen
import com.corvix.player.ui.screens.ProfileScreen
import com.corvix.player.ui.screens.IpodMenuScreen
import com.corvix.player.ui.screens.LibraryScreen
import com.corvix.player.ui.screens.NowPlayingScreen
import com.corvix.player.ui.screens.PlaylistsScreen
import com.corvix.player.ui.screens.SettingsScreen
import com.corvix.player.ui.screens.WelcomeScreen
import com.corvix.player.ui.screens.WidgetPreviewScreen
import com.corvix.player.ui.screens.WinampClassicSkinScreen
import com.corvix.player.ui.theme.CorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.ui.theme.ProvideCorvixTheme
import com.corvix.player.ui.theme.buildCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel

@Composable
fun CorvixRootApp(viewModel: CorvixViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.rescanMediaLibrary()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.rescanMediaLibrary()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP ||
                (event == Lifecycle.Event.ON_RESUME && viewModel.uiState.value.activeSheet == SheetType.NONE)
            ) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestAudioScan = {
        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.rescanMediaLibrary()
        } else {
            permLauncher.launch(audioPermission)
        }
    }

    CorvixShellContent(
        state = state,
        viewModel = viewModel,
        onRequestRescanPermission = requestAudioScan
    )
}

@Composable
fun CorvixShellContent(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    onRequestRescanPermission: () -> Unit
) {
    val track = state.currentTrack
    val adaptive = state.adaptivePaletteOverride
    val resolvedPrimary = adaptive?.primary ?: track.primaryColor
    val resolvedSecondary = adaptive?.secondary ?: track.secondaryColor
    val resolvedOrbs = adaptive?.orbs ?: track.orbColors
    val palette = remember(
        state.glassTone,
        state.themePreset,
        state.appSkin,
        state.adaptiveAlbumColor,
        track.id,
        state.adaptivePaletteOverride,
        state.glassBlurPx
    ) {
        buildCorvixPalette(
            tone = state.glassTone,
            preset = state.themePreset,
            skin = state.appSkin,
            adaptiveAlbumColor = state.adaptiveAlbumColor,
            trackPrimary = resolvedPrimary,
            trackSecondary = resolvedSecondary,
            trackOrbs = resolvedOrbs,
            blurIntensity = state.glassBlurPx
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val metrics = remember(maxWidth, maxHeight) {
            CorvixMetrics.fromViewport(maxWidth, maxHeight)
        }

        ProvideCorvixTheme(
            palette = palette,
            typography = state.typography,
            metrics = metrics,
            liquidGlassConfig = state.liquidGlassConfig
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalArtworkVersion provides state.artworkVersion
            ) {
            if (state.showWelcome) {
                WelcomeScreen(
                    initialSkin = state.appSkin,
                    onScanAndStart = { chosenSkin ->
                        viewModel.completeWelcome(chosenSkin) { viewModel.rescanMediaLibrary() }
                    }
                )
                return@CompositionLocalProvider
            }
            val activePalette = LocalCorvixPalette.current
            val font = LocalCorvixFont.current
            val activeMetrics = LocalCorvixMetrics.current
            val isIpod = state.appSkin == AppSkin.CLASSIC_IPOD
            val isWinamp = state.appSkin == AppSkin.WINAMP_CLASSIC
            val context = LocalContext.current
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = true) {
                when {
                    showExitDialog -> showExitDialog = false
                    state.activeSheet != SheetType.NONE -> viewModel.closeSheet()
                    isWinamp && state.currentScreen != AppScreen.NOW_PLAYING -> viewModel.navigate(AppScreen.NOW_PLAYING)
                    isWinamp -> showExitDialog = true
                    state.currentScreen == AppScreen.NOW_PLAYING -> viewModel.navigate(AppScreen.LIBRARY)
                    state.currentScreen == AppScreen.WIDGET -> viewModel.navigate(AppScreen.SETTINGS)
                    state.currentScreen != AppScreen.LIBRARY && (!isIpod || state.currentScreen != AppScreen.IPOD_MENU) -> {
                        viewModel.navigate(if (isIpod) AppScreen.IPOD_MENU else AppScreen.LIBRARY)
                    }
                    else -> showExitDialog = true
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activePalette.backgroundBrush)
            ) {
                // Animated Liquid Orbs Backdrop (hidden in hardware iPod/Winamp chassis)
                if (!isIpod && !isWinamp) {
                    CorvixAmbientOrbs()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isIpod || isWinamp || state.currentScreen != AppScreen.NOW_PLAYING) {
                                Modifier
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                        .padding(
                            horizontal = if (isIpod) activeMetrics.horizontalPadding.coerceAtMost(14.dp) else 0.dp,
                            vertical = if (isIpod) 10.dp else 0.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val viewportMod = if (isIpod) {
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = activeMetrics.maxContentWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(2.dp, Color(0xFF1F242B), RoundedCornerShape(10.dp))
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = activeMetrics.maxContentWidth)
                    }

                    Box(modifier = viewportMod) {
                        if (isWinamp) {
                            when (state.currentScreen) {
                                AppScreen.SETTINGS,
                                AppScreen.FOLDERS,
                                AppScreen.PLAYLISTS,
                                AppScreen.PROFILE,
                                AppScreen.LIBRARY,
                                AppScreen.WIDGET -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF222436))
                                                .border(1.dp, Color(0xFF5B627B))
                                                .clickable { viewModel.navigate(AppScreen.NOW_PLAYING) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ ← Return to Winamp Classic",
                                                style = font.style(12.5f, FontWeight.Bold, Color(0xFFD8B95B), mono = true)
                                            )
                                            Text(
                                                text = "BACK",
                                                style = font.style(11f, FontWeight.Bold, Color(0xFF00FF00), mono = true)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            when (state.currentScreen) {
                                                AppScreen.SETTINGS -> SettingsScreen(
                                                    state = state,
                                                    viewModel = viewModel,
                                                    onRequestRescanPermission = onRequestRescanPermission,
                                                    isVisible = true
                                                )
                                                AppScreen.FOLDERS -> FoldersScreen(state = state, viewModel = viewModel)
                                                AppScreen.PLAYLISTS -> PlaylistsScreen(state = state, viewModel = viewModel)
                                                AppScreen.LIBRARY -> LibraryScreen(state = state, viewModel = viewModel)
                                                AppScreen.WIDGET -> WidgetPreviewScreen(state = state, viewModel = viewModel)
                                                else -> {}
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    WinampClassicSkinScreen(state = state, viewModel = viewModel)
                                }
                            }
                        } else if (!isIpod) {
                            // Keep all 4 primary tabs mounted in composition so tab taps run in <50ms
                            val primaryTabs = remember {
                                listOf(AppScreen.LIBRARY, AppScreen.PLAYLISTS, AppScreen.PROFILE, AppScreen.SETTINGS)
                            }
                            val onPrimaryTab = state.currentScreen in primaryTabs
                            primaryTabs.forEach { tabScreen ->
                                androidx.compose.runtime.key(tabScreen) {
                                    val isTabVisible = state.currentScreen == tabScreen
                                    val tabAlpha by animateFloatAsState(
                                        targetValue = if (isTabVisible) 1f else 0f,
                                        animationSpec = tween(75),
                                        label = "tab_alpha_${tabScreen.name}"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .zIndex(if (isTabVisible) 1f else -1f)
                                            .graphicsLayer {
                                                alpha = tabAlpha
                                                translationX = if (isTabVisible || tabAlpha > 0.01f) 0f else 100_000f
                                            }
                                    ) {
                                        when (tabScreen) {
                                            AppScreen.LIBRARY -> LibraryScreen(state = state, viewModel = viewModel)
                                            AppScreen.PROFILE -> ProfileScreen(state = state, viewModel = viewModel)
                                            AppScreen.PLAYLISTS -> PlaylistsScreen(state = state, viewModel = viewModel)
                                            AppScreen.SETTINGS -> SettingsScreen(
                                                state = state,
                                                viewModel = viewModel,
                                                onRequestRescanPermission = onRequestRescanPermission,
                                                isVisible = isTabVisible
                                            )
                                            else -> {}
                                        }
                                    }
                                }
                            }

                            // Non-primary overlay routes (Now Playing / Home Screen Widget)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !onPrimaryTab,
                                enter = fadeIn(tween(90)),
                                exit = fadeOut(tween(80)),
                                modifier = Modifier.fillMaxSize().zIndex(4f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(activePalette.backgroundBrush)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {}
                                ) {
                                    if (!isIpod) CorvixAmbientOrbs()
                                    when (state.currentScreen) {
                                        AppScreen.NOW_PLAYING -> NowPlayingScreen(state = state, viewModel = viewModel)
                                        AppScreen.WIDGET -> WidgetPreviewScreen(state = state, viewModel = viewModel)
                                        else -> {}
                                    }
                                }
                            }
                        } else {
                            AnimatedContent(
                                targetState = state.currentScreen,
                                transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(75)) },
                                label = "corvix_ipod_screen"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.IPOD_MENU -> IpodMenuScreen(state = state, viewModel = viewModel)
                                    AppScreen.LIBRARY -> LibraryScreen(state = state, viewModel = viewModel)
                                    AppScreen.FOLDERS -> FoldersScreen(state = state, viewModel = viewModel)
                                    AppScreen.PROFILE -> ProfileScreen(state = state, viewModel = viewModel)
                                    AppScreen.PLAYLISTS -> PlaylistsScreen(state = state, viewModel = viewModel)
                                    AppScreen.SETTINGS -> SettingsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onRequestRescanPermission = onRequestRescanPermission,
                                        isVisible = true
                                    )
                                    AppScreen.NOW_PLAYING -> NowPlayingScreen(state = state, viewModel = viewModel)
                                    AppScreen.WIDGET -> WidgetPreviewScreen(state = state, viewModel = viewModel)
                                }
                            }
                        }
                    }

                    if (isIpod && state.currentScreen != AppScreen.WIDGET) {
                        ClassicIpodWheel(
                            wheelDegrees = state.ipodWheelDegrees,
                            onStepWheel = { delta, deg -> viewModel.stepIpodWheel(delta, deg) },
                            onMenuClick = { viewModel.ipodMenuAction() },
                            onPrevClick = { viewModel.prevTrack() },
                            onNextClick = { viewModel.nextTrack() },
                            onPlayPauseClick = { viewModel.togglePlay() },
                            onCenterClick = { viewModel.ipodCenterAction() }
                        )
                    }
                }

                // Floating Double-Capsule Liquid Glass Bottom Dock
                val showFloatingDock = !isIpod && !isWinamp &&
                    state.currentScreen != AppScreen.NOW_PLAYING &&
                    state.currentScreen != AppScreen.WIDGET

                if (showFloatingDock) {
                    FloatingGlassBottomDock(
                        currentScreen = state.currentScreen,
                        currentTrack = track,
                        isPlaying = state.isPlaying,
                        onNavigate = { viewModel.navigate(it) },
                        onTogglePlay = { viewModel.togglePlay() },
                        onNextTrack = { viewModel.nextTrack() },
                        elapsedSecFlow = viewModel.elapsedSecFlow,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Toast notification banner (Queue additions, .m3u8 exports, etc.)
                if (state.toastBanner != null) {
                    GlassSurface(
                        strong = true,
                        cornerRadius = 999.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 46.dp)
                    ) {
                        Text(state.toastBanner, style = font.style(11.5f, FontWeight.SemiBold, activePalette.textPrimary))
                    }
                }

                // In-App Liquid Glass Action Bottom Sheets
                CorvixBottomSheetsHost(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Exit App Confirmation Prompt
                if (showExitDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99050811))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showExitDialog = false }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassSurface(
                            strong = true,
                            cornerRadius = 28.dp,
                            contentPadding = PaddingValues(20.dp),
                            modifier = Modifier
                                .widthIn(max = 380.dp)
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Exit Corvix?",
                                    style = font.style(18f, FontWeight.Bold, activePalette.textPrimary)
                                )
                                Text(
                                    text = "Are you sure you want to exit the app?",
                                    style = font.style(13f, FontWeight.Normal, activePalette.textMuted)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassPill(
                                        onClick = { showExitDialog = false },
                                        modifier = Modifier.weight(1f),
                                        padding = PaddingValues(vertical = 11.dp)
                                    ) {
                                        Text("Cancel", style = font.style(13f, FontWeight.SemiBold, activePalette.textPrimary))
                                    }
                                    GlassPill(
                                        onClick = {
                                            showExitDialog = false
                                            (context as? Activity)?.finish()
                                        },
                                        active = true,
                                        modifier = Modifier.weight(1f),
                                        padding = PaddingValues(vertical = 11.dp)
                                    ) {
                                        Text("Exit App", style = font.style(13f, FontWeight.SemiBold, activePalette.activePillContent))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}




