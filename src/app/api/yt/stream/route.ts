import { NextRequest } from 'next/server';
import { getStream } from '@/lib/yt-server';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const Q_INDEX: Record<string, number> = { high: 0, mid: 1, low: 2 };

type Meta = NonNullable<Awaited<ReturnType<typeof getStream>>['stream']>;

const UA_FALLBACK =
  'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';

/** googlevideo stops sending partway through an un-ranged request — a 3.4 MB
 *  track arrived ~55% complete — so the upstream is always read in ranges.
 *  Cap: pot-bound WEB_REMIX URLs answer 403 for range spans beyond ~1 MB
 *  (verified: 1 MB → 206, 1.25 MB → 403), so chunks stay at 768 KB. */
const CHUNK = 768 * 1024;

function upstreamHeaders(ua: string | undefined, range: string): Record<string, string> {
  return {
    'user-agent': ua ?? UA_FALLBACK,
    referer: 'https://music.youtube.com/',
    range,
  };
}

/**
 * A continuous stream of `start..end` assembled from successive ranged
 * requests. On upstream 403/429 (the per-URL byte quota on pot-bound URLs),
 * the URL is refreshed via a fresh extraction and the chunk retries — the
 * position keeps advancing from where the previous chunk stopped.
 */
function rangedStream(
  holder: { meta: Meta },
  refresh: () => Promise<Meta | null>,
  start: number,
  end: number,
): ReadableStream<Uint8Array> {
  let pos = start;
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  let refreshes = 0;

  return new ReadableStream<Uint8Array>({
    async pull(controller) {
      for (;;) {
        if (!reader) {
          if (pos > end) {
            controller.close();
            return;
          }
          const stop = Math.min(pos + CHUNK - 1, end);
          let res: Response;
          try {
            res = await fetch(holder.meta.url, {
              headers: upstreamHeaders(holder.meta.ua, `bytes=${pos}-${stop}`),
              signal: AbortSignal.timeout(60000),
            });
          } catch (e: any) {
            if (refreshes < 4) {
              // transient network hiccup — refresh and retry once
              refreshes++;
              const fresh = await refresh();
              if (fresh) { holder.meta = fresh; continue; }
            }
            controller.error(new Error(`upstream fetch: ${e?.message ?? e}`));
            return;
          }
          if ((res.status === 403 || res.status === 429) && refreshes < 4) {
            refreshes++;
            try { await res.arrayBuffer().catch(() => undefined); } catch { /* drain */ }
            const fresh = await refresh();
            if (fresh) {
              holder.meta = fresh;
              continue; // retry the same byte span on the fresh URL
            }
            controller.error(new Error('upstream 403 — no fresh URL'));
            return;
          }
          if (!res.ok || !res.body) {
            controller.error(new Error(`upstream ${res.status}`));
            return;
          }
          reader = res.body.getReader();
        }

        const { done, value } = await reader.read();
        if (done) {
          reader = null;
          continue; // next range
        }
        pos += value.byteLength;
        controller.enqueue(value);
        return;
      }
    },
    cancel() {
      reader?.cancel().catch(() => {});
    },
  });
}

/** Total byte length, from the format metadata or from a 1-byte probe. */
async function totalSize(meta: Meta): Promise<number | null> {
  if (meta.size && meta.size > 0) return meta.size;
  try {
    const res = await fetch(meta.url, {
      headers: upstreamHeaders(meta.ua, 'bytes=0-0'),
      signal: AbortSignal.timeout(20000),
    });
    await res.arrayBuffer().catch(() => undefined);
    const total = res.headers.get('content-range')?.split('/')[1];
    const n = total ? Number(total) : NaN;
    return Number.isFinite(n) && n > 0 ? n : null;
  } catch {
    return null;
  }
}

function parseRange(header: string | null, total: number): { start: number; end: number } | null {
  const m = /^bytes=(\d*)-(\d*)$/.exec(header?.trim() ?? '');
  if (!m) return null;
  const [, rawStart, rawEnd] = m;
  if (rawStart === '' && rawEnd === '') return null;
  // suffix form: last N bytes
  if (rawStart === '') {
    const len = Number(rawEnd);
    if (!Number.isFinite(len) || len <= 0) return null;
    return { start: Math.max(0, total - len), end: total - 1 };
  }
  const start = Number(rawStart);
  const end = rawEnd === '' ? total - 1 : Math.min(Number(rawEnd), total - 1);
  if (!Number.isFinite(start) || start > end || start < 0) return null;
  return { start, end };
}

/**
 * GET /api/yt/stream?id=<videoId>&q=high|mid|low
 *
 * Server-side audio PROXY: extracts a stream URL (bound to THIS server's IP)
 * and pipes the bytes through, so the browser/APK never touches googlevideo
 * directly (no CORS, no IP-lock). Supports Range for seek and resume.
 * Cache-busting: &fresh=1 skips the extraction cache.
 */
export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const id = (searchParams.get('id') ?? '').trim();
  const q = Q_INDEX[searchParams.get('q') ?? 'high'] ?? 0;
  const fresh = searchParams.get('fresh') === '1';
  if (!id) return json({ ok: false, error: 'missing id' }, 400);

  let meta: Meta;
  try {
    const out = await getStream(id, q, fresh);
    if (!out.stream) return json({ ok: false, error: out.code, code: out.code, attempts: out.attempts }, 502);
    meta = out.stream;
  } catch (e: any) {
    return json({ ok: false, error: String(e?.message ?? 'extract failed') }, 502);
  }

  const total = await totalSize(meta);
  if (!total) return json({ ok: false, error: 'NO_LENGTH' }, 502);

  const wanted = parseRange(req.headers.get('range'), total);
  const start = wanted?.start ?? 0;
  const end = wanted?.end ?? total - 1;

  const headers = new Headers({
    'content-type': meta.mime ?? 'audio/webm',
    'content-length': String(end - start + 1),
    'accept-ranges': 'bytes',
    'access-control-allow-origin': '*',
    'cache-control': 'no-store',
  });
  if (wanted) headers.set('content-range', `bytes ${start}-${end}/${total}`);
  // observability: the client normally learns this from /api/yt/player
  if (meta.previewLimited) headers.set('x-jix-preview', '1');

  const holder = { meta };
  const refresh = async (): Promise<Meta | null> => {
    try {
      const out = await getStream(id, q, true); // fresh extraction (pots stay cached)
      return out.stream ?? null;
    } catch {
      return null;
    }
  };

  return new Response(rangedStream(holder, refresh, start, end), {
    status: wanted ? 206 : 200,
    headers,
  });
}

function json(body: unknown, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
  });
}
