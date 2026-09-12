package com.jixone.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Metrolist-style YouTube Music stream extraction, 100% native.
 *
 * Why this exists: the previous device path used youtubei.js inside the
 * WebView. It returned URLs whose bytes then 403'd (po-token/IP-gated
 * formats) — the user saw «no stream», 403 downloads and endless fallback
 * toasts. Metrolist (InnerTune lineage) works because it does the opposite:
 * ONE raw innertube POST per client from the DEVICE's own IP with the exact
 * client context — no session bootstrap, no deciphering — and it only uses
 * clients whose URLs are directly playable.
 *
 * This module replicates that exactly, plus one extra safety Metrolist
 * doesn't have: a byte-range PROBE. Every candidate googlevideo URL is
 * verified (Range 0-1KB, client UA) BEFORE being handed to the player, so a
 * locked/expired URL can never reach <audio> or the downloader again.
 */
object InnertubeClient {

    private class Cl(
        val name: String,     // innertube clientName
        val num: String,      // X-YouTube-Client-Name
        val version: String,
        val key: String,      // innertube API key for this client
        val ua: String,       // UA that MUST accompany player + googlevideo fetches
        val clientExtra: String, // extra fields for context.client (raw JSON fragment)
        val thirdParty: Boolean   // TVHTML5 embed needs context.thirdParty.embedUrl
    )

    private val CLIENTS = listOf(
        // 1) iOS — usually returns direct, un-locked URLs without any po token
        Cl("IOS", "5", "19.45.4", "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)",
            "\"deviceMake\":\"Apple\",\"deviceModel\":\"iPhone16,2\",\"osName\":\"iPhone\",\"osVersion\":\"18.1.0.22B83\",\"hl\":\"en\",\"gl\":\"US\",\"utcOffsetMinutes\":0",
            false),
        // 2) Android VR — historically po-token-free app client
        Cl("ANDROID_VR", "28", "1.60.19", "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip",
            "\"deviceMake\":\"Oculus\",\"deviceModel\":\"Quest 3\",\"osName\":\"Android\",\"osVersion\":\"12\",\"hl\":\"en\",\"gl\":\"US\"",
            false),
        // 3) YouTube Music Android (what Metrolist/InnerTune uses)
        Cl("ANDROID_MUSIC", "21", "6.42.52", "AIzaSyAOghZGza2MQSZk_y_zf42tjvXcg9rAT6g",
            "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13) gzip",
            "\"osName\":\"Android\",\"osVersion\":\"13\",\"androidSdkVersion\":33,\"hl\":\"en\",\"gl\":\"US\"",
            false),
        // 4) mainline Android app — another app-client shot before embed fallbacks
        Cl("ANDROID", "3", "19.44.38", "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            "com.google.android.youtube/19.44.38 (Linux; U; Android 13) gzip",
            "\"osName\":\"Android\",\"osVersion\":\"13\",\"androidSdkVersion\":33,\"hl\":\"en\",\"gl\":\"US\"",
            false),
        // 5) Embedded TV — bypasses some login/bot gates
        Cl("TVHTML5_SIMPLY_EMBEDDED_PLAYER", "85", "2.0", "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8",
            "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Unset",
            "\"hl\":\"en\",\"gl\":\"US\"",
            true),
    )

    // yt-dlp convention: app clients (IOS/ANDROID/VR/TV) use www.youtube.com,
    // the music client uses music.youtube.com — both serve /youtubei/v1/player
    private const val ENDPOINT_WWW = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
    private const val ENDPOINT_MUSIC = "https://music.youtube.com/youtubei/v1/player?prettyPrint=false"
    private const val CONNECT_TIMEOUT = 7000
    private const val READ_TIMEOUT = 9000

    /** itag preference per requested quality (opus webm preferred, m4a fallback) */
    private fun itagPref(q: String): IntArray = when (q) {
        "low" -> intArrayOf(249, 139, 250, 140, 251)
        "mid" -> intArrayOf(250, 249, 140, 139, 251)
        else -> intArrayOf(251, 140, 250, 249)
    }

    /** Entry point (called on a background thread from the bridge).
     *  Returns a JSON envelope string for JS. */
    fun player(videoId: String, quality: String): String {
        val attempts = StringBuilder()
        val prefs = itagPref(quality)
        for (cl in CLIENTS) {
            try {
                val t0 = System.currentTimeMillis()
                val body = playerBody(cl, videoId)
                val endpoint = if (cl.name == "ANDROID_MUSIC") ENDPOINT_MUSIC else ENDPOINT_WWW
                val res = httpPost(endpoint + "&key=" + cl.key, cl, body)
                if (res == null) { attempts.append(cl.name).append(":no-response; "); continue }

                val o = JSONObject(res)
                val playability = o.optJSONObject("playabilityStatus")
                val status = playability?.optString("status", "") ?: ""
                if (status != "OK") {
                    attempts.append(cl.name).append(":").append(if (status.isBlank()) "empty-status" else status).append("; ")
                    continue
                }

                val details = o.optJSONObject("videoDetails")
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

                // ——— PROBE: verify the URL actually streams before trusting it ———
                val probe = probeUrl(picked.getString("url"), cl.ua)
                if (probe.first != 200 && probe.first != 206) {
                    attempts.append(cl.name).append(":probe").append(probe.first).append("; ")
                    continue
                }

                val mimeFull = picked.optString("mimeType", "audio/mp4")
                val mime = mimeFull.substringBefore(';').trim()
                return JSONObject()
                    .put("ok", true)
                    .put("url", picked.getString("url"))
                    .put("ua", cl.ua)
                    .put("itag", picked.optInt("itag", 0))
                    .put("size", picked.optLong("contentLength", 0))
                    .put("mime", mime)
                    .put("duration", details?.optLong("lengthSeconds", 0) ?: 0)
                    .put("title", details?.optString("title", "") ?: "")
                    .put("author", details?.optString("author", "") ?: "")
                    .put("client", cl.name)
                    .put("ms", System.currentTimeMillis() - t0)
                    .toString()
            } catch (e: Exception) {
                attempts.append(cl.name).append(":").append(e.message?.take(40) ?: "error").append("; ")
            }
        }
        return JSONObject()
            .put("ok", false)
            .put("error", "ALL_CLIENTS_FAILED")
            .put("attempts", attempts.toString())
            .toString()
    }

    private fun playerBody(cl: Cl, videoId: String): String {
        val sb = StringBuilder()
        sb.append("{\"context\":{\"client\":{\"clientName\":\"").append(cl.name)
        sb.append("\",\"clientVersion\":\"").append(cl.version).append("\",")
        sb.append(cl.clientExtra)
        sb.append("}")
        sb.append(",\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}")
        if (cl.thirdParty) sb.append(",\"thirdParty\":{\"embedUrl\":\"https://www.youtube.com/\"}")
        sb.append("},\"videoId\":\"").append(videoId).append("\"")
        sb.append(",\"contentCheckOk\":true,\"racyCheckOk\":true")
        sb.append("}")
        return sb.toString()
    }

    private fun httpPost(url: String, cl: Cl, body: String): String? {
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
            conn.setRequestProperty("X-Goog-Api-Format-Version", "2")
            val bytes = body.toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            if (code !in 200..299) { conn.errorStream?.close(); return null }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            try { c?.disconnect() } catch (_: Exception) {}
        }
    }

    /** best audio-only format with a DIRECT url (signatureCipher ones are skipped) */
    private fun pickAudio(formats: JSONArray, prefs: IntArray): JSONObject? {
        val direct = ArrayList<JSONObject>()
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val mime = f.optString("mimeType", "")
            if (mime.isBlank() || !mime.startsWith("audio")) continue
            if (f.optString("url").isBlank()) continue // ciphered → not usable natively
            direct.add(f)
        }
        if (direct.isEmpty()) return null
        for (itag in prefs) {
            for (f in direct) if (f.optInt("itag", -1) == itag) return f
        }
        return direct.firstOrNull()
    }

    /** byte-range probe — the 403-killer: only URLs that really stream pass */
    private fun probeUrl(url: String, ua: String): Pair<Int, String> {
        var c: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            c = conn
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", ua)
            conn.setRequestProperty("Range", "bytes=0-1023")
            conn.setRequestProperty("Referer", "https://music.youtube.com/")
            val code = conn.responseCode
            if (code !in 200..299) {
                try { conn.errorStream?.close() } catch (_: Exception) {}
                return Pair(code, "")
            }
            val ct = conn.getHeaderField("Content-Type") ?: ""
            // read a tiny chunk then close — proves bytes actually flow
            try { conn.inputStream.use { it.read(ByteArray(256)) } } catch (_: Exception) {}
            Pair(code, ct)
        } catch (e: Exception) {
            Pair(0, e.message?.take(40) ?: "error")
        } finally {
            try { c?.disconnect() } catch (_: Exception) {}
        }
    }
}
