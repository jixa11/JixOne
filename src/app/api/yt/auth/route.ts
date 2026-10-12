import { NextRequest, NextResponse } from 'next/server';
import { setYtmCookie, clearYtmCookie, ytmSignedInDeep, effectiveYtmCookie, probeAccountName } from '@/lib/ytm-auth';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/** GET /api/yt/auth → { ok, signedIn, name?, source } */
export async function GET() {
  try {
    const signedIn = await ytmSignedInDeep();
    let name = '';
    if (signedIn) {
      const cookie = effectiveYtmCookie();
      if (cookie) name = (await probeAccountName(cookie)) ?? '';
    }
    return NextResponse.json({
      ok: true,
      signedIn,
      name,
      source: process.env.YTM_COOKIE ? 'env' : 'file',
    });
  } catch {
    return NextResponse.json({ ok: false, signedIn: false, name: '' }, { status: 200 });
  }
}

/** POST /api/yt/auth { cookie } → validate + save the YouTube Music session. */
export async function POST(req: NextRequest) {
  let body: { cookie?: string } = {};
  try { body = await req.json(); } catch { /* empty body */ }
  const raw = typeof body.cookie === 'string' ? body.cookie : '';
  const out = await setYtmCookie(raw);
  return NextResponse.json(out, { status: 200 });
}

/** DELETE /api/yt/auth → sign out (removes the stored cookie). */
export async function DELETE() {
  await clearYtmCookie();
  return NextResponse.json({ ok: true });
}
