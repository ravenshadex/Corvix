package com.corvix.player.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.corvix.player.MainActivity
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.ReverbSpace
import com.corvix.player.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.IdentityHashMap
import kotlin.math.abs

enum class CorvixMediaCommand {
    TOGGLE_PLAY,
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS
}

object CorvixMediaCommandBus {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _commands = MutableSharedFlow<CorvixMediaCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<CorvixMediaCommand> = _commands.asSharedFlow()

    @Volatile
    var directHandler: ((CorvixMediaCommand) -> Unit)? = null

    fun emit(command: CorvixMediaCommand): Boolean {
        val handler = directHandler
        if (handler != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                handler(command)
            } else {
                mainHandler.post { handler(command) }
            }
            return true
        }
        return _commands.tryEmit(command)
    }
}

/**
 * Process-scoped queue snapshot so CorvixPlaybackService can continue advancing
 * tracks or responding to notification Next/Prev even if the Activity/ViewModel
 * was destroyed in the background.
 */
object CorvixServiceQueue {
    @Volatile var tracks: List<Track> = emptyList()
    @Volatile var currentIndex: Int = 0
    @Volatile var isShuffle: Boolean = false
    @Volatile var repeatMode: RepeatMode = RepeatMode.ALL
    @Volatile var nextUpIds: List<Long> = emptyList()

    fun sync(
        tracks: List<Track>,
        currentIndex: Int,
        isShuffle: Boolean,
        repeatMode: RepeatMode,
        nextUpIds: List<Long>
    ) {
        this.tracks = tracks
        this.currentIndex = currentIndex
        this.isShuffle = isShuffle
        this.repeatMode = repeatMode
        this.nextUpIds = nextUpIds
    }

    @Synchronized
    fun nextTrackFallback(): Track? {
        val list = tracks
        if (list.isEmpty()) return null
        val queuedId = nextUpIds.firstOrNull()
        if (queuedId != null) {
            nextUpIds = nextUpIds.drop(1)
            val qIdx = list.indexOfFirst { it.id == queuedId }
            if (qIdx >= 0) {
                currentIndex = qIdx
                return list[qIdx]
            }
        }
        val nextIdx = if (isShuffle && list.size > 1) {
            val candidates = list.indices.filter { it != currentIndex }
            candidates.random()
        } else {
            (currentIndex + 1) % list.size
        }
        currentIndex = nextIdx
        return list[nextIdx]
    }

    @Synchronized
    fun prevTrackFallback(): Track? {
        val list = tracks
        if (list.isEmpty()) return null
        val prevIdx = (currentIndex - 1 + list.size) % list.size
        currentIndex = prevIdx
        return list[prevIdx]
    }
}

/**
 * Shared in-process hardware DSP host bound directly to the active ExoPlayer audioSessionId.
 */
object CorvixDspEngine {
    private const val TAG = "CorvixDspEngine"
    private val primaryCenterHz = intArrayOf(60, 230, 910, 3600, 14000)
    private val allTenCenterHz = intArrayOf(60, 230, 910, 3600, 14000, 31, 125, 500, 1800, 8000)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var attachedSessionId: Int = 0

    private var eqEnabled: Boolean = true
    private var bandsDb: List<Int> = listOf(4, 2, 0, 3, 2, 3, 1, -1, 2, 4)
    private var eq10Band: Boolean = false
    private var bassPct: Int = 45
    private var virtPct: Int = 35
    private var reverbSpace: ReverbSpace = ReverbSpace.ROOM

    @Synchronized
    fun attachSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (sessionId != attachedSessionId) {
            release()
            attachedSessionId = sessionId
            runCatching { equalizer = Equalizer(100, sessionId) }
                .onFailure { Log.w(TAG, "Equalizer init failed: ${it.message}") }
            runCatching { bassBoost = BassBoost(100, sessionId) }
                .onFailure { Log.w(TAG, "BassBoost init failed: ${it.message}") }
            runCatching { virtualizer = Virtualizer(100, sessionId) }
                .onFailure { Log.w(TAG, "Virtualizer init failed: ${it.message}") }
            runCatching { reverb = PresetReverb(100, sessionId) }
                .onFailure { Log.w(TAG, "PresetReverb init failed: ${it.message}") }
        }
        applyLocked()
    }

    @Synchronized
    fun updateSettings(
        enabled: Boolean,
        bandsDb: List<Int>,
        eq10Band: Boolean = this.eq10Band,
        bassPct: Int,
        virtPct: Int,
        reverbSpace: ReverbSpace
    ) {
        this.eqEnabled = enabled
        this.bandsDb = bandsDb
        this.eq10Band = eq10Band
        this.bassPct = bassPct
        this.virtPct = virtPct
        this.reverbSpace = reverbSpace
        applyLocked()
    }

    private fun applyLocked() {
        equalizer?.let { eq ->
            runCatching {
                eq.enabled = eqEnabled
                val hwBands = eq.numberOfBands.toInt().coerceAtLeast(0)
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()

                for (hwIdx in 0 until hwBands) {
                    val centerHz = (eq.getCenterFreq(hwIdx.toShort()) / 1000).coerceAtLeast(20)
                    val db = resolveDbForFreq(centerHz)
                    val targetMb = (db * 100).coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(hwIdx.toShort(), targetMb)
                }
            }.onFailure { Log.w(TAG, "EQ apply failed: ${it.message}") }
        }

        bassBoost?.let { bb ->
            runCatching {
                bb.enabled = eqEnabled && bassPct > 0
                if (bb.strengthSupported) {
                    bb.setStrength((bassPct.coerceIn(0, 100) * 10).toShort())
                }
            }.onFailure { Log.w(TAG, "BassBoost apply failed: ${it.message}") }
        }

        virtualizer?.let { virt ->
            runCatching {
                virt.enabled = eqEnabled && virtPct > 0
                if (virt.strengthSupported) {
                    virt.setStrength((virtPct.coerceIn(0, 100) * 10).toShort())
                }
            }.onFailure { Log.w(TAG, "Virtualizer apply failed: ${it.message}") }
        }

        reverb?.let { rev ->
            runCatching {
                val active = eqEnabled && reverbSpace != ReverbSpace.OFF
                rev.enabled = active
                if (active) {
                    rev.preset = reverbSpace.presetId
                }
            }.onFailure { Log.w(TAG, "Reverb apply failed: ${it.message}") }
        }
    }

    private fun resolveDbForFreq(hwCenterHz: Int): Int {
        if (bandsDb.isEmpty()) return 0
        if (!eq10Band) {
            val bestIdx = primaryCenterHz.indices.minByOrNull { abs(primaryCenterHz[it] - hwCenterHz) } ?: 0
            return bandsDb.getOrElse(bestIdx) { 0 }.coerceIn(-12, 12)
        }
        // In 10-band mode, average the two closest UI sliders weighted toward nearest frequency
        val sortedByDist = allTenCenterHz.indices
            .filter { it < bandsDb.size }
            .sortedBy { abs(allTenCenterHz[it] - hwCenterHz) }
        val first = sortedByDist.firstOrNull() ?: return 0
        val second = sortedByDist.getOrNull(1)
        val raw = if (second != null) {
            (bandsDb[first] * 0.7f + bandsDb[second] * 0.3f).toInt()
        } else {
            bandsDb[first]
        }
        return raw.coerceIn(-12, 12)
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
        attachedSessionId = 0
    }
}

class CorvixPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var multiTapCount = 0
    private var serviceErrorCount = 0
    private var audioManager: AudioManager? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null

    private fun dispatchMediaCommand(cmd: CorvixMediaCommand) {
        if (CorvixMediaCommandBus.directHandler != null) {
            CorvixMediaCommandBus.emit(cmd)
            return
        }
        val exo = player ?: return
        when (cmd) {
            CorvixMediaCommand.TOGGLE_PLAY -> {
                if (exo.isPlaying) exo.pause() else exo.play()
            }
            CorvixMediaCommand.PLAY -> exo.play()
            CorvixMediaCommand.PAUSE -> exo.pause()
            CorvixMediaCommand.NEXT -> {
                val track = CorvixServiceQueue.nextTrackFallback() ?: return
                loadAndPlayFallbackTrack(exo, track)
            }
            CorvixMediaCommand.PREVIOUS -> {
                val track = CorvixServiceQueue.prevTrackFallback() ?: return
                loadAndPlayFallbackTrack(exo, track)
            }
        }
    }

    private fun loadAndPlayFallbackTrack(exo: ExoPlayer, track: Track) {
        serviceScope.launch {
            val uri = LocalLibraryScanner.ensureShowcaseAudioUri(this@CorvixPlaybackService, track)
            val meta = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .build()
            val item = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(meta)
                .build()
            exo.playWhenReady = true
            exo.setMediaItem(item, true)
            exo.prepare()
            exo.play()
        }
    }

    private val multiTapRunnable = Runnable {
        val count = multiTapCount
        multiTapCount = 0
        when {
            count == 1 -> dispatchMediaCommand(CorvixMediaCommand.TOGGLE_PLAY)
            count == 2 -> dispatchMediaCommand(CorvixMediaCommand.NEXT)
            count >= 3 -> dispatchMediaCommand(CorvixMediaCommand.PREVIOUS)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager = am
        if (am != null) {
            val recCb = object : AudioManager.AudioRecordingCallback() {
                override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                    val activeMicCapture = configs.orEmpty().any { cfg ->
                        val silenced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cfg.isClientSilenced
                        val src = cfg.clientAudioSource
                        !silenced && src != MediaRecorder.AudioSource.REMOTE_SUBMIX && src != 1999 // HOTWORD
                    }
                    if (activeMicCapture && (exo.isPlaying || exo.playWhenReady)) {
                        Log.i("CorvixPlaybackService", "External audio recording detected; pausing playback")
                        exo.pause()
                    }
                }
            }
            recordingCallback = recCb
            am.registerAudioRecordingCallback(recCb, mainHandler)
        }

        exo.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                CorvixDspEngine.attachSession(audioSessionId)
            }
        })
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        serviceErrorCount = 0
                        CorvixDspEngine.attachSession(exo.audioSessionId)
                    }
                    Player.STATE_ENDED -> {
                        if (exo.playWhenReady &&
                            CorvixMediaCommandBus.directHandler == null &&
                            exo.repeatMode != Player.REPEAT_MODE_ONE
                        ) {
                            dispatchMediaCommand(CorvixMediaCommand.NEXT)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    CorvixDspEngine.attachSession(exo.audioSessionId)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("CorvixPlaybackService", "Playback error: ${error.errorCodeName}", error)
                if (exo.playWhenReady &&
                    CorvixMediaCommandBus.directHandler == null &&
                    serviceErrorCount < 3
                ) {
                    serviceErrorCount += 1
                    dispatchMediaCommand(CorvixMediaCommand.NEXT)
                }
            }
        })
        if (exo.audioSessionId > 0) {
            CorvixDspEngine.attachSession(exo.audioSessionId)
        }

        val sessionPlayer = object : ForwardingPlayer(exo) {
            private val wrappedListeners = IdentityHashMap<Player.Listener, Player.Listener>()

            private fun augmentedCommands(base: Player.Commands): Player.Commands {
                return base.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun addListener(listener: Player.Listener) {
                val forwarding = this
                val wrapper = CommandAugmentingPlayerListener(
                    delegate = listener,
                    forwardingOwner = forwarding,
                    augmentCommands = ::augmentedCommands
                )
                wrappedListeners[listener] = wrapper
                super.addListener(wrapper)
            }

            override fun removeListener(listener: Player.Listener) {
                val wrapper = wrappedListeners.remove(listener) ?: listener
                super.removeListener(wrapper)
            }

            override fun getAvailableCommands(): Player.Commands {
                return augmentedCommands(super.getAvailableCommands())
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean = true

            override fun hasPreviousMediaItem(): Boolean = true

            override fun seekToNext() {
                dispatchMediaCommand(CorvixMediaCommand.NEXT)
            }

            override fun seekToNextMediaItem() {
                dispatchMediaCommand(CorvixMediaCommand.NEXT)
            }

            override fun seekToPrevious() {
                dispatchMediaCommand(CorvixMediaCommand.PREVIOUS)
            }

            override fun seekToPreviousMediaItem() {
                dispatchMediaCommand(CorvixMediaCommand.PREVIOUS)
            }
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                    .build()
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                mediaButtonEvent: Intent
            ): Boolean {
                val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }
                if (keyEvent == null) {
                    return super.onMediaButtonEvent(session, controllerInfo, mediaButtonEvent)
                }
                return when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            mainHandler.removeCallbacks(multiTapRunnable)
                            multiTapCount += 1
                            if (multiTapCount >= 3) {
                                multiTapCount = 0
                                dispatchMediaCommand(CorvixMediaCommand.PREVIOUS)
                            } else {
                                mainHandler.postDelayed(multiTapRunnable, 380L)
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            dispatchMediaCommand(CorvixMediaCommand.NEXT)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            dispatchMediaCommand(CorvixMediaCommand.PREVIOUS)
                        }
                        true
                    }
                    else -> super.onMediaButtonEvent(session, controllerInfo, mediaButtonEvent)
                }
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        player = exo
        mediaSession = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(multiTapRunnable)
        recordingCallback?.let { cb ->
            runCatching { audioManager?.unregisterAudioRecordingCallback(cb) }
        }
        recordingCallback = null
        audioManager = null
        serviceScope.cancel()
        CorvixDspEngine.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

/**
 * Explicit Player.Listener forwarder so every Java 8 default callback in Media3
 * (onPlaybackStateChanged, onTimelineChanged, onIsPlayingChanged, etc.) reaches
 * MediaSessionImpl reliably while augmenting availableCommands with Next/Prev.
 */
@OptIn(UnstableApi::class)
private class CommandAugmentingPlayerListener(
    private val delegate: Player.Listener,
    private val forwardingOwner: Player,
    private val augmentCommands: (Player.Commands) -> Player.Commands
) : Player.Listener {

    override fun onEvents(player: Player, events: Player.Events) =
        delegate.onEvents(forwardingOwner, events)

    override fun onTimelineChanged(timeline: Timeline, reason: Int) =
        delegate.onTimelineChanged(timeline, reason)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
        delegate.onMediaItemTransition(mediaItem, reason)

    override fun onTracksChanged(tracks: Tracks) =
        delegate.onTracksChanged(tracks)

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) =
        delegate.onMediaMetadataChanged(mediaMetadata)

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) =
        delegate.onPlaylistMetadataChanged(mediaMetadata)

    override fun onIsLoadingChanged(isLoading: Boolean) =
        delegate.onIsLoadingChanged(isLoading)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLoadingChanged(isLoading: Boolean) =
        delegate.onLoadingChanged(isLoading)

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
        delegate.onAvailableCommandsChanged(augmentCommands(availableCommands))

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
        delegate.onTrackSelectionParametersChanged(parameters)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
        delegate.onPlayerStateChanged(playWhenReady, playbackState)

    override fun onPlaybackStateChanged(playbackState: Int) =
        delegate.onPlaybackStateChanged(playbackState)

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
        delegate.onPlayWhenReadyChanged(playWhenReady, reason)

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
        delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)

    override fun onIsPlayingChanged(isPlaying: Boolean) =
        delegate.onIsPlayingChanged(isPlaying)

    override fun onRepeatModeChanged(repeatMode: Int) =
        delegate.onRepeatModeChanged(repeatMode)

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
        delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)

    override fun onPlayerError(error: PlaybackException) =
        delegate.onPlayerError(error)

    override fun onPlayerErrorChanged(error: PlaybackException?) =
        delegate.onPlayerErrorChanged(error)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPositionDiscontinuity(reason: Int) =
        delegate.onPositionDiscontinuity(reason)

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) = delegate.onPositionDiscontinuity(oldPosition, newPosition, reason)

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
        delegate.onPlaybackParametersChanged(playbackParameters)

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) =
        delegate.onSeekBackIncrementChanged(seekBackIncrementMs)

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
        delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)

    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
        delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)

    override fun onAudioSessionIdChanged(audioSessionId: Int) =
        delegate.onAudioSessionIdChanged(audioSessionId)

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) =
        delegate.onAudioAttributesChanged(audioAttributes)

    override fun onVolumeChanged(volume: Float) =
        delegate.onVolumeChanged(volume)

    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) =
        delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) =
        delegate.onDeviceInfoChanged(deviceInfo)

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) =
        delegate.onDeviceVolumeChanged(volume, muted)

    override fun onVideoSizeChanged(videoSize: VideoSize) =
        delegate.onVideoSizeChanged(videoSize)

    override fun onSurfaceSizeChanged(width: Int, height: Int) =
        delegate.onSurfaceSizeChanged(width, height)

    override fun onRenderedFirstFrame() =
        delegate.onRenderedFirstFrame()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCues(cues: MutableList<Cue>) =
        delegate.onCues(cues)

    override fun onCues(cueGroup: CueGroup) =
        delegate.onCues(cueGroup)

    override fun onMetadata(metadata: Metadata) =
        delegate.onMetadata(metadata)
}
