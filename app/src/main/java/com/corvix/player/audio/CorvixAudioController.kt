package com.corvix.player.audio

import android.content.ComponentName
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.corvix.player.model.RepeatMode
import com.corvix.player.model.ReverbSpace
import com.corvix.player.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class CorvixAudioController(
    private val context: Context,
    private val scope: CorvixCoroutineScopeWrapper? = null,
    private val onPlaybackStateChanged: (isPlaying: Boolean, positionSec: Int) -> Unit,
    private val onTrackCompleted: () -> Unit,
    private val onHardwareOutputDetected: (String) -> Unit
) {
    constructor(
        context: Context,
        scope: CoroutineScope,
        onPlaybackStateChanged: (isPlaying: Boolean, positionSec: Int) -> Unit,
        onTrackCompleted: () -> Unit,
        onHardwareOutputDetected: (String) -> Unit
    ) : this(
        context = context,
        scope = CorvixCoroutineScopeWrapper(scope),
        onPlaybackStateChanged = onPlaybackStateChanged,
        onTrackCompleted = onTrackCompleted,
        onHardwareOutputDetected = onHardwareOutputDetected
    )

    class CorvixCoroutineScopeWrapper(val delegate: CoroutineScope)

    private val workScope: CoroutineScope
        get() = scope?.delegate ?: kotlinx.coroutines.GlobalScope

    private var controller: MediaController? = null
    private var pendingTrack: Track? = null
    private var pendingAutoPlay: Boolean = false
    private var nowPlayingVisible: Boolean = false
    private var isPlayingFlag: Boolean = false
    private var visualizerJob: Job? = null

    private val _playbackPositionSec = MutableStateFlow(0)
    val playbackPositionSecFlow: StateFlow<Int> = _playbackPositionSec.asStateFlow()

    private val _visualizerLevels = MutableStateFlow(FloatArray(12) { 0.25f })
    val visualizerLevelsFlow: StateFlow<FloatArray> = _visualizerLevels.asStateFlow()

    fun connect() {
        val token = SessionToken(context, ComponentName(context, CorvixPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching {
                    val ctrl = future.get()
                    controller = ctrl
                    ctrl.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            isPlayingFlag = isPlaying
                            val sec = (ctrl.currentPosition / 1000L).toInt().coerceAtLeast(0)
                            _playbackPositionSec.value = sec
                            onPlaybackStateChanged(isPlaying, sec)
                            updateVisualizerLoop()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                onTrackCompleted()
                            }
                        }

                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                            CorvixDspEngine.attachSession(audioSessionId)
                        }
                    })
                    pendingTrack?.let { loadTrack(it, pendingAutoPlay) }
                    registerDeviceCallback()
                    detectAudioSink()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private var deviceCallbackRegistered = false

    private fun registerDeviceCallback() {
        if (deviceCallbackRegistered) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        deviceCallbackRegistered = true
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    detectAudioSink()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    detectAudioSink()
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    fun setNowPlayingVisible(visible: Boolean) {
        if (nowPlayingVisible == visible) return
        nowPlayingVisible = visible
        updateVisualizerLoop()
    }

    private fun updateVisualizerLoop() {
        if (!nowPlayingVisible || !isPlayingFlag) {
            visualizerJob?.cancel()
            visualizerJob = null
            return
        }
        if (visualizerJob?.isActive == true) return
        visualizerJob = workScope.launch(Dispatchers.Default) {
            var tick = 0f
            while (isActive && nowPlayingVisible && isPlayingFlag) {
                tick += 0.28f
                val levels = FloatArray(12) { idx ->
                    (0.45f + 0.50f * sin(tick + idx * 0.55f)).coerceIn(0.12f, 1f)
                }
                _visualizerLevels.value = levels
                delay(48L)
            }
        }
    }

    fun detectAudioSink() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val usbDev = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        val wiredDev = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }
        val btDev = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        val label = when {
            usbDev != null -> usbDev.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { "USB DAC · $it" } ?: "USB DAC"
            wiredDev != null -> wiredDev.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { "Wired IEM · $it" } ?: "Wired IEM"
            btDev != null -> btDev.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { "BT · $it" } ?: "Bluetooth LDAC"
            else -> "Phone Speaker"
        }
        onHardwareOutputDetected(label)
    }

    fun loadTrack(track: Track, autoPlay: Boolean) {
        val ctrl = controller
        if (ctrl == null) {
            pendingTrack = track
            pendingAutoPlay = autoPlay
            return
        }
        workScope.launch(Dispatchers.Main) {
            val uri = LocalLibraryScanner.ensureShowcaseAudioUri(context, track)
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
            ctrl.setMediaItem(item)
            ctrl.prepare()
            if (autoPlay) {
                ctrl.play()
            } else {
                ctrl.pause()
            }
        }
    }

    fun setPlaying(play: Boolean) {
        isPlayingFlag = play
        updateVisualizerLoop()
        val ctrl = controller ?: return
        if (play) {
            if (ctrl.mediaItemCount == 0 && pendingTrack != null) {
                loadTrack(pendingTrack!!, true)
            } else {
                ctrl.play()
            }
        } else {
            ctrl.pause()
        }
    }

    fun seekToSec(sec: Int) {
        _playbackPositionSec.value = sec.coerceAtLeast(0)
        val ctrl = controller ?: return
        val dur = ctrl.duration.takeIf { it > 0L } ?: (sec * 1000L)
        val targetMs = (sec * 1000L).coerceIn(0L, dur)
        ctrl.seekTo(targetMs)
    }

    fun currentPositionSec(): Int? {
        val ctrl = controller ?: return null
        if (ctrl.mediaItemCount == 0) return null
        val sec = (ctrl.currentPosition / 1000L).toInt().coerceAtLeast(0)
        _playbackPositionSec.value = sec
        return sec
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setVolume(volumeFraction: Float) {
        controller?.volume = volumeFraction.coerceIn(0f, 1f)
    }

    fun applyEqualizer(
        enabled: Boolean,
        bandsDb: List<Int>,
        eq10Band: Boolean = false,
        bassPct: Int,
        virtPct: Int,
        reverbSpace: ReverbSpace
    ) {
        CorvixDspEngine.updateSettings(
            enabled = enabled,
            bandsDb = bandsDb,
            eq10Band = eq10Band,
            bassPct = bassPct,
            virtPct = virtPct,
            reverbSpace = reverbSpace
        )
    }

    fun release() {
        visualizerJob?.cancel()
        visualizerJob = null
        controller?.release()
        controller = null
    }
}
