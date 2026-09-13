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

/** Why extraction failed, so the UI can say something the user can act on. */
export type ExtractCode = 'LOGIN_REQUIRED' | 'LOGIN_STALE' | 'NETWORK' | 'FAILED';

export class ExtractError extends Error {
  constructor(readonly code: ExtractCode, detail?: string) {
    super(detail ? `${code}: ${detail}` : code);
    this.name = 'ExtractError';
  }
}

export function extractCodeOf(e: unknown): ExtractCode | null {
  return e instanceof ExtractError ? e.code : null;
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

/** Device-side extraction — NATIVE FAST PATH (Metrolist-style).
 *  One raw innertube POST per client executed in Kotlin (device IP, exact
 *  client context) + a byte-range probe so only URLs that really stream come
 *  back. This replaces youtubei.js as the primary path: the JS chain kept
 *  returning po-token-locked URLs that 403'd on fetch (no stream / 403). */
function b64ToUtf8Fallback(b64: string): string {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new TextDecoder('utf-8').decode(bytes);
}

async function extractNative(videoId: string, quality: QualityId): Promise<ExtractedStream | null> {
  const b = (typeof window !== 'undefined' ? (window as any).AndroidBridge : undefined);
  if (typeof b?.innertubePlayer2 !== 'function') return null;
  const w = window as any;
  if (!w.__itDone) {
    w.__itPending = {} as Record<string, (b64: string) => void>;
    w.__itDone = (id: string, b64: string) => {
      try { w.__itPending?.[id]?.(b64); } catch { /* dropped */ }
      delete w.__itPending?.[id];
    };
  }
  const callId = Math.random().toString(36).slice(2) + Date.now().toString(36);
  const raw = await new Promise<string>((resolve, reject) => {
    const timer = setTimeout(() => {
      delete w.__itPending[callId];
      reject(new Error('native extract timeout'));
    }, 30000);
    w.__itPending[callId] = (b64: string) => {
      clearTimeout(timer);
      try {
        resolve(typeof b64 === 'string' && !b64.startsWith('{') ? b64ToUtf8Fallback(b64) : b64);
      } catch (e) { reject(e); }
    };
    try {
      b.innertubePlayer2(callId, videoId, quality);
    } catch (e: any) {
      clearTimeout(timer);
      delete w.__itPending[callId];
      reject(new Error(`bridge call failed: ${e?.message ?? e}`));
    }
  });
  let data: any;
  try { data = JSON.parse(raw); } catch { throw new Error('native envelope malformed'); }
  if (!data?.ok || !data?.url) {
    const code = data?.code;
    if (code === 'LOGIN_REQUIRED' || code === 'LOGIN_STALE' || code === 'NETWORK') {
      throw new ExtractError(code, String(data?.attempts ?? '').slice(0, 120));
    }
    throw new Error(`NATIVE ${data?.error ?? 'failed'} ${data?.attempts ?? ''}`.slice(0, 160));
  }
  const picked: PickedAudio = {
    url: data.url,
    itag: data.itag ?? 0,
    size: data.size ?? undefined,
    mime: data.mime ?? undefined,
    durationSec: data.duration ?? undefined,
    title: data.title ?? undefined,
    artist: data.author ?? undefined,
    ua: data.ua,
  };
  return {
    url: jixStreamUrl(picked),
    itag: picked.itag,
    size: picked.size,
    durationSec: picked.durationSec,
    title: picked.title,
    artist: picked.artist,
    proxied: true,
  };
}

/** Device-side extraction: native innertube first (Metrolist-style), then the
 *  youtubei.js client chain through the native bridge as a second chance. */
async function extractOnDevice(videoId: string, quality: QualityId): Promise<ExtractedStream> {
  // — 1) native Kotlin extractor (primary, exactly like Metrolist/InnerTune)
  try {
    const native = await extractNative(videoId, quality);
    if (native) return native;
  } catch (nativeErr) {
    // A sign-in gate is not something the JS chain can get past — it shares the
    // device's session — so report it straight away instead of stalling on retries.
    if (nativeErr instanceof ExtractError) throw nativeErr;
    // — 2) youtubei.js chain via native bridge (legacy second chance)
    const errors: string[] = [`native:${(nativeErr as Error)?.message?.slice(0, 60) ?? 'failed'}`];
    try {
      const { Innertube } = await import('youtubei.js/web.bundle');
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
    } catch (chainErr: any) {
      errors.push(`chain:${(chainErr?.message ?? 'error').slice(0, 60)}`);
    }
    throw new Error(`EXTRACT_CHAIN_FAILED ${errors.join(' | ')}`);
  }
  // bridge has no innertubePlayer2 (old APK) → run the legacy chain directly
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
        return { url: jixStreamUrl(picked), itag: picked.itag, size: picked.size, durationSec: picked.durationSec, title: picked.title, artist: picked.artist, proxied: true };
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
      // the device is the primary path — its verdict is the one worth reporting
      if (deviceError instanceof ExtractError) throw deviceError;
      throw new Error(
        `NO_STREAM (device: ${deviceError instanceof Error ? deviceError.message.slice(0, 80) : 'failed'} | server: ${e instanceof Error ? e.message.slice(0, 60) : 'failed'})`
      );
    }
  }

  // Plain browser → same-origin server proxy. Ask for the metadata first: it
  // shares the server's extraction cache with the proxy (so this costs no extra
  // extraction) and it is the only way to learn WHY a track will not play —
  // otherwise the failure only surfaces as an opaque <audio> error.
  const qs = `id=${encodeURIComponent(videoId)}&q=${encodeURIComponent(quality)}`;
  const r = await fetch(`/api/yt/player?${qs}`, { cache: 'no-store' });
  const d = await r.json().catch(() => null);
  if (!d?.ok) {
    const code = d?.code;
    throw new ExtractError(
      code === 'LOGIN_REQUIRED' || code === 'LOGIN_STALE' || code === 'NETWORK' ? code : 'FAILED',
      String(d?.attempts ?? '').slice(0, 120)
    );
  }
  return {
    url: `/api/yt/stream?${qs}`,
    itag: d.itag ?? 0,
    size: d.size,
    durationSec: d.durationSec,
    title: d.title,
    artist: d.artist,
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
