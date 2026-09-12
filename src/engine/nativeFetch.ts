'use client';

/** CORS-proof transport for the Android APK.
 *  YouTube rejects cross-origin (CORS) API calls from any non-Google web origin —
 *  so inside the WebView we route HTTP through the Kotlin bridge instead
 *  (native sockets have no CORS). The JS side gets a normal Response back.
 *
 *  v2: the bridge call is ASYNC (callback based). The previous synchronous
 *  bridge blocked the WebView JS thread for the whole network round-trip,
 *  which froze the whole app on slow/blocked networks (reported as
 *  "اپ کنده"). Async keeps the UI responsive while innertube runs. */

export function hasNativeBridge(): boolean {
  if (typeof window === 'undefined') return false;
  const b = (window as any).AndroidBridge;
  return typeof b?.nativeFetch === 'function' || typeof b?.nativeFetch2 === 'function';
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

function sanitizeHeaders(h: Record<string, string>): Record<string, string> {
  // the native stack manages these itself; sending them can trigger anti-abuse
  delete h['accept-encoding'];
  delete h['content-length'];
  delete h['host'];
  delete h['origin'];
  delete h['referer'];
  return h;
}

function bodyToString(rawBody: RequestInit['body']): string | null {
  if (rawBody == null) return null;
  if (typeof rawBody === 'string') return rawBody;
  if (rawBody instanceof URLSearchParams) return rawBody.toString();
  return String(rawBody);
}

/** decode a base64 (UTF-8) payload coming from the Kotlin side */
function b64ToUtf8(b64: string): string {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new TextDecoder('utf-8').decode(bytes);
}

/** register the Kotlin→JS callback once */
function ensureCallback(): void {
  if (typeof window === 'undefined') return;
  const w = window as any;
  if (w.__nfDone) return;
  w.__nfPending = {} as Record<string, (raw: string) => void>;
  w.__nfDone = (id: string, b64: string) => {
    try {
      const raw = typeof b64 === 'string' && !b64.startsWith('{') ? b64ToUtf8(b64) : b64;
      w.__nfPending?.[id]?.(raw);
    } catch { /* dropped */ }
  };
}

/** async bridge call — never blocks the JS thread */
function callBridge(
  url: string,
  method: string,
  headersJson: string,
  body: string | null,
  timeoutMs = 35000,
): Promise<string> {
  ensureCallback();
  return new Promise((resolve, reject) => {
    const b = (window as any).AndroidBridge;
    if (typeof b?.nativeFetch2 === 'function') {
      const w = window as any;
      const id = Math.random().toString(36).slice(2) + Date.now().toString(36);
      const timer = setTimeout(() => {
        if (w.__nfPending[id]) { delete w.__nfPending[id]; reject(new TypeError('bridge timeout')); }
      }, timeoutMs);
      w.__nfPending[id] = (raw: string) => { clearTimeout(timer); delete w.__nfPending[id]; resolve(raw); };
      try {
        b.nativeFetch2(id, url, method, headersJson, body);
      } catch (e: any) {
        clearTimeout(timer);
        delete w.__nfPending[id];
        reject(new TypeError(`bridge call failed: ${e?.message ?? e}`));
      }
    } else if (typeof b?.nativeFetch === 'function') {
      // legacy synchronous bridge (older APK) — still works, just blocks
      try { resolve(b.nativeFetch(url, method, headersJson, body)); }
      catch (e: any) { reject(new TypeError(`bridge call failed: ${e?.message ?? e}`)); }
    } else {
      reject(new TypeError('native bridge unavailable'));
    }
  });
}

export async function nativeFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const b = (window as any).AndroidBridge;
  if (typeof b?.nativeFetch !== 'function' && typeof b?.nativeFetch2 !== 'function') {
    throw new TypeError('native bridge unavailable');
  }

  // youtubei.js passes a fully-built Request object as `input` (its HTTPClient
  // does `new Request(url, init)` then `customFetch(request, {body, headers…})`).
  // The second argument it hands us does NOT contain `method`, so we MUST read
  // method/body/headers from the Request itself or every innertube POST is
  // silently downgraded to a body-less GET (this was the real-device search bug).
  const req: Request | null = typeof input === 'object' && input !== null && !(input instanceof URL) && typeof (input as Request).url === 'string'
    ? (input as Request)
    : null;

  const url = typeof input === 'string' ? input : input instanceof URL ? input.href : (req as Request).url;
  const method = (init?.method ?? req?.method ?? 'GET').toUpperCase();

  const rawHeaders = init?.headers ?? req?.headers;
  const headers = sanitizeHeaders(headersToObject(rawHeaders));

  let body: string | null;
  if (init?.body !== undefined && init.body !== null) {
    body = bodyToString(init.body);
  } else if (req && req.method !== 'GET' && req.method !== 'HEAD') {
    // Request body is a stream — clone before the transport reads it
    try { body = await req.clone().text(); }
    catch { body = null; }
  } else {
    body = null;
  }

  const raw: string = await callBridge(url, method, JSON.stringify(headers), body);

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
