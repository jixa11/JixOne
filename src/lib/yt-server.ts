import 'server-only';
import { Innertube } from 'youtubei.js';
import { flattenYTM, RawSong } from '@/lib/ytm-shared';

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
 *  lets us probe the resulting googlevideo URL before trusting it. */
const RAW_CLIENTS: { name: string; num: string; ver: string; key: string; ua: string; client: any; thirdParty?: boolean; musicHost?: boolean }[] = [
  {
    name: 'IOS', num: '5', ver: '19.45.4', key: 'AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc',
    ua: 'com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)',
    client: { clientName: 'IOS', clientVersion: '19.45.4', deviceMake: 'Apple', deviceModel: 'iPhone16,2', osName: 'iPhone', osVersion: '18.1.0.22B83', hl: 'en', gl: 'US', timeZone: 'UTC', utcOffsetMinutes: 0 },
  },
  {
    name: 'ANDROID_VR', num: '28', ver: '1.60.19', key: 'AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w',
    ua: 'com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip',
    client: { clientName: 'ANDROID_VR', clientVersion: '1.60.19', deviceMake: 'Oculus', deviceModel: 'Quest 3', osName: 'Android', osVersion: '12', hl: 'en', gl: 'US' },
  },
  {
    name: 'ANDROID_MUSIC', num: '21', ver: '6.42.52', key: 'AIzaSyAOghZGza2MQSZk_y_zf42tjvXcg9rAT6g',
    ua: 'com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13) gzip',
    client: { clientName: 'ANDROID_MUSIC', clientVersion: '6.42.52', osName: 'Android', osVersion: '13', androidSdkVersion: 33, hl: 'en', gl: 'US' },
    musicHost: true,
  },
  {
    name: 'ANDROID', num: '3', ver: '19.44.38', key: 'AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w',
    ua: 'com.google.android.youtube/19.44.38 (Linux; U; Android 13) gzip',
    client: { clientName: 'ANDROID', clientVersion: '19.44.38', osName: 'Android', osVersion: '13', androidSdkVersion: 33, hl: 'en', gl: 'US' },
  },
  {
    name: 'TVHTML5_SIMPLY_EMBEDDED_PLAYER', num: '85', ver: '2.0', key: 'AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8',
    ua: 'Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Unset',
    client: { clientName: 'TVHTML5_SIMPLY_EMBEDDED_PLAYER', clientVersion: '2.0', hl: 'en', gl: 'US' },
    thirdParty: true,
  },
];

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
  const body = {
    context: {
      client: cl.client,
      request: { internalExperimentFlags: [], useSsl: true },
      ...(cl.thirdParty ? { thirdParty: { embedUrl: 'https://www.youtube.com/' } } : {}),
    },
    videoId,
    contentCheckOk: true,
    racyCheckOk: true,
  };
  const r = await fetch(`${host}/youtubei/v1/player?key=${cl.key}&prettyPrint=false`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'user-agent': cl.ua,
      'x-youtube-client-name': cl.num,
      'x-youtube-client-version': cl.ver,
      'x-goog-api-format-version': '2',
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
export async function extractStreamServer(videoId: string, qualityIdx = 0): Promise<ServerStream | null> {
  const pref = ITAG_PREF[Math.min(Math.max(qualityIdx, 0), ITAG_PREF.length - 1)];
  const errors: string[] = [];
  for (const cl of RAW_CLIENTS) {
    try {
      const o: any = await rawPlayerRequest(cl, videoId);
      const status = o?.playabilityStatus?.status;
      if (status !== 'OK') throw new Error(status || 'empty-status');
      const fmts: any[] = o?.streamingData?.adaptiveFormats ?? [];
      const picked = pickRawAudio(fmts, pref);
      if (!picked) throw new Error('no-direct-audio');
      const probe = await probeUrl(picked.url, cl.ua);
      if (probe !== 200 && probe !== 206) throw new Error(`probe ${probe}`);
      const details = o?.videoDetails;
      return {
        url: picked.url,
        itag: picked.itag,
        size: picked.contentLength ? Number(picked.contentLength) : undefined,
        mime: typeof picked.mimeType === 'string' ? picked.mimeType.split(';')[0].trim() : undefined,
        durationSec: details?.lengthSeconds ? Number(details.lengthSeconds) : undefined,
        title: details?.title,
        artist: details?.author,
        ua: cl.ua,
      };
    } catch (e: any) {
      errors.push(`${cl.name}:${String(e?.message ?? 'error').slice(0, 50)}`);
    }
  }
  console.error('extractStreamServer chain failed:', errors.join(' | '));
  return null;
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
