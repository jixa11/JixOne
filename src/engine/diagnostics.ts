'use client';
import { hasNativeBridge, nativeFetch } from '@/engine/nativeFetch';
import { searchTracks } from '@/engine/ytclient';
import { getApiBase } from '@/engine/apiBase';

export type DiagState = 'run' | 'pass' | 'fail';

export interface DiagStep {
  id: 'net' | 'yt' | 'search';
  state: DiagState;
  detail?: string;
}

export interface DiagReport {
  steps: DiagStep[];
  mode: 'device' | 'server';
  verdictFa: string;
  verdictEn: string;
}

const INNERTUBE_PROBE_BODY = JSON.stringify({
  context: { client: { clientName: 'WEB', clientVersion: '2.20240101.00.00', hl: 'en', gl: 'US' } },
  query: 'a',
});

function withTimeout<T>(p: Promise<T>, ms: number, label = 'timeout'): Promise<T> {
  return Promise.race([
    p,
    new Promise<T>((_, rej) => setTimeout(() => rej(new Error(label)), ms)),
  ]);
}

export async function runDiagnostics(onUpdate: (steps: DiagStep[]) => void): Promise<DiagReport> {
  const steps: DiagStep[] = [];
  const mode: 'device' | 'server' = hasNativeBridge() ? 'device' : 'server';
  const push = (s: DiagStep) => { steps.push(s); onUpdate([...steps]); };

  // 1 — generic internet
  push({ id: 'net', state: 'run' });
  try {
    await withTimeout(
      fetch('https://www.google.com/generate_204', { mode: 'no-cors', cache: 'no-store' }),
      8000,
      'timeout'
    );
    steps[0] = { id: 'net', state: 'pass' };
  } catch (e: any) {
    steps[0] = { id: 'net', state: 'fail', detail: e?.message ?? 'offline' };
    onUpdate([...steps]);
    return {
      steps,
      mode,
      verdictFa: 'اینترنت گوشی وصل نیست — وای‌فای یا دیتا را چک کن.',
      verdictEn: 'No internet connection — check Wi-Fi or mobile data.',
    };
  }
  onUpdate([...steps]);

  // 2 — YouTube reachability
  push({ id: 'yt', state: 'run' });
  try {
    if (mode === 'device') {
      const res = await withTimeout(
        nativeFetch(
          'https://youtubei.googleapis.com/youtubei/v1/search?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&prettyPrint=false',
          { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: INNERTUBE_PROBE_BODY }
        ),
        15000,
        'timeout'
      );
      // any HTTP answer means the network path exists; 200 = fully healthy
      steps[1] = { id: 'yt', state: 'pass', detail: `HTTP ${res.status}` };
    } else {
      const r = await withTimeout(fetch('/api/yt/search?q=a', { cache: 'no-store' }), 20000, 'timeout');
      const d = await r.json().catch(() => null);
      steps[1] = d?.ok ? { id: 'yt', state: 'pass', detail: 'server ok' } : { id: 'yt', state: 'fail', detail: 'server error' };
    }
  } catch (e: any) {
    steps[1] = { id: 'yt', state: 'fail', detail: e?.message ?? 'blocked' };
  }
  onUpdate([...steps]);

  if (steps[1].state === 'fail') {
    const helper = hasNativeBridge() ? getApiBase() : '';
    if (helper) {
      return {
        steps,
        mode,
        verdictFa: 'یوتیوب مستقیم از شبکه‌ی گوشی در دسترس نیست، ولی سرور کمکی تنظیم شده — جستجو از طریق سرور انجام می‌شود و باید کار کند.',
        verdictEn: 'YouTube is not directly reachable from this network, but a helper server is configured — search runs through it and should work.',
      };
    }
    return {
      steps,
      mode,
      verdictFa:
        mode === 'device'
          ? 'یوتیوب از شبکه‌ی تو در دسترس نیست. اگه فیلترشکن داری روشنش کن و تست را دوباره بگیر — مثل وقتی که یوتیوب را توی مرورگر باز می‌کنی. یا از تنظیمات، «سرور کمکی جستجو» را وارد کن.'
          : 'سرور به یوتیوب دسترسی ندارد — تنظیمات سرور را چک کن.',
      verdictEn:
        mode === 'device'
          ? 'YouTube is unreachable from your network. If you use a VPN, turn it ON and run this test again — the app needs the same access as opening youtube.com in a browser. Or set a "Search helper server" in Settings.'
          : 'The server cannot reach YouTube — check the server settings.',
    };
  }

  // 3 — real search through the real engine
  push({ id: 'search', state: 'run' });
  try {
    const tracks = await withTimeout(searchTracks('test'), 30000, 'timeout');
    steps[2] = { id: 'search', state: tracks.length > 0 ? 'pass' : 'fail', detail: `${tracks.length}` };
  } catch (e: any) {
    steps[2] = { id: 'search', state: 'fail', detail: e?.message ?? 'error' };
  }
  onUpdate([...steps]);

  const allPass = steps.every((s) => s.state === 'pass');
  return {
    steps,
    mode,
    verdictFa: allPass
      ? 'همه‌چیز برقراره! جستجو باید کار کنه. اگه توی صفحه‌ی جستجو بازم خالی بود، یک بار اپ را ببند و باز کن.'
      : 'اتصال برقراره ولی جستجو خطا داد — مشکل فنی است؛ گزارش را کپی کن و برایم بفرست.',
    verdictEn: allPass
      ? 'Everything works! Search should be functional now.'
      : 'Connection is fine but search errored — technical issue; please copy the report and send it.',
  };
}

export function diagReportText(steps: DiagStep[], mode: string, verdict: string): string {
  const lines = steps.map((s) => `${s.id}: ${s.state}${s.detail ? ` (${s.detail})` : ''}`);
  return `JixOne diagnostics [${mode}]\n${lines.join('\n')}\nverdict: ${verdict}`;
}
