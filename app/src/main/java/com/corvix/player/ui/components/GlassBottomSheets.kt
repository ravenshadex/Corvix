package com.corvix.player.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corvix.player.audio.ArtworkCandidate
import com.corvix.player.audio.LyricsCandidate
import com.corvix.player.model.AppScreen
import com.corvix.player.model.AudioSinkInfo
import com.corvix.player.model.CorvixSampleData
import com.corvix.player.model.SheetType
import com.corvix.player.model.formatSeconds
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CorvixBottomSheetsHost(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    if (state.activeSheet == SheetType.NONE) return

    val metrics = LocalCorvixMetrics.current
    val palette = LocalCorvixPalette.current

    val lrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importLrcUri(uri)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.applyLocalCustomArtUri(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x94050811))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.closeSheet() }
            .imePadding()
            .padding(metrics.horizontalPadding.coerceAtMost(14.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassSurface(
            strong = true,
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = metrics.heightDp * 0.85f)
                .then(
                    if (state.activeSheet == SheetType.LYRICS) {
                        Modifier.background(
                            color = if (palette.isDark) Color(0xF00B0E14) else Color(0xF5F4F7FC),
                            shape = RoundedCornerShape(28.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (state.activeSheet) {
                    SheetType.TAGS -> TagEditorSheet(state, viewModel)
                    SheetType.RINGTONE -> RingtoneCutterSheet(state, viewModel)
                    SheetType.SLEEP -> SleepTimerSheet(state, viewModel)
                    SheetType.AUTO -> AndroidAutoSheet(state, viewModel)
                    SheetType.SIGNAL -> SignalPathSheet(state, viewModel)
                    SheetType.LYRICS -> LyricsManagerSheet(
                        state = state,
                        viewModel = viewModel,
                        onPickLocalFile = { lrcPicker.launch(arrayOf("text/*", "application/*")) }
                    )
                    SheetType.NP_MENU -> NowPlayingMenuSheet(state, viewModel)
                    SheetType.TRACK_ACTIONS -> TrackActionsSheet(state, viewModel)
                    SheetType.ARTWORK_SEARCH -> ArtworkSearchSheet(
                        state = state,
                        viewModel = viewModel,
                        onPickLocalImage = { imagePicker.launch("image/*") }
                    )
                    SheetType.PLAYLIST_PICKER -> PlaylistPickerSheet(state, viewModel)
                    SheetType.CREATE_PLAYLIST -> CreatePlaylistSheet(viewModel)
                    SheetType.RENAME_PLAYLIST -> RenamePlaylistSheet(state, viewModel)
                    SheetType.SORT_ORDER -> SortOrderSheet(state, viewModel)
                    SheetType.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun SortOrderSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val orders = com.corvix.player.model.LibrarySortOrder.entries

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort Library", style = font.style(15f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }

        orders.forEachIndexed { idx, order ->
            val selected = state.librarySortOrder == order
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.setLibrarySortOrder(order)
                        viewModel.closeSheet()
                    }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.label,
                    style = font.style(
                        13.5f,
                        if (selected) FontWeight.Bold else FontWeight.Medium,
                        if (selected) palette.accentText else palette.textPrimary
                    )
                )
                if (selected) {
                    Text("✓", style = font.style(14f, FontWeight.Bold, palette.accentText))
                }
            }
            if (idx < orders.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(1.dp)
                        .background(palette.divider)
                )
            }
        }
    }
}

@Composable
private fun TagEditorSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val track = state.tracks.getOrElse(state.editingTrackIndex) { state.currentTrack }
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var genre by remember(track.id) { mutableStateOf(track.genre) }
    var trackNo by remember(track.id) { mutableStateOf("01 / 12") }
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Edit ID3 / Vorbis Tags", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Done", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        SheetInputField(value = title, onValueChange = { title = it }, placeholder = "Title")
        SheetInputField(value = artist, onValueChange = { artist = it }, placeholder = "Artist")
        SheetInputField(value = album, onValueChange = { album = it }, placeholder = "Album")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetInputField(value = genre, onValueChange = { genre = it }, placeholder = "Genre", modifier = Modifier.weight(1f))
            SheetInputField(value = trackNo, onValueChange = { trackNo = it }, placeholder = "Track #", mono = true, modifier = Modifier.weight(1f))
        }
        GlassPill(
            onClick = { viewModel.saveTagEdits(title, artist, album, genre) },
            active = true,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = 10.dp)
        ) {
            Text("Save Embedded Tags", style = font.style(12f, FontWeight.SemiBold, palette.activePillContent))
        }
    }
}

@Composable
private fun RingtoneCutterSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Ringtone Cutter", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
                Text(state.currentTrack.title, style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Clip Window", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
            Text(viewModel.formattedRingtoneWindow(), style = font.style(11.5f, FontWeight.SemiBold, palette.accentText, mono = true))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trim Start", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                Text(formatSeconds(state.ringtoneStartSec), style = font.style(11.5f, FontWeight.Medium, palette.textPrimary, mono = true))
            }
            CorvixHorizontalSlider(
                value = state.ringtoneStartSec.toFloat(),
                valueRange = 0f..180f,
                onValueChange = { viewModel.updateRingtoneBounds(it.toInt(), state.ringtoneEndSec) }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trim End", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                Text(formatSeconds(state.ringtoneEndSec), style = font.style(11.5f, FontWeight.Medium, palette.textPrimary, mono = true))
            }
            CorvixHorizontalSlider(
                value = state.ringtoneEndSec.toFloat(),
                valueRange = 10f..240f,
                onValueChange = { viewModel.updateRingtoneBounds(state.ringtoneStartSec, it.toInt()) }
            )
        }
        GlassPill(
            onClick = { viewModel.saveRingtone() },
            active = true,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = 10.dp)
        ) {
            Text(state.ringtoneSaveLabel, style = font.style(12f, FontWeight.SemiBold, palette.activePillContent))
        }
    }
}

@Composable
private fun SleepTimerSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val options = listOf("Off" to "Off", "15m" to "15 min", "30m" to "30 min", "45m" to "45 min", "60m" to "60 min", "End" to "End Track")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sleep Timer", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        options.chunked(3).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (key, label) ->
                    val active = (key == "Off" && state.sleepTimerLabel == "Timer") || state.sleepTimerLabel == key
                    GlassPill(
                        onClick = { viewModel.setSleepTimer(key) },
                        active = active,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        padding = PaddingValues(vertical = 9.dp)
                    ) {
                        Text(label, style = font.style(11.5f, if (active) FontWeight.SemiBold else FontWeight.Medium, if (active) palette.activePillContent else palette.textPrimary))
                    }
                }
            }
        }
        GlassSectionDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart Sleep Fade (60s LPF)", style = font.style(12f, FontWeight.SemiBold, palette.textPrimary))
                Text("Gradual low-pass taper & volume fade", style = font.style(10.5f, FontWeight.Normal, palette.textMuted))
            }
            GlassToggleSwitch(checked = state.sleepSmartLpf, onCheckedChange = { viewModel.setSleepSmartLpf(it) })
        }
    }
}

@Composable
private fun AndroidAutoSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF34D399)))
                Text("Android Auto Dashboard", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Exit Car UI", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        GlassSurface(cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkBox(
                    artStyle = state.currentTrack.artStyle,
                    track = state.currentTrack,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(54.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.currentTrack.title, style = font.style(14f, FontWeight.Bold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state.currentTrack.artist} · ${state.currentTrack.shortFormat}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill(onClick = { viewModel.prevTrack() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 12.dp)) {
                Text("⏮ Prev", style = font.style(12f, FontWeight.Bold, palette.textPrimary))
            }
            GlassPill(onClick = { viewModel.togglePlay() }, active = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 12.dp)) {
                Text(if (state.isPlaying) "⏸ Pause" else "▶ Play", style = font.style(12f, FontWeight.Bold, palette.activePillContent))
            }
            GlassPill(onClick = { viewModel.nextTrack() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f), padding = PaddingValues(vertical = 12.dp)) {
                Text("Next ⏭", style = font.style(12f, FontWeight.Bold, palette.textPrimary))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill(
                onClick = {
                    viewModel.shuffleAll()
                    viewModel.closeSheet()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                padding = PaddingValues(vertical = 9.dp)
            ) {
                Text("Voice: Shuffle Mix", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
            GlassPill(
                onClick = {
                    viewModel.navigate(AppScreen.FOLDERS)
                    viewModel.closeSheet()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                padding = PaddingValues(vertical = 9.dp)
            ) {
                Text("Drive Folders", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
    }
}

@Composable
private fun SignalPathSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Bit-Perfect Signal Path", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Done", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        GlassSurface(cornerRadius = 16.dp, contentPadding = PaddingValues(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Source: ${state.currentTrack.formatLabel} (${state.currentTrack.bpm} BPM)", style = font.style(11f, FontWeight.Normal, palette.textPrimary, mono = true))
                Text("DSP: Biquad EQ · ReplayGain · Direct Float", style = font.style(11f, FontWeight.Normal, palette.textMuted, mono = true))
                Text("Output: ${state.activeSink.name} · ${state.activeSink.detail}", style = font.style(11f, FontWeight.SemiBold, palette.accentText, mono = true))
            }
        }
        Text("AUDIODEVICECALLBACK OUTPUT SINKS", style = font.style(10.5f, FontWeight.SemiBold, palette.textMuted))
        CorvixSampleData.outputSinks.forEach { sink: AudioSinkInfo ->
            val active = sink.name == state.activeSink.name
            GlassPill(
                onClick = { viewModel.setAudioSink(sink) },
                active = active,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sink.name, style = font.style(11.5f, FontWeight.Medium, if (active) palette.activePillContent else palette.textPrimary))
                    Text(sink.rateLabel, style = font.style(11f, FontWeight.Normal, if (active) palette.activePillContent else palette.textMuted, mono = true))
                }
            }
        }
    }
}

@Composable
private fun LyricsManagerSheet(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    onPickLocalFile: () -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val track = state.currentTrack
    var rawText by remember { mutableStateOf("") }
    var searchQuery by remember(track.id) { mutableStateOf("${track.title} ${track.artist}".trim()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lyrics Manager", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
                Text("${track.title} — ${track.artist}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Done", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }

        // English / Romanized Lyrics Preference Toggle
        GlassSurface(cornerRadius = 16.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "English / Romanized Lyrics Only",
                        style = font.style(12f, FontWeight.SemiBold, palette.textPrimary)
                    )
                    Text(
                        text = "Prioritizes English results & romanizes Tamil / Hindi / Telugu / Malayalam",
                        style = font.style(10.5f, FontWeight.Normal, palette.textMuted)
                    )
                }
                GlassToggleSwitch(
                    checked = state.englishLyricsOnly,
                    onCheckedChange = { viewModel.toggleEnglishLyricsOnly() }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill(
                onClick = { viewModel.switchLyricsTab(false) },
                active = !state.lyricsSheetTabIsUpload,
                modifier = Modifier.weight(1f),
                padding = PaddingValues(vertical = 7.dp)
            ) {
                Text("🔍 Fetch & Select", style = font.style(11f, FontWeight.SemiBold, if (!state.lyricsSheetTabIsUpload) palette.activePillContent else palette.textPrimary))
            }
            GlassPill(
                onClick = { viewModel.switchLyricsTab(true) },
                active = state.lyricsSheetTabIsUpload,
                modifier = Modifier.weight(1f),
                padding = PaddingValues(vertical = 7.dp)
            ) {
                Text("↑ Manual Upload", style = font.style(11f, FontWeight.SemiBold, if (state.lyricsSheetTabIsUpload) palette.activePillContent else palette.textPrimary))
            }
        }

        if (state.lyricsSheetTabIsUpload) {
            GlassPill(onClick = onPickLocalFile, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 9.dp)) {
                Text("Choose Local .lrc / .txt File", style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
            }
            SheetInputField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = "[00:18] Paste .lrc or plain lyric lines...",
                mono = true,
                singleLine = false
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(
                    onClick = { viewModel.saveManualLyrics(rawText) },
                    active = true,
                    modifier = Modifier.weight(1f),
                    padding = PaddingValues(vertical = 9.dp)
                ) {
                    Text("Save Locally", style = font.style(11.5f, FontWeight.SemiBold, palette.activePillContent))
                }
                GlassPill(onClick = { viewModel.clearTrackLyrics() }, padding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) {
                    Text("Clear", style = font.style(11f, FontWeight.Medium, palette.textPrimary))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SheetInputField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Song title & artist...",
                    modifier = Modifier.weight(1f)
                )
                GlassPill(
                    onClick = { if (!state.isSearchingLyrics) viewModel.searchLyricsCandidates(searchQuery) },
                    active = true,
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = palette.activePillContent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isSearchingLyrics) "..." else "Find", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
                }
            }

            Text(state.lyricsFetchStatus, style = font.style(11f, FontWeight.Normal, palette.textMuted))

            if (state.lyricsCandidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    state.lyricsCandidates.forEach { candidate ->
                        LyricsCandidateRow(
                            candidate = candidate,
                            englishMode = state.englishLyricsOnly,
                            onSelect = { viewModel.selectLyricsCandidate(candidate) }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(
                    onClick = { if (!state.isFetchingLyrics) viewModel.fetchLyricsFromSite("LRCLIB (Synced .lrc)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Auto LRCLIB", style = font.style(11f, FontWeight.SemiBold, palette.textPrimary))
                }
                GlassPill(
                    onClick = { if (!state.isFetchingLyrics) viewModel.fetchLyricsFromSite("Lyrics.ovh") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Auto Lyrics.ovh", style = font.style(11f, FontWeight.SemiBold, palette.textPrimary))
                }
                GlassPill(
                    onClick = { viewModel.clearTrackLyrics() },
                    shape = RoundedCornerShape(14.dp),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Clear", style = font.style(11f, FontWeight.Medium, palette.textMuted))
                }
            }
        }
    }
}

@Composable
private fun LyricsCandidateRow(
    candidate: LyricsCandidate,
    englishMode: Boolean,
    onSelect: () -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val scriptTag = when {
        !candidate.hasNonLatinScript -> "Latin / ENG"
        englishMode -> "Transliterated EN"
        else -> "Original Script"
    }
    GlassSurface(
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${candidate.trackName} — ${candidate.artistName}",
                    style = font.style(12f, FontWeight.Bold, palette.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                Text(
                    text = if (candidate.isSynced) "SYNCED" else "PLAIN",
                    style = font.style(9.5f, FontWeight.Bold, palette.accentText, mono = true)
                )
            }
            Text(
                text = "${candidate.sourceLabel} · $scriptTag · ${formatSeconds(candidate.durationSec)}",
                style = font.style(10.5f, FontWeight.Medium, palette.accentText)
            )
            if (candidate.previewText.isNotBlank()) {
                Text(
                    text = candidate.previewText,
                    style = font.style(11f, FontWeight.Normal, palette.textMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NowPlayingMenuSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val track = state.currentTrack
    val isLiked = track.id in state.likedTrackIds
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Track Options", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        val items = listOf(
            Triple("🎚 Equalizer & Audio Effects", "›") {
                viewModel.openSettingsEq()
                viewModel.closeSheet()
            },
            Triple("🖼 Search & Change Album Art", "›") {
                viewModel.openArtworkSearch(state.currentTrackIndex)
            },
            Triple("🌐 Fetch / Re-Select Lyrics", "›") {
                viewModel.openLyricsSheet(false)
            },
            Triple("📁 Add to Playlist", "›") {
                viewModel.openPlaylistPickerForTrack(track.id)
            },
            Triple(if (isLiked) "♥ Remove from Favorites" else "♡ Add to Favorites", "›") {
                viewModel.toggleFavoriteTrack(track.id)
                viewModel.closeSheet()
            },
            Triple("📤 Share Song Card", "›") {
                viewModel.shareSongCard(track.id)
            },
            Triple("✎ Edit ID3 Tags", "›") {
                viewModel.openSheet(SheetType.TAGS)
            },
            Triple("✂ Cut Ringtone", "›") {
                viewModel.openRingtoneCutter()
            },
            Triple("⏱ Sleep Timer", "›") {
                viewModel.openSheet(SheetType.SLEEP)
            }
        )
        items.forEachIndexed { idx, (label, trail, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = action)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = font.style(13.5f, FontWeight.Medium, palette.textPrimary))
                Text(trail, style = font.style(14f, FontWeight.Medium, palette.textMuted))
            }
            if (idx < items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(1.dp)
                        .background(palette.divider)
                )
            }
        }
    }
}

@Composable
private fun TrackActionsSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val track = state.contextTrack
    val isFav = track.id in state.likedTrackIds

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkBox(
                    artStyle = track.artStyle,
                    track = track,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(46.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = font.style(14f, FontWeight.Bold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.artist} · ${track.album}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }

        val actionItems = listOf(
            "▶  Play Next" to { viewModel.playNext(track.id) },
            "➕ Add to Queue" to {
                viewModel.addToQueue(track.id)
                viewModel.closeSheet()
            },
            "📁 Add to Playlist" to { viewModel.openPlaylistPickerForTrack(track.id) },
            (if (isFav) "♥  Remove from Favorites" else "♡  Add to Favorites") to {
                viewModel.toggleFavoriteTrack(track.id)
                viewModel.closeSheet()
            },
            "📤 Share Song Card" to { viewModel.shareSongCard(track.id) },
            "🖼 Search & Change Album Art" to { viewModel.openArtworkSearch(state.contextTrackIndex) },
            "📝 Search & Select Lyrics" to {
                viewModel.playTrack(state.contextTrackIndex)
                viewModel.openLyricsSheet(false)
            },
            "✎  Edit ID3 Tags" to { viewModel.openSheet(SheetType.TAGS, state.contextTrackIndex) }
        )

        actionItems.forEachIndexed { idx, (label, onAction) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = font.style(13.5f, FontWeight.Medium, palette.textPrimary))
                Text("›", style = font.style(14f, FontWeight.Medium, palette.textMuted))
            }
            if (idx < actionItems.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(1.dp)
                        .background(palette.divider)
                )
            }
        }
    }
}

@Composable
private fun ArtworkSearchSheet(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    onPickLocalImage: () -> Unit
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val track = state.tracks.getOrElse(state.editingTrackIndex) { state.currentTrack }
    var query by remember(track.id) { mutableStateOf("${track.title} ${track.artist}".trim()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Search & Change Album Art", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
                Text("${track.title} — ${track.artist}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SheetInputField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search album / artist cover...",
                modifier = Modifier.weight(1f)
            )
            GlassPill(
                onClick = { if (!state.isSearchingArtwork) viewModel.searchArtwork(query) },
                active = true,
                padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.activePillContent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (state.isSearchingArtwork) "..." else "Search", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
            }
        }

        GlassPill(
            onClick = onPickLocalImage,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = 9.dp)
        ) {
            Text("Choose Photo from Device Gallery", style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
        }

        if (state.isSearchingArtwork) {
            Text("Searching online album covers...", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
        } else if (state.artworkCandidates.isEmpty()) {
            Text("No cover art candidates loaded. Search above or choose from gallery.", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
        } else {
            val rows = remember(state.artworkCandidates) { state.artworkCandidates.chunked(3) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { cand ->
                            ArtworkCandidateTile(
                                candidate = cand,
                                onClick = { viewModel.applyArtworkCandidate(cand) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat((3 - rowItems.size).coerceAtLeast(0)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkCandidateTile(
    candidate: ArtworkCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var bmp by remember(candidate.artworkUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(candidate.artworkUrl) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(candidate.artworkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (conn.responseCode == 200) {
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.pillBrush, RoundedCornerShape(14.dp))
            .border(1.dp, palette.pillBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A2233)),
            contentAlignment = Alignment.Center
        ) {
            val image = bmp
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = candidate.album,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("...", style = font.style(11f, FontWeight.Medium, palette.textMuted))
            }
        }
        Text(
            text = candidate.album,
            style = font.style(10.5f, FontWeight.SemiBold, palette.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = candidate.artist,
            style = font.style(9.5f, FontWeight.Normal, palette.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistPickerSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val track = state.contextTrack
    var newName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Add to Playlist", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
                Text(track.title, style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Close", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SheetInputField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "New playlist name...",
                modifier = Modifier.weight(1f)
            )
            GlassPill(
                onClick = { viewModel.createCustomPlaylist(newName, initialTrackId = track.id) },
                active = true,
                padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("+ Create", style = font.style(11.5f, FontWeight.Bold, palette.activePillContent))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.playlists.forEach { pl ->
                GlassPill(
                    onClick = { viewModel.addTrackToPlaylist(pl.id, track.id) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.title, style = font.style(12f, FontWeight.SemiBold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pl.subtitle, style = font.style(10.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("+ Add", style = font.style(11.5f, FontWeight.SemiBold, palette.accentText))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistSheet(viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    var name by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Create New Playlist", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Cancel", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        SheetInputField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Enter playlist name (e.g. Late Night Drive)..."
        )
        GlassPill(
            onClick = { viewModel.createCustomPlaylist(name) },
            active = true,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = 10.dp)
        ) {
            Text("Create Playlist", style = font.style(12f, FontWeight.Bold, palette.activePillContent))
        }
    }
}

@Composable
private fun RenamePlaylistSheet(state: CorvixUiState, viewModel: CorvixViewModel) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val targetPl = state.playlists.firstOrNull { it.id == state.editingPlaylistId }
    var name by remember(targetPl?.id) { mutableStateOf(targetPl?.title.orEmpty()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Rename Playlist", style = font.style(14f, FontWeight.Bold, palette.textPrimary))
            GlassPill(onClick = { viewModel.closeSheet() }, padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Cancel", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary))
            }
        }
        SheetInputField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Playlist name..."
        )
        GlassPill(
            onClick = {
                targetPl?.let { viewModel.renamePlaylist(it.id, name) } ?: viewModel.closeSheet()
            },
            active = true,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = 10.dp)
        ) {
            Text("Save Name", style = font.style(12f, FontWeight.Bold, palette.activePillContent))
        }
    }
}

@Composable
private fun SheetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    singleLine: Boolean = true
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.pillBrush, RoundedCornerShape(14.dp))
            .border(1.dp, palette.pillBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = font.style(11.5f, FontWeight.Normal, palette.textMuted, mono = mono))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = font.style(12f, FontWeight.Normal, palette.textPrimary, mono = mono),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
