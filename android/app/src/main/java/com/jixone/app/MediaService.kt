package com.jixone.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MediaService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private var title = ""
    private var artist = ""
    private var duration = 0L
    private var position = 0L
    private var playing = false
    private var artworkUrl = ""
    private var artwork: Bitmap? = null
    private var accent = -1
    private var gotMedia = false

    companion object {
        const val CHANNEL = "jixone_media"
        const val NOTIF_ID = 42
        const val ACTION_PLAY = "com.jixone.PLAY"
        const val ACTION_PAUSE = "com.jixone.PAUSE"
        const val ACTION_NEXT = "com.jixone.NEXT"
        const val ACTION_PREV = "com.jixone.PREV"
        const val ACTION_STOP = "com.jixone.STOP"
        const val EXTRA_MEDIA = "media_json"
        const val EXTRA_PLAYING = "playing"

        fun cmd(context: Context, mediaJson: String? = null, playing: Boolean? = null) {
            val i = Intent(context, MediaService::class.java)
            if (mediaJson != null) i.putExtra(EXTRA_MEDIA, mediaJson)
            if (playing != null) i.putExtra(EXTRA_PLAYING, playing)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun action(context: Context, action: String) {
            context.startService(Intent(context, MediaService::class.java).setAction(action))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSessionCompat(this, "JixOneSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = dispatchJs("play")
                override fun onPause() = dispatchJs("pause")
                override fun onSkipToNext() = dispatchJs("next")
                override fun onSkipToPrevious() = dispatchJs("prev")
                override fun onSeekTo(pos: Long) = dispatchJs("seek(${pos / 1000})")
                override fun onStop() {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun dispatchJs(call: String) {
        JixOneHost.activity?.evalJs("window.NativeAction && NativeAction.$call()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> dispatchJs("play")
            ACTION_PAUSE -> dispatchJs("pause")
            ACTION_NEXT -> dispatchJs("next")
            ACTION_PREV -> dispatchJs("prev")
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        intent?.let {
            if (it.hasExtra(EXTRA_MEDIA)) handleMediaJson(it.getStringExtra(EXTRA_MEDIA) ?: "")
            if (it.hasExtra(EXTRA_PLAYING)) {
                playing = it.getBooleanExtra(EXTRA_PLAYING, playing)
                pushState()
                refreshNotification()
            }
        }
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "JixOne", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (title.isBlank()) "JixOne" else title)
            .setContentText(if (artist.isBlank()) (getString(R.string.app_name)) else artist)
            .setContentIntent(pi)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(style)
        if (accent != -1) b.color = accent
        artwork?.let { b.setLargeIcon(it) }

        val pf = PendingIntent.getService(this, 1, Intent(this, MediaService::class.java).setAction(ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pp = PendingIntent.getService(this, 2, Intent(this, MediaService::class.java).setAction(if (playing) ACTION_PAUSE else ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pn = PendingIntent.getService(this, 3, Intent(this, MediaService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        b.addAction(R.drawable.ic_prev, "Prev", pf)
        b.addAction(if (playing) R.drawable.ic_pause else R.drawable.ic_play, if (playing) "Pause" else "Play", pp)
        b.addAction(R.drawable.ic_next, "Next", pn)
        return b.build()
    }

    private fun pushState() {
        val ms = mediaSession ?: return
        ms.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title.ifBlank { "JixOne" })
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .build()
        )
        val actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP
        ms.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    position * 1000, if (playing) 1f else 0f
                )
                .build()
        )
        WidgetProvider.pushUpdate(this, title, artist, artwork, playing)
    }

    private fun refreshNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun loadArtworkIfNeeded(url: String) {
        if (url == artworkUrl && artwork != null) return
        artworkUrl = url
        Thread {
            try {
                val full = if (url.startsWith("http")) url else baseURLPrefix() + url
                val conn = URL(full).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                if (bmp != null) {
                    artwork = bmp
                    refreshNotification()
                    pushState()
                }
            } catch (_: Exception) { /* offline; keep old art */ }
        }.start()
    }

    private fun baseURLPrefix(): String =
        getSharedPreferences("jixone", MODE_PRIVATE).getString("base_url", "") ?: ""

    private fun handleMediaJson(json: String) {
        try {
            val o = JSONObject(json)
            if (o.has("themeAccent")) {
                try {
                    accent = android.graphics.Color.parseColor(o.getString("themeAccent"))
                    refreshNotification()
                } catch (_: Exception) { }
                return
            }
            if (o.has("pos")) {
                position = o.optLong("pos", position)
                pushState()
                return
            }
            if (!gotMedia) gotMedia = true
            title = o.optString("title", title)
            artist = o.optString("artist", artist)
            duration = o.optLong("duration", duration)
            val art = o.optString("artwork", "")
            if (art.isNotBlank()) loadArtworkIfNeeded(art)
            pushState()
            refreshNotification()
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}

/** JS bridge attached to the WebView — forwards media state to MediaService */
class WebBridge(private val context: Context) {
    @android.webkit.JavascriptInterface
    fun updateMedia(json: String) {
        try {
            val o = JSONObject(json)
            if (o.has("themeAccent")) {
                WidgetProvider.saveAccent(context, o.getString("themeAccent"))
            }
        } catch (_: Exception) { }
        MediaService.cmd(context, mediaJson = json)
    }

    @android.webkit.JavascriptInterface
    fun setPlaying(p: Boolean) {
        MediaService.cmd(context, playing = p)
    }

    @android.webkit.JavascriptInterface
    fun notify(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    /** Advanced mode: current custom server URL ("" = built-in standalone mode) */
    @android.webkit.JavascriptInterface
    fun getServerUrl(): String =
        context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
            .getString("base_url", "") ?: ""

    @android.webkit.JavascriptInterface
    fun setServerUrl(url: String) {
        val u = url.trim().trimEnd('/')
        context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
            .edit().putString("base_url", u).apply()
        JixOneHost.activity?.runOnUiThread { JixOneHost.activity?.recreate() }
    }

    @android.webkit.JavascriptInterface
    fun clearServerUrl() {
        context.getSharedPreferences("jixone", Context.MODE_PRIVATE)
            .edit().remove("base_url").apply()
        JixOneHost.activity?.runOnUiThread { JixOneHost.activity?.recreate() }
    }
}
