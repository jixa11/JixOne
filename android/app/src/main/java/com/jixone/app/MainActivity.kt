package com.jixone.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("jixone", MODE_PRIVATE)
        val saved = prefs.getString("base_url", null)

        if (saved.isNullOrBlank()) {
            showServerPrompt(prefs) { url -> launchWeb(url, null) }
            return
        }
        launchWeb(saved, savedInstanceState)
    }

    private fun showServerPrompt(prefs: android.content.SharedPreferences, onGo: (String) -> Unit) {
        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 96, 64, 32)
            setBackgroundColor(Color.parseColor("#0a0a0f"))
        }
        val title = TextView(ctx).apply {
            text = "JixOne"
            setTextColor(Color.parseColor("#ff2d55"))
            textSize = 30f
            setPadding(0, 0, 0, 16)
        }
        val hint = TextView(ctx).apply {
            text = "آدرس سرور اپ را وارد کنید:\n(لینکی که در چت برایتان ارسال شده)"
            setTextColor(Color.parseColor("#9b9ba7"))
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }
        val input = EditText(ctx).apply {
            setHint("https://...")
            setTextColor(Color.WHITE)
            setSingleLine(true)
        }
        val go = Button(ctx).apply {
            text = "اتصال"
            setBackgroundColor(Color.parseColor("#ff2d55"))
            setTextColor(Color.BLACK)
        }
        layout.addView(title)
        layout.addView(hint)
        layout.addView(input)
        layout.addView(go)
        setContentView(layout)

        go.setOnClickListener {
            val url = input.text.toString().trim().trimEnd('/')
            if (url.startsWith("http")) {
                prefs.edit().putString("base_url", url).apply()
                onGo(url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun launchWeb(baseUrl: String, state: Bundle?) {
        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(webView)
        window.statusBarColor = Color.parseColor("#0a0a0f")

        WebSettingsCompat(webView.settings)
        webView.webViewClient = NeoClient(baseUrl)
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebBridge(applicationContext), "AndroidBridge")
        webView.setBackgroundColor(Color.parseColor("#0a0a0f"))

        if (state != null) {
            webView.restoreState(state)
        } else {
            webView.loadUrl(baseUrl)
        }
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

    private inner class NeoClient(private val baseUrl: String) : WebViewClient() {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else moveTaskToBack(true)
    }

    override fun onDestroy() {
        JixOneHost.activity = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        JixOneHost.activity = this
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
