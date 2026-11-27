package com.corvix.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.MoodType
import com.corvix.player.ui.components.ArtworkBox
import com.corvix.player.ui.components.GlassPill
import com.corvix.player.ui.components.GlassSurface
import com.corvix.player.ui.components.IpodListEntry
import com.corvix.player.ui.components.IpodRightGraphic
import com.corvix.player.ui.components.IpodSplitScreenScaffold
import com.corvix.player.ui.components.PlayingEqBars
import com.corvix.player.ui.theme.LocalCorvixFont
import com.corvix.player.ui.theme.LocalCorvixMetrics
import com.corvix.player.ui.theme.LocalCorvixPalette
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel

@Composable
fun FoldersScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val bottomClearance = if (palette.isIpod) 14.dp else metrics.dockBottomClearance

    if (palette.isIpod) {
        val entries = state.folders.map { folder ->
            val preview = state.tracks.firstOrNull {
                it.folderPath.contains(folder.name, ignoreCase = true) ||
                    it.folderPath.equals(folder.displayPath, ignoreCase = true)
            } ?: state.currentTrack
            IpodListEntry(
                title = folder.name,
                trailingValue = "${folder.trackCount}",
                previewTrack = preview,
                onClick = { viewModel.selectFolder(folder) }
            )
        }
        IpodSplitScreenScaffold(
            headerTitle = "Folders",
            isPlaying = state.isPlaying,
            entries = entries,
            focusedIndex = state.ipodFocusIndex,
            rightGraphic = IpodRightGraphic.TRACK_ART,
            fallbackTrack = state.currentTrack,
            rightCaptionTitle = state.folders.getOrNull(state.ipodFocusIndex.coerceIn(0, (state.folders.size - 1).coerceAtLeast(0)))?.name,
            rightCaptionSub = state.folders.getOrNull(state.ipodFocusIndex.coerceIn(0, (state.folders.size - 1).coerceAtLeast(0)))?.sizeLabel,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = metrics.horizontalPadding, end = metrics.horizontalPadding, top = 8.dp, bottom = bottomClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Folders", style = font.style(22f, FontWeight.Bold, palette.textPrimary))
                    Text("${state.folders.size} albums & collections · On device", style = font.style(12f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                GlassPill(onClick = { viewModel.navigate(AppScreen.SETTINGS) }, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Scan in Settings", style = font.style(11.5f, FontWeight.Medium, palette.textPrimary), maxLines = 1)
                }
            }
        }

        // Real Phone Model + Hardware Image & Live Device Storage Card
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhoneHardwareIllustration(
                        isNothingPhone = state.isNothingPhone,
                        accentColor = palette.accent,
                        modifier = Modifier.size(width = 46.dp, height = 84.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.deviceModelName,
                                style = font.style(14.5f, FontWeight.Bold, palette.textPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = state.storageSummaryLabel,
                                style = font.style(12f, FontWeight.SemiBold, palette.textMuted, mono = true)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(palette.sliderTrack, CircleShape)
                                .border(0.6.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                .padding(1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.storageUsedFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(palette.activePillBrush, CircleShape)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Excluding voice notes <30s", style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                            Text("Auto-watch ON", style = font.style(11.5f, FontWeight.SemiBold, palette.accentText))
                        }
                    }
                }
            }
        }

        itemsIndexed(
            items = state.folders,
            key = { _, folder -> "fld_${folder.displayPath}" },
            contentType = { _, _ -> "folder_row" }
        ) { _, folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.selectFolder(folder) }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPill(onClick = { viewModel.selectFolder(folder) }, shape = RoundedCornerShape(14.dp), padding = PaddingValues(10.dp)) {
                        Icon(Icons.Rounded.Folder, null, tint = palette.accentText, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, style = font.style(13f, FontWeight.SemiBold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${folder.trackCount} tracks · ${folder.codecSummary}", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("${folder.sizeLabel} ›", style = font.style(12f, FontWeight.Normal, palette.textMuted, mono = true))
            }
        }

        item(key = "folder_status", contentType = "status_footer") {
            Text(
                text = state.folderStatusMessage,
                style = font.style(11.5f, FontWeight.Normal, palette.textMuted),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun PlaylistsScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val bottomClearance = if (palette.isIpod) 14.dp else metrics.dockBottomClearance

    val activeQueue = state.queues.firstOrNull { it.key == state.activeQueueKey } ?: state.queues.first()
    val queueTracks = activeQueue.trackIds.mapNotNull { id -> state.tracks.find { it.id == id } }

    if (palette.isIpod) {
        val playlistEntries = state.playlists.map { pl ->
            val preview = pl.queueIndices.firstOrNull()?.let { state.tracks.getOrNull(it) } ?: state.currentTrack
            IpodListEntry(
                title = pl.title,
                trailingValue = "›",
                previewTrack = preview,
                onClick = { viewModel.loadSmartPlaylist(pl) }
            )
        } + queueTracks.map { trk ->
            IpodListEntry(
                title = trk.title,
                trailingValue = trk.durationFormatted,
                previewTrack = trk,
                onClick = { viewModel.playTrackById(trk.id) }
            )
        }
        val focusItem = playlistEntries.getOrNull(state.ipodFocusIndex.coerceIn(0, (playlistEntries.size - 1).coerceAtLeast(0)))
        IpodSplitScreenScaffold(
            headerTitle = "Playlists",
            isPlaying = state.isPlaying,
            entries = playlistEntries,
            focusedIndex = state.ipodFocusIndex,
            rightGraphic = IpodRightGraphic.TRACK_ART,
            fallbackTrack = state.currentTrack,
            rightCaptionTitle = focusItem?.title,
            rightCaptionSub = focusItem?.previewTrack?.artist,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = metrics.horizontalPadding, end = metrics.horizontalPadding, top = 8.dp, bottom = bottomClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Playlists", style = font.style(22f, FontWeight.Bold, palette.textPrimary))
                    Text("Custom lists, smart mixes & .m3u8", style = font.style(12f, FontWeight.Normal, palette.textMuted))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassPill(onClick = { viewModel.openCreatePlaylistSheet() }, active = true, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("+ Playlist", style = font.style(11.5f, FontWeight.SemiBold, palette.activePillContent))
                    }
                    GlassPill(onClick = { viewModel.autoCreateSmartPlaylists() }, padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Auto Create", style = font.style(11.5f, FontWeight.SemiBold, palette.textPrimary))
                    }
                    GlassPill(onClick = { viewModel.exportM3u8Playlist() }, padding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                        Text(".m3u8", style = font.style(11f, FontWeight.Medium, palette.textPrimary))
                    }
                }
            }
        }

        // On-Device Mood & Tempo Engine
        item {
            GlassSurface(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mood & Tempo Engine", style = font.style(12.5f, FontWeight.Bold, palette.textPrimary))
                            Text(state.bpmStatusText, style = font.style(11.5f, FontWeight.Normal, palette.textMuted))
                        }
                        GlassPill(onClick = { viewModel.scanBpmEnergy() }, padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(if (state.isBpmScanning) "FFT Pass..." else "Scan BPM", style = font.style(10.5f, FontWeight.SemiBold, palette.textPrimary))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(MoodType.CHILL, "🌙 Chill", "<95 BPM"),
                            Triple(MoodType.FOCUS, "⚡ Focus", "95–115"),
                            Triple(MoodType.PEAK, "🔥 Peak", ">115 BPM")
                        ).forEach { (mood, title, range) ->
                            GlassPill(
                                onClick = { viewModel.loadMoodMix(mood) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                padding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(title, style = font.style(11f, FontWeight.SemiBold, palette.textPrimary))
                                    Text(range, style = font.style(9.5f, FontWeight.Normal, palette.textMuted))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playlists Header
        item(key = "pl_heading", contentType = "section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PLAYLISTS (${state.playlists.size})", style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted))
                Text("Long-press tracks to add", style = font.style(10.5f, FontWeight.Normal, palette.textMuted))
            }
        }

        itemsIndexed(
            items = state.playlists,
            key = { _, pl -> "pl_${pl.id}" },
            contentType = { _, _ -> "playlist_row" }
        ) { _, pl ->
            val isExpanded = state.selectedPlaylistIdForDetail == pl.id
            val plTracks = pl.queueIndices.mapNotNull { idx -> state.tracks.getOrNull(idx) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.selectPlaylistDetail(pl.id) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkBox(
                            artStyle = pl.artStyle,
                            track = pl.queueIndices.firstOrNull()?.let { state.tracks.getOrNull(it) },
                            cornerRadius = 11.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.title, style = font.style(13f, FontWeight.SemiBold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pl.subtitle, style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassPill(
                            onClick = { viewModel.loadSmartPlaylist(pl) },
                            padding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text("Play", style = font.style(11f, FontWeight.SemiBold, palette.accentText))
                        }
                        GlassPill(
                            onClick = { viewModel.openRenamePlaylist(pl.id) },
                            padding = PaddingValues(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Rename playlist",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        GlassPill(
                            onClick = { viewModel.removePlaylist(pl.id) },
                            padding = PaddingValues(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete playlist",
                                tint = palette.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                if (isExpanded) {
                    GlassSurface(cornerRadius = 16.dp, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                        if (plTracks.isEmpty()) {
                            Text(
                                text = "Playlist is empty. Long-press any song in Library -> Add to Playlist.",
                                style = font.style(11.5f, FontWeight.Normal, palette.textMuted),
                                modifier = Modifier.padding(4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                plTracks.forEachIndexed { idx, trk ->
                                    val isCurrent = trk.id == state.currentTrack.id
                                    @OptIn(ExperimentalFoundationApi::class)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) palette.rowActiveBrush else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .combinedClickable(
                                                onClick = { viewModel.playTrackById(trk.id) },
                                                onLongClick = { viewModel.openTrackActions(trk.id) }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = font.style(11f, FontWeight.Medium, palette.textMuted, mono = true),
                                                modifier = Modifier.width(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(trk.title, style = font.style(12f, FontWeight.SemiBold, if (isCurrent) palette.accentText else palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${trk.artist} · ${trk.durationFormatted}", style = font.style(10.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove from playlist",
                                            tint = palette.textMuted,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .clickable { viewModel.removeTrackFromPlaylist(pl.id, trk.id) }
                                                .padding(2.dp)
                                        )
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

/**
 * Hardware phone illustration shown on the Folders storage card (renders Nothing Glyph ring styling
 * on Nothing devices or a clean sleek camera-bar smartphone silhouette on other Android models).
 */
@Composable
private fun PhoneHardwareIllustration(
    isNothingPhone: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(w * 0.18f, w * 0.18f)

        // Metallic frame
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFDCE2EC), Color(0xFFC2CBD8))),
            cornerRadius = radius
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.72f),
            cornerRadius = radius,
            style = Stroke(width = 2.2f)
        )

        // Inner rear glass backplate
        val pad = w * 0.08f
        drawRoundRect(
            color = if (isNothingPhone) Color(0xFFF1F5F9) else Color(0xFFE2E8F0),
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2f, h - pad * 2f),
            cornerRadius = CornerRadius(w * 0.13f, w * 0.13f)
        )

        // Vertical dual camera pill (Nothing / modern vertical module)
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.17f, h * 0.09f),
            size = Size(w * 0.21f, h * 0.24f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
        )
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.068f, center = Offset(w * 0.275f, h * 0.15f))
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.068f, center = Offset(w * 0.275f, h * 0.265f))

        if (isNothingPhone) {
            // Iconic Nothing Phone circular Glyph C-ring + top & bottom glyph strips
            drawArc(
                color = Color.White,
                startAngle = -35f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(w * 0.24f, h * 0.37f),
                size = Size(w * 0.52f, w * 0.52f),
                style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor.copy(alpha = 0.55f),
                startAngle = -35f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(w * 0.24f, h * 0.37f),
                size = Size(w * 0.52f, w * 0.52f),
                style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = w * 0.10f,
                center = Offset(w * 0.50f, h * 0.51f)
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.50f, h * 0.72f),
                end = Offset(w * 0.50f, h * 0.85f),
                strokeWidth = w * 0.05f,
                cap = StrokeCap.Round
            )
        } else {
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = w * 0.14f,
                center = Offset(w * 0.50f, h * 0.52f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}
