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

        val prefs = getSharedPreferences("jixone", MODE_PRIVATE)
        val remote = prefs.getString("base_url", null)?.trim()?.trimEnd('/')
        launchWeb(remote?.takeIf { it.isNotBlank() })
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
            return assetLoader.shouldInterceptRequest(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url ?: return false
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

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) webView.onPause()
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

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else moveTaskToBack(true)
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
