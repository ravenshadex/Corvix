package com.corvix.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import com.corvix.player.MainActivity
import com.corvix.player.R
import com.corvix.player.model.formatSeconds

/**
 * Shared bridge connecting CorvixViewModel playback state & transport commands
 * to both home screen widgets (Liquid Glass & Winamp Classic).
 */
object CorvixWidgetBridge {
    const val ACTION_PREV = "com.corvix.player.widget.ACTION_PREV"
    const val ACTION_PLAY_PAUSE = "com.corvix.player.widget.ACTION_PLAY_PAUSE"
    const val ACTION_PLAY = "com.corvix.player.widget.ACTION_PLAY"
    const val ACTION_PAUSE = "com.corvix.player.widget.ACTION_PAUSE"
    const val ACTION_STOP = "com.corvix.player.widget.ACTION_STOP"
    const val ACTION_NEXT = "com.corvix.player.widget.ACTION_NEXT"
    const val ACTION_SHUFFLE = "com.corvix.player.widget.ACTION_SHUFFLE"
    const val EXTRA_WIDGET_TARGET = "extra_widget_target"

    private const val PREFS_NAME = "corvix_widget_prefs"

    @Volatile
    var actionHandler: ((String) -> Unit)? = null

    data class WidgetSnapshot(
        val title: String,
        val artist: String,
        val album: String,
        val shortFormat: String,
        val durationFormatted: String,
        val isPlaying: Boolean,
        val elapsedSec: Int = 0,
        val durationSec: Int = 225,
        val kbps: String = "320",
        val khz: String = "44",
        val isShuffle: Boolean = false,
        val artBitmap: Bitmap? = null
    )

    @Volatile
    private var latestArtBitmap: Bitmap? = null

    fun hasActiveWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val glassIds = manager.getAppWidgetIds(ComponentName(context, CorvixGlassWidgetProvider::class.java))
        val winampIds = manager.getAppWidgetIds(ComponentName(context, CorvixWinampWidgetProvider::class.java))
        val winamp2xIds = manager.getAppWidgetIds(ComponentName(context, WinampWidgetProvider::class.java))
        return glassIds.isNotEmpty() || winampIds.isNotEmpty() || winamp2xIds.isNotEmpty()
    }

    fun publishState(context: Context, snapshot: WidgetSnapshot) {
        latestArtBitmap = snapshot.artBitmap
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("title", snapshot.title)
            .putString("artist", snapshot.artist)
            .putString("album", snapshot.album)
            .putString("format", snapshot.shortFormat)
            .putString("duration", snapshot.durationFormatted)
            .putBoolean("playing", snapshot.isPlaying)
            .putInt("elapsed", snapshot.elapsedSec)
            .putInt("durSec", snapshot.durationSec)
            .putString("kbps", snapshot.kbps)
            .putString("khz", snapshot.khz)
            .putBoolean("shuffle", snapshot.isShuffle)
            .apply()
        updateAllWidgets(context)
    }

    fun readSnapshot(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WidgetSnapshot(
            title = prefs.getString("title", "Corvix Hi-Fi") ?: "Corvix Hi-Fi",
            artist = prefs.getString("artist", "Local Audio") ?: "Local Audio",
            album = prefs.getString("album", "Offline Library") ?: "Offline Library",
            shortFormat = prefs.getString("format", "FLAC") ?: "FLAC",
            durationFormatted = prefs.getString("duration", "03:45") ?: "03:45",
            isPlaying = prefs.getBoolean("playing", false),
            elapsedSec = prefs.getInt("elapsed", 0),
            durationSec = prefs.getInt("durSec", 225),
            kbps = prefs.getString("kbps", "320") ?: "320",
            khz = prefs.getString("khz", "44") ?: "44",
            isShuffle = prefs.getBoolean("shuffle", false),
            artBitmap = latestArtBitmap
        )
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val snap = readSnapshot(context)

        val glassIds = manager.getAppWidgetIds(ComponentName(context, CorvixGlassWidgetProvider::class.java))
        for (id in glassIds) {
            manager.updateAppWidget(id, buildGlassRemoteViews(context, snap))
        }

        val winampIds = manager.getAppWidgetIds(ComponentName(context, CorvixWinampWidgetProvider::class.java))
        for (id in winampIds) {
            manager.updateAppWidget(id, buildWinampRemoteViews(context, snap))
        }

        WinampWidgetProvider.updateAll(context)
    }

    fun requestPinGlassWidget(context: Context): Boolean =
        requestPinWidget(context, CorvixGlassWidgetProvider::class.java)

    fun requestPinWinampWidget(context: Context): Boolean =
        requestPinWidget(context, CorvixWinampWidgetProvider::class.java)

    private fun requestPinWidget(context: Context, providerClass: Class<*>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, providerClass)
        return manager.requestPinAppWidget(provider, null, null)
    }

    internal fun handleWidgetIntent(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action in setOf(ACTION_PREV, ACTION_PLAY_PAUSE, ACTION_PLAY, ACTION_PAUSE, ACTION_STOP, ACTION_NEXT, ACTION_SHUFFLE)) {
            val handler = actionHandler
            if (handler != null) {
                handler(action)
            } else {
                val launch = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(launch)
            }
        }
    }

    private fun openAppPendingIntent(
        context: Context,
        requestCode: Int,
        target: String = "now_playing"
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WIDGET_TARGET, target)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionPendingIntent(
        context: Context,
        providerClass: Class<*>,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, providerClass).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildGlassRemoteViews(context: Context, snap: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_corvix_glass)
        views.setTextViewText(R.id.widget_glass_title, snap.title)
        views.setTextViewText(R.id.widget_glass_subtitle, "${snap.artist} · ${snap.shortFormat}")
        views.setTextViewText(R.id.widget_glass_play, if (snap.isPlaying) "⏸" else "▶")
        if (snap.artBitmap != null) {
            views.setImageViewBitmap(R.id.widget_glass_art, snap.artBitmap)
        } else {
            views.setImageViewResource(R.id.widget_glass_art, R.mipmap.ic_launcher)
        }
        views.setOnClickPendingIntent(R.id.widget_glass_root, openAppPendingIntent(context, 100))
        views.setOnClickPendingIntent(
            R.id.widget_glass_prev,
            actionPendingIntent(context, CorvixGlassWidgetProvider::class.java, ACTION_PREV, 101)
        )
        views.setOnClickPendingIntent(
            R.id.widget_glass_play,
            actionPendingIntent(context, CorvixGlassWidgetProvider::class.java, ACTION_PLAY_PAUSE, 102)
        )
        views.setOnClickPendingIntent(
            R.id.widget_glass_next,
            actionPendingIntent(context, CorvixGlassWidgetProvider::class.java, ACTION_NEXT, 103)
        )
        return views
    }

    private fun buildWinampRemoteViews(context: Context, snap: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_corvix_winamp)
        val progressPct = ((snap.elapsedSec.toFloat() / snap.durationSec.coerceAtLeast(1)) * 100f).toInt().coerceIn(0, 100)
        val spectrumPct = if (snap.isPlaying) ((snap.elapsedSec * 37 + 45) % 65 + 30) else 12

        views.setTextViewText(R.id.widget_winamp_status_glyph, if (snap.isPlaying) "▶" else "⏸")
        views.setTextViewText(R.id.widget_winamp_time, formatSeconds(snap.elapsedSec))
        views.setProgressBar(R.id.widget_winamp_spectrum, 100, spectrumPct, false)
        views.setTextViewText(R.id.widget_winamp_title, "${snap.title.uppercase()} — ${snap.artist.uppercase()}   ")
        views.setTextViewText(R.id.widget_winamp_kbps, snap.kbps)
        views.setTextViewText(R.id.widget_winamp_khz, snap.khz)
        views.setProgressBar(R.id.widget_winamp_seek, 100, progressPct, false)
        views.setTextColor(
            R.id.widget_winamp_shuffle,
            if (snap.isShuffle) 0xFF00FF00.toInt() else 0xFFA4ABC2.toInt()
        )

        views.setOnClickPendingIntent(R.id.widget_winamp_lcd_box, openAppPendingIntent(context, 200, "now_playing"))
        views.setOnClickPendingIntent(R.id.widget_winamp_title, openAppPendingIntent(context, 201, "now_playing"))
        views.setOnClickPendingIntent(R.id.widget_winamp_eq, openAppPendingIntent(context, 202, "eq"))
        views.setOnClickPendingIntent(R.id.widget_winamp_pl, openAppPendingIntent(context, 203, "playlists"))
        views.setOnClickPendingIntent(R.id.widget_winamp_bolt, openAppPendingIntent(context, 204, "now_playing"))

        views.setOnClickPendingIntent(
            R.id.widget_winamp_prev,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_PREV, 210)
        )
        views.setOnClickPendingIntent(
            R.id.widget_winamp_play,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_PLAY, 211)
        )
        views.setOnClickPendingIntent(
            R.id.widget_winamp_pause,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_PAUSE, 212)
        )
        views.setOnClickPendingIntent(
            R.id.widget_winamp_stop,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_STOP, 213)
        )
        views.setOnClickPendingIntent(
            R.id.widget_winamp_next,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_NEXT, 214)
        )
        views.setOnClickPendingIntent(
            R.id.widget_winamp_shuffle,
            actionPendingIntent(context, CorvixWinampWidgetProvider::class.java, ACTION_SHUFFLE, 215)
        )
        return views
    }
}

class CorvixGlassWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CorvixWidgetBridge.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        CorvixWidgetBridge.handleWidgetIntent(context, intent)
    }
}

class CorvixWinampWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CorvixWidgetBridge.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        CorvixWidgetBridge.handleWidgetIntent(context, intent)
    }
}
