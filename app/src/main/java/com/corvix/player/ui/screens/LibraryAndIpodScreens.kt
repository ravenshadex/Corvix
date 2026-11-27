package com.corvix.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import androidx.compose.ui.unit.dp
import com.corvix.player.model.AppScreen
import com.corvix.player.model.LibrarySortOrder
import com.corvix.player.model.LibraryView
import com.corvix.player.model.SheetType
import com.corvix.player.model.Track
import com.corvix.player.model.TrackLanguage
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
import com.corvix.player.ui.theme.liquidGlassSurface
import com.corvix.player.viewmodel.CorvixUiState
import com.corvix.player.viewmodel.CorvixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IpodMenuScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        IpodListEntry("Music") { viewModel.setLibraryView(LibraryView.SONGS); viewModel.navigate(AppScreen.LIBRARY) },
        IpodListEntry("Playlists") { viewModel.navigate(AppScreen.PLAYLISTS) },
        IpodListEntry("Artists") { viewModel.setLibraryView(LibraryView.ARTISTS); viewModel.navigate(AppScreen.LIBRARY) },
        IpodListEntry("Albums") { viewModel.setLibraryView(LibraryView.ALBUMS); viewModel.navigate(AppScreen.LIBRARY) },
        IpodListEntry("Songs") { viewModel.setLibraryView(LibraryView.SONGS); viewModel.navigate(AppScreen.LIBRARY) },
        IpodListEntry("Genres") { viewModel.setLibraryView(LibraryView.GENRES); viewModel.navigate(AppScreen.LIBRARY) },
        IpodListEntry("Folders") { viewModel.navigate(AppScreen.FOLDERS) },
        IpodListEntry("Search") { viewModel.openIpodSearch() },
        IpodListEntry("Now Playing") { viewModel.navigate(AppScreen.NOW_PLAYING) },
        IpodListEntry("Settings") { viewModel.navigate(AppScreen.SETTINGS) }
    )

    IpodSplitScreenScaffold(
        headerTitle = "iPod",
        isPlaying = state.isPlaying,
        entries = entries,
        focusedIndex = state.ipodFocusIndex,
        rightGraphic = IpodRightGraphic.MUSIC_NOTE,
        fallbackTrack = state.currentTrack,
        modifier = modifier
    )
}

private sealed interface LibraryRowItem {
    val key: String

    data class LangHeader(val language: String) : LibraryRowItem {
        override val key: String = "lang_$language"
    }

    data class SongItem(val index: Int, val track: Track) : LibraryRowItem {
        override val key: String = "song_${track.id}"
    }
}

@Composable
fun LibraryScreen(
    state: CorvixUiState,
    viewModel: CorvixViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val searchFocusRequester = remember { FocusRequester() }
    val filteredTracks = state.filteredTracks
    val bottomClearance = if (palette.isIpod) 14.dp else metrics.dockBottomClearance
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val groupedEntries: List<Pair<String, List<Track>>> = remember(filteredTracks, state.libraryView) {
        when (state.libraryView) {
            LibraryView.SONGS -> emptyList()
            LibraryView.ARTISTS -> filteredTracks.groupBy { it.artist }.toList()
            LibraryView.ALBUMS -> filteredTracks.groupBy { it.album }.toList()
            LibraryView.GENRES -> filteredTracks.groupBy { it.genre }.toList()
        }
    }

    val songRows: List<LibraryRowItem> = remember(filteredTracks, state.libraryView) {
        if (state.libraryView != LibraryView.SONGS) {
            emptyList()
        } else {
            filteredTracks.mapIndexed { idx, track ->
                LibraryRowItem.SongItem(idx, track)
            }
        }
    }

    if (palette.isIpod) {
        val q = state.searchQuery.trim().lowercase()
        val ipodTracks = remember(state.tracks, state.searchQuery) {
            if (q.isEmpty()) state.tracks
            else state.tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    it.genre.lowercase().contains(q)
            }
        }
        val ipodGrouped = remember(ipodTracks, state.libraryView) {
            when (state.libraryView) {
                LibraryView.SONGS -> emptyList()
                LibraryView.ARTISTS -> ipodTracks.groupBy { it.artist }.toList()
                LibraryView.ALBUMS -> ipodTracks.groupBy { it.album }.toList()
                LibraryView.GENRES -> ipodTracks.groupBy { it.genre }.toList()
            }
        }
        val entries = if (state.libraryView == LibraryView.SONGS) {
            ipodTracks.map { trk ->
                IpodListEntry(
                    title = trk.title,
                    trailingValue = trk.durationFormatted,
                    previewTrack = trk,
                    onClick = {
                        viewModel.playTrackById(trk.id)
                        viewModel.navigate(AppScreen.NOW_PLAYING)
                    }
                )
            }
        } else {
            ipodGrouped.map { (groupName, groupTracks) ->
                val firstTrack = groupTracks.first()
                IpodListEntry(
                    title = groupName,
                    trailingValue = "${groupTracks.size}",
                    previewTrack = firstTrack,
                    onClick = {
                        viewModel.playTrackById(firstTrack.id)
                        viewModel.navigate(AppScreen.NOW_PLAYING)
                    }
                )
            }
        }
        val safeFocus = state.ipodFocusIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
        val previewEntry = entries.getOrNull(safeFocus)
        Column(modifier = modifier.fillMaxSize().background(Color.White)) {
            if (state.ipodSearchFocusSignal > 0 || state.searchQuery.isNotEmpty()) {
                LaunchedEffect(state.ipodSearchFocusSignal) {
                    if (state.ipodSearchFocusSignal > 0) {
                        runCatching { searchFocusRequester.requestFocus() }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty()) {
                            Text("Search songs, artists, albums...", style = font.style(12.5f, FontWeight.Normal, Color(0xFF64748B)))
                        }
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            singleLine = true,
                            textStyle = font.style(12.5f, FontWeight.SemiBold, Color(0xFF0F172A)),
                            cursorBrush = SolidColor(Color(0xFF1D6CE0)),
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester)
                        )
                    }
                    if (state.searchQuery.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            style = font.style(11.5f, FontWeight.SemiBold, Color(0xFF1D6CE0)),
                            modifier = Modifier.clickable { viewModel.updateSearchQuery("") }
                        )
                    }
                }
            }
            IpodSplitScreenScaffold(
                headerTitle = state.libraryView.label,
                isPlaying = state.isPlaying,
                entries = entries,
                focusedIndex = safeFocus,
                rightGraphic = IpodRightGraphic.TRACK_ART,
                fallbackTrack = state.currentTrack,
                rightCaptionTitle = previewEntry?.title ?: state.currentTrack.title,
                rightCaptionSub = previewEntry?.previewTrack?.artist ?: state.currentTrack.artist,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val listState = rememberLazyListState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = metrics.horizontalPadding, end = metrics.horizontalPadding, top = 8.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            item(key = "header", contentType = "lib_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Library", style = font.style(26f, FontWeight.Bold, palette.textPrimary))
                        Text("${state.indexedTrackCount} tracks · On device", style = font.style(12.5f, FontWeight.Normal, palette.textMuted))
                    }
                    GlassPill(
                        onClick = { runCatching { searchFocusRequester.requestFocus() } },
                        modifier = Modifier.size(44.dp),
                        padding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search local files", tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Search Capsule (48dp height)
            item(key = "search") {
                GlassSurface(
                    cornerRadius = 999.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search songs, artists, albums...",
                                    style = font.style(12.5f, FontWeight.Normal, palette.textMuted)
                                )
                            }
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                singleLine = true,
                                textStyle = font.style(12.5f, FontWeight.Normal, palette.textPrimary),
                                cursorBrush = SolidColor(palette.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                            )
                        }
                    }
                }
            }

            // Quick Action Pills (48dp equal-height Shuffle All & Sort)
            item(key = "actions") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPill(
                        onClick = { viewModel.shuffleAll() },
                        active = true,
                        modifier = Modifier.weight(1f).height(48.dp),
                        padding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = palette.activePillContent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle All", style = font.style(12.5f, FontWeight.SemiBold, palette.activePillContent))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassPill(
                            onClick = { viewModel.openSheet(SheetType.SORT_ORDER) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            padding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Sort: ${state.librarySortOrder.label}",
                                style = font.style(12.5f, FontWeight.SemiBold, palette.textPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Library Browse View Tabs (34dp height capsules, 8dp spacing)
            item(key = "tabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryView.entries.filter { it != LibraryView.GENRES }.forEach { view ->
                        val active = state.libraryView == view
                        GlassPill(
                            onClick = { viewModel.setLibraryView(view) },
                            active = active,
                            modifier = Modifier.height(34.dp),
                            padding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = view.label,
                                style = font.style(
                                    sizeSp = 12f,
                                    weight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (active) palette.activePillContent else palette.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Recently Played Coverflow Carousel
            item(key = "recent_shelf") {
                RecentlyPlayedCoverflow(
                    tracks = state.recentlyPlayedTracks,
                    onPlayTrack = { viewModel.playTrackFromCarousel(it.id) },
                    onLongPressTrack = { viewModel.openTrackActions(it.id) }
                )
            }

            // List Heading
            item(key = "list_heading") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.libraryView.heading.uppercase(), style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted))
                    Text(
                        text = "${filteredTracks.size} tracks",
                        style = font.style(10.5f, FontWeight.Normal, palette.textMuted)
                    )
                }
            }

            // Virtualized Tracks or Grouped Entries
            if (state.libraryView == LibraryView.SONGS) {
                if (songRows.isEmpty()) {
                    item(key = "empty_songs", contentType = "empty_msg") {
                        Text(
                            text = "No matching local files.",
                            style = font.style(12f, FontWeight.Normal, palette.textMuted),
                            modifier = Modifier.padding(vertical = 18.dp)
                        )
                    }
                } else {
                    items(
                        items = songRows,
                        key = { it.key },
                        contentType = { if (it is LibraryRowItem.LangHeader) "lang_header" else "song_row" }
                    ) { item ->
                        when (item) {
                            is LibraryRowItem.LangHeader -> {
                                Text(
                                    text = item.language.uppercase(),
                                    style = font.style(10f, FontWeight.Bold, palette.accentText),
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                            is LibraryRowItem.SongItem -> {
                                val track = item.track
                                val isActive = track.id == state.currentTrack.id
                                BoxlessTrackRow(
                                    trackNumber = item.index + 1,
                                    track = track,
                                    isActive = isActive,
                                    isPlaying = state.isPlaying,
                                    ipodHighlighted = palette.isIpod && item.index == state.ipodFocusIndex,
                                    onClick = { viewModel.playTrackById(track.id) },
                                    onLongClick = { viewModel.openTrackActions(track.id) },
                                    onAddToQueue = { viewModel.addToQueue(track.id) },
                                    onEditTags = {
                                        val realIdx = state.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                        viewModel.openSheet(SheetType.TAGS, realIdx)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                if (groupedEntries.isEmpty()) {
                    item(key = "empty_groups", contentType = "empty_msg") {
                        Text(
                            text = "No matching tracks.",
                            style = font.style(12f, FontWeight.Normal, palette.textMuted),
                            modifier = Modifier.padding(vertical = 18.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = groupedEntries,
                        key = { _, entry -> "grp_${state.libraryView.name}_${entry.first}" },
                        contentType = { _, _ -> "group_row" }
                    ) { _, (groupName, items) ->
                        val formats = items.map { it.shortFormat }.distinct().joinToString(" · ")
                        val firstTrack = items.first()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.playTrackById(firstTrack.id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkBox(
                                    artStyle = firstTrack.artStyle,
                                    track = firstTrack,
                                    cornerRadius = 11.dp,
                                    modifier = Modifier.size(42.dp)
                                )
                                Column {
                                    Text(groupName, style = font.style(12.5f, FontWeight.SemiBold, palette.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${items.size} tracks · $formats", style = font.style(11.5f, FontWeight.Normal, palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            GlassPill(
                                onClick = { viewModel.playTrackById(firstTrack.id) },
                                padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Play ›", style = font.style(10.5f, FontWeight.SemiBold, palette.textPrimary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentlyPlayedCoverflow(
    tracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onLongPressTrack: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val count = tracks.size
    val totalPages = if (count > 1) Int.MAX_VALUE else 1
    val startPage = remember(count) {
        if (count > 1) {
            val mid = Int.MAX_VALUE / 2
            mid - (mid % count) + (if (count >= 3) 1 else 0)
        } else 0
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { totalPages })
    val activeIdx = pagerState.currentPage % count

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "RECENTLY PLAYED",
            style = font.style(11.5f, FontWeight.SemiBold, palette.textMuted)
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cardSize = (maxWidth * 0.44f).coerceIn(142.dp, 188.dp)
            val sidePad = ((maxWidth - cardSize) / 2f).coerceAtLeast(60.dp)

            // Ambient glass glow behind center card
            Box(
                modifier = Modifier
                    .size(cardSize * 1.25f)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.accent.copy(alpha = if (palette.isDark) 0.28f else 0.16f),
                                palette.accentSecondary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                contentPadding = PaddingValues(horizontal = sidePad),
                pageSpacing = (-10).dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val track = tracks[page % count]
                val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = rawOffset.absoluteValue
                val cardScale = lerp(1.0f, 0.76f, absOffset.coerceIn(0f, 1.4f))
                val cardAlpha = lerp(1.0f, 0.56f, absOffset.coerceIn(0f, 1.5f))
                val tiltDeg = (-rawOffset * 26f).coerceIn(-36f, 36f)
                val isCenter = absOffset < 0.35f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .zIndex(10f - absOffset)
                        .graphicsLayer {
                            scaleX = cardScale
                            scaleY = cardScale
                            rotationY = tiltDeg
                            alpha = cardAlpha
                            cameraDistance = 12f * density
                        }
                ) {
                    // Main Album Card inside Liquid Glass Bezel (24dp radius)
                    Box(
                        modifier = Modifier
                            .size(cardSize)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = if (isCenter) 1.5.dp else 1.dp,
                                color = if (isCenter) Color.White.copy(alpha = 0.55f) else palette.surfaceBorder,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (pagerState.currentPage == page) {
                                        onPlayTrack(track)
                                    } else {
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    }
                                },
                                onLongClick = { onLongPressTrack(track) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkBox(
                            artStyle = track.artStyle,
                            track = track,
                            highRes = isCenter,
                            cornerRadius = 24.dp,
                            showPlayIcon = false,
                            showBorder = false,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Curved top specular sheen on bezel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardSize * 0.30f)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = if (isCenter) 0.22f else 0.12f), Color.Transparent)
                                    )
                                )
                        )
                        // Glossy Liquid Glass center play button
                        if (isCenter) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f), CircleShape)
                                    .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // True alpha fade-away mirror reflection (no solid box)
                    Box(
                        modifier = Modifier
                            .width(cardSize)
                            .height(26.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (isCenter) 0.40f else 0.20f),
                                            Color.Transparent
                                        )
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(cardSize)
                                .graphicsLayer { scaleY = -1f }
                        ) {
                            ArtworkBox(
                                artStyle = track.artStyle,
                                track = track,
                                cornerRadius = 20.dp,
                                showBorder = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Focused track title + artist
        val focused = tracks.getOrNull(activeIdx)
        if (focused != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayTrack(focused) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = focused.title,
                    style = font.style(13.5f, FontWeight.Bold, palette.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = focused.artist,
                    style = font.style(11.5f, FontWeight.Normal, palette.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Carousel indicator dots (18x5dp active pill + 5dp translucent glass dots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tracks.forEachIndexed { idx, _ ->
                val selected = idx == activeIdx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(if (selected) 18.dp else 5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.25f))
                        .clickable {
                            val delta = idx - activeIdx
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + delta) }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxlessTrackRow(
    trackNumber: Int,
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    ipodHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onEditTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCorvixPalette.current
    val font = LocalCorvixFont.current
    val metrics = LocalCorvixMetrics.current
    val useActiveBg = isActive || ipodHighlighted
    val rowBg = if (useActiveBg) palette.rowActiveBrush else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    val titleColor = when {
        palette.isIpod && useActiveBg -> Color.White
        isActive -> palette.accentText
        else -> palette.textPrimary
    }
    val subColor = if (palette.isIpod && useActiveBg) Color.White.copy(alpha = 0.88f) else palette.textMuted

    val rowModifier = if (!palette.isIpod && !palette.isWinamp) {
        modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(18.dp),
                isDark = palette.isDark,
                isAmoled = palette.isAmoled,
                strong = isActive,
                elevation = 0.dp,
                brightRim = isActive,
                preset = palette.preset,
                accent = palette.accent,
                accentSecondary = palette.accentSecondary
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(rowBg, RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trackNumber.toString(),
                style = font.style(11.5f, FontWeight.SemiBold, if (isActive) palette.accentText else palette.textMuted, mono = true),
                modifier = Modifier.width(26.dp)
            )
            ArtworkBox(
                artStyle = track.artStyle,
                track = track,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .size(48.dp)
                    .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = font.style(13.5f, FontWeight.SemiBold, titleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} · ${track.shortFormat}",
                    style = font.style(12f, FontWeight.Normal, subColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp)
        ) {
            if (isActive) {
                PlayingEqBars(
                    isPlaying = isPlaying,
                    barColor = if (palette.isIpod) Color.White else palette.eqBarColor
                )
            }
            if (!metrics.isCompactWidth) {
                GlassPill(
                    onClick = onClick,
                    padding = PaddingValues(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(track.shortFormat, style = font.style(9.5f, FontWeight.SemiBold, palette.textPrimary, mono = true))
                }
            }
        }
    }
}
