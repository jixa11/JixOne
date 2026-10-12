import 'server-only';
import { Innertube, Platform } from 'youtubei.js';
import { flattenYTM, RawSong } from '@/lib/ytm-shared';
import { ytmAuthHeaders, ytmSignedIn, ytmBundle } from '@/lib/ytm-auth';
import { getPlayerPoToken, getStreamingPoToken } from '@/lib/po-token';
import vm from 'node:vm';

// youtubei.js v18 ships WITHOUT a JS evaluator (needed to run the decipher
// scripts it extracts from base.js). Provide one via node:vm — once.
if (!(globalThis as any).__jixYtEvalPatched) {
  try {
    Platform.load({
      ...Platform.shim,
      eval: async (data: any, env: any) => {
        const tail = `\n;process(${JSON.stringify(env.n ?? '')}, ${JSON.stringify(env.sp ?? '')}, ${JSON.stringify(env.sig ?? '')});`;
        const code = `(function(){\n${data.output}\n${tail}\n})()`;
        return vm.runInNewContext(code, { console: { log() {}, warn() {}, error() {} } }, { timeout: 10000 });
      },
    });
    (globalThis as any).__jixYtEvalPatched = true;
  } catch { /* platform already patched */ }
}

let ytPromise: Promise<Innertube> | null = null;

async function getYT(): Promise<Innertube> {
  if (!ytPromise) {
    ytPromise = Innertube.create().catch((e) => { ytPromise = null; throw e; });
  }
  return ytPromise;
}

/** The deciphering player (base.js sig/nsig machinery) with the streaming
 *  pot bound — created only for the signed-in WEB_REMIX path. */
let ytPlayerCache: { player: any; pot: string; at: number } | null = null;
async function getYtPlayer(): Promise<any | null> {
  const bundle = await ytmBundle();
  const sid = bundle?.dataSyncId || bundle?.visitorData || null;
  const pot = sid ? await getStreamingPoToken(sid) : null;
  if (!pot) return null;
  if (ytPlayerCache && ytPlayerCache.pot === pot && Date.now() - ytPlayerCache.at < 4 * 60 * 60 * 1000) {
    return ytPlayerCache.player;
  }
  try {
    const yts = await Innertube.create({
      client_name: 'WEB_REMIX',
      retrieve_player: true,
      generate_session_locally: true,
      enable_session_cache: false,
    } as any);
    const player = yts.session?.player;
    if (!player) return null;
    player.po_token = pot; // decipher() appends &pot= to googlevideo URLs
    ytPlayerCache = { player, pot, at: Date.now() };
    return player;
  } catch {
    ytPlayerCache = null;
    return null;
  }
}

export type { RawSong };

export async function searchYTMusic(query: string, filter: 'songs' | 'videos' = 'songs'): Promise<RawSong[]> {
  const yt = await getYT();
  const res: any = await yt.music.search(query, { filter } as any);
  const out = flattenYTM(res?.contents ?? res, []).slice(0, 40);
  return out;
}

export async function getTrackMeta(videoId: string): Promise<RawSong | null> {
  const yt = await getYT();
  try {
    const info: any = await yt.music.getInfo(videoId);
    const title = info?.title ?? 'Unknown';
    const artist = info?.artist?.name ?? info?.author?.name ?? '';
    const dur = info?.duration?.text;
    const t = info?.thumbnails?.[info.thumbnails.length - 1]?.url ?? '';
    return { videoId, title, artist, duration: dur, thumb: t };
  } catch {
    return null;
  }
}

export interface ServerStream {
  url: string;
  itag: number;
  size?: number;
  durationSec?: number;
  title?: string;
  artist?: string;
  mime?: string;
  /** UA that must be used when fetching `url` from googlevideo */
  ua?: string;
  /** CDN will only serve the first ~1 MiB of this file (geo-gated content
   *  from this server's egress region — gcr=<cc> URLs). The client should go
   *  straight to the official player instead of a ~60 s mid-track cutoff. */
  previewLimited?: boolean;
}

const ITAG_PREF: number[][] = [
  [251, 140, 250, 249],
  [250, 249, 140, 251],
  [249, 139, 250, 140, 251],
];

/** Raw innertube clients — the SAME chain as InnertubeClient.kt (Metrolist-style).
 *  youtubei.js is avoided here on purpose: its session bootstrap adds moving
 *  parts; a single POST per client is what Metrolist/InnerTune does and it
 *  lets us probe the resulting googlevideo URL before trusting it.
 *
 *  IOS and ANDROID are gone: both answer HTTP 400 "Precondition check failed"
 *  without app attestation, and TVHTML5_SIMPLY_EMBEDDED_PLAYER answers "no
 *  longer supported" — keeping them only added latency to every failure. */
const RAW_CLIENTS: { name: string; num: string; ver: string; ua: string; client: any; thirdParty?: boolean; musicHost?: boolean; loginSupported?: boolean }[] = [
  {
    name: 'ANDROID_VR', num: '28', ver: '1.62.27',
    ua: 'com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip',
    client: { clientName: 'ANDROID_VR', clientVersion: '1.62.27', deviceMake: 'Oculus', deviceModel: 'Quest 3', osName: 'Android', osVersion: '12', androidSdkVersion: 32, hl: 'en', gl: 'US' },
  },
  {
    name: 'ANDROID_MUSIC', num: '21', ver: '6.42.52',
    ua: 'com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13) gzip',
    client: { clientName: 'ANDROID_MUSIC', clientVersion: '6.42.52', osName: 'Android', osVersion: '13', androidSdkVersion: 33, hl: 'en', gl: 'US' },
    musicHost: true,
  },
  {
    name: 'TVHTML5', num: '7', ver: '7.20250101.10.00',
    ua: 'Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15',
    client: { clientName: 'TVHTML5', clientVersion: '7.20250101.10.00', hl: 'en', gl: 'US' },
  },
  {
    name: 'WEB_REMIX', num: '67', ver: '1.20250310.01.00',
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0',
    client: { clientName: 'WEB_REMIX', clientVersion: '1.20250310.01.00', hl: 'en', gl: 'US' },
    musicHost: true,
    loginSupported: true,
  },
];

/** Anonymous session id every real client sends; without it YouTube gates harder. */
let visitorPromise: Promise<string | null> | null = null;
function getVisitorData(): Promise<string | null> {
  if (!visitorPromise) {
    visitorPromise = (async () => {
      try {
        const r = await fetch('https://www.youtube.com/sw.js_data', {
          headers: { 'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36' },
          signal: AbortSignal.timeout(10000),
        });
        if (!r.ok) return null;
        const parsed = JSON.parse((await r.text()).replace(/^\)\]\}'/, '').trim());
        return findVisitor(parsed);
      } catch {
        return null;
      }
    })().catch(() => null);
  }
  return visitorPromise;
}

/** visitorData is a base64url protobuf blob that starts "Cg"; it runs to ~500
 *  characters, so the upper bound has to be generous. */
function findVisitor(node: unknown, depth = 0): string | null {
  if (depth > 12) return null;
  if (typeof node === 'string') return node.length >= 20 && node.length <= 2048 && node.startsWith('Cg') ? node : null;
  if (Array.isArray(node)) {
    for (const n of node) {
      const hit = findVisitor(n, depth + 1);
      if (hit) return hit;
    }
  }
  return null;
}

/** byte-range probe: only return URLs that REALLY stream (the 403-killer) */
async function probeUrl(url: string, ua: string, range = 'bytes=0-1023'): Promise<number> {
  try {
    const r = await fetch(url, {
      headers: { 'user-agent': ua, range, referer: 'https://music.youtube.com/' },
      signal: AbortSignal.timeout(12000),
    });
    try { await r.arrayBuffer(); } catch { /* body read failure is fine if status ok */ }
    return r.status;
  } catch {
    return 0;
  }
}

function pickRawAudio(fmts: any[], pref: number[]): any | null {
  const direct = fmts.filter((f) => (f.mimeType ?? '').startsWith('audio') && f.url);
  for (const itag of pref) {
    const hit = direct.find((f) => f.itag === itag);
    if (hit) return hit;
  }
  return direct[0] ?? null;
}

async function rawPlayerRequest(cl: (typeof RAW_CLIENTS)[number], videoId: string, extra?: { onBehalfOfUser?: string | null; signatureTimestamp?: number | null; poToken?: string | null; visitorData?: string | null }): Promise<any> {
  const host = cl.musicHost ? 'https://music.youtube.com' : 'https://www.youtube.com';
  const visitor = extra?.visitorData || await getVisitorData();
  const body = {
    context: {
      client: { ...cl.client, ...(visitor ? { visitorData: visitor } : {}) },
      request: { internalExperimentFlags: [], useSsl: true },
      user: {
        lockedSafetyMode: false,
        ...(extra?.onBehalfOfUser ? { onBehalfOfUser: extra.onBehalfOfUser } : {}),
      },
      ...(cl.thirdParty ? { thirdParty: { embedUrl: 'https://www.youtube.com/' } } : {}),
    },
    videoId,
    contentCheckOk: true,
    racyCheckOk: true,
    ...(extra?.signatureTimestamp ? { playbackContext: { contentPlaybackContext: { signatureTimestamp: extra.signatureTimestamp } } } : {}),
    ...(extra?.poToken ? { serviceIntegrityDimensions: { poToken: extra.poToken } } : {}),
  };
  const r = await fetch(`${host}/youtubei/v1/player?prettyPrint=false`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'user-agent': cl.ua,
      'x-goog-api-format-version': '1',
      'x-youtube-client-name': cl.num,
      'x-youtube-client-version': cl.ver,
      'x-origin': cl.musicHost ? 'https://music.youtube.com' : 'https://www.youtube.com',
      ...(cl.musicHost ? { referer: 'https://music.youtube.com/' } : {}),
      ...(visitor ? { 'x-goog-visitor-id': visitor } : {}),
      // cookies ONLY on loginSupported clients — web cookies on app clients → 400
      // + session invalidation risk (verified the hard way).
      ...(cl.loginSupported ? ytmAuthHeaders() : {}),
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(15000),
  });
  if (!r.ok) throw new Error(`http ${r.status}`);
  return r.json();
}

/** signatureTimestamp from the current base.js (cached ~12 h).
 *  OuterTune: required for WEB-family clients to receive working streams —
 *  without it WEB_REMIX answers UNPLAYABLE "The page needs to be reloaded". */
let stsCache: { value: number; at: number } | null = null;
const STS_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0';
async function getSignatureTimestamp(videoId: string): Promise<number | null> {
  if (stsCache && Date.now() - stsCache.at < 12 * 60 * 60 * 1000) return stsCache.value;
  try {
    // base.js URL sources: embed page (player refs incl. underscores!) or homepage ytcfg
    let baseJsUrl: string | null = null;
    try {
      const emb = await fetch(`https://www.youtube.com/embed/${videoId}`, {
        headers: { 'user-agent': STS_UA },
        signal: AbortSignal.timeout(12000),
      });
      const embHtml = await emb.text();
      baseJsUrl = embHtml.match(/(?:s|js|html5player)\/player\/[a-zA-Z0-9_.\/-]+\/base\.js/)?.[0] ?? null;
    } catch { /* try homepage */ }
    if (!baseJsUrl) {
      const hp = await fetch('https://www.youtube.com', { headers: { 'user-agent': STS_UA }, signal: AbortSignal.timeout(12000) });
      const hpHtml = await hp.text();
      baseJsUrl = hpHtml.match(/"PLAYER_JS_URL":"([^"]+\/base\.js)"/)?.[1]?.replace(/\\u002f/g, '/') ?? null;
    }
    if (baseJsUrl) {
      const baseJs = await fetch(`https://www.youtube.com${baseJsUrl.startsWith('/') ? '' : '/'}${baseJsUrl}`, {
        headers: { 'user-agent': STS_UA },
        signal: AbortSignal.timeout(15000),
      });
      const js = await baseJs.text();
      const sts = js.match(/signatureTimestamp\\?"?\s*[:=]\s*(\d{4,6})/)?.[1]
        ?? js.match(/signatureTimestamp\D{0,10}(\d{4,6})/)?.[1];
      if (sts) {
        stsCache = { value: Number(sts), at: Date.now() };
        return stsCache.value;
      }
    }
  } catch { /* STS is best-effort */ }
  return null;
}

/**
 * Server-side stream extraction — raw Metrolist-style client chain + byte-range
 * PROBE. A URL is only returned after proof that its bytes actually stream
 * (200/206 from googlevideo with the matching client UA); otherwise the chain
 * moves to the next client. NOTE: on datacenter IPs YouTube bot-gates ALL
 * clients (LOGIN_REQUIRED / FAILED_PRECONDITION) — this only works from
 * "clean" (residential) server IPs; the APK extracts on the device instead.
 */
export type ExtractCode = 'OK' | 'LOGIN_REQUIRED' | 'LOGIN_STALE' | 'NETWORK' | 'FAILED';

export interface ExtractOutcome {
  stream: ServerStream | null;
  code: ExtractCode;
  attempts: string;
}

export async function extractStreamServer(videoId: string, qualityIdx = 0): Promise<ExtractOutcome> {
  const pref = ITAG_PREF[Math.min(Math.max(qualityIdx, 0), ITAG_PREF.length - 1)];
  const errors: string[] = [];
  let sawLoginRequired = false;
  let sawNetwork = false;

  // warm the session cache FIRST — ytmSignedIn() is sync and reads the
  // in-memory bundle, which is empty until the file has been read once
  const bundle = await ytmBundle();
  const signedIn = !!(bundle?.cookie && ytmSignedIn());

  const tryOutcome = async (cl: (typeof RAW_CLIENTS)[number], o: any): Promise<ExtractOutcome | null> => {
    const status = o?.playabilityStatus?.status;
    if (status !== 'OK') {
      if (status === 'LOGIN_REQUIRED') sawLoginRequired = true;
      throw new Error(status || 'empty-status');
    }
    const fmts: any[] = o?.streamingData?.adaptiveFormats ?? [];
    const audioFmts = fmts.filter((f) => String(f.mimeType ?? '').startsWith('audio'));
    let picked = pickRawAudio(fmts, pref);
    let url: string | null = picked?.url ?? null;

    // WEB_REMIX answers with ciphered formats — decipher them (OuterTune's
    // "Could not parse deobfuscation function" fallback, done properly here
    // via youtubei.js's Player + our VM evaluator)
    if (!url && audioFmts.length && cl.name === 'WEB_REMIX') {
      const player = await getYtPlayer();
      if (player) {
        const nsigCache = new Map<string, string>();
        const byPref = [...pref, ...audioFmts.map((f) => f.itag)].filter((v, i, a) => a.indexOf(v) === i);
        for (const itag of byPref) {
          const f = audioFmts.find((x) => x.itag === itag);
          if (!f) continue;
          try {
            const dec = await player.decipher(f.url, f.signatureCipher, f.cipher, nsigCache);
            if (dec) { picked = f; url = dec; break; }
          } catch { /* next format */ }
        }
      }
    }
    if (!picked || !url) throw new Error('no-direct-audio');
    // streaming pot: appended by decipher() when present; add it manually for
    // direct (unciphered) URLs
    let finalUrl: string = url;
    if (!finalUrl.includes('pot=') && cl.name === 'WEB_REMIX') {
      const bundle = await ytmBundle();
      const sid = bundle?.dataSyncId || bundle?.visitorData;
      const spot = sid ? await getStreamingPoToken(sid) : null;
      if (spot) finalUrl = `${finalUrl}&pot=${spot}`;
    }
    const probe = await probeUrl(finalUrl, cl.ua);
    if (probe !== 200 && probe !== 206) throw new Error(`probe ${probe}`);
    // preview-limit detection: geo-gated content (e.g. Persian music from a
    // HK egress — the URL carries gcr=<cc>) serves only the first ~1 MiB,
    // then the CDN 403s every further byte. Detect it BEFORE the client
    // commits, so it can switch to the official player up-front instead of
    // dying ~60 s into the track. Bias toward "not limited": only a hard
    // 403/416 at +1 MiB counts (a flaky network must not break playback).
    let previewLimited = false;
    const totalBytes = picked.contentLength ? Number(picked.contentLength) : 0;
    if (totalBytes > 1048576 + 4096) {
      // signal 1: gcr param — YouTube marks geo-credential-restricted URLs
      // with gcr=<cc>; every observed gcr URL enforced the ~1 MiB wall
      const gcr = /[?&]gcr=[A-Za-z]{2}/.test(finalUrl);
      // signal 2: a REAL 4 KiB span at +1 MiB (2-byte probes are flaky — the
      // CDN sometimes serves crumbs past the wall while refusing real spans)
      const past = await probeUrl(finalUrl, cl.ua, 'bytes=1048576-1052671');
      previewLimited = gcr || past === 403 || past === 416 || past === 451;
      console.log(`[preview-probe] ${cl.name} itag=${picked.itag} total=${totalBytes} gcr=${gcr} past1MiB=${past} → limited=${previewLimited}`);
    }
    const details = o?.videoDetails;
    return {
      code: 'OK',
      attempts: errors.join(' | '),
      stream: {
        url: finalUrl,
        itag: picked.itag,
        size: picked.contentLength ? Number(picked.contentLength) : undefined,
        mime: typeof picked.mimeType === 'string' ? picked.mimeType.split(';')[0].trim() : undefined,
        durationSec: details?.lengthSeconds ? Number(details.lengthSeconds) : undefined,
        title: details?.title,
        artist: details?.author,
        ua: cl.ua,
        previewLimited,
      },
    };
  };

  // ---- signed-in: the OuterTune-grade WEB_REMIX path (pot + STS + dataSyncId) ----
  if (signedIn) {
    const remix = RAW_CLIENTS.find((c) => c.name === 'WEB_REMIX')!;
    try {
      const sessionId = bundle?.dataSyncId || bundle?.visitorData || null;
      // player poToken per video + the deciphering player (whose extracted
      // STS is more reliable than any regex on base.js)
      const [playerPot, ytPlayer] = await Promise.all([
        getPlayerPoToken(videoId, sessionId),
        getYtPlayer(),
      ]);
      const sts = ytPlayer?.signature_timestamp ?? await getSignatureTimestamp(videoId);
      const o: any = await rawPlayerRequest(remix, videoId, {
        onBehalfOfUser: bundle?.dataSyncId ?? null,
        signatureTimestamp: sts,
        poToken: playerPot,
        visitorData: bundle?.visitorData ?? null,
      });
      const out = await tryOutcome(remix, o);
      if (out) return out;
    } catch (e: any) {
      const msg = String(e?.message ?? 'error').slice(0, 60);
      if (msg.startsWith('http ') || msg.includes('timeout') || msg.includes('fetch')) sawNetwork = true;
      errors.push(`WEB_REMIX+auth:${msg}`);
    }
  }

  // ---- anonymous app-client chain (no cookies — never) ----
  for (const cl of RAW_CLIENTS.filter((c) => c.name !== 'WEB_REMIX')) {
    try {
      const o: any = await rawPlayerRequest(cl, videoId);
      const out = await tryOutcome(cl, o);
      if (out) return out;
    } catch (e: any) {
      const msg = String(e?.message ?? 'error').slice(0, 50);
      if (msg.startsWith('http ') || msg.includes('timeout') || msg.includes('fetch')) sawNetwork = true;
      errors.push(`${cl.name}:${msg}`);
    }
  }

  const authed = signedIn;
  const code: ExtractCode = sawLoginRequired && !authed ? 'LOGIN_REQUIRED'
    : sawLoginRequired ? 'LOGIN_STALE'
    : sawNetwork ? 'NETWORK'
    : 'FAILED';
  const attempts = errors.join(' | ');
  console.error(`extractStreamServer ${code} (authed=${authed}):`, attempts);
  return { stream: null, code, attempts };
}

/** Shared extraction cache — the player route and the stream proxy must hit the
 *  same entry, otherwise asking for metadata first would extract twice. */
const streamCache = new Map<string, { at: number; out: ExtractOutcome }>();

export async function getStream(videoId: string, qualityIdx: number, fresh = false): Promise<ExtractOutcome> {
  const key = `${videoId}:${qualityIdx}`;
  const hit = streamCache.get(key);
  if (!fresh && hit && Date.now() - hit.at < 30 * 60_000) return hit.out;
  const out = await extractStreamServer(videoId, qualityIdx);
  if (out.stream) {
    streamCache.set(key, { at: Date.now(), out });
    if (streamCache.size > 300) streamCache.delete(streamCache.keys().next().value as string);
  } else {
    streamCache.delete(key);
  }
  return out;
}

// simple TTL cache
const cache = new Map<string, { at: number; data: unknown }>();
export function cached<T>(key: string, ttlMs: number, fn: () => Promise<T>): Promise<T> {
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < ttlMs) return Promise.resolve(hit.data as T);
  return fn().then((data) => {
    cache.set(key, { at: Date.now(), data });
    if (cache.size > 200) cache.delete(cache.keys().next().value as string);
    return data;
  });
}
