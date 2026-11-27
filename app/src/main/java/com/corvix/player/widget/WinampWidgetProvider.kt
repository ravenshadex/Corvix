package com.corvix.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.RemoteViews
import com.corvix.player.MainActivity
import com.corvix.player.R
import com.corvix.player.model.formatSeconds

/**
 * Classic Winamp 2.x 4x2 Home Screen Widget Provider.
 * Reuses live system MediaSession state (via WinampMediaListenerService) and
 * CorvixWidgetBridge playback state.
 */
class WinampWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (val action = intent.action) {
            ACTION_PREV -> WinampMediaListenerService.dispatchTransport(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS, action)
            ACTION_PLAY -> WinampMediaListenerService.dispatchTransport(context, KeyEvent.KEYCODE_MEDIA_PLAY, action)
            ACTION_PAUSE -> WinampMediaListenerService.dispatchTransport(context, KeyEvent.KEYCODE_MEDIA_PAUSE, action)
            ACTION_STOP -> WinampMediaListenerService.dispatchTransport(context, KeyEvent.KEYCODE_MEDIA_STOP, action)
            ACTION_NEXT -> WinampMediaListenerService.dispatchTransport(context, KeyEvent.KEYCODE_MEDIA_NEXT, action)
            ACTION_SHUFFLE -> {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val next = !prefs.getBoolean("shuffle", false)
                prefs.edit().putBoolean("shuffle", next).apply()
                CorvixWidgetBridge.actionHandler?.invoke(CorvixWidgetBridge.ACTION_SHUFFLE)
                updateAll(context)
            }
            ACTION_REPEAT -> {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val next = !prefs.getBoolean("repeat", false)
                prefs.edit().putBoolean("repeat", next).apply()
                updateAll(context)
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.corvix.player.winamp2x.ACTION_PREV"
        const val ACTION_PLAY = "com.corvix.player.winamp2x.ACTION_PLAY"
        const val ACTION_PAUSE = "com.corvix.player.winamp2x.ACTION_PAUSE"
        const val ACTION_STOP = "com.corvix.player.winamp2x.ACTION_STOP"
        const val ACTION_NEXT = "com.corvix.player.winamp2x.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.corvix.player.winamp2x.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.corvix.player.winamp2x.ACTION_REPEAT"
        private const val PREFS = "winamp_2x_widget_prefs"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WinampWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val views = buildViews(context)
            for (id in ids) {
                mgr.updateAppWidget(id, views)
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_winamp_player)
            val sys = WinampMediaListenerService.latestSystemState
            val corvixSnap = CorvixWidgetBridge.readSnapshot(context)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            val title = sys?.title ?: "${corvixSnap.title} — ${corvixSnap.artist}"
            val isPlaying = sys?.isPlaying ?: corvixSnap.isPlaying
            val elapsed = sys?.elapsedSec ?: corvixSnap.elapsedSec
            val duration = (sys?.durationSec ?: corvixSnap.durationSec).coerceAtLeast(1)
            val progressPct = ((elapsed.toFloat() / duration.toFloat()) * 100f).toInt().coerceIn(0, 100)
            val spectrumLevel = sys?.spectrumLevel ?: if (isPlaying) ((elapsed % 3) + 1) else 0
            val shuffleOn = prefs.getBoolean("shuffle", corvixSnap.isShuffle)
            val repeatOn = prefs.getBoolean("repeat", false)

            views.setTextViewText(R.id.winamp_status_glyph, if (isPlaying) "▶" else "⏸")
            views.setTextViewText(R.id.winamp_timer, formatSeconds(elapsed))
            views.setInt(R.id.winamp_spectrum_img, "setImageLevel", spectrumLevel)
            views.setTextViewText(R.id.winamp_marquee_title, "${title.uppercase()}   ")
            views.setTextViewText(R.id.winamp_kbps_val, corvixSnap.kbps)
            views.setTextViewText(R.id.winamp_khz_val, corvixSnap.khz)
            views.setProgressBar(R.id.winamp_seek_progress, 100, progressPct, false)
            views.setTextColor(R.id.winamp_btn_shuffle, if (shuffleOn) 0xFF008800.toInt() else 0xFF222222.toInt())
            views.setTextColor(R.id.winamp_btn_repeat, if (repeatOn) 0xFF008800.toInt() else 0xFF222222.toInt())

            // LCD & PL open active media player (or Corvix)
            views.setOnClickPendingIntent(R.id.winamp_lcd_box, openPlayerPendingIntent(context, 300, "now_playing"))
            views.setOnClickPendingIntent(R.id.winamp_marquee_title, openPlayerPendingIntent(context, 301, "now_playing"))
            views.setOnClickPendingIntent(R.id.winamp_btn_pl, openPlayerPendingIntent(context, 302, "playlists"))
            views.setOnClickPendingIntent(R.id.winamp_btn_eq, openPlayerPendingIntent(context, 303, "eq"))
            views.setOnClickPendingIntent(R.id.winamp_btn_eject, openPlayerPendingIntent(context, 304, "now_playing"))

            // Lightning bolt opens permission/config screen
            val configIntent = Intent(context, WinampWidgetConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.winamp_btn_bolt,
                PendingIntent.getActivity(context, 305, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Transport PendingIntents
            views.setOnClickPendingIntent(R.id.winamp_btn_prev, broadcastPendingIntent(context, ACTION_PREV, 310))
            views.setOnClickPendingIntent(R.id.winamp_btn_play, broadcastPendingIntent(context, ACTION_PLAY, 311))
            views.setOnClickPendingIntent(R.id.winamp_btn_pause, broadcastPendingIntent(context, ACTION_PAUSE, 312))
            views.setOnClickPendingIntent(R.id.winamp_btn_stop, broadcastPendingIntent(context, ACTION_STOP, 313))
            views.setOnClickPendingIntent(R.id.winamp_btn_next, broadcastPendingIntent(context, ACTION_NEXT, 314))
            views.setOnClickPendingIntent(R.id.winamp_btn_shuffle, broadcastPendingIntent(context, ACTION_SHUFFLE, 315))
            views.setOnClickPendingIntent(R.id.winamp_btn_repeat, broadcastPendingIntent(context, ACTION_REPEAT, 316))

            return views
        }

        private fun openPlayerPendingIntent(context: Context, reqCode: Int, target: String): PendingIntent {
            val activePlayerIntent = if (target == "now_playing") {
                WinampMediaListenerService.getActiveMediaIntent(context)
            } else null

            val intent = activePlayerIntent ?: Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(CorvixWidgetBridge.EXTRA_WIDGET_TARGET, target)
            }
            return PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun broadcastPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(context, WinampWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
