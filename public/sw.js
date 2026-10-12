/* JixOne service worker — offline shell + static asset caching.
 *
 * Deliberately conservative:
 *  - /api/*, /jixstream/* and *.apk are live data — NEVER intercepted.
 *  - /_next/static is content-hashed — cached forever, safe.
 *  - Navigations are network-first so the user always gets fresh HTML;
 *    the cached shell is only the offline fallback.
 */
const VERSION = 'jixone-v1.5.2';
const SHELL_CACHE = `${VERSION}-shell`;
const STATIC_CACHE = `${VERSION}-static`;

const SHELL_ASSETS = ['/', '/manifest.webmanifest', '/icons/icon-192.png', '/icons/icon-512.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(
    (async () => {
      const c = await caches.open(SHELL_CACHE);
      // allSettled: one 404 must not roll back the whole install
      await Promise.allSettled(SHELL_ASSETS.map((a) => c.add(a)));
      await self.skipWaiting();
    })()
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(keys.filter((k) => !k.startsWith(VERSION)).map((k) => caches.delete(k)));
      await self.clients.claim();
    })()
  );
});

const NEVER = [/^\/api\//, /^\/jixstream\//, /\.apk$/i];

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  if (NEVER.some((re) => re.test(url.pathname))) return;

  // Navigations: network-first, cached shell as the offline fallback.
  if (req.mode === 'navigate') {
    e.respondWith(
      (async () => {
        try {
          const fresh = await fetch(req);
          const c = await caches.open(SHELL_CACHE);
          c.put('/', fresh.clone()).catch(() => {});
          return fresh;
        } catch {
          const c = await caches.open(SHELL_CACHE);
          return (
            (await c.match('/')) ||
            (await c.match('/index.html')) ||
            Response.error()
          );
        }
      })()
    );
    return;
  }

  // Immutable assets: hashed chunks, theme artwork, icons — cache-first.
  const cacheable =
    url.pathname.startsWith('/_next/static/') ||
    url.pathname.startsWith('/themes/') ||
    url.pathname.startsWith('/icons/');
  if (!cacheable) return;

  e.respondWith(
    (async () => {
      const c = await caches.open(STATIC_CACHE);
      const hit = await c.match(req);
      if (hit) return hit;
      try {
        const fresh = await fetch(req);
        if (fresh.ok) c.put(req, fresh.clone()).catch(() => {});
        return fresh;
      } catch {
        return Response.error();
      }
    })()
  );
});
