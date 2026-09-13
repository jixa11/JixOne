import 'server-only';
import { createHash } from 'node:crypto';

/**
 * YouTube Music cookie session for the server extractor — the same scheme the
 * Android app uses (see YTMAuth.kt).
 *
 * YouTube answers anonymous innertube player calls with LOGIN_REQUIRED
 * ("Sign in to confirm you're not a bot") for most tracks. Supplying an account
 * cookie lifts that gate. Self-hosters set YTM_COOKIE to the full cookie string
 * copied from a signed-in music.youtube.com session.
 */

export const YTM_ORIGIN = 'https://music.youtube.com';

export function ytmCookie(): string | null {
  return process.env.YTM_COOKIE?.trim() || null;
}

function sapisidOf(cookie: string): string | null {
  const jar: Record<string, string> = {};
  for (const part of cookie.split(';')) {
    const i = part.indexOf('=');
    if (i > 0) jar[part.slice(0, i).trim()] = part.slice(i + 1).trim();
  }
  return jar.SAPISID ?? jar['__Secure-3PAPISID'] ?? jar['__Secure-1PAPISID'] ?? null;
}

/** `SAPISIDHASH <unix>_<sha1(unix + " " + SAPISID + " " + origin)>` */
function sapisidHash(sapisid: string, origin: string): string {
  const ts = Math.floor(Date.now() / 1000);
  const hash = createHash('sha1').update(`${ts} ${sapisid} ${origin}`).digest('hex');
  return `SAPISIDHASH ${ts}_${hash}`;
}

/** Auth headers for innertube, or `{}` when no session is configured. */
export function ytmAuthHeaders(): Record<string, string> {
  const cookie = ytmCookie();
  if (!cookie) return {};
  const sapisid = sapisidOf(cookie);
  if (!sapisid) return {};
  return {
    cookie,
    authorization: sapisidHash(sapisid, YTM_ORIGIN),
    'x-goog-authuser': '0',
    origin: YTM_ORIGIN,
    'x-origin': YTM_ORIGIN,
  };
}

export function ytmSignedIn(): boolean {
  const c = ytmCookie();
  return !!c && !!sapisidOf(c);
}
