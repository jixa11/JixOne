import { NextRequest, NextResponse } from 'next/server';
import { getStream } from '@/lib/yt-server';

const Q_INDEX: Record<string, number> = { high: 0, mid: 1, low: 2 };

/** GET /api/yt/player?id=<videoId>&q=high|mid|low
 *  Server-side stream extraction for plain-browser contexts (no CORS to YouTube). */
export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const id = (searchParams.get('id') ?? '').trim();
  const q = Q_INDEX[searchParams.get('q') ?? 'high'] ?? 0;
  if (!id) return NextResponse.json({ ok: false, error: 'missing id' }, { status: 200 });
  try {
    const out = await getStream(id, q);
    if (!out.stream) {
      return NextResponse.json({ ok: false, error: out.code, code: out.code, attempts: out.attempts }, { status: 200 });
    }
    return NextResponse.json({ ok: true, ...out.stream });
  } catch (e: any) {
    console.error('player error', e?.message ?? e);
    return NextResponse.json({ ok: false, error: 'extract failed', code: 'FAILED' }, { status: 200 });
  }
}
