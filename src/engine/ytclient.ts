'use client';
import { flattenYTM, MOOD_QUERIES, RawSong } from '@/lib/ytm-shared';
import { hasNativeBridge, nativeFetch } from '@/engine/nativeFetch';

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

export async function searchTracks(query: string): Promise<RawSong[]> {
  if (canUseClientEngine()) {
    const yt = await getYTClient();
    const res: any = await yt.music.search(query, { filter: 'songs' });
    const out = flattenYTM(res?.contents ?? res, []).slice(0, 40);
    if (out.length > 0 || isE2E()) return out;
    // device engine found nothing → still try server (if any) before giving up
  }
  // plain browser (no native bridge): search via our server, no CORS there
  const r = await fetch(`/api/yt/search?q=${encodeURIComponent(query)}`, { cache: 'no-store' });
  const d = await r.json();
  return (d?.tracks ?? []) as RawSong[];
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
