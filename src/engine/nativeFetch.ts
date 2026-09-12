'use client';

/** CORS-proof transport for the Android APK.
 *  YouTube rejects cross-origin (CORS) API calls from any non-Google web origin —
 *  so inside the WebView we route HTTP through the Kotlin bridge instead
 *  (native sockets have no CORS). The JS side gets a normal Response back. */

export function hasNativeBridge(): boolean {
  if (typeof window === 'undefined') return false;
  const b = (window as any).AndroidBridge;
  return typeof b?.nativeFetch === 'function';
}

function headersToObject(h: RequestInit['headers']): Record<string, string> {
  const out: Record<string, string> = {};
  if (!h) return out;
  if (h instanceof Headers) {
    h.forEach((v, k) => { out[k] = v; });
  } else if (Array.isArray(h)) {
    for (const [k, v] of h) out[k] = String(v);
  } else {
    for (const k of Object.keys(h as any)) out[k] = String((h as any)[k]);
  }
  return out;
}

export async function nativeFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const b = (window as any).AndroidBridge;
  if (typeof b?.nativeFetch !== 'function') throw new TypeError('native bridge unavailable');

  const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
  const method = (init?.method ?? 'GET').toUpperCase();

  const headers = headersToObject(init?.headers);
  // the native stack manages these itself; sending them can trigger anti-abuse
  delete headers['accept-encoding'];
  delete headers['content-length'];
  delete headers['host'];
  delete headers['origin'];
  delete headers['referer'];

  const rawBody = init?.body;
  const body =
    typeof rawBody === 'string'
      ? rawBody
      : rawBody instanceof URLSearchParams
        ? rawBody.toString()
        : rawBody == null
          ? null
          : String(rawBody);

  const raw: string = b.nativeFetch(url, method, JSON.stringify(headers), body);

  let env: any;
  try { env = JSON.parse(raw); } catch {
    throw new TypeError('native bridge: malformed envelope');
  }
  if (env?.error) throw new TypeError(`native fetch failed: ${env.error}`);

  const status = typeof env.status === 'number' ? env.status : 0;
  const resHeaders = new Headers(env.headers ?? {});
  const bodyText = status === 204 || status === 304 ? null : (env.body ?? '');
  return new Response(bodyText, { status, headers: resHeaders });
}
