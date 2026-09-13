package com.jixone.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * YouTube Music sign-in.
 *
 * Google refuses its login pages inside a WebView that advertises the stock
 * "; wv" user agent (`disallowed_useragent`), so the WebView presents itself
 * as desktop Chrome. Once the flow lands back on music.youtube.com the session
 * cookie is read straight out of the CookieManager and handed to YTMAuth —
 * the same approach Metrolist uses.
 */
class LoginActivity : Activity() {

    private lateinit var webView: WebView
    private var saved = false

    companion object {
        private const val LOGIN_URL =
            "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
                "&continue=https%3A%2F%2Fmusic.youtube.com%2F"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
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
            userAgentString = DESKTOP_UA
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.startsWith("https://music.youtube.com")) captureCookie()
            }
        }
        webView.loadUrl(LOGIN_URL)
    }

    /** Pull the session cookie once the flow reaches music.youtube.com. */
    private fun captureCookie() {
        if (saved) return
        val cm = CookieManager.getInstance()
        val raw = cm.getCookie(YTMAuth.ORIGIN) ?: return
        if (!YTMAuth.looksLikeSession(raw)) return
        saved = true
        cm.flush()
        YTMAuth.save(applicationContext, raw)
        fetchAccountName(raw)
        finish()
    }

    /** Best-effort display name for the settings screen; login already succeeded. */
    private fun fetchAccountName(rawCookie: String) {
        val ctx = applicationContext
        Thread {
            val name = InnertubeClient.accountName(ctx)
            if (!name.isNullOrBlank()) YTMAuth.save(ctx, rawCookie, name)
            JixOneHost.activity?.evalJs("window.__ytmAuthChanged && window.__ytmAuthChanged()")
        }.start()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
