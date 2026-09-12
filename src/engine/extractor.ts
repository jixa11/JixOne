'use client';
import type { QualityId } from '@/lib/types';
import { pickAudioFromInfo, CLIENT_UA, b64urlEncode, type PickedAudio } from '@/lib/streamClients';
import { STREAM_CLIENTS } from '@/lib/streamClients';
import { apiFetch } from '@/engine/apiBase';
import { hasNativeBridge, nativeFetch } from '@/engine/nativeFetch';

export interface ExtractedStream {
  url: string;
  itag: number;
  size?: number;
  durationSec?: number;
  title?: string;
  artist?: string;
  /** true when the URL streams through our own layer (jixstream intercept or server proxy) */
  proxied?: boolean;
}

const PREF: Record<QualityId, number[]> = {
  high: [251, 140, 250, 249],
  mid: [250, 249, 140, 251],
  low: [249, 139, 250, 140, 251],
};

/** The WebView intercept endpoint baked into MainActivity.kt (same-origin → no CORS) */
const APPASSETS = 'https://appassets.androidplatform.net';

function withTimeout<T>(p: Promise<T>, ms: number, label = 'timeout'): Promise<T> {
  return Promise.race([
    p,
    new Promise<T>((_, rej) => setTimeout(() => rej(new Error(label)), ms)),
  ]);
}

/** e2e debug mode (?e2e=1) keeps everything client-side with fixtures */
function isE2E(): boolean {
  return typeof window !== 'undefined' && window.location.search.includes('e2e=1');
}

/** Wrap an upstream googlevideo URL so the NATIVE layer fetches it
 *  (correct client UA + Range passthrough). The resulting URL is same-origin
 *  for the WebView page → <audio> and fetch() both work, no CORS, no IP-lock. */
function jixStreamUrl(picked: PickedAudio): string {
  const params = new URLSearchParams();
  params.set('u', b64urlEncode(picked.url));
  params.set('ua', b64urlEncode(picked.ua ?? CLIENT_UA.WEB_REMIX));
  if (picked.mime) params.set('ct', b64urlEncode(picked.mime));
  return `${APPASSETS}/jixstream/?${params.toString()}`;
}

/** Device-side extraction: try each innertube client through the native bridge
 *  until one yields a playable audio URL. Runs 100% on the phone. */
async function extractOnDevice(videoId: string, quality: QualityId): Promise<ExtractedStream> {
  const { Innertube } = await import('youtubei.js/web.bundle');
  const errors: string[] = [];
  for (const clientType of STREAM_CLIENTS) {
    try {
      const yt = await withTimeout(
        Innertube.create({ client_type: clientType as any, fetch: nativeFetch as any, retrieve_player: clientType === 'WEB_REMIX' }),
        12000,
        `${clientType} boot`
      );
      const info: any = await withTimeout(yt.getBasicInfo(videoId), 12000, `${clientType} extract`);
      const base = pickAudioFromInfo(info, PREF[quality]);
      if (base) {
        const picked: PickedAudio = { ...base, ua: CLIENT_UA[clientType] };
        const wrapped = jixStreamUrl(picked);
        return {
          url: wrapped,
          itag: picked.itag,
          size: picked.size,
          durationSec: picked.durationSec,
          title: picked.title,
          artist: picked.artist,
          proxied: true,
        };
      }
      errors.push(`${clientType}:no-url`);
    } catch (e: any) {
      errors.push(`${clientType}:${(e?.message ?? 'error').slice(0, 40)}`);
    }
  }
  throw new Error(`EXTRACT_CHAIN_FAILED ${errors.join(' | ')}`);
}

/** E2E fixture path — mock tube, relative URL, no wrapping. */
async function extractE2E(videoId: string, quality: QualityId): Promise<ExtractedStream | null> {
  const { getYTClient } = await import('@/engine/ytclient');
  const yt = await getYTClient();
  const info = await yt.getInfo(videoId);
  const formats: any[] = info?.streaming_data?.adaptive_formats ?? [];
  const pick = (itag: number) => formats.find((f) => f.itag === itag && f.url);
  for (const itag of PREF[quality]) {
    const f = pick(itag);
    if (f) return { url: f.url, itag, size: f.content_length, durationSec: info?.basic_info?.duration, title: info?.basic_info?.title, artist: info?.basic_info?.author };
  }
  const auds = formats.filter((f) => f.has_audio && !f.has_video && f.url);
  if (auds[0]) return { url: auds[0].url, itag: auds[0].itag, size: auds[0].content_length, durationSec: info?.basic_info?.duration };
  return null;
}

/**
 * Resolve a playable/downloadable audio URL for a video.
 *
 * APK (native bridge):  device extraction (native sockets, correct client UA)
 *                       → stream through the /jixstream/ WebView intercept.
 *                       Falls back to the helper-server proxy when the device
 *                       path yields nothing (e.g. YouTube blocked without VPN).
 * Plain browser:        /api/yt/stream proxy (same-origin, server does the
 *                       extraction + piping).
 * E2E (?e2e=1):         fixture stream.
 */
export async function extractStream(videoId: string, quality: QualityId): Promise<ExtractedStream> {
  if (isE2E()) {
    const m = await extractE2E(videoId, quality);
    if (m) return m;
    throw new Error('NO_STREAM');
  }

  if (hasNativeBridge()) {
    let deviceError: unknown = null;
    try {
      return await withTimeout(extractOnDevice(videoId, quality), 45000, 'device extract total timeout');
    } catch (e) {
      deviceError = e;
    }
    // helper-server proxy fallback (absolute URL via bridge; no CORS there)
    try {
      const base = (await import('@/engine/apiBase')).getApiBase();
      if (!base) throw new Error('NO_HELPER_SERVER');
      const r = await apiFetch(`/api/yt/player?id=${encodeURIComponent(videoId)}&q=${encodeURIComponent(quality)}`, { cache: 'no-store' }, 25000);
      const d = await r.json();
      if (d?.ok && d?.url) {
        return { url: `${base}/api/yt/stream?id=${encodeURIComponent(videoId)}&q=${encodeURIComponent(quality)}`, itag: d.itag ?? 0, size: d.size, durationSec: d.durationSec, title: d.title, artist: d.artist, proxied: true };
      }
      throw new Error(d?.error ?? 'SERVER_NO_STREAM');
    } catch (e) {
      throw new Error(
        `NO_STREAM (device: ${deviceError instanceof Error ? deviceError.message.slice(0, 80) : 'failed'} | server: ${e instanceof Error ? e.message.slice(0, 60) : 'failed'})`
      );
    }
  }

  // plain browser → same-origin server proxy (streams audio directly)
  return {
    url: `/api/yt/stream?id=${encodeURIComponent(videoId)}&q=${encodeURIComponent(quality)}`,
    itag: 0,
    proxied: true,
  };
}

export async function extractMetaOnly(videoId: string): Promise<{ title?: string; artist?: string; durationSec?: number }> {
  if (isE2E()) return {};
  if (hasNativeBridge()) {
    try {
      const { Innertube } = await import('youtubei.js/web.bundle');
      const yt = await withTimeout(Innertube.create({ client_type: 'WEB_REMIX' as any, fetch: nativeFetch as any }), 12000, 'boot');
      const info: any = await withTimeout(yt.getBasicInfo(videoId), 12000, 'meta');
      return { title: info?.basic_info?.title, artist: info?.basic_info?.author, durationSec: info?.basic_info?.duration };
    } catch {
      /* fall through */
    }
  }
  try {
    const r = await apiFetch(`/api/yt/search?id=${encodeURIComponent(videoId)}`, { cache: 'no-store' }, 20000);
    const d = await r.json();
    if (d?.ok && d?.track) {
      return { title: d.track.title, artist: d.track.artist, durationSec: undefined };
    }
  } catch { /* offline */ }
  return {};
}
