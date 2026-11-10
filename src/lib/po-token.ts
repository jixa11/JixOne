import 'server-only';
/**
 * PoToken minting — the bgutil-ytdlp-pot-provider flow (2026-08 patched) that
 * OuterTune / yt-dlp use, running server-side in Node via jsdom.
 *
 * Why: YouTube gates WEB-family player calls on a valid BotGuard poToken.
 * With a signed-in session the token is minted against the account's
 * dataSyncId (OuterTune: "signed in sessions use dataSyncId as identifier");
 * anonymous sessions use visitorData instead.
 *
 * Cost profile: minter creation ≈ 2–4 s once per process/session; each
 * player token ≈ tens of ms. Streaming token is minted once and reused on
 * googlevideo URLs (&pot=...).
 */

const REQUEST_KEY = 'O43z0dpjhgX20SCx4KAo';

interface MinterState {
  sessionId: string;
  minter: any;
  at: number;
}

let minterState: MinterState | null = null;
let streamingPot: { value: string; sessionId: string; at: number } | null = null;

const TTL = 6 * 60 * 60 * 1000; // integrity token lives ~12 h; refresh at 6 h

let globalsReady = false;
async function ensureGlobals() {
  if (globalsReady) return;
  const { JSDOM } = await import('jsdom');
  const { USER_AGENT } = await import('bgutils-js/utils');
  const dom = new JSDOM('<!DOCTYPE html><html lang="en"><head><title></title></head><body></body></html>', {
    url: 'https://www.youtube.com/',
    referrer: 'https://www.youtube.com/',
    resources: { userAgent: USER_AGENT },
  });
  Object.assign(globalThis, {
    window: dom.window,
    document: dom.window.document,
    location: dom.window.location,
    origin: dom.window.origin,
  });
  if (!Reflect.has(globalThis, 'navigator')) {
    Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator });
  }
  globalsReady = true;
}

async function createMinter(sessionId: string): Promise<any> {
  await ensureGlobals();
  const bgutils = await import('bgutils-js/utils');
  const botguard = await import('bgutils-js/botguard');
  const webpo = await import('bgutils-js/webpo');
  const { buildURL, getHeaders, USER_AGENT, parseLooseJSON } = bgutils as any;

  // 1) homepage challenge (ytcfg + ytAtN)
  const page = await fetch('https://www.youtube.com', {
    headers: { accept: '*/*', 'accept-language': 'en-US,en;q=0.7', 'user-agent': USER_AGENT },
    signal: AbortSignal.timeout(15000),
  });
  const html = await page.text();
  const ytcfgMatch = html.match(/ytcfg\.set\((\{[\s\S]+?\})\);/);
  if (ytcfgMatch) {
    const ytObj = { config_: JSON.parse(ytcfgMatch[1]) };
    (globalThis as any).yt = ytObj;
    (globalThis as any).window.yt = ytObj;
  }
  const attMatch = html.match(/window\.ytAtN\(\s*(\{[\s\S]*?\})\s*\)/);
  if (!attMatch) throw new Error('bg-challenge-missing');
  const attData = parseLooseJSON(attMatch[1]);
  const bgChallenge = attData?.R?.bgChallenge;
  if (!bgChallenge?.program || !bgChallenge?.interpreterUrl) throw new Error('bg-challenge-incomplete');

  // 2) interpreter
  const interpUrl = bgChallenge.interpreterUrl.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue;
  const interpResp = await fetch(`https:${interpUrl}`, { headers: { 'user-agent': USER_AGENT } });
  const interpreterJS = await interpResp.text();
  new Function(interpreterJS)();

  // 3) BotGuard snapshot
  const bgClient = await (botguard as any).BotGuardClient.create({
    program: bgChallenge.program,
    globalName: bgChallenge.globalName,
    globalObject: globalThis,
  });
  const webPoSignalOutput: any[] = [];
  const bgResponse = await bgClient.snapshot({ webPoSignalOutput });

  // 4) GenerateIT
  const itResp = await fetch(buildURL('GenerateIT'), {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify([REQUEST_KEY, bgResponse]),
    signal: AbortSignal.timeout(15000),
  });
  const [integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken] = await itResp.json();
  if (!integrityToken) throw new Error('generateit-failed');

  // 5) minter (bound to this session's signal output)
  const minter = await (webpo as any).WebPoMinter.create(
    { integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken },
    webPoSignalOutput,
  );
  // bind the session right away
  await minter.mintAsWebsafeString(sessionId);
  return minter;
}

async function getMinter(sessionId: string | null | undefined): Promise<any | null> {
  const sid = sessionId?.trim() || null;
  if (!sid) return null;
  if (minterState && minterState.sessionId === sid && Date.now() - minterState.at < TTL) {
    return minterState.minter;
  }
  try {
    const minter = await createMinter(sid);
    minterState = { sessionId: sid, minter, at: Date.now() };
    return minter;
  } catch {
    minterState = null;
    return null;
  }
}

/** poToken for a specific videoId's player request. Always freshly minted:
 *  YouTube returns the SAME googlevideo URL for a repeated (videoId, pot)
 *  pair, and pot-bound URLs carry a per-URL byte quota — a cached pot would
 *  keep handing out an exhausted URL. */
export async function getPlayerPoToken(videoId: string, sessionId: string | null | undefined): Promise<string | null> {
  const minter = await getMinter(sessionId);
  if (!minter) return null;
  try {
    return await minter.mintAsWebsafeString(videoId);
  } catch {
    return null;
  }
}

/** poToken appended to googlevideo stream URLs (one per session). */
export async function getStreamingPoToken(sessionId: string | null | undefined): Promise<string | null> {
  const sid = sessionId?.trim() || null;
  if (!sid) return null;
  if (streamingPot && streamingPot.sessionId === sid && Date.now() - streamingPot.at < TTL) {
    return streamingPot.value;
  }
  const minter = await getMinter(sid);
  if (!minter) return null;
  try {
    const value = await minter.mintAsWebsafeString(sid);
    streamingPot = { value, sessionId: sid, at: Date.now() };
    return value;
  } catch {
    return null;
  }
}
