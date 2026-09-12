import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

// artwork proxy: fetch remote YT thumbnails server-side (avoids CORS / hotlink issues in WebView + widget)
export async function GET(req: NextRequest) {
  const u = new URL(req.url).searchParams.get('u');
  if (!u || !/^https:\/\/(yt3|i9?|lh3)\.ggpht\.com|^https:\/\/i\.ytimg\.com/.test(u)) {
    return NextResponse.json({ ok: false }, { status: 400 });
  }
  try {
    const res = await fetch(u, { next: { revalidate: 86400 } });
    if (!res.ok) return NextResponse.json({ ok: false }, { status: 502 });
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
