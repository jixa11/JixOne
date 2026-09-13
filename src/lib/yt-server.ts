import 'server-only';
import { Innertube } from 'youtubei.js';
import { flattenYTM, RawSong } from '@/lib/ytm-shared';
import { ytmAuthHeaders, ytmSignedIn } from '@/lib/ytm-auth';

let ytPromise: Promise<Innertube> | null = null;

async function getYT(): Promise<Innertube> {
  if (!ytPromise) {
    ytPromise = Innertube.create().catch((e) => { ytPromise = null; throw e; });
  }
  return ytPromise;
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
const RAW_CLIENTS: { name: string; num: string; ver: string; ua: string; client: any; thirdParty?: boolean; musicHost?: boolean }[] = [
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
    name: 'WEB_REMIX', num: '67', ver: '1.20250101.01.00',
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    client: { clientName: 'WEB_REMIX', clientVersion: '1.20250101.01.00', hl: 'en', gl: 'US' },
    musicHost: true,
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
async function probeUrl(url: string, ua: string): Promise<number> {
  try {
    const r = await fetch(url, {
      headers: { 'user-agent': ua, range: 'bytes=0-1023', referer: 'https://music.youtube.com/' },
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

async function rawPlayerRequest(cl: (typeof RAW_CLIENTS)[number], videoId: string): Promise<any> {
  const host = cl.musicHost ? 'https://music.youtube.com' : 'https://www.youtube.com';
  const visitor = await getVisitorData();
  const body = {
    context: {
      client: { ...cl.client, ...(visitor ? { visitorData: visitor } : {}) },
      request: { internalExperimentFlags: [], useSsl: true },
      user: { lockedSafetyMode: false },
      ...(cl.thirdParty ? { thirdParty: { embedUrl: 'https://www.youtube.com/' } } : {}),
    },
    videoId,
    contentCheckOk: true,
    racyCheckOk: true,
  };
  const r = await fetch(`${host}/youtubei/v1/player?prettyPrint=false`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'user-agent': cl.ua,
      'x-youtube-client-name': cl.num,
      'x-youtube-client-version': cl.ver,
      ...(visitor ? { 'x-goog-visitor-id': visitor } : {}),
      ...ytmAuthHeaders(),
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(12000),
  });
  if (!r.ok) throw new Error(`http ${r.status}`);
  return r.json();
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

  for (const cl of RAW_CLIENTS) {
    try {
      const o: any = await rawPlayerRequest(cl, videoId);
      const status = o?.playabilityStatus?.status;
      if (status !== 'OK') {
        if (status === 'LOGIN_REQUIRED') sawLoginRequired = true;
        throw new Error(status || 'empty-status');
      }
      const fmts: any[] = o?.streamingData?.adaptiveFormats ?? [];
      const picked = pickRawAudio(fmts, pref);
      if (!picked) throw new Error('no-direct-audio');
      const probe = await probeUrl(picked.url, cl.ua);
      if (probe !== 200 && probe !== 206) throw new Error(`probe ${probe}`);
      const details = o?.videoDetails;
      return {
        code: 'OK',
        attempts: errors.join(' | '),
        stream: {
          url: picked.url,
          itag: picked.itag,
          size: picked.contentLength ? Number(picked.contentLength) : undefined,
          mime: typeof picked.mimeType === 'string' ? picked.mimeType.split(';')[0].trim() : undefined,
          durationSec: details?.lengthSeconds ? Number(details.lengthSeconds) : undefined,
          title: details?.title,
          artist: details?.author,
          ua: cl.ua,
        },
      };
    } catch (e: any) {
      const msg = String(e?.message ?? 'error').slice(0, 50);
      if (msg.startsWith('http ') || msg.includes('timeout') || msg.includes('fetch')) sawNetwork = true;
      errors.push(`${cl.name}:${msg}`);
    }
  }

  const authed = ytmSignedIn();
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
