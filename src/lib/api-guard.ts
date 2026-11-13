import { timingSafeEqual } from 'node:crypto';
import type { NextRequest } from 'next/server';

/**
 * Guards for the helper server's own API.
 *
 * Writes to the stored YouTube Music session must come from the app itself:
 *  - with JIXONE_ADMIN_TOKEN set (do this on any public deployment), the
 *    request must carry the same value in the x-jixone-token header;
 *  - without it, only same-origin browser requests are accepted, which stops
 *    other websites from changing the session but not direct HTTP clients.
 */
export const ADMIN_TOKEN_HEADER = 'x-jixone-token';

function sameOrigin(req: NextRequest): boolean {
  const origin = req.headers.get('origin');
  const host = req.headers.get('x-forwarded-host') ?? req.headers.get('host');
  if (!origin || !host) return false;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

export function mayChangeSession(req: NextRequest): boolean {
  const expected = process.env.JIXONE_ADMIN_TOKEN;
  if (expected) {
    const got = Buffer.from(req.headers.get(ADMIN_TOKEN_HEADER) ?? '');
    const want = Buffer.from(expected);
    return got.length === want.length && timingSafeEqual(got, want);
  }
  return sameOrigin(req);
}

/** YouTube video ids are exactly 11 url-safe base64 characters. */
export function isVideoId(id: string): boolean {
  return /^[A-Za-z0-9_-]{11}$/.test(id);
}

const WINDOW_MS = 60_000;
const hits = new Map<string, number[]>();

/** Sliding-window limit per client address; true when the call may proceed. */
export function withinRateLimit(req: NextRequest, bucket: string, max: number): boolean {
  const ip = (req.headers.get('x-forwarded-for') ?? '').split(',')[0].trim() || 'local';
  const key = `${bucket}:${ip}`;
  const now = Date.now();
  const recent = (hits.get(key) ?? []).filter((t) => now - t < WINDOW_MS);
  if (recent.length >= max) {
    hits.set(key, recent);
    return false;
  }
  recent.push(now);
  hits.set(key, recent);
  if (hits.size > 5000) {
    for (const [k, v] of hits) if (v.every((t) => now - t >= WINDOW_MS)) hits.delete(k);
  }
  return true;
}
