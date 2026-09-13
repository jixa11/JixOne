package com.jixone.app

import android.content.Context
import java.security.MessageDigest

/**
 * YouTube Music session (cookie auth) — the Metrolist/InnerTune scheme.
 *
 * YouTube bot-gates anonymous innertube player calls ("Sign in to confirm
 * you're not a bot" → playabilityStatus LOGIN_REQUIRED), which is why
 * extraction used to fail for most tracks. A signed-in session lifts that
 * gate, so every innertube request carries the account's cookie plus the
 * SAPISIDHASH authorization Google's own web clients send.
 */
object YTMAuth {

    const val ORIGIN = "https://music.youtube.com"

    private const val PREFS = "jixone"
    private const val KEY_COOKIE = "ytm_cookie"
    private const val KEY_ACCOUNT = "ytm_account"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cookie(ctx: Context): String? =
        prefs(ctx).getString(KEY_COOKIE, null)?.takeIf { it.isNotBlank() }

    fun isLoggedIn(ctx: Context): Boolean = sapisid(cookie(ctx)) != null

    fun account(ctx: Context): String = prefs(ctx).getString(KEY_ACCOUNT, "") ?: ""

    fun save(ctx: Context, rawCookie: String, accountName: String? = null) {
        val editor = prefs(ctx).edit()
        editor.putString(KEY_COOKIE, rawCookie.trim())
        if (accountName != null) editor.putString(KEY_ACCOUNT, accountName)
        editor.apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_COOKIE).remove(KEY_ACCOUNT).apply()
    }

    /** true when the cookie string carries a usable session id */
    fun looksLikeSession(rawCookie: String?): Boolean = sapisid(rawCookie) != null

    /**
     * Headers that turn an anonymous innertube call into an authenticated one.
     * Empty when signed out, so callers can always splat them in.
     */
    fun authHeaders(ctx: Context): Map<String, String> {
        val raw = cookie(ctx) ?: return emptyMap()
        val sapisid = sapisid(raw) ?: return emptyMap()
        return mapOf(
            "Cookie" to raw,
            "Authorization" to sapisidHash(sapisid, ORIGIN),
            "X-Goog-AuthUser" to "0",
            "Origin" to ORIGIN,
            "X-Origin" to ORIGIN,
        )
    }

    /** SAPISID, or the 1P/3P variants Google sets on newer accounts */
    private fun sapisid(rawCookie: String?): String? {
        val jar = parse(rawCookie ?: return null)
        return jar["SAPISID"] ?: jar["__Secure-3PAPISID"] ?: jar["__Secure-1PAPISID"]
    }

    /** `SAPISIDHASH <unix>_<sha1(unix + " " + SAPISID + " " + origin)>` */
    private fun sapisidHash(sapisid: String, origin: String): String {
        val ts = System.currentTimeMillis() / 1000
        return "SAPISIDHASH ${ts}_${sha1("$ts $sapisid $origin")}"
    }

    private fun parse(raw: String): Map<String, String> {
        val out = HashMap<String, String>()
        for (part in raw.split(';')) {
            val i = part.indexOf('=')
            if (i <= 0) continue
            out[part.substring(0, i).trim()] = part.substring(i + 1).trim()
        }
        return out
    }

    private fun sha1(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(s.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append("%02x".format(b))
        return sb.toString()
    }
}
