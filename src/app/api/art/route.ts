import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const ART_HOSTS = /^((yt3|i9?|lh3)\.ggpht\.com|i\.ytimg\.com|(yt3|lh3)\.googleusercontent\.com)$/;

// artwork proxy: fetch remote YT thumbnails server-side (avoids CORS / hotlink issues in WebView + widget)
export async function GET(req: NextRequest) {
  const u = new URL(req.url).searchParams.get('u');
  // Match on the parsed hostname: a prefix test on the raw string also
  // accepted hosts like yt3.ggpht.com.evil.example (SSRF).
  let target: URL | null = null;
  try { target = u ? new URL(u) : null; } catch { target = null; }
  if (
    !target ||
    target.protocol !== 'https:' ||
    target.username || target.password || target.port ||
    !ART_HOSTS.test(target.hostname)
  ) {
    return NextResponse.json({ ok: false }, { status: 400 });
  }
  try {
    // redirect: 'error' — an allowed host must not bounce us somewhere else
    const res = await fetch(target, { redirect: 'error', next: { revalidate: 86400 } });
    if (!res.ok) return NextResponse.json({ ok: false }, { status: 502 });
    if (!(res.headers.get('content-type') ?? '').startsWith('image/')) {
      return NextResponse.json({ ok: false }, { status: 502 });
    }
    const buf = await res.arrayBuffer();
    return new NextResponse(buf, {
      headers: {
        'Content-Type': res.headers.get('content-type') ?? 'image/jpeg',
        'Cache-Control': 'public, max-age=86400',
      },
    });
  } catch {
    return NextResponse.json({ ok: false }, { status: 502 });
  }
}
