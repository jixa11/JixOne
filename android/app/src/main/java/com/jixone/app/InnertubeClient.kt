package com.jixone.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * YouTube Music stream extraction, 100% native (Metrolist/InnerTune lineage).
 *
 * One raw innertube player POST per client from the device's own IP, then a
 * byte-range probe so only URLs that really stream reach the player.
 *
 * Two things decide whether this works at all:
 *
 *  1. `visitorData` — an anonymous session id every real client sends. Without
 *     it YouTube treats each call as a brand-new stranger and gates harder.
 *  2. The signed-in cookie (see [YTMAuth]). Anonymous player calls now answer
 *     LOGIN_REQUIRED / "Sign in to confirm you're not a bot" for most tracks;
 *     an authenticated session lifts that gate. This is the single biggest
 *     reason extraction fails when signed out.
 *
 * The client list is deliberately short: IOS and ANDROID answer HTTP 400
 * ("Precondition check failed") and TVHTML5_SIMPLY_EMBEDDED_PLAYER answers
 * "no longer supported", so carrying them only added latency to every failure.
 */
object InnertubeClient {

    private class Cl(
        val name: String,
        val num: String,
        val version: String,
        val ua: String,
        val clientExtra: String,
        val host: String,
        val thirdParty: Boolean = false,
    )

    /** Ordered by how reliably each returns DIRECT (un-ciphered) audio URLs. */
    private val CLIENTS = listOf(
        // Android VR: the one app client that still hands out direct URLs with
        // no po-token. Primary path, signed in or out.
        Cl("ANDROID_VR", "28", "1.62.27",
            "com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip",
            "\"deviceMake\":\"Oculus\",\"deviceModel\":\"Quest 3\",\"osName\":\"Android\",\"osVersion\":\"12\",\"androidSdkVersion\":32,\"hl\":\"en\",\"gl\":\"US\"",
            "https://www.youtube.com"),
        // YouTube Music app client — gated when anonymous, excellent once the
        // account cookie is attached (it is the client Metrolist leans on).
        Cl("ANDROID_MUSIC", "21", "6.42.52",
            "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13) gzip",
            "\"osName\":\"Android\",\"osVersion\":\"13\",\"androidSdkVersion\":33,\"hl\":\"en\",\"gl\":\"US\"",
            "https://music.youtube.com"),
        // TV client — ungated for some catalogues the app clients refuse.
        Cl("TVHTML5", "7", "7.20250101.10.00",
            "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15",
            "\"hl\":\"en\",\"gl\":\"US\"",
            "https://www.youtube.com"),
        // Authenticated web client, last resort: many of its formats are
        // signature-ciphered and get skipped, but some tracks only come back here.
        Cl("WEB_REMIX", "67", "1.20250101.01.00",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "\"hl\":\"en\",\"gl\":\"US\"",
            "https://music.youtube.com"),
    )

    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 12000

    private fun itagPref(q: String): IntArray = when (q) {
        "low" -> intArrayOf(249, 139, 250, 140, 251)
        "mid" -> intArrayOf(250, 249, 140, 139, 251)
        else -> intArrayOf(251, 140, 250, 249)
    }

    // ————————————————————————— visitorData —————————————————————————

    @Volatile private var cachedVisitor: String? = null

    /** Anonymous session id, fetched once per process from YouTube's own bootstrap. */
    private fun visitorData(): String? {
        cachedVisitor?.let { return it }
        synchronized(this) {
            cachedVisitor?.let { return it }
            val body = httpGet(
                "https://www.youtube.com/sw.js_data",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            ) ?: return null
            val json = body.removePrefix(")]}'").trim()
            val found = try { findVisitor(JSONArray(json)) } catch (_: Exception) { null }
            cachedVisitor = found
            return found
        }
    }

    /** visitorData is a base64url protobuf blob that starts "Cg"; it runs to
     *  ~500 characters, so the upper bound has to be generous. */
    private fun findVisitor(node: Any?, depth: Int = 0): String? {
        if (depth > 12) return null
        when (node) {
            is String -> if (node.length in 20..2048 && node.startsWith("Cg")) return node
            is JSONArray -> for (i in 0 until node.length()) findVisitor(node.opt(i), depth + 1)?.let { return it }
            is JSONObject -> for (k in node.keys()) findVisitor(node.opt(k), depth + 1)?.let { return it }
        }
        return null
    }

    // ————————————————————————— player —————————————————————————

    /**
     * Resolve one playable audio URL. Returns a JSON envelope for the web layer:
     * `{ok:true, url, ua, itag, …}` or `{ok:false, error, code, attempts}` where
     * `code` is LOGIN_REQUIRED / NETWORK / FAILED so the UI can say something useful.
     */
    fun player(ctx: Context, videoId: String, quality: String): String {
        val attempts = StringBuilder()
        val prefs = itagPref(quality)
        val auth = YTMAuth.authHeaders(ctx)
        val visitor = visitorData()
        var sawLoginRequired = false
        var sawNetwork = false

        for (cl in CLIENTS) {
            try {
                val t0 = System.currentTimeMillis()
                val res = httpPost(cl, videoId, visitor, auth)
                if (res.body == null) {
                    sawNetwork = true
                    attempts.append(cl.name).append(":http").append(res.code).append("; ")
                    continue
                }

                val o = JSONObject(res.body)
                val status = o.optJSONObject("playabilityStatus")?.optString("status", "") ?: ""
                if (status != "OK") {
                    if (status == "LOGIN_REQUIRED") sawLoginRequired = true
                    attempts.append(cl.name).append(":").append(status.ifBlank { "empty-status" }).append("; ")
                    continue
                }

                val formats = o.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
                if (formats == null || formats.length() == 0) {
                    attempts.append(cl.name).append(":no-formats; ")
                    continue
                }
                val picked = pickAudio(formats, prefs)
                if (picked == null) {
                    attempts.append(cl.name).append(":no-direct-audio; ")
                    continue
                }

                val probe = probeUrl(picked.getString("url"), cl.ua)
                if (probe != 200 && probe != 206) {
                    attempts.append(cl.name).append(":probe").append(probe).append("; ")
                    continue
                }

                val details = o.optJSONObject("videoDetails")
                return JSONObject()
                    .put("ok", true)
                    .put("url", picked.getString("url"))
                    .put("ua", cl.ua)
                    .put("itag", picked.optInt("itag", 0))
                    .put("size", picked.optLong("contentLength", 0))
                    .put("mime", picked.optString("mimeType", "audio/mp4").substringBefore(';').trim())
                    .put("duration", details?.optLong("lengthSeconds", 0) ?: 0)
                    .put("title", details?.optString("title", "") ?: "")
                    .put("author", details?.optString("author", "") ?: "")
                    .put("client", cl.name)
                    .put("authed", auth.isNotEmpty())
                    .put("ms", System.currentTimeMillis() - t0)
                    .toString()
            } catch (e: Exception) {
                attempts.append(cl.name).append(":").append(e.message?.take(40) ?: "error").append("; ")
            }
        }

        val code = when {
            sawLoginRequired && auth.isEmpty() -> "LOGIN_REQUIRED"
            sawLoginRequired -> "LOGIN_STALE"
            sawNetwork -> "NETWORK"
            else -> "FAILED"
        }
        return JSONObject()
            .put("ok", false)
            .put("code", code)
            .put("error", code)
            .put("authed", auth.isNotEmpty())
            .put("attempts", attempts.toString())
            .toString()
    }

    /** Display name of the signed-in account, or null. */
    fun accountName(ctx: Context): String? {
        val auth = YTMAuth.authHeaders(ctx).ifEmpty { return null }
        val cl = CLIENTS.first { it.name == "WEB_REMIX" }
        val body = "{\"context\":{\"client\":{\"clientName\":\"${cl.name}\",\"clientVersion\":\"${cl.version}\"," +
            "${cl.clientExtra}${visitorData()?.let { ",\"visitorData\":\"$it\"" } ?: ""}}}}"
        val res = request("${cl.host}/youtubei/v1/account/account_menu?prettyPrint=false", cl, body, auth)
        val json = res.body ?: return null
        return try { findAccountName(JSONObject(json)) } catch (_: Exception) { null }
    }

    private fun findAccountName(node: Any?, depth: Int = 0): String? {
        if (depth > 10) return null
        when (node) {
            is JSONObject -> {
                node.optJSONObject("accountName")?.let { n ->
                    val t = n.optString("simpleText", "").ifBlank {
                        n.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                    }
                    if (t.isNotBlank()) return t
                }
                for (k in node.keys()) findAccountName(node.opt(k), depth + 1)?.let { return it }
            }
            is JSONArray -> for (i in 0 until node.length()) findAccountName(node.opt(i), depth + 1)?.let { return it }
        }
        return null
    }

    // ————————————————————————— transport —————————————————————————

    private class Res(val code: Int, val body: String?)

    private fun httpPost(cl: Cl, videoId: String, visitor: String?, auth: Map<String, String>): Res {
        val client = StringBuilder()
            .append("\"clientName\":\"").append(cl.name)
            .append("\",\"clientVersion\":\"").append(cl.version).append("\",")
            .append(cl.clientExtra)
        if (visitor != null) client.append(",\"visitorData\":\"").append(visitor).append("\"")

        val body = StringBuilder()
            .append("{\"context\":{\"client\":{").append(client).append("}")
            .append(",\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}")
            .append(",\"user\":{\"lockedSafetyMode\":false}")
        if (cl.thirdParty) body.append(",\"thirdParty\":{\"embedUrl\":\"https://www.youtube.com/\"}")
        body.append("},\"videoId\":\"").append(videoId).append("\"")
            .append(",\"contentCheckOk\":true,\"racyCheckOk\":true}")

        return request("${cl.host}/youtubei/v1/player?prettyPrint=false", cl, body.toString(), auth, visitor)
    }

    private fun request(url: String, cl: Cl, body: String, auth: Map<String, String>, visitor: String? = null): Res {
        var c: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            c = conn
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.doOutput = true
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", cl.ua)
            conn.setRequestProperty("X-YouTube-Client-Name", cl.num)
            conn.setRequestProperty("X-YouTube-Client-Version", cl.version)
            visitor?.let { conn.setRequestProperty("X-Goog-Visitor-Id", it) }
            for ((k, v) in auth) conn.setRequestProperty(k, v)

            val bytes = body.toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            if (code !in 200..299) {
                try { conn.errorStream?.close() } catch (_: Exception) {}
                Res(code, null)
            } else {
                Res(code, conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            }
        } catch (_: Exception) {
            Res(0, null)
        } finally {
            try { c?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun httpGet(url: String, ua: String): String? {
        var c: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            c = conn
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("User-Agent", ua)
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            try { c?.disconnect() } catch (_: Exception) {}
        }
    }

    /** best audio-only format with a DIRECT url (signature-ciphered ones are skipped) */
    private fun pickAudio(formats: JSONArray, prefs: IntArray): JSONObject? {
        val direct = ArrayList<JSONObject>()
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            if (!f.optString("mimeType", "").startsWith("audio")) continue
            if (f.optString("url").isBlank()) continue
            direct.add(f)
        }
        if (direct.isEmpty()) return null
        for (itag in prefs) {
            for (f in direct) if (f.optInt("itag", -1) == itag) return f
        }
        return direct.firstOrNull()
    }

    /** byte-range probe — only URLs that really stream pass */
    private fun probeUrl(url: String, ua: String): Int {
        var c: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            c = conn
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", ua)
            conn.setRequestProperty("Range", "bytes=0-1023")
            conn.setRequestProperty("Referer", "https://music.youtube.com/")
            val code = conn.responseCode
            if (code !in 200..299) {
                try { conn.errorStream?.close() } catch (_: Exception) {}
                return code
            }
            try { conn.inputStream.use { it.read(ByteArray(256)) } } catch (_: Exception) {}
            code
        } catch (_: Exception) {
            0
        } finally {
            try { c?.disconnect() } catch (_: Exception) {}
        }
    }
}
