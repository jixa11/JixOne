package com.jixone.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val p = context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
        pushUpdate(
            context,
            p.getString("w_title", "JixOne") ?: "JixOne",
            p.getString("w_artist", "") ?: "",
            loadArt(context),
            p.getBoolean("w_playing", false)
        )
    }

    companion object {
        fun saveAccent(context: Context, hex: String) {
            try {
                context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
                    .edit().putString("w_accent", hex).apply()
            } catch (_: Exception) { }
        }

        private fun loadArt(context: Context): Bitmap? =
            try {
                BitmapFactory.decodeFile(context.filesDir.absolutePath + "/widget_art.png")
            } catch (_: Exception) { null }

        fun pushUpdate(context: Context, title: String, artist: String, art: Bitmap?, playing: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val widget = ComponentName(context, WidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(widget) ?: return
            if (ids.isEmpty()) return

            val prefs = context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
            val accent = try {
                Color.parseColor(prefs.getString("w_accent", "#ff2d55"))
            } catch (_: Exception) { Color.parseColor("#ff2d55") }

            // persist for reboot restore
            prefs.edit()
                .putString("w_title", title)
                .putString("w_artist", artist)
                .putBoolean("w_playing", playing)
                .apply()

            if (art != null) {
                try {
                    val side = 512
                    val scaled = android.graphics.Bitmap.createScaledBitmap(art, side, side, true)
                    val f = java.io.File(context.filesDir, "widget_art.png")
                    f.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 90, it) }
                } catch (_: Exception) { }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_jix)
            views.setTextViewText(R.id.w_title, title.ifBlank { "JixOne" })
            views.setTextViewText(R.id.w_artist, artist)
            if (art != null) views.setImageViewBitmap(R.id.w_art, art)
            views.setTextColor(R.id.w_title, Color.WHITE)
            views.setTextColor(R.id.w_artist, Color.parseColor("#9b9ba7"))
            views.setInt(R.id.w_play_btn, "setBackgroundColor", accent)
            views.setImageViewResource(R.id.w_play_icon, if (playing) R.drawable.ic_pause else R.drawable.ic_play)

            val prev = android.app.PendingIntent.getBroadcast(
                context, 10,
                Intent(context, WidgetActions::class.java).setAction(WidgetActions.PREV),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val play = android.app.PendingIntent.getBroadcast(
                context, 11,
                Intent(context, WidgetActions::class.java).setAction(if (playing) WidgetActions.PAUSE else WidgetActions.PLAY),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val next = android.app.PendingIntent.getBroadcast(
                context, 12,
                Intent(context, WidgetActions::class.java).setAction(WidgetActions.NEXT),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.w_prev_btn, prev)
            views.setOnClickPendingIntent(R.id.w_play_btn, play)
            views.setOnClickPendingIntent(R.id.w_next_btn, next)

            manager.updateAppWidget(ids, views)
        }
    }
}

/** Widget button receiver → forwards to MediaService → WebView */
class WidgetActions : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PLAY -> MediaService.action(context, MediaService.ACTION_PLAY)
            PAUSE -> MediaService.action(context, MediaService.ACTION_PAUSE)
            NEXT -> MediaService.action(context, MediaService.ACTION_NEXT)
            PREV -> MediaService.action(context, MediaService.ACTION_PREV)
        }
    }

    companion object {
        const val PLAY = "com.jixone.W_PLAY"
        const val PAUSE = "com.jixone.W_PAUSE"
        const val NEXT = "com.jixone.W_NEXT"
        const val PREV = "com.jixone.W_PREV"
    }
}
