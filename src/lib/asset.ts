'use client';

/**
 * URL for a file in public/.
 *
 * The APK build is a static export served under basePath "/assets/web". Next
 * rewrites its own asset URLs to match, but not paths written by hand and not
 * `url()` inside CSS — so theme artwork would 404 inside the app while working
 * fine on the web, which is exactly the kind of bug that only shows on device.
 *
 * The prefix is read back off one of Next's own tags rather than from build
 * config, so it is correct wherever the page is actually served from.
 */

let cached: string | null = null;

function basePath(): string {
  if (cached !== null) return cached;
  if (typeof document === 'undefined') return '';

  const el = document.querySelector('script[src*="/_next/"], link[href*="/_next/"]');
  const raw = el?.getAttribute('src') ?? el?.getAttribute('href') ?? '';
  const marker = raw.indexOf('/_next/');
  let base = marker > 0 ? raw.slice(0, marker) : '';

  if (/^https?:/i.test(base)) {
    try { base = new URL(base).pathname; } catch { base = ''; }
  }
  cached = base.replace(/\/$/, '');
  return cached;
}

export function assetUrl(path: string): string {
  return `${basePath()}${path}`;
}
