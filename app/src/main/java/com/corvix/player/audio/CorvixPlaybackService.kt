package com.corvix.player.audio

import android.content.Intent
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
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.corvix.player.model.ReverbSpace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.abs

enum class CorvixMediaCommand {
    TOGGLE_PLAY,
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS
}

object CorvixMediaCommandBus {
    private val _commands = MutableSharedFlow<CorvixMediaCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<CorvixMediaCommand> = _commands.asSharedFlow()

    fun emit(command: CorvixMediaCommand) {
        _commands.tryEmit(command)
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var multiTapCount = 0

    private val multiTapRunnable = Runnable {
        val count = multiTapCount
        multiTapCount = 0
        when {
            count == 1 -> CorvixMediaCommandBus.emit(CorvixMediaCommand.TOGGLE_PLAY)
            count == 2 -> CorvixMediaCommandBus.emit(CorvixMediaCommand.NEXT)
            count >= 3 -> CorvixMediaCommandBus.emit(CorvixMediaCommand.PREVIOUS)
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
            .build()

        exo.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                CorvixDspEngine.attachSession(audioSessionId)
            }
        })
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    CorvixDspEngine.attachSession(exo.audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    CorvixDspEngine.attachSession(exo.audioSessionId)
                }
            }
        })
        if (exo.audioSessionId > 0) {
            CorvixDspEngine.attachSession(exo.audioSessionId)
        }

        val sessionPlayer = object : ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
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
                CorvixMediaCommandBus.emit(CorvixMediaCommand.NEXT)
            }

            override fun seekToNextMediaItem() {
                CorvixMediaCommandBus.emit(CorvixMediaCommand.NEXT)
            }

            override fun seekToPrevious() {
                CorvixMediaCommandBus.emit(CorvixMediaCommand.PREVIOUS)
            }

            override fun seekToPreviousMediaItem() {
                CorvixMediaCommandBus.emit(CorvixMediaCommand.PREVIOUS)
            }
        }

        val callback = object : MediaSession.Callback {
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
                                CorvixMediaCommandBus.emit(CorvixMediaCommand.PREVIOUS)
                            } else {
                                mainHandler.postDelayed(multiTapRunnable, 380L)
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            CorvixMediaCommandBus.emit(CorvixMediaCommand.NEXT)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            CorvixMediaCommandBus.emit(CorvixMediaCommand.PREVIOUS)
                        }
                        true
                    }
                    else -> super.onMediaButtonEvent(session, controllerInfo, mediaButtonEvent)
                }
            }
        }

        player = exo
        mediaSession = MediaSession.Builder(this, sessionPlayer)
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
