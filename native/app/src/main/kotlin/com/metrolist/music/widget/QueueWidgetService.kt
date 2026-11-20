package com.metrolist.music.widget

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.metrolist.music.R

/**
 * The play queue as the widget last saw it. MusicService refreshes it on every
 * widget update and saves it, so the list still shows (and its rows still
 * work) after the app process has been killed.
 */
object WidgetQueue {
    @Volatile var items: List<Pair<String, String>> = emptyList()
    @Volatile var current: Int = -1

    private const val PREFS = "widget_queue"

    fun save(context: Context) {
        val json = JSONArray()
        items.forEach { (title, artist) -> json.put(JSONArray().put(title).put(artist)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("items", json.toString())
            .putInt("current", current)
            .apply()
    }

    fun loadIfEmpty(context: Context) {
        if (items.isNotEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrNull() ?: return
        items = (0 until json.length()).map { i ->
            val row = json.getJSONArray(i)
            row.optString(0) to row.optString(1)
        }
        current = prefs.getInt("current", -1)
    }
}

/** Feeds the queue list shown under the player when the widget is stretched taller. */
class QueueWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        object : RemoteViewsFactory {
            private var items: List<Pair<String, String>> = emptyList()
            private var current = -1

            override fun onCreate() = Unit

            override fun onDataSetChanged() {
                WidgetQueue.loadIfEmpty(applicationContext)
                items = WidgetQueue.items
                current = WidgetQueue.current
            }

            override fun onDestroy() = Unit

            override fun getCount() = items.size

            override fun getViewAt(position: Int): RemoteViews {
                val views = RemoteViews(packageName, R.layout.widget_queue_item)
                val (title, artist) = items.getOrNull(position) ?: ("" to "")
                views.setTextViewText(R.id.widget_queue_title, title)
                views.setTextViewText(R.id.widget_queue_artist, artist)
                // The playing track is tinted, like the queue in the app.
                views.setTextColor(
                    R.id.widget_queue_title,
                    if (position == current) 0xFFB9A8FF.toInt() else 0xFFFFFFFF.toInt(),
                )
                views.setOnClickFillInIntent(
                    R.id.widget_queue_row,
                    Intent().putExtra(MusicWidgetReceiver.EXTRA_INDEX, position),
                )
                return views
            }

            override fun getLoadingView(): RemoteViews? = null

            override fun getViewTypeCount() = 1

            override fun getItemId(position: Int) = position.toLong()

            override fun hasStableIds() = false
        }
}
