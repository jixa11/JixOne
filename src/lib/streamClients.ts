/** Shared innertube client-chain config for stream extraction.
 *  Used by BOTH the device engine (extractor.ts, via native bridge) and the
 *  server engine (yt-server.ts).
 *
 *  Reality (verified 2026-09 with live probes):
 *   - YouTube withholds `streaming_data` for datacenter IPs on ALL clients
 *     (playability LOGIN_REQUIRED — bot-gating). Residential/mobile IPs are
 *     usually fine, which is why extraction happens on the DEVICE in the APK.
 *   - `getInfo()` in youtubei.js v18 pairs the player call with a /next call
 *     that 404s for some clients → we must use getBasicInfo (player only).
 *   - WEB-family clients need a poToken for streams on most IPs → prefer
 *     app clients first.
 */
export const STREAM_CLIENTS = ['ANDROID_VR', 'TVHTML5_SIMPLY', 'iOS', 'WEB_REMIX', 'ANDROID'] as const;
export type StreamClient = (typeof STREAM_CLIENTS)[number];

/** User-Agent that must accompany googlevideo requests for each client
 *  (googlevideo may 403 media requests whose UA doesn't match the client). */
export const CLIENT_UA: Record<StreamClient, string> = {
  ANDROID_VR:
    'com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip',
  TVHTML5_SIMPLY: 'Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Unset',
  iOS: 'com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)',
  WEB_REMIX:
    'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
  ANDROID: 'com.google.android.youtube/19.44.38 (Linux; U; Android 13) gzip',
};

export interface PickedAudio {
  url: string;
  itag: number;
  size?: number;
  mime?: string;
  durationSec?: number;
  title?: string;
  artist?: string;
  /** UA to use when fetching the googlevideo URL (set by the caller per client) */
  ua?: string;
}

/** Pick the best audio-only format from a getInfo/getBasicInfo response. */
export function pickAudioFromInfo(info: any, itagPref: number[]): PickedAudio | null {
  const formats: any[] = info?.streaming_data?.adaptive_formats ?? [];
  const basics = info?.basic_info ?? {};
  const hasUrl = (f: any) => !!f.url;
  const pick = (itag: number) => formats.find((f) => f.itag === itag && hasUrl(f));
  for (const itag of itagPref) {
    const f = pick(itag);
    if (f) return toPicked(f, basics);
  }
  const auds = formats
    .filter((f) => f.has_audio && !f.has_video && hasUrl(f))
    .sort((a, b) => (b.bitrate ?? 0) - (a.bitrate ?? 0));
  if (auds[0]) return toPicked(auds[0], basics);
  return null;
}

function toPicked(f: any, basics: any): PickedAudio {
  return {
    url: f.url,
    itag: f.itag,
    size: f.content_length ?? undefined,
    mime: normalizeMime(f.mime_type ?? f.mimeType),
    durationSec: basics.duration,
    title: basics.title,
    artist: basics.author,
  };
}

export function normalizeMime(m: any): string | undefined {
  if (!m || typeof m !== 'string') return undefined;
  const raw = m.split(';')[0].trim();
  if (raw.includes('webm')) return 'audio/webm';
  if (raw.includes('mp4') || raw.includes('m4a')) return 'audio/mp4';
  return raw || undefined;
}

/** b64url helpers (URL-safe, no padding) shared by JS-side jixstream wrapping. */
export function b64urlEncode(s: string): string {
  const b64 = typeof window !== 'undefined' ? btoa(unescape(encodeURIComponent(s))) : Buffer.from(s, 'utf-8').toString('base64');
  return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
