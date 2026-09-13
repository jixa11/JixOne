'use client';

/**
 * YouTube Music session as seen from the web layer.
 *
 * The cookie itself never reaches JS — the native side captures it during
 * sign-in (LoginActivity) and attaches it to every innertube call. This module
 * only reflects the state so Settings can show it and drive the login screen.
 */

export interface YTMSession {
  /** the running build can sign in at all (Android app with a new enough bridge) */
  available: boolean;
  signedIn: boolean;
  name: string;
}

function bridge(): any {
  return typeof window === 'undefined' ? undefined : (window as any).AndroidBridge;
}

export function ytmSession(): YTMSession {
  const b = bridge();
  if (typeof b?.ytmAccount !== 'function') return { available: false, signedIn: false, name: '' };
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
  bridge()?.ytmLogout?.();
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
