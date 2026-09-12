'use client';
import { flattenYTM, MOOD_QUERIES, RawSong } from '@/lib/ytm-shared';
import { hasNativeBridge, nativeFetch } from '@/engine/nativeFetch';
import { apiFetch } from '@/engine/apiBase';

let clientPromise: Promise<any> | null = null;

/** E2E debug mode (?e2e=1): swap transport for local fixtures, keep all app logic real */
function isE2E(): boolean {
  return typeof window !== 'undefined' && window.location.search.includes('e2e=1');
}

function makeMockTube(): any {
  return {
    music: {
      search: async () => {
        const { mockSearchResponse } = await import('@/engine/e2eFixtures');
        return mockSearchResponse;
      },
    },
    getInfo: async () => {
      const { mockPlayerResponse } = await import('@/engine/e2eFixtures');
      return mockPlayerResponse;
    },
  };
}

function withTimeout<T>(p: Promise<T>, ms: number, label = 'timeout'): Promise<T> {
  return Promise.race([
    p,
    new Promise<T>((_, rej) => setTimeout(() => rej(new Error(label)), ms)),
  ]);
}

/**
 * Shared innertube client.
 * Transport reality check:
 *  - YouTube does NOT allow cross-origin (CORS) innertube calls from non-Google
 *    web origins → a plain browser cannot call YouTube directly.
 *  - In the APK we register a custom fetch that routes through the Android
 *    native bridge → no CORS applies, everything runs on the device.
 *  - In a plain browser we skip innertube entirely and use this app's own
 *    server routes (/api/yt/*) which call YouTube server-to-server.
 */
export async function getYTClient(): Promise<any> {
  if (!clientPromise) {
    if (isE2E()) {
      clientPromise = Promise.resolve(makeMockTube());
      return clientPromise;
    }
    clientPromise = import('youtubei.js/web.bundle')
      .then((m: any) =>
        m.Innertube.create({
          client_type: 'WEB',
          fetch: hasNativeBridge() ? nativeFetch : undefined,
        })
      )
      .catch((e) => { clientPromise = null; throw e; });
  }
  return clientPromise;
}

/** true when innertube can run in THIS context (device bridge or E2E mock) */
export function canUseClientEngine(): boolean {
  return hasNativeBridge() || isE2E();
}

/**
 * Search with a resilient fallback chain:
 *   1. device engine (APK): innertube over the native bridge, 12s timeout
 *   2. helper server (/api/yt/search) — in the browser it is same-origin,
 *      in the APK it goes through the bridge to the configured helper server
 * Throws only when BOTH fail, so the UI can show a real error state.
 */
export async function searchTracks(query: string): Promise<RawSong[]> {
  const errors: string[] = [];

  if (canUseClientEngine()) {
    try {
      const yt = await withTimeout(getYTClient(), 15000, 'client boot timeout');
      const res: any = await withTimeout(
        yt.music.search(query, { filter: 'songs' }),
        12000,
        'device search timeout'
      );
      const out = flattenYTM(res?.contents ?? res, []).slice(0, 40);
      if (out.length > 0 || isE2E()) return out;
      errors.push('device:0 results');
    } catch (e: any) {
      errors.push(`device:${e?.message ?? 'error'}`);
    }
  }

  // helper server (browser: relative same-origin; APK: absolute via bridge)
  try {
    const r = await apiFetch(`/api/yt/search?q=${encodeURIComponent(query)}`, { cache: 'no-store' });
    const d = await r.json();
    const tracks = (d?.tracks ?? []) as RawSong[];
    if (tracks.length > 0) return tracks;
    if (d?.ok) return tracks; // server answered, genuinely no results
    errors.push(`server:${d?.error ?? 'bad response'}`);
  } catch (e: any) {
    errors.push(`server:${e?.message ?? 'error'}`);
  }

  throw new Error(errors.join(' | ') || 'no search path available');
}

export async function homeTracks(mood = 'iran_now'): Promise<RawSong[]> {
  const queries = (MOOD_QUERIES[mood] ?? MOOD_QUERIES.iran_now).slice(0, 2);
  const results = await Promise.all(
    queries.map((q) => searchTracks(q).catch(() => []))
  );
  const seen = new Set<string>();
  const out: RawSong[] = [];
  const maxLen = Math.max(0, ...results.map((r) => r.length));
  for (let i = 0; i < maxLen; i++) {
    for (const r of results) {
      const t = r[i];
      if (t && !seen.has(t.videoId)) { seen.add(t.videoId); out.push(t); }
    }
  }
  return out.slice(0, 30);
}
