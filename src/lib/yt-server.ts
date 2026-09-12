import 'server-only';
import { Innertube } from 'youtubei.js';
import { flattenYTM, RawSong } from '@/lib/ytm-shared';
import { STREAM_CLIENTS, CLIENT_UA, pickAudioFromInfo, type StreamClient } from '@/lib/streamClients';

let ytPromise: Promise<Innertube> | null = null;

async function getYT(): Promise<Innertube> {
  if (!ytPromise) {
    ytPromise = Innertube.create().catch((e) => { ytPromise = null; throw e; });
  }
  return ytPromise;
}

/** per-client innertube sessions for stream extraction */
const clientCache = new Map<string, Promise<Innertube>>();
function getClientFor(clientType: StreamClient): Promise<Innertube> {
  let p = clientCache.get(clientType);
  if (!p) {
    p = Innertube.create({ client_type: clientType as any }).catch((e) => {
      clientCache.delete(clientType);
      throw e;
    });
    clientCache.set(clientType, p);
  }
  return p;
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

/**
 * Server-side stream extraction with a multi-client chain (getBasicInfo —
 * player endpoint only; the v18 getInfo /next pairing 404s for app clients).
 * NOTE: on datacenter IPs YouTube bot-gates ALL clients (LOGIN_REQUIRED, no
 * streaming_data) — this only works from "clean" (residential) server IPs.
 */
export async function extractStreamServer(videoId: string, qualityIdx = 0): Promise<ServerStream | null> {
  const pref = ITAG_PREF[Math.min(Math.max(qualityIdx, 0), ITAG_PREF.length - 1)];
  const errors: string[] = [];
  for (const clientType of STREAM_CLIENTS) {
    try {
      const yt = await getClientFor(clientType);
      const info: any = await yt.getBasicInfo(videoId);
      const picked = pickAudioFromInfo(info, pref);
      if (picked) {
        return { ...picked, ua: CLIENT_UA[clientType] };
      }
      errors.push(`${clientType}:no-url`);
    } catch (e: any) {
      errors.push(`${clientType}:${String(e?.message ?? 'error').slice(0, 50)}`);
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
