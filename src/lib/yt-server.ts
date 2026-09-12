import 'server-only';
import { Innertube } from 'youtubei.js';

let ytPromise: Promise<Innertube> | null = null;

async function getYT(): Promise<Innertube> {
  if (!ytPromise) {
    ytPromise = Innertube.create().catch((e) => { ytPromise = null; throw e; });
  }
  return ytPromise;
}

export interface RawSong {
  videoId: string;
  title: string;
  artist: string;
  duration?: string;
  thumb: string;
}

// walk nested YT structures and collect song-like items
function collect(node: any, out: RawSong[], depth = 0) {
  if (!node || depth > 6 || out.length > 60) return;
  if (Array.isArray(node)) { node.forEach((n) => collect(n, out, depth + 1)); return; }
  if (typeof node !== 'object') return;
  const vid =
    node.videoId ??
    node.video_id ??
    node?.title?.endpoint?.payload?.videoId ??
    node?.on_tap?.payload?.videoId ??
    node?.overlay?.music_item_thumbnail_overlay?.content?.music_play_button_renderer?.play_navigation_endpoint?.watchEndpoint?.videoId;
  const title = node.title?.text ?? node.title?.runs?.[0]?.text;
  if (vid && typeof vid === 'string' && title && node.type !== 'MusicCardShelfHeader') {
    const sub: string =
      node.subtitle?.text ??
      node.flex_columns?.[1]?.title?.runs?.map((r: any) => r.text).join(' ') ??
      node.authors?.map((a: any) => a.name).join(', ') ?? '';
    // artist = second segment of subtitle like "Song • Artist • 3:20" or plain
    let artist = sub;
    let dur: string | undefined;
    if (sub.includes('•')) {
      const parts = sub.split('•').map((s: string) => s.trim());
      parts.forEach((p) => { if (/^\d+:\d\d(:\d\d)?$/.test(p)) dur = p; });
      artist = parts.filter((p) => p !== dur && !/^(song|video|آهنگ|ویدیو)$/i.test(p)).slice(-2).join(', ') || sub;
    }
    const t =
      node.thumbnail?.contents?.[0]?.url ??
      node.thumbnails?.[0]?.url ??
      node.thumbnail_renderer?.music_thumbnail_renderer?.data?.thumbnail?.contents?.[0]?.url;
    if (t) out.push({ videoId: vid, title, artist, duration: dur, thumb: t });
    return;
  }
  for (const k of Object.keys(node)) {
    if (k === 'menu' || k === 'overlay' && !node?.overlay?.music_item_thumbnail_overlay) continue;
    const v = node[k];
    if (v && typeof v === 'object') collect(v, out, depth + 1);
  }
}

export async function searchYTMusic(query: string, filter: 'songs' | 'videos' = 'songs'): Promise<RawSong[]> {
  const yt = await getYT();
  const res: any = await yt.music.search(query, { filter } as any);
  const out: RawSong[] = [];
  collect(res.contents ?? res, out);
  // dedupe
  const seen = new Set<string>();
  return out.filter((s) => !seen.has(s.videoId) && seen.add(s.videoId)).slice(0, 40);
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
