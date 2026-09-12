import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

// E2E test fixture (?e2e=1 debug mode only): canned YTMusic search response.
// Shape is consumed by flattenYTM (videoId + title + subtitle + thumbnails on one node).
function thumb(c1: string, c2: string, label: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="${c1}"/><stop offset="100%" stop-color="${c2}"/></linearGradient></defs><rect width="100" height="100" fill="url(#g)"/><text x="50" y="50" text-anchor="middle" dominant-baseline="central" font-family="sans-serif" font-weight="800" font-size="30" fill="rgba(255,255,255,.95)">${label}</text></svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

const SONGS: { id: string; title: string; artist: string; dur: string; c: [string, string] }[] = [
  { id: 'np_mock_01', title: 'Blinding Lights', artist: 'The Weeknd', dur: '3:20', c: ['#ff2d55', '#8b5cf6'] },
  { id: 'np_mock_02', title: 'Levitating', artist: 'Dua Lipa', dur: '3:23', c: ['#22d3ee', '#6366f1'] },
  { id: 'np_mock_03', title: 'Shape of You', artist: 'Ed Sheeran', dur: '3:53', c: ['#1db954', '#14b8a6'] },
  { id: 'np_mock_04', title: 'bad guy', artist: 'Billie Eilish', dur: '3:14', c: ['#ff6b35', '#f59e0b'] },
  { id: 'np_mock_05', title: 'Believer', artist: 'Imagine Dragons', dur: '3:24', c: ['#3b82f6', '#ec4899'] },
  { id: 'np_mock_06', title: 'Clocks', artist: 'Coldplay', dur: '5:07', c: ['#8b5cf6', '#0ea5e9'] },
];

export async function GET() {
  return NextResponse.json({
    contents: SONGS.map((s, i) => ({
      videoId: s.id,
      title: { text: s.title },
      subtitle: { text: `${s.artist} • ${s.dur}` },
      thumbnails: [{ url: thumb(s.c[0], s.c[1], s.title.slice(0, 2).toUpperCase()) }],
      index: i,
    })),
  });
}
