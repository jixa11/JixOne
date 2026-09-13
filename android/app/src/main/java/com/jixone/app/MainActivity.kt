package com.jixone.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.net.HttpURLConnection
import java.net.URL

/**
 * JixOne is standalone: the whole web app ships inside the APK and is served from
 * app assets over https://appassets.androidplatform.net (a secure origin, so
 * OPFS offline storage, localStorage and mediaSession all work).
 *
 * Optional advanced mode: if a custom server URL is stored in prefs ("base_url"),
 * the WebView loads that remote JixOne web app instead of the bundled one.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    companion object {
        const val EMBEDDED_BASE = "https://appassets.androidplatform.net/"
        const val EMBEDDED_INDEX = EMBEDDED_BASE + "assets/web/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        val prefs = getSharedPreferences("jixone", MODE_PRIVATE)
        val remote = prefs.getString("base_url", null)?.trim()?.trimEnd('/')
        launchWeb(remote?.takeIf { it.isNotBlank() })
    }

    /**
     * From Android 13 POST_NOTIFICATIONS is a runtime permission. It was
     * declared in the manifest but never asked for, so the media notification
     * and its lock-screen controls silently never appeared on any recent phone
     * — the foreground service still ran, but nothing was shown.
     */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun launchWeb(remoteUrl: String?) {
        val baseUrl = remoteUrl ?: EMBEDDED_BASE

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(webView)
        window.statusBarColor = Color.parseColor("#0a0a0f")

        WebSettingsCompat(webView.settings)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView.webViewClient = NeoClient(baseUrl, assetLoader)
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebBridge(applicationContext), "AndroidBridge")
        webView.setBackgroundColor(Color.parseColor("#0a0a0f"))

        // Always load fresh — never restore a dead WebView session (black screen fix).
        // Local assets + HTTP cache make this fast.
        webView.loadUrl(remoteUrl ?: EMBEDDED_INDEX)
    }

    private fun WebSettingsCompat(s: WebSettings) {
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
    }

    private inner class NeoClient(
        private val baseUrl: String,
        private val assetLoader: WebViewAssetLoader,
    ) : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? {
            val url = request?.url ?: return null
            // Native stream proxy: JS hands us a base64url-encoded upstream URL
            // (googlevideo) and the UA that must accompany it. Fetching happens
            // in the native layer with the client-matching User-Agent and Range
            // passthrough — this is what makes ad-free playback and downloads
            // work (direct WebView→googlevideo fails: CORS + IP/UA checks).
            if (url.host == "appassets.androidplatform.net" && url.path == "/jixstream/") {
                return streamProxyResponse(request)
            }
            return assetLoader.shouldInterceptRequest(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url ?: return false
            // stream-proxy requests are handled internally regardless of page origin
            if (url.host == "appassets.androidplatform.net" && url.path == "/jixstream/") return false
            // keep our host inside; open others (youtube pages) in browser
            return if (url.toString().startsWith(baseUrl)) false else true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            // re-attach native action bridge after each load
            view?.evaluateJavascript(
                """(function(){ if(!window.__nativeBridgeReady){ window.__nativeBridgeReady=true; }})();""",
                null
            )
        }
    }

    /** b64url (no padding) → String */
    private fun b64urlDecode(s: String): String {
        val norm = s.replace('-', '+').replace('_', '/')
        val padded = norm + "=".repeat((4 - norm.length % 4) % 4)
        return String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), Charsets.UTF_8)
    }

    /** Stream googlevideo through the native layer (UA-matched, Range-aware). */
    private fun streamProxyResponse(request: WebResourceRequest): WebResourceResponse? {
        var conn: HttpURLConnection? = null
        return try {
            val url = request.url
            val upstream = b64urlDecode(url.getQueryParameter("u") ?: return plainResponse(400, "missing u"))
            val ua = url.getQueryParameter("ua")?.let { b64urlDecode(it) }
                ?: "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            val mime = url.getQueryParameter("ct")?.let { b64urlDecode(it) }

            val c = URL(upstream).openConnection() as HttpURLConnection
            conn = c
            c.connectTimeout = 12000
            c.readTimeout = 30000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", ua)
            c.setRequestProperty("Referer", "https://music.youtube.com/")
            request.requestHeaders["range"]?.let { c.setRequestProperty("Range", it) }

            val code = c.responseCode
            if (code !in 200..299) {
                return plainResponse(code, "upstream $code")
            }

            val headers = mutableMapOf<String, String>()
            for ((k, v) in c.headerFields) {
                if (k == null || v.isEmpty()) continue
                val key = k.lowercase()
                if (key == "content-encoding" || key == "transfer-encoding" || key == "connection") continue
                headers[key] = v.joinToString(", ")
            }
            headers["access-control-allow-origin"] = "*"
            val contentType = mime ?: headers["content-type"] ?: "audio/mp4"
            WebResourceResponse(contentType, null, code, if (code == 206) "Partial Content" else "OK", headers, c.inputStream)
        } catch (e: Exception) {
            try { conn?.disconnect() } catch (_: Exception) {}
            plainResponse(502, "proxy error")
        }
    }

    private fun plainResponse(status: Int, reason: String): WebResourceResponse {
        val body = reason.toByteArray(Charsets.UTF_8).inputStream()
        return WebResourceResponse("text/plain", "utf-8", status, reason, mutableMapOf(), body)
    }

    /**
     * WebView.onPause() suspends the whole DOM, the <audio> element included,
     * so backgrounding the app or locking the screen killed playback outright.
     * Only suspend when nothing is playing; while a track runs, the media
     * foreground service is what keeps the process alive.
     */
    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized && !MediaService.isPlaying()) webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        JixOneHost.activity = this
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        JixOneHost.activity = null
        if (::webView.isInitialized) {
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        super.onDestroy()
    }

    /**
     * The app is a single page with its own navigation stack, so canGoBack()
     * was always false and every back press dropped straight to
     * moveTaskToBack — i.e. back closed the app instead of going back a
     * screen. Ask the page first; it closes the queue, then the now-playing
     * sheet, then pops its view stack, and answers false only when there is
     * genuinely nothing left to leave.
     */
    override fun onBackPressed() {
        if (!::webView.isInitialized) {
            moveTaskToBack(true)
            return
        }
        webView.evaluateJavascript("(window.__jixBack && window.__jixBack()) === true") { result ->
            if (result?.trim() != "true") moveTaskToBack(true)
        }
    }


    fun evalJs(js: String) {
        if (::webView.isInitialized) {
            runOnUiThread { webView.evaluateJavascript(js, null) }
        }
    }
}

object JixOneHost {
    var activity: MainActivity? = null
}
