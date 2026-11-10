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
 * can also save it at runtime (stored under .data/, gitignored).
 *
 * 2026-10: a signed-in WEB_REMIX player call additionally wants the session's
 * visitorData + dataSyncId (window.yt.config_ on music.youtube.com) — the
 * OuterTune recipe. The bundle is saved as JSON; a plain cookie string is
 * still accepted (visitorData is then fetched fresh, dataSyncId absent).
 */

export const YTM_ORIGIN = 'https://music.youtube.com';

/** Runtime state must live OUTSIDE .next — a rebuild wipes the standalone
 *  tree and would take the saved session with it. JIXONE_DATA_DIR pins it. */
const DATA_DIR = process.env.JIXONE_DATA_DIR || join(process.cwd(), '.data');
const SESSION_FILE = join(DATA_DIR, 'ytm-session.json');
const LEGACY_COOKIE_FILE = join(DATA_DIR, 'ytm-cookie.txt');

export interface YTMSessionBundle {
  cookie: string;
  visitorData?: string;
  dataSyncId?: string;
  savedAt?: string;
}

let fileBundle: YTMSessionBundle | null | undefined;

function parseCookieString(cookie: string): Record<string, string> {
  const jar: Record<string, string> = {};
  for (const part of cookie.split(';')) {
    const i = part.indexOf('=');
    if (i > 0) jar[part.slice(0, i).trim()] = part.slice(i + 1).trim();
  }
  return jar;
}

async function readSessionFile(): Promise<YTMSessionBundle | null> {
  if (fileBundle !== undefined) return fileBundle;
  let raw = '';
  try {
    raw = (await readFile(SESSION_FILE, 'utf-8')).trim();
  } catch {
    try {
      // legacy single-cookie storage (v1.5.0)
      const legacy = (await readFile(LEGACY_COOKIE_FILE, 'utf-8')).trim();
      fileBundle = legacy ? { cookie: legacy } : null;
      return fileBundle;
    } catch {
      fileBundle = null;
      return null;
    }
  }
  try {
    if (raw.startsWith('{')) {
      const j = JSON.parse(raw);
      fileBundle = j?.cookie ? { cookie: String(j.cookie), visitorData: j.visitorData || undefined, dataSyncId: j.dataSyncId || undefined, savedAt: j.savedAt } : null;
    } else {
      fileBundle = raw ? { cookie: raw } : null;
    }
  } catch {
    fileBundle = null;
  }
  return fileBundle;
}

/** Accepts a plain cookie string OR a JSON bundle string. */
function coerceInput(raw: string): YTMSessionBundle | null {
  const s = (raw ?? '').trim();
  if (!s) return null;
  if (s.startsWith('{')) {
    try {
      const j = JSON.parse(s);
      if (j?.cookie) return { cookie: String(j.cookie), visitorData: j.visitorData || undefined, dataSyncId: j.dataSyncId || undefined };
    } catch { /* fall through */ }
  }
  return { cookie: s };
}

export function ytmCookie(): string | null {
  return process.env.YTM_COOKIE?.trim() || null;
}

async function ytmBundleAsync(): Promise<YTMSessionBundle | null> {
  if (process.env.YTM_COOKIE?.trim()) {
    return coerceInput(process.env.YTM_COOKIE);
  }
  return readSessionFile();
}

/** Cookie used by the request path (sync hot path: env wins, file bundle is
 *  read once and cached by setYtmCookie / clearYtmCookie). */
export function effectiveYtmCookie(): string | null {
  if (process.env.YTM_COOKIE?.trim()) return process.env.YTM_COOKIE.trim();
  return fileBundle?.cookie ?? null;
}

/** visitorData of the stored session (empty → fetch fresh per request). */
export function ytmSessionVisitorData(): string | null {
  return fileBundle?.visitorData ?? null;
}

/** dataSyncId of the stored session — the signed-in poToken identifier. */
export function ytmSessionDataSyncId(): string | null {
  return fileBundle?.dataSyncId ?? null;
}

export interface CookieSaveResult {
  ok: boolean;
  code?: 'EMPTY' | 'NO_SAPISID' | 'SAVE_FAILED';
  name?: string;
}

/** Validate + persist a pasted cookie (or JSON bundle). Returns the account
 *  name when the session actually answers for us. */
export async function setYtmCookie(raw: string): Promise<CookieSaveResult> {
  const bundle = coerceInput(raw);
  if (!bundle) return { ok: false, code: 'EMPTY' };
  const jar = parseCookieString(bundle.cookie);
  if (!jar.SAPISID && !jar['__Secure-3PAPISID'] && !jar['__Secure-1PAPISID']) {
    return { ok: false, code: 'NO_SAPISID' };
  }
  const name = await probeAccountName(bundle.cookie);
  if (name === null) {
    // the session did not answer — reject so the user immediately knows
    return { ok: false, code: 'SAVE_FAILED' };
  }
  try {
    await mkdir(dirname(SESSION_FILE), { recursive: true });
    await writeFile(SESSION_FILE, JSON.stringify({ ...bundle, savedAt: new Date().toISOString() }, null, 2), 'utf-8');
    await rm(LEGACY_COOKIE_FILE, { force: true }).catch(() => {});
    fileBundle = { ...bundle };
  } catch {
    return { ok: false, code: 'SAVE_FAILED' };
  }
  return { ok: true, name: name || undefined };
}

export async function clearYtmCookie(): Promise<void> {
  fileBundle = null;
  try { await rm(SESSION_FILE, { force: true }); } catch { /* already gone */ }
  try { await rm(LEGACY_COOKIE_FILE, { force: true }); } catch { /* already gone */ }
}

/** Ask account_menu who this session belongs to. null = session rejected. */
export async function probeAccountName(cookie: string): Promise<string | null> {
  const jar = parseCookieString(cookie);
  // OuterTune parity: only the first-party SAPISID produces a valid hash;
  // without it the cookie travels alone (no Authorization header at all)
  const sapisid1p = jar.SAPISID ?? null;
  try {
    const r = await fetch(`${YTM_ORIGIN}/youtubei/v1/account/account_menu?prettyPrint=false`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0',
        origin: YTM_ORIGIN,
        'x-origin': YTM_ORIGIN,
        'x-goog-authuser': '0',
        ...(sapisid1p ? { authorization: sapisidHash(sapisid1p, YTM_ORIGIN) } : {}),
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
  const jar = parseCookieString(cookie);
  return jar.SAPISID ?? jar['__Secure-3PAPISID'] ?? jar['__Secure-1PAPISID'] ?? null;
}

/** `SAPISIDHASH <unix>_<sha1(unix + " " + SAPISID + " " + origin)>` */
function sapisidHash(sapisid: string, origin: string): string {
  const ts = Math.floor(Date.now() / 1000);
  const hash = createHash('sha1').update(`${ts} ${sapisid} ${origin}`).digest('hex');
  return `SAPISIDHASH ${ts}_${hash}`;
}

/** Auth headers for innertube — ONLY for loginSupported clients (WEB_REMIX).
 *  Sending web cookies to app clients (ANDROID_VR etc.) answers HTTP 400 and
 *  risks the whole session being invalidated. OuterTune gates the same way. */
export function ytmAuthHeaders(): Record<string, string> {
  const cookie = effectiveYtmCookie();
  if (!cookie) return {};
  const jar = parseCookieString(cookie);
  const out: Record<string, string> = {
    cookie,
    'x-goog-authuser': '0',
    origin: YTM_ORIGIN,
    'x-origin': YTM_ORIGIN,
    referer: `${YTM_ORIGIN}/`,
  };
  // OuterTune: hash ONLY from the first-party SAPISID; 1P/3P variants appended.
  // Without SAPISID the cookie travels alone.
  if (jar.SAPISID) {
    const ts = Math.floor(Date.now() / 1000);
    const hash = createHash('sha1').update(`${ts} ${jar.SAPISID} ${YTM_ORIGIN}`).digest('hex');
    out.authorization = `SAPISIDHASH ${ts}_${hash} SAPISID1PHASH ${ts}_${hash} SAPISID3PHASH ${ts}_${hash}`;
  }
  return out;
}

/** Sync signed-in check (env + cached file bundle) — used by extraction hot path. */
export function ytmSignedIn(): boolean {
  const c = effectiveYtmCookie();
  return !!c && !!sapisidOf(c);
}

/** Async signed-in check that also reads the session file on first call. */
export async function ytmSignedInDeep(): Promise<boolean> {
  await readSessionFile();
  return ytmSignedIn();
}

/** Async bundle access for the extraction engine. */
export async function ytmBundle(): Promise<YTMSessionBundle | null> {
  return ytmBundleAsync();
}
