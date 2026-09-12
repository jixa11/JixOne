import { NextRequest, NextResponse } from 'next/server';
import { searchYTMusic, cached, getTrackMeta } from '@/lib/yt-server';

export const dynamic = 'force-dynamic';

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const q = (searchParams.get('q') ?? '').trim();
  const id = searchParams.get('id');
  if (id) {
    try {
      const meta = await cached(`track:${id}`, 60 * 60_000, () => getTrackMeta(id));
      return NextResponse.json({ ok: true, track: meta });
    } catch {
      return NextResponse.json({ ok: false, track: null }, { status: 200 });
    }
  }
  if (!q) return NextResponse.json({ ok: false, tracks: [], error: 'empty query' });
  try {
    const tracks = await cached(`q:${q}`, 10 * 60_000, () => searchYTMusic(q, 'songs'));
    return NextResponse.json({ ok: true, tracks });
  } catch (e) {
    console.error('search error', e);
    return NextResponse.json({ ok: false, tracks: [], error: 'search failed' }, { status: 200 });
  }
}
