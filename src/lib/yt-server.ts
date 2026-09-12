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

const ITAG_PREF: number[][] = [
  [251, 140, 250, 249],
  [250, 249, 140, 251],
  [249, 139, 250, 140, 251],
];

export interface ServerStream {
  url: string;
  itag: number;
  size?: number;
  durationSec?: number;
  title?: string;
  artist?: string;
}

/** Server-side stream extraction (browser origins cannot call YouTube directly — CORS). */
export async function extractStreamServer(videoId: string, qualityIdx = 0): Promise<ServerStream | null> {
  const yt = await getYT();
  let info: any;
  try {
    info = await yt.getInfo(videoId, { client: 'IOS' });
  } catch {
    info = await yt.getInfo(videoId);
  }
  const formats: any[] = info?.streaming_data?.adaptive_formats ?? [];
  const basics = info?.basic_info ?? {};
  const pref = ITAG_PREF[Math.min(Math.max(qualityIdx, 0), ITAG_PREF.length - 1)];
  for (const itag of pref) {
    const f = formats.find((x) => x.itag === itag && x.url);
    if (f) return { url: f.url, itag, size: f.content_length, durationSec: basics.duration, title: basics.title, artist: basics.author };
  }
  const auds = formats.filter((f) => f.has_audio && !f.has_video && f.url).sort((a, b) => (b.bitrate ?? 0) - (a.bitrate ?? 0));
  if (auds[0]) return { url: auds[0].url, itag: auds[0].itag, size: auds[0].content_length, durationSec: basics.duration, title: basics.title, artist: basics.author };
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
