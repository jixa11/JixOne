'use client';

/**
 * YouTube Music session as seen from the web layer.
 *
 * Two worlds:
 *  - APK: the cookie never reaches JS — the native side captures it during
 *    sign-in (LoginActivity) and attaches it to every innertube call.
 *  - Plain web: the helper server stores the session (Settings → paste
 *    cookie → POST /api/yt/auth) and uses it for server-side extraction.
 * This module presents both through one interface.
 */

export interface YTMSession {
  /** the running build can sign in at all (Android app with a new enough bridge, or web with a helper server) */
  available: boolean;
  signedIn: boolean;
  name: string;
}

function bridge(): any {
  return typeof window === 'undefined' ? undefined : (window as any).AndroidBridge;
}

/** true when running inside the APK (native sign-in available) */
export function hasNativeAuth(): boolean {
  return typeof bridge()?.ytmAccount === 'function';
}

let webCache: YTMSession | null = null;

export async function ytmSessionWeb(): Promise<YTMSession> {
  if (typeof window === 'undefined') return { available: false, signedIn: false, name: '' };
  try {
    const r = await fetch('/api/yt/auth', { cache: 'no-store' });
    const d = await r.json();
    if (d?.ok) {
      webCache = { available: true, signedIn: !!d.signedIn, name: d.name ?? '' };
      return webCache;
    }
  } catch { /* helper server without the route */ }
  return webCache ?? { available: true, signedIn: false, name: '' };
}

export function ytmSession(): YTMSession {
  const b = bridge();
  if (typeof b?.ytmAccount !== 'function') {
    // web: last known state (refreshed by ytmSessionWeb) or optimistic default
    return webCache ?? { available: true, signedIn: false, name: '' };
  }
  try {
    const d = JSON.parse(b.ytmAccount());
    return { available: true, signedIn: !!d?.signedIn, name: d?.name ?? '' };
  } catch {
    return { available: true, signedIn: false, name: '' };
  }
}

export function ytmLogin(): void {
  bridge()?.ytmLogin?.();
}

export function ytmLogout(): void {
  const b = bridge();
  if (typeof b?.ytmLogout === 'function') b.ytmLogout();
  else if (typeof window !== 'undefined') fetch('/api/yt/auth', { method: 'DELETE' }).catch(() => { /* offline */ });
}

/** true when the build accepts a hand-pasted cookie (APK bridge or web) */
export function ytmCanPasteCookie(): boolean {
  if (typeof window === 'undefined') return false;
  return typeof bridge()?.ytmSetCookie === 'function' || !bridge();
}

export type PasteResult =
  | { ok: true; name?: string }
  | { ok: false; code: 'EMPTY' | 'NO_SAPISID' | 'SAVE_FAILED' | 'UNSUPPORTED' | 'SERVER_REJECTED' };

/**
 * Sign in from a Cookie header copied out of a desktop browser. Works both in
 * the APK (native storage) and on the web (helper server storage).
 */
export async function ytmSetCookie(raw: string): Promise<PasteResult> {
  const b = bridge();
  if (typeof b?.ytmSetCookie === 'function') {
    try {
      const d = JSON.parse(b.ytmSetCookie(raw));
      return d?.ok ? { ok: true, name: d.name } : { ok: false, code: d?.code ?? 'EMPTY' };
    } catch {
      return { ok: false, code: 'EMPTY' };
    }
  }
  if (!b) {
    try {
      const r = await fetch('/api/yt/auth', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ cookie: raw }),
      });
      const d = await r.json();
      if (d?.ok) {
        webCache = { available: true, signedIn: true, name: d.name ?? '' };
        return { ok: true, name: d.name };
      }
      return { ok: false, code: d?.code === 'NO_SAPISID' ? 'NO_SAPISID' : d?.code === 'SAVE_FAILED' ? 'SERVER_REJECTED' : 'EMPTY' };
    } catch {
      return { ok: false, code: 'SAVE_FAILED' };
    }
  }
  return { ok: false, code: 'UNSUPPORTED' };
}

/** Native fires `window.__ytmAuthChanged` once sign-in finishes. */
export function onYTMAuthChange(cb: () => void): () => void {
  if (typeof window === 'undefined') return () => {};
  const w = window as any;
  const prev = w.__ytmAuthChanged;
  w.__ytmAuthChanged = () => { prev?.(); cb(); };
  // returning from the login screen also resumes this page
  const onVisible = () => { if (document.visibilityState === 'visible') cb(); };
  document.addEventListener('visibilitychange', onVisible);
  return () => {
    w.__ytmAuthChanged = prev;
    document.removeEventListener('visibilitychange', onVisible);
  };
}
