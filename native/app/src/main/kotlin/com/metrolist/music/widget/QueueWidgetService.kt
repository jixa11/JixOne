package com.metrolist.music.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.metrolist.music.R

/** The play queue as the widget last saw it. MusicService refreshes it on every widget update. */
object WidgetQueue {
    @Volatile var items: List<Pair<String, String>> = emptyList()
    @Volatile var current: Int = -1
}

/** Feeds the queue list shown under the player when the widget is stretched taller. */
class QueueWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        object : RemoteViewsFactory {
            private var items: List<Pair<String, String>> = emptyList()
            private var current = -1

            override fun onCreate() = Unit

            override fun onDataSetChanged() {
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
