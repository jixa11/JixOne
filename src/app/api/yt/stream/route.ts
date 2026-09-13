import { NextRequest } from 'next/server';
import { getStream } from '@/lib/yt-server';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const Q_INDEX: Record<string, number> = { high: 0, mid: 1, low: 2 };

type Meta = NonNullable<Awaited<ReturnType<typeof getStream>>['stream']>;

/**
 * GET /api/yt/stream?id=<videoId>&q=high|mid|low
 *
 * Server-side audio PROXY: extracts a fresh stream URL (bound to THIS server's
 * IP) and pipes the bytes through, so the browser/APK never touches
 * googlevideo directly (no CORS, no IP-lock). Supports Range (seek + resume).
 * Cache-busting: &fresh=1 skips the extraction cache.
 */
export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const id = (searchParams.get('id') ?? '').trim();
  const q = Q_INDEX[searchParams.get('q') ?? 'high'] ?? 0;
  const fresh = searchParams.get('fresh') === '1';
  if (!id) return json({ ok: false, error: 'missing id' }, 400);

  let meta: Meta | null = null;
  try {
    const out = await getStream(id, q, fresh);
    if (!out.stream) return json({ ok: false, error: out.code, code: out.code, attempts: out.attempts }, 502);
    meta = out.stream;
  } catch (e: any) {
    return json({ ok: false, error: String(e?.message ?? 'extract failed') }, 502);
  }

  try {
    const baseHeaders: Record<string, string> = {
      'user-agent': meta.ua ?? 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
      referer: 'https://music.youtube.com/',
    };
    const range = req.headers.get('range');
    const upstream = await fetch(meta.url, {
      headers: range ? { ...baseHeaders, range } : baseHeaders,
      signal: AbortSignal.timeout(60000),
    });

    // expired/locked URL → re-extract once and retry
    if (upstream.status === 403 || upstream.status === 410) {
      const retry = (await getStream(id, q, true)).stream;
      if (retry?.url) {
        const r2 = await fetch(retry.url, {
          headers: { 'user-agent': retry.ua ?? baseHeaders['user-agent'], referer: baseHeaders.referer, ...(range ? { range } : {}) },
          signal: AbortSignal.timeout(60000),
        });
        if (r2.ok && r2.body) return pipe(r2, retry.mime);
      }
    }

    if (!upstream.ok || !upstream.body) {
      return json({ ok: false, error: `upstream ${upstream.status}` }, 502);
    }
    return pipe(upstream, meta.mime);
  } catch (e: any) {
    return json({ ok: false, error: String(e?.message ?? 'proxy failed') }, 502);
  }
}

function pipe(upstream: Response, mime?: string) {
  const headers = new Headers();
  headers.set('content-type', upstream.headers.get('content-type') ?? mime ?? 'audio/mp4');
  const len = upstream.headers.get('content-length');
  if (len) headers.set('content-length', len);
  const cr = upstream.headers.get('content-range');
  if (cr) headers.set('content-range', cr);
  headers.set('accept-ranges', 'bytes');
  headers.set('access-control-allow-origin', '*');
  headers.set('cache-control', 'no-store');
  return new Response(upstream.body, { status: upstream.status, headers });
}

function json(body: unknown, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
  });
}
