import 'server-only';
import { createHash } from 'node:crypto';
import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import { dirname, join } from 'node:path';

/**
 * YouTube Music cookie session for the server extractor — the same scheme the
 * Android app uses (see YTMAuth.kt).
 *
 * YouTube answers anonymous innertube player calls with LOGIN_REQUIRED
 * ("Sign in to confirm you're not a bot") for most tracks. Supplying an account
 * cookie lifts that gate. Self-hosters set YTM_COOKIE to the full cookie string
 * copied from a signed-in music.youtube.com session; the in-app Settings page
 * can also save the cookie at runtime (stored under .data/, gitignored).
 */

export const YTM_ORIGIN = 'https://music.youtube.com';

const COOKIE_FILE = join(process.cwd(), '.data', 'ytm-cookie.txt');

let fileCookie: string | null | undefined;

async function readCookieFile(): Promise<string | null> {
  if (fileCookie !== undefined) return fileCookie;
  try {
    const txt = (await readFile(COOKIE_FILE, 'utf-8')).trim();
    fileCookie = txt || null;
  } catch {
    fileCookie = null;
  }
  return fileCookie;
}

export function ytmCookie(): string | null {
  return process.env.YTM_COOKIE?.trim() || null;
}

async function ytmCookieAsync(): Promise<string | null> {
  return process.env.YTM_COOKIE?.trim() || (await readCookieFile());
}

/** Cookie used by the request path (sync hot path: env wins, file cookie is
 *  read once and cached by setYtmCookie / clearYtmCookie). */
export function effectiveYtmCookie(): string | null {
  return process.env.YTM_COOKIE?.trim() || fileCookie || null;
}

export interface CookieSaveResult {
  ok: boolean;
  code?: 'EMPTY' | 'NO_SAPISID' | 'SAVE_FAILED';
  name?: string;
}

/** Validate + persist a pasted cookie. Returns the account name when the
 *  session actually answers for us. */
export async function setYtmCookie(raw: string): Promise<CookieSaveResult> {
  const cookie = (raw ?? '').trim();
  if (!cookie) return { ok: false, code: 'EMPTY' };
  const jar: Record<string, string> = {};
  for (const part of cookie.split(';')) {
    const i = part.indexOf('=');
    if (i > 0) jar[part.slice(0, i).trim()] = part.slice(i + 1).trim();
  }
  if (!jar.SAPISID && !jar['__Secure-3PAPISID'] && !jar['__Secure-1PAPISID']) {
    return { ok: false, code: 'NO_SAPISID' };
  }
  const name = await probeAccountName(cookie);
  if (name === null) {
    // the session did not answer — reject so the user immediately knows
    return { ok: false, code: 'SAVE_FAILED' };
  }
  try {
    await mkdir(dirname(COOKIE_FILE), { recursive: true });
    await writeFile(COOKIE_FILE, cookie, 'utf-8');
    fileCookie = cookie;
  } catch {
    return { ok: false, code: 'SAVE_FAILED' };
  }
  return { ok: true, name: name || undefined };
}

export async function clearYtmCookie(): Promise<void> {
  fileCookie = null;
  try { await rm(COOKIE_FILE, { force: true }); } catch { /* already gone */ }
}

/** Ask account_menu who this session belongs to. null = session rejected. */
export async function probeAccountName(cookie: string): Promise<string | null> {
  const sapisid = sapisidOf(cookie);
  if (!sapisid) return null;
  try {
    const r = await fetch(`${YTM_ORIGIN}/youtubei/v1/account/account_menu?prettyPrint=false`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        origin: YTM_ORIGIN,
        'x-origin': YTM_ORIGIN,
        'x-goog-authuser': '0',
        authorization: sapisidHash(sapisid, YTM_ORIGIN),
        cookie,
      },
      body: JSON.stringify({
        context: {
          client: { clientName: 'WEB_REMIX', clientVersion: '1.20250310.01.00', hl: 'en', gl: 'US' },
          request: {},
          user: { lockedSafetyMode: false },
        },
      }),
      signal: AbortSignal.timeout(12000),
    });
    if (!r.ok) return null;
    const walk = (node: unknown, depth = 0): string | null => {
      if (depth > 9) return null;
      if (node && typeof node === 'object') {
        const o = node as Record<string, unknown>;
        // accountItemRenderer.accountName.runs[0].text (2025+ shape)
        if (o.accountName && Array.isArray((o.accountName as any)?.runs)) {
          const t = (o.accountName as any).runs[0]?.text;
          if (typeof t === 'string' && t) return t;
        }
        for (const v of Object.values(o)) {
          const hit = walk(v, depth + 1);
          if (hit) return hit;
        }
      }
      if (Array.isArray(node)) {
        for (const v of node) {
          const hit = walk(v, depth + 1);
          if (hit) return hit;
        }
      }
      return null;
    };
    const data: any = await r.json();
    // responses without a session still return 200 with an empty menu
    const txt = walk(data);
    return txt; // may be null → treated as rejected session
  } catch {
    return null;
  }
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
  const cookie = effectiveYtmCookie();
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

/** Sync signed-in check (env + cached file cookie) — used by extraction hot path. */
export function ytmSignedIn(): boolean {
  const c = effectiveYtmCookie();
  return !!c && !!sapisidOf(c);
}

/** Async signed-in check that also reads the cookie file on first call. */
export async function ytmSignedInDeep(): Promise<boolean> {
  await readCookieFile();
  return ytmSignedIn();
}
