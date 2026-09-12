'use client';
import { hasNativeBridge, nativeFetch } from './nativeFetch';

/**
 * Helper-server (fallback) base for the standalone APK.
 *
 * The device engine (direct YouTube via the native bridge) is always tried
 * first. When YouTube is unreachable from the phone's network (e.g. blocked
 * without VPN), search/playback retry through a JixOne web server that can
 * reach YouTube server-side — the same path the web app uses.
 *
 * Base URL resolution order:
 *   1. localStorage 'jixone-api-base'  (set in Settings → helper server)
 *   2. AndroidBridge.getServerUrl()    (advanced custom-server pref)
 *   3. DEFAULT_API_BASE                (baked at build time; '' = disabled)
 */
export const API_BASE_KEY = 'jixone-api-base';
export const DEFAULT_API_BASE = '';

function stripTrailing(u: string): string {
  return u.trim().replace(/\/+$/, '');
}

export function getApiBase(): string {
  if (typeof window === 'undefined') return '';
  try {
    const ls = window.localStorage.getItem(API_BASE_KEY);
    if (ls && stripTrailing(ls)) return stripTrailing(ls);
  } catch { /* storage unavailable */ }
  try {
    const pref = (window as any).AndroidBridge?.getServerUrl?.() ?? '';
    if (pref && stripTrailing(pref)) return stripTrailing(pref);
  } catch { /* no bridge */ }
  return DEFAULT_API_BASE;
}

export function setApiBase(url: string): void {
  try {
    const u = stripTrailing(url);
    if (u) window.localStorage.setItem(API_BASE_KEY, u);
    else window.localStorage.removeItem(API_BASE_KEY);
  } catch { /* storage unavailable */ }
}

/** fetch with a guaranteed absolute URL inside the APK (bridge = no CORS);
 *  in the plain browser it is a same-origin relative fetch. */
export async function apiFetch(path: string, init?: RequestInit, timeoutMs = 20000): Promise<Response> {
  const base = getApiBase();
  // APK without a helper server: a relative URL would hit the asset loader (404)
  if (!base && hasNativeBridge()) throw new Error('NO_HELPER_SERVER');
  const url = base ? base + path : path;
  const p = hasNativeBridge() ? nativeFetch(url, init) : fetch(url, init);
  if (!timeoutMs) return p;
  return Promise.race([
    p,
    new Promise<Response>((_, rej) => setTimeout(() => rej(new Error('timeout')), timeoutMs)),
  ]);
}
