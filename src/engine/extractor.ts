'use client';
import type { QualityId } from '@/lib/types';
import { getYTClient } from '@/engine/ytclient';

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

/** Extract a direct audio stream URL on the DEVICE (residential IP / real browser). */
export async function extractStream(videoId: string, quality: QualityId): Promise<ExtractedStream> {
  const yt = await getYTClient();
  let info: any;
  try {
    // iOS client: usually returns streams from residential IPs without poToken
    info = await yt.getInfo(videoId, 'IOS');
  } catch {
    info = await yt.getInfo(videoId); // web fallback
  }
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
  throw new Error('NO_STREAM');
}

export async function extractMetaOnly(videoId: string): Promise<{ title?: string; artist?: string; durationSec?: number }> {
  try {
    const yt = await getYTClient();
    const info: any = await yt.getInfo(videoId, 'IOS');
    return { title: info?.basic_info?.title, artist: info?.basic_info?.author, durationSec: info?.basic_info?.duration };
  } catch {
    return {};
  }
}
