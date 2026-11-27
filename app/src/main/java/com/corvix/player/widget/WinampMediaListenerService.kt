package com.corvix.player.widget

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.KeyEvent

/**
 * Observes active system MediaSessions (Corvix, Spotify, YouTube Music, Poweramp, etc.)
 * and pushes live metadata, elapsed timer, seek progress, and spectrum ticks to WinampWidgetProvider.
 */
class WinampMediaListenerService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var spectrumTick = 1

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            pushFromController(activeController)
            scheduleTickIfPlaying()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            pushFromController(activeController)
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindBestController(controllers.orEmpty())
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            val ctrl = activeController
            val playing = ctrl?.playbackState?.state == PlaybackState.STATE_PLAYING
            if (playing) {
                spectrumTick = (spectrumTick % 3) + 1
                pushFromController(ctrl)
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        val mgr = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        sessionManager = mgr
        val listenerComp = ComponentName(this, WinampMediaListenerService::class.java)
        runCatching {
            mgr?.addOnActiveSessionsChangedListener(sessionsListener, listenerComp)
            val current = mgr?.getActiveSessions(listenerComp).orEmpty()
            bindBestController(current)
        }
    }

    override fun onListenerDisconnected() {
        mainHandler.removeCallbacks(tickRunnable)
        runCatching { activeController?.unregisterCallback(controllerCallback) }
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        activeController = null
        instance = null
        super.onListenerDisconnected()
    }

    private fun bindBestController(controllers: List<MediaController>) {
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (chosen?.sessionToken != activeController?.sessionToken) {
            runCatching { activeController?.unregisterCallback(controllerCallback) }
            activeController = chosen
            runCatching { chosen?.registerCallback(controllerCallback) }
        }
        pushFromController(chosen)
        scheduleTickIfPlaying()
    }

    private fun scheduleTickIfPlaying() {
        mainHandler.removeCallbacks(tickRunnable)
        if (activeController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            mainHandler.postDelayed(tickRunnable, 1000L)
        }
    }

    private fun pushFromController(ctrl: MediaController?) {
        if (ctrl == null) {
            WinampWidgetProvider.updateAll(this)
            return
        }
        val meta = ctrl.metadata
        val pb = ctrl.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "WINAMP 2.x RETRO PLAYER"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(1000L)

        val isPlaying = pb?.state == PlaybackState.STATE_PLAYING
        val basePosMs = (pb?.position ?: 0L).coerceAtLeast(0L)
        val elapsedMs = if (isPlaying && pb != null) {
            val delta = SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
            val speed = if (pb.playbackSpeed > 0f) pb.playbackSpeed else 1f
            (basePosMs + (delta * speed).toLong()).coerceIn(0L, durationMs)
        } else {
            basePosMs.coerceIn(0L, durationMs)
        }

        val displayTitle = if (artist.isNotBlank()) "$title — $artist" else title
        latestSystemState = SystemMediaSnapshot(
            title = displayTitle,
            isPlaying = isPlaying,
            elapsedSec = (elapsedMs / 1000L).toInt(),
            durationSec = (durationMs / 1000L).toInt().coerceAtLeast(1),
            spectrumLevel = if (isPlaying) spectrumTick else 0,
            packageName = ctrl.packageName
        )
        WinampWidgetProvider.updateAll(this)
    }

    companion object {
        data class SystemMediaSnapshot(
            val title: String,
            val isPlaying: Boolean,
            val elapsedSec: Int,
            val durationSec: Int,
            val spectrumLevel: Int,
            val packageName: String?
        )

        @Volatile
        private var instance: WinampMediaListenerService? = null

        @Volatile
        var latestSystemState: SystemMediaSnapshot? = null
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val comp = ComponentName(context, WinampMediaListenerService::class.java).flattenToString()
            return flat.split(":").any { it.equals(comp, ignoreCase = true) }
        }

        fun dispatchTransport(context: Context, keyCode: Int, action: String) {
            val ctrl = instance?.activeController
            if (ctrl != null) {
                val tc = ctrl.transportControls
                when (action) {
                    WinampWidgetProvider.ACTION_PREV -> tc.skipToPrevious()
                    WinampWidgetProvider.ACTION_PLAY -> tc.play()
                    WinampWidgetProvider.ACTION_PAUSE -> tc.pause()
                    WinampWidgetProvider.ACTION_STOP -> tc.stop()
                    WinampWidgetProvider.ACTION_NEXT -> tc.skipToNext()
                    else -> dispatchMediaKeyFallback(context, keyCode)
                }
                return
            }
            // Fallback to Corvix in-app handler or global AudioManager media key
            val corvixAction = when (action) {
                WinampWidgetProvider.ACTION_PREV -> CorvixWidgetBridge.ACTION_PREV
                WinampWidgetProvider.ACTION_PLAY -> CorvixWidgetBridge.ACTION_PLAY
                WinampWidgetProvider.ACTION_PAUSE -> CorvixWidgetBridge.ACTION_PAUSE
                WinampWidgetProvider.ACTION_STOP -> CorvixWidgetBridge.ACTION_STOP
                WinampWidgetProvider.ACTION_NEXT -> CorvixWidgetBridge.ACTION_NEXT
                WinampWidgetProvider.ACTION_SHUFFLE -> CorvixWidgetBridge.ACTION_SHUFFLE
                else -> null
            }
            val handler = CorvixWidgetBridge.actionHandler
            if (handler != null && corvixAction != null) {
                handler(corvixAction)
                return
            }
            dispatchMediaKeyFallback(context, keyCode)
        }

        fun getActiveMediaIntent(context: Context): android.content.Intent? {
            val pkg = latestSystemState?.packageName
            if (!pkg.isNullOrBlank()) {
                context.packageManager.getLaunchIntentForPackage(pkg)?.let { return it }
            }
            return null
        }

        private fun dispatchMediaKeyFallback(context: Context, keyCode: Int) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }
}
