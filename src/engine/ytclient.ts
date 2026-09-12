'use client';
import { flattenYTM, MOOD_QUERIES, RawSong } from '@/lib/ytm-shared';

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

/** Shared browser innertube client (real browser env = trusted by YouTube) */
export async function getYTClient(): Promise<any> {
  if (!clientPromise) {
    if (isE2E()) {
      clientPromise = Promise.resolve(makeMockTube());
      return clientPromise;
    }
    clientPromise = import('youtubei.js/web.bundle')
      .then((m: any) => m.Innertube.create({ client_type: 'WEB' }))
      .catch((e) => { clientPromise = null; throw e; });
  }
  return clientPromise;
}

export async function searchTracks(query: string): Promise<RawSong[]> {
  const yt = await getYTClient();
  const res: any = await yt.music.search(query, { filter: 'songs' });
  return flattenYTM(res?.contents ?? res, []).slice(0, 40);
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
