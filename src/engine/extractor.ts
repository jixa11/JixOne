'use client';
import type { QualityId } from '@/lib/types';
import { getYTClient, canUseClientEngine } from '@/engine/ytclient';

export interface ExtractedStream {
  url: string;
  itag: number;
  size?: number;
  durationSec?: number;
  title?: string;
  artist?: string;
  thumb?: string;
}

const PREF: Record<QualityId, number[]> = {
  high: [251, 140, 250, 249],
  mid: [250, 249, 140, 251],
  low: [249, 139, 250, 140, 251],
};

function pickFromInfo(info: any, quality: QualityId): ExtractedStream | null {
  const formats: any[] = info?.streaming_data?.adaptive_formats ?? [];
  const basics = info?.basic_info ?? {};
  const pick = (itag: number) => formats.find((f) => f.itag === itag && f.url);
  for (const itag of PREF[quality]) {
    const f = pick(itag);
    if (f) return { url: f.url, itag, size: f.content_length, durationSec: basics.duration, title: basics.title, artist: basics.author };
  }
  // fallback: best available audio-only
  const auds = formats.filter((f) => f.has_audio && !f.has_video && f.url).sort((a, b) => (b.bitrate ?? 0) - (a.bitrate ?? 0));
  if (auds[0]) return { url: auds[0].url, itag: auds[0].itag, size: auds[0].content_length, durationSec: basics.duration, title: basics.title, artist: basics.author };
  return null;
}

/** Extract a direct audio stream URL.
 *  - APK: extraction happens on the DEVICE through the native bridge (no CORS, no server).
 *  - plain web: through our own server route (/api/yt/player) — CORS-free. */
export async function extractStream(videoId: string, quality: QualityId): Promise<ExtractedStream> {
  if (canUseClientEngine()) {
    const yt = await getYTClient();
    let info: any;
    try {
      // iOS client: usually returns streams from residential IPs without poToken
      info = await yt.getInfo(videoId, { client: 'IOS' });
    } catch {
      info = await yt.getInfo(videoId); // web fallback
    }
    const picked = pickFromInfo(info, quality);
    if (picked) return picked;
    if (canUseClientEngine() && (window as any).AndroidBridge) {
      // device mode has no server fallback
      throw new Error('NO_STREAM');
    }
  }
  // plain browser: server-side extraction
  const r = await fetch(`/api/yt/player?id=${encodeURIComponent(videoId)}&q=${encodeURIComponent(quality)}`, { cache: 'no-store' });
  const d = await r.json();
  if (d?.ok && d?.url) {
    return { url: d.url, itag: d.itag ?? 0, size: d.size, durationSec: d.durationSec, title: d.title, artist: d.artist };
  }
  throw new Error(d?.error ?? 'NO_STREAM');
}

export async function extractMetaOnly(videoId: string): Promise<{ title?: string; artist?: string; durationSec?: number }> {
  if (canUseClientEngine()) {
    try {
      const yt = await getYTClient();
      const info: any = await yt.getInfo(videoId, { client: 'IOS' });
      return { title: info?.basic_info?.title, artist: info?.basic_info?.author, durationSec: info?.basic_info?.duration };
    } catch {
      /* fall through to server meta */
    }
  }
  try {
    const r = await fetch(`/api/yt/search?id=${encodeURIComponent(videoId)}`, { cache: 'no-store' });
    const d = await r.json();
    if (d?.ok && d?.track) {
      return { title: d.track.title, artist: d.track.artist, durationSec: undefined };
    }
  } catch { /* offline */ }
  return {};
}
