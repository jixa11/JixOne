'use client';

/** tiny localStorage cache for instant UI (stale-while-revalidate) */

export function readCache<T>(key: string): T | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return null;
    const o = JSON.parse(raw);
    if (!o || typeof o !== 'object' || !('v' in o)) return null;
    return o.v as T;
  } catch {
    return null;
  }
}

export function writeCache(key: string, v: unknown): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(key, JSON.stringify({ at: Date.now(), v }));
  } catch {
    // storage full — drop oldest jixone cache entries and retry once
    try {
      const drop: string[] = [];
      for (let i = 0; i < window.localStorage.length; i++) {
        const k = window.localStorage.key(i);
        if (k && k.startsWith('np-cache:')) drop.push(k);
      }
      drop.slice(0, Math.max(1, drop.length >> 1)).forEach((k) => window.localStorage.removeItem(k));
      window.localStorage.setItem(key, JSON.stringify({ at: Date.now(), v }));
    } catch { /* give up silently */ }
  }
}

export function cacheKey(...parts: (string | number)[]): string {
  return `np-cache:${parts.join(':')}`;
}
