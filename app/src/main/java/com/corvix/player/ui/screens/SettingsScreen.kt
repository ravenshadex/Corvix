package com.corvix.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.AppSkin
import com.corvix.player.model.AppTypography
import com.corvix.player.model.AutoEqProfile
import com.corvix.player.model.BtCodec
import com.corvix.player.model.EqPreset
import com.corvix.player.model.GlassThemePreset
import com.corvix.player.model.GlassTone
import com.corvix.player.model.ReverbSpace
import com.corvix.player.model.SeekBarStyle
import com.corvix.player.model.SheetType
import com.corvix.player.ui.components.CorvixHorizontalSlider
import com.corvix.player.ui.components.CorvixSeekSlider
import com.corvix.player.ui.components.CorvixVerticalEqSlider
import com.corvix.player.ui.components.GlassPill
import com.corvix.player.ui.components.GlassSectionDivider
import com.corvix.player.ui.components.GlassSurface
import com.corvix.player.ui.components.GlassToggleSwitch
import com.corvix.player.ui.components.IpodListEntry
import com.corvix.player.ui.components.IpodRightGraphic
import com.corvix.player.ui.components.IpodSplitScreenScaffold
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    onRequestRescanPermission: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val listState = rememberLazyListState()
    val bottomClearance = if (palette.isIpod) 14.dp else metrics.dockBottomClearance

    if (palette.isIpod) {
        var showSkinPicker by remember { mutableStateOf(false) }
        if (showSkinPicker) {
            val skinEntries = listOf(
                IpodListEntry(
                    title = "← Back",
                    showChevron = false,
                    onClick = { showSkinPicker = false }
                )
            ) + AppSkin.entries.map { skin ->
                IpodListEntry(
                    title = skin.title,
                    trailingValue = if (state.appSkin == skin) "✓" else "",
                    showChevron = false,
                    onClick = {
                        showSkinPicker = false
                        viewModel.setAppSkin(skin)
                    }
                )
            }
            IpodSplitScreenScaffold(
                headerTitle = "Choose Skin",
                isPlaying = state.isPlaying,
                entries = skinEntries,
                focusedIndex = state.ipodFocusIndex.coerceIn(0, skinEntries.lastIndex),
                rightGraphic = IpodRightGraphic.GEAR,
                fallbackTrack = state.currentTrack,
                modifier = modifier
            )
            return
        }

        val entries = listOf(
            IpodListEntry(
                title = "About",
                trailingValue = "${state.indexedTrackCount} tracks",
                onClick = { viewModel.triggerIpodSettingsRow(0, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Skin",
                trailingValue = state.appSkin.title,
                onClick = { showSkinPicker = true }
            ),
            IpodListEntry(
                title = "Shuffle",
                trailingValue = if (state.isShuffle) "Songs" else "Off",
                onClick = { viewModel.triggerIpodSettingsRow(2, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Repeat",
                trailingValue = state.repeatMode.badge,
                onClick = { viewModel.triggerIpodSettingsRow(3, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "EQ",
                trailingValue = if (state.eqEnabled) state.activeEqPreset.label else "Off",
                onClick = { viewModel.triggerIpodSettingsRow(4, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Speed",
                trailingValue = state.speedLabel,
                onClick = { viewModel.triggerIpodSettingsRow(5, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Gapless",
                trailingValue = if (state.gaplessPlayback) "On" else "Off",
                onClick = { viewModel.triggerIpodSettingsRow(6, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Rescan Library",
                trailingValue = if (state.isRescanning) "..." else "›",
                onClick = { viewModel.triggerIpodSettingsRow(7, onRequestRescanPermission) }
            ),
            IpodListEntry(
                title = "Reset All Settings",
                showChevron = false,
                onClick = { viewModel.triggerIpodSettingsRow(8, onRequestRescanPermission) }
            )
        )
        IpodSplitScreenScaffold(
            headerTitle = "Settings",
            isPlaying = state.isPlaying,
            entries = entries,
            focusedIndex = state.ipodFocusIndex,
            rightGraphic = IpodRightGraphic.GEAR,
            fallbackTrack = state.currentTrack,
            modifier = modifier
        )
        return
    }

    var themeOpen by remember { mutableStateOf(false) }
    var skinOpen by remember { mutableStateOf(false) }
    var fontOpen by remember { mutableStateOf(false) }
    var seekOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.scrollToEqSignal) {
        if (state.scrollToEqSignal > 0) {
            listState.animateScrollToItem(6)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = metrics.horizontalPadding, end = metrics.horizontalPadding, top = 8.dp, bottom = bottomClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Quick Tone Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Settings", style = font.style(22f, FontWeight.Bold, palette.textPrimary))
                    Text("Liquid themes, typography, scan, EQ & widgets", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassTone.entries.forEach { tone ->
                        val active = state.glassTone == tone
                        GlassPill(
                            onClick = { viewModel.setGlassTone(tone) },
                            active = active,
                            modifier = Modifier.weight(1f),
                            padding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(
                                text = tone.label,
                                style = font.style(11f, FontWeight.SemiBold, if (active) palette.activePillContent else palette.textPrimary)
                            )
                        }
                    }
                }
            }
        }

        // 1. Liquid Glass Theme Dropdown Card
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Liquid Glass Theme", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("Select refraction shader", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { themeOpen = !themeOpen }, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(state.themePreset.title, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                            Icon(if (themeOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    AnimatedVisibility(visible = themeOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassSectionDivider()
                            GlassThemePreset.entries.forEach { preset ->
                                val active = state.themePreset == preset
                                GlassPill(
                                    onClick = {
                                        viewModel.setThemePreset(preset)
                                        themeOpen = false
                                    },
                                    active = active,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = "${preset.title} — ${preset.subtitle}",
                                            style = font.style(11.5f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary)
                                        )
                                        if (active) Text("✓", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
                                    }
                                }
                            }
                        }
                    }

                    GlassSectionDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Adapt Glass Color to Album Art", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                        GlassToggleSwitch(checked = state.adaptiveAlbumColor, onCheckedChange = { viewModel.setAdaptiveAlbumColor(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AMOLED True Black Mode", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                        GlassToggleSwitch(
                            checked = state.glassTone == GlassTone.AMOLED,
                            onCheckedChange = { viewModel.setGlassTone(if (it) GlassTone.AMOLED else GlassTone.DARK) }
                        )
                    }

                    GlassSectionDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val lg = state.liquidGlassConfig
                        var showGlassEngine by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showGlassEngine = !showGlassEngine }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Liquid Glass Engine", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                                Text(
                                    text = if (showGlassEngine) "Tap to collapse shader controls" else "Tap to tune blur, refraction & rim light",
                                    style = font.style(10.5f, FontWeight.Normal, palette.textMuted)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (showGlassEngine) {
                                    GlassPill(
                                        onClick = { viewModel.resetLiquidGlassConfig() },
                                        padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Reset", style = font.style(10.5f, FontWeight.SemiBold, palette.textPrimary))
                                    }
                                }
                                GlassPill(
                                    onClick = { showGlassEngine = !showGlassEngine },
                                    active = showGlassEngine,
                                    padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showGlassEngine) "Hide" else "Tune",
                                        style = font.style(10.5f, FontWeight.SemiBold, if (showGlassEngine) palette.activePillContent else palette.textPrimary)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showGlassEngine) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SliderRow("Color Opacity", "${(lg.colorOpacity * 100).roundToInt()}%", lg.colorOpacity, 0f..1.00f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(colorOpacity = v) }
                                }
                                SliderRow("Contrast Boost", "%.2fx".format(lg.contrastBoost), lg.contrastBoost, 0.50f..1.60f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(contrastBoost = v) }
                                }
                                SliderRow("Rim Light Strength", "${(lg.rimLightStrength * 100).roundToInt()}%", lg.rimLightStrength, 0f..1f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(rimLightStrength = v) }
                                }
                                SliderRow("Rim Light Rotation", "${lg.rimLightRotationDeg.roundToInt()}°", lg.rimLightRotationDeg, 0f..360f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(rimLightRotationDeg = v) }
                                }
                                SliderRow("Soft Rim Strength", "${(lg.softRimStrength * 100).roundToInt()}%", lg.softRimStrength, 0f..0.60f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(softRimStrength = v) }
                                }
                                SliderRow("Soft Rim Width", "%.1f dp".format(lg.softRimWidthDp), lg.softRimWidthDp, 0.4f..3.0f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(softRimWidthDp = v) }
                                }
                                SliderRow("Glass Blur", "${state.glassBlurPx.roundToInt()} px", state.glassBlurPx, 0f..100f) { v ->
                                    viewModel.setBlurPx(v)
                                }
                                SliderRow("Refraction Strength", "%.1f".format(lg.refractionStrength), lg.refractionStrength, 0f..8f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(refractionStrength = v) }
                                }
                                SliderRow("Refraction Width", "${(lg.refractionWidth * 100).roundToInt()}%", lg.refractionWidth, 0.02f..0.35f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(refractionWidth = v) }
                                }
                                SliderRow("Chromatic Aberration", "%.1f".format(lg.chromaticAberration), lg.chromaticAberration, 0f..3f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(chromaticAberration = v) }
                                }
                                SliderRow("Shadow Strength", "${(lg.shadowStrength * 100).roundToInt()}%", lg.shadowStrength, 0f..1f) { v ->
                                    viewModel.updateLiquidGlassConfig { it.copy(shadowStrength = v) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1B. Player Skin (Hardware Shell & Navigation Mode — Separate from Glass Theme)
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Skin", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("Hardware shell & player UI", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { skinOpen = !skinOpen }, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(state.appSkin.title, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                            Icon(if (skinOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    AnimatedVisibility(visible = skinOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassSectionDivider()
                            AppSkin.entries.forEach { skin ->
                                val active = state.appSkin == skin
                                GlassPill(
                                    onClick = {
                                        viewModel.setAppSkin(skin)
                                        skinOpen = false
                                    },
                                    active = active,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = "${skin.title} — ${skin.subtitle}",
                                            style = font.style(11.5f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary)
                                        )
                                        if (active) Text("✓", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. App Typography / Font (11 Fonts)
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("App Typography / Font", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("11 offline system fonts", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { fontOpen = !fontOpen }, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(state.typography.badgeLabel, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                            Icon(if (fontOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    AnimatedVisibility(visible = fontOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassSectionDivider()
                            AppTypography.entries.forEach { typo ->
                                val active = state.typography == typo
                                GlassPill(
                                    onClick = {
                                        viewModel.setTypography(typo)
                                        fontOpen = false
                                    },
                                    active = active,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(typo.menuLabel, style = font.style(11.5f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                                        if (active) Text("✓", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2B. Playback Seek Slider (5 Styles + Live Preview isolated to leaf when visible)
        item(key = "seek_style", contentType = "settings_card") {
            val dur = state.currentTrack.durationSec.coerceAtLeast(1)

            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Playback Seek Slider", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("5 interactive progress bar styles", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { seekOpen = !seekOpen }, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(state.seekBarStyle.title, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                            Icon(if (seekOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    SettingsLiveSeekPreview(
                        viewModel = viewModel,
                        dur = dur,
                        seekStyle = state.seekBarStyle,
                        isPlaying = state.isPlaying && isVisible,
                        collectFlow = isVisible
                    )

                    AnimatedVisibility(visible = seekOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassSectionDivider()
                            SeekBarStyle.entries.forEachIndexed { idx, style ->
                                val active = state.seekBarStyle == style
                                GlassPill(
                                    onClick = {
                                        viewModel.setSeekBarStyle(style)
                                        seekOpen = false
                                    },
                                    active = active,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${idx + 1}. ${style.title} — ${style.subtitle}", style = font.style(11.5f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                                        if (active) Text("✓", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Library Scan & Indexing
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Library Scan & Indexing", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                        Text(state.scanStatusSub, style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                    }
                    GlassPill(
                        onClick = onRequestRescanPermission,
                        active = true,
                        padding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = palette.activePillContent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(5.dp))
                        Text(if (state.isRescanning) "Scanning..." else "Rescan", style = font.style(11.5f, FontWeight.SemiBold, palette.activePillContent))
                    }
                }
            }
        }

        // 4. Equalizer & Audio Effects (index 6 for quick-scroll!)
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Equalizer & Audio Effects", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("FLAC · WAV · ALAC · APE · DSD · MP3 · OPUS · OGG", style = font.style(11f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassToggleSwitch(checked = state.eqEnabled, onCheckedChange = { viewModel.setEqEnabled(it) })
                    }

                    // EQ Preset Chips + 5/10-Band Mode Pill
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            EqPreset.entries.forEach { preset ->
                                val active = state.activeEqPreset == preset
                                GlassPill(onClick = { viewModel.applyEqPreset(preset) }, active = active, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(preset.label, style = font.style(11f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                                }
                            }
                        }
                        GlassPill(onClick = { viewModel.toggleEqMode() }, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(if (state.eq10Band) "10-Band" else "5-Band", style = font.style(10.5f, FontWeight.SemiBold, palette.textPrimary))
                        }
                    }

                    // Primary 5-Band EQ Sliders
                    val primaryLabels = listOf("60Hz", "230", "910", "3.6k", "14k")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        primaryLabels.forEachIndexed { i, lbl ->
                            val v = state.eqBandsDb[i]
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (v > 0) "+$v" else "$v", style = font.style(11f, FontWeight.Normal, palette.textPrimary, mono = true))
                                CorvixVerticalEqSlider(valueDb = v, onValueChange = { viewModel.updateEqBand(i, it) })
                                Text(lbl, style = font.style(10.5f, FontWeight.Normal, palette.textMuted))
                            }
                        }
                    }

                    // Extra 5 Parametric Bands (Expanded in 10-Band Mode)
                    AnimatedVisibility(visible = state.eq10Band) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassSectionDivider()
                            val extraLabels = listOf("31Hz", "125", "500", "1.8k", "8kHz")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                extraLabels.forEachIndexed { offset, lbl ->
                                    val bandIdx = 5 + offset
                                    val v = state.eqBandsDb[bandIdx]
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (v > 0) "+$v" else "$v", style = font.style(11f, FontWeight.Normal, palette.textPrimary, mono = true))
                                        CorvixVerticalEqSlider(valueDb = v, onValueChange = { viewModel.updateEqBand(bandIdx, it) })
                                        Text(lbl, style = font.style(10.5f, FontWeight.Normal, palette.textMuted))
                                    }
                                }
                            }
                        }
                    }

                    // Reverb Environment Selector
                    GlassSectionDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reverb Space", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                            Text(state.reverbSpace.fullName, style = font.style(11.5f, FontWeight.SemiBold, palette.accentText))
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ReverbSpace.entries.forEach { rev ->
                                val active = state.reverbSpace == rev
                                GlassPill(onClick = { viewModel.setReverbSpace(rev) }, active = active, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(rev.chipLabel, style = font.style(11f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                                }
                            }
                        }
                    }

                    // AutoEQ Headphone Curve Matcher
                    GlassSectionDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AutoEQ Curve Matcher", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                            Text(state.autoEqProfile.fullName, style = font.style(11.5f, FontWeight.SemiBold, palette.accentText))
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            AutoEqProfile.entries.forEach { prof ->
                                val active = state.autoEqProfile == prof
                                GlassPill(onClick = { viewModel.applyAutoEq(prof) }, active = active, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(prof.chipLabel, style = font.style(11f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                                }
                            }
                        }
                    }

                    // Bass Boost, Virtualizer, Crossfade, Gapless, ReplayGain
                    GlassSectionDivider()
                    SliderRow("Bass Boost", "${state.bassBoostPct}%", state.bassBoostPct.toFloat(), 0f..100f) {
                        viewModel.setBassBoost(it.roundToInt())
                    }
                    SliderRow("3D Soundstage / Virtualizer", "${state.virtualizerPct}%", state.virtualizerPct.toFloat(), 0f..100f) {
                        viewModel.setVirtualizer(it.roundToInt())
                    }
                    SliderRow("Crossfade Duration", "%.1fs".format(state.crossfadeSec), state.crossfadeSec, 0f..12f) {
                        viewModel.setCrossfade((it * 2f).roundToInt() / 2f)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Gapless Playback & DSD-over-PCM", style = font.style(12f, FontWeight.Normal, palette.textPrimary))
                        GlassToggleSwitch(checked = state.gaplessPlayback, onCheckedChange = { viewModel.setGapless(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ReplayGain Tag Normalization", style = font.style(12f, FontWeight.Normal, palette.textPrimary))
                        GlassToggleSwitch(checked = state.replayGainEnabled, onCheckedChange = { viewModel.setReplayGain(it) })
                    }
                }
            }
        }

        // 5. Hi-Res Output & DAC Controls
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hi-Res Output & DAC", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text(
                                text = if (state.bitPerfectDac) "Bit-Perfect · 32-bit / 384 kHz" else "Android Mixer · 16-bit / 48 kHz",
                                style = font.style(11.5f, FontWeight.Normal, palette.textMuted)
                            )
                        }
                        GlassPill(onClick = { viewModel.openSheet(SheetType.SIGNAL) }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(
                                text = state.activeSink.shortName,
                                maxLines = 1,
                                style = font.style(10.5f, FontWeight.SemiBold, palette.accentText)
                            )
                        }
                    }
                    GlassSectionDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Bit-Perfect USB DAC Passthrough", style = font.style(12f, FontWeight.Normal, palette.textPrimary))
                        GlassToggleSwitch(checked = state.bitPerfectDac, onCheckedChange = { viewModel.setBitPerfectDac(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bluetooth Audio Codec", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        Text(state.btCodec.statusLabel, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary, mono = true))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BtCodec.entries.forEach { codec ->
                            val active = state.btCodec == codec
                            GlassPill(onClick = { viewModel.setBtCodec(codec) }, active = active, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(codec.chipLabel, style = font.style(11f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                            }
                        }
                    }
                }
            }
        }

        // 6. Utilities & Offline-Only Privacy
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Utilities & Offline Privacy", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("Zero internet permission · Ad-free", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = {}, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Air-Gapped", style = font.style(10.5f, FontWeight.SemiBold, Color(0xFF34D399)))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill(onClick = { viewModel.openRingtoneCutter() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 9.dp)) {
                            Text("✂ Ringtone Cutter", style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                        }
                        GlassPill(onClick = { viewModel.openSheet(SheetType.SLEEP) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 9.dp)) {
                            Text("⏱ Sleep Timer", style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                        }
                    }
                    GlassSectionDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Enforce Offline-Only (Block LAN)", style = font.style(12f, FontWeight.Normal, palette.textPrimary))
                        GlassToggleSwitch(checked = state.offlinePrivacyLock, onCheckedChange = { viewModel.setOfflinePrivacyLock(it) })
                    }
                }
            }
        }

        // 7. Android Auto, Cast & LAN
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Android Auto, Cast & LAN", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("Output: ${state.castDeviceLabel}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { viewModel.openSheet(SheetType.AUTO) }, active = true, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Car UI", style = font.style(11.5f, FontWeight.SemiBold, palette.activePillContent))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Phone" to "This Phone Speaker",
                            "Chromecast" to "Living Room TV (Chromecast)",
                            "Hi-Fi Cast" to "Studio DAC Cast"
                        ).forEach { (chip, full) ->
                            val active = state.castDeviceLabel == full
                            GlassPill(onClick = { viewModel.setCastDevice(full) }, active = active, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(chip, style = font.style(11f, if (active) FontWeight.SemiBold else FontWeight.Normal, if (active) palette.activePillContent else palette.textPrimary))
                            }
                        }
                    }
                    GlassSectionDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                state.offlinePrivacyLock -> "Plex / Jellyfin LAN (Blocked by Privacy)"
                                state.lanServerConnected -> "Connected: Jellyfin (192.168.1.40:8096)"
                                else -> "Plex / Jellyfin LAN Streaming"
                            },
                            style = font.style(11.5f, FontWeight.Normal, palette.textMuted),
                            modifier = Modifier.weight(1f)
                        )
                        GlassToggleSwitch(
                            checked = state.lanServerConnected,
                            enabled = !state.offlinePrivacyLock,
                            onCheckedChange = { viewModel.setLanServerConnected(it) }
                        )
                    }
                }
            }
        }

        // 8. Home Screen Widget Config
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Home Screen Widgets", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text("Liquid Glass & Winamp Classic widgets", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { viewModel.navigate(AppScreen.WIDGET) }, active = true, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Add / Preview", style = font.style(11.5f, FontWeight.SemiBold, palette.activePillContent))
                        }
                    }
                    GlassSectionDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Codec Badge on Widget", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                        GlassToggleSwitch(checked = state.showWidgetCodecBadge, onCheckedChange = { viewModel.setShowWidgetCodecBadge(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
            Text(valueText, style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary, mono = true))
        }
        CorvixHorizontalSlider(value = value, valueRange = valueRange, onValueChange = onValueChange)
    }
}

@Composable
private fun SettingsLiveSeekPreview(
    viewModel: CorvixViewModel,
    dur: Int,
    seekStyle: SeekBarStyle,
    isPlaying: Boolean,
    collectFlow: Boolean
) {
    val elapsedSec = if (collectFlow) {
        viewModel.elapsedSecFlow.collectAsState().value
    } else {
        0
    }
    val frac = (elapsedSec.toFloat() / dur.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    CorvixSeekSlider(
        valueFraction = frac,
        onValueFractionChange = { newFrac -> viewModel.seekTo((newFrac * dur).roundToInt()) },
        seekStyle = seekStyle,
        isPlaying = isPlaying
    )
}
