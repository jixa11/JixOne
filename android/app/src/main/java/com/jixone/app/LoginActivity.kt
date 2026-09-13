package com.jixone.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * YouTube Music sign-in, reproducing Metrolist's flow step for step.
 *
 * The previous version spoofed a desktop Chrome user agent, which is exactly
 * what makes Google answer "this browser or app may not be secure": a page
 * claiming to be desktop Chrome while every other signal says WebView reads as
 * a hijacked browser. Metrolist ships the stock user agent untouched and gets
 * through, so this does too.
 *
 * Once the flow lands on music.youtube.com the session is read out of the page
 * itself — cookie from the CookieManager, and visitorData / dataSyncId /
 * SESSION_INDEX out of `window.yt.config_`. Those last three are what make the
 * session look like a real signed-in client to innertube; deriving visitorData
 * separately from sw.js_data produced an id that did not belong to the account.
 */
class LoginActivity : Activity() {

    private lateinit var webView: WebView
    private var saved = false
    private val main = Handler(Looper.getMainLooper())
    private val bridge = LoginBridge()

    companion object {
        private const val LOGIN_URL =
            "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
        private const val MAX_PROBES = 20
        private const val PROBE_INTERVAL_MS = 500L
    }

    /** Values the page hands back through `window.Android.*`. */
    private class LoginBridge {
        @Volatile var visitorData: String? = null
        @Volatile var dataSyncId: String? = null
        @Volatile var authUser: String? = null

        @JavascriptInterface
        fun onRetrieveVisitorData(value: String?) { visitorData = value?.takeIf { it != "null" } }

        @JavascriptInterface
        fun onRetrieveDataSyncId(value: String?) { dataSyncId = value?.takeIf { it != "null" } }

        @JavascriptInterface
        fun onRetrieveAuthUser(value: String?) { authUser = value?.takeIf { it != "null" } }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0a0a0f")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0a0f"))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 20, 28, 20)
        }
        bar.addView(TextView(this).apply {
            text = getString(R.string.login_title)
            setTextColor(Color.WHITE)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        bar.addView(Button(this).apply {
            text = getString(R.string.login_cancel)
            setOnClickListener { finish() }
        })
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Android WebView tags every request with `X-Requested-With: <package>`,
        // another signal Google reads to spot an embedded browser. An empty
        // allow-list means the header goes to no origin at all. (Recent WebView
        // builds already drop it; this covers the ones that do not.)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, emptySet())
        }

        webView.addJavascriptInterface(bridge, "Android")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val host = try { android.net.Uri.parse(url ?: "").host } catch (_: Exception) { null }
                if (host == "music.youtube.com") probeSession(0)
            }
        }
        webView.loadUrl(LOGIN_URL)
    }

    /**
     * The cookie and `yt.config_` do not both exist the instant the page
     * finishes loading, so poll for up to ten seconds the way Metrolist does.
     */
    private fun probeSession(attempt: Int) {
        if (saved || attempt >= MAX_PROBES || isFinishing) return

        webView.evaluateJavascript(
            "Android.onRetrieveVisitorData(window.yt&&window.yt.config_?window.yt.config_.VISITOR_DATA:null)", null
        )
        webView.evaluateJavascript(
            "Android.onRetrieveDataSyncId(window.yt&&window.yt.config_?window.yt.config_.DATASYNC_ID:null)", null
        )
        webView.evaluateJavascript(
            "Android.onRetrieveAuthUser(window.yt&&window.yt.config_?String(window.yt.config_.SESSION_INDEX||0):'0')", null
        )

        main.postDelayed({
            if (saved || isFinishing) return@postDelayed
            val cm = CookieManager.getInstance()
            val raw = cm.getCookie(YTMAuth.ORIGIN)
            val visitor = bridge.visitorData
            if (YTMAuth.looksLikeSession(raw) && !visitor.isNullOrBlank()) {
                saved = true
                cm.flush()
                YTMAuth.save(
                    ctx = applicationContext,
                    rawCookie = raw!!,
                    visitorData = visitor,
                    dataSyncId = bridge.dataSyncId?.substringBefore("||").orEmpty(),
                    authUser = bridge.authUser?.filter(Char::isDigit)?.ifBlank { "0" } ?: "0",
                )
                fetchAccountName()
                finish()
            } else {
                probeSession(attempt + 1)
            }
        }, PROBE_INTERVAL_MS)
    }

    /** Best-effort display name for the settings screen; login already succeeded. */
    private fun fetchAccountName() {
        val ctx = applicationContext
        Thread {
            val name = InnertubeClient.accountName(ctx)
            if (!name.isNullOrBlank()) YTMAuth.saveAccountName(ctx, name)
            JixOneHost.activity?.evalJs("window.__ytmAuthChanged && window.__ytmAuthChanged()")
        }.start()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        webView.destroy()
        super.onDestroy()
    }
}
