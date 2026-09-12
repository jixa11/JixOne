import { NextRequest, NextResponse } from 'next/server';
import { searchYTMusic, cached } from '@/lib/yt-server';

export const dynamic = 'force-dynamic';

const MOODS: Record<string, string[]> = {
  iran_now: ['آهنگ جدید پاپ ایرانی', 'آهنگ ایرانی پرطرفدار', 'persian pop hits'],
  pop: ['top pop hits 2026', 'pop international hits'],
  classic: ['persian classic hits 70s 80s', 'timeless persian songs'],
  electronic: ['electronic chill mix', 'edm top hits'],
  hiphop: ['hip hop hits 2026', 'رپ فارسی جدید'],
  lofi: ['lofi chill beats', 'lofi study music'],
  rock: ['rock classics', 'best rock songs'],
  ambient: ['ambient relax music', 'piano calm music'],
};

export async function GET(req: NextRequest) {
  const mood = new URL(req.url).searchParams.get('mood') ?? 'iran_now';
  const queries = MOODS[mood] ?? MOODS.iran_now;
  try {
    const results = await Promise.all(
      queries.map((q) => cached(`mood:${q}`, 30 * 60_000, () => searchYTMusic(q, 'songs')).catch(() => []))
    );
    // interleave & dedupe
    const seen = new Set<string>();
    const tracks: unknown[] = [];
    const maxLen = Math.max(...results.map((r) => r.length));
    for (let i = 0; i < maxLen; i++) {
      for (const r of results) {
        const t = r[i];
        if (t && !seen.has(t.videoId)) { seen.add(t.videoId); tracks.push(t); }
      }
    }
    return NextResponse.json({ ok: true, tracks: tracks.slice(0, 30) });
  } catch (e) {
    console.error('home error', e);
    return NextResponse.json({ ok: false, tracks: [] }, { status: 200 });
  }
}
