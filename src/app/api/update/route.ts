import { NextResponse } from 'next/server';
import { APP_VERSION } from '@/lib/appInfo';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const REPO = 'jixa11/JixOne';

/** '1.10.0' > '1.9.2' → 1 ; equal → 0 ; smaller → -1 */
function cmpVersion(a: string, b: string): number {
  const pa = a.split('.').map((x) => parseInt(x, 10) || 0);
  const pb = b.split('.').map((x) => parseInt(x, 10) || 0);
  const n = Math.max(pa.length, pb.length);
  for (let i = 0; i < n; i++) {
    const d = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (d !== 0) return d > 0 ? 1 : -1;
  }
  return 0;
}

/** Latest tag via the releases redirect — no API token, no rate limit. */
async function latestTagViaRedirect(): Promise<string | null> {
  try {
    const r = await fetch(`https://github.com/${REPO}/releases/latest`, {
      redirect: 'follow',
      headers: { 'user-agent': 'JixOne-UpdateCheck' },
      signal: AbortSignal.timeout(9000),
      cache: 'no-store',
    });
    const m = /\/releases\/tag\/v?([\d.]+)/.exec(r.url);
    return m ? m[1] : null;
  } catch {
    return null;
  }
}

export interface UpdateInfo {
  ok: boolean;
  current: string;
  latest?: string;
  hasUpdate?: boolean;
  apkUrl?: string;
  apkSizeMB?: number;
  releaseUrl?: string;
  publishedAt?: string;
  notes?: string;
  error?: string;
}

/**
 * GET /api/update — compare APP_VERSION against the latest GitHub release.
 *
 * The tag comes from the releases redirect (unlimited); the GitHub REST API
 * is only a best-effort enrichment for notes/asset size — it rate-limits to
 * 60 req/h per IP unauthenticated, so its failure must never fail the check.
 */
export async function GET(): Promise<NextResponse<UpdateInfo>> {
  // 1) latest tag — redirect trick (no rate limit)
  const tag = await latestTagViaRedirect();
  if (!tag) {
    return NextResponse.json({ ok: false, current: APP_VERSION, error: 'redirect' });
  }
  const hasUpdate = cmpVersion(tag, APP_VERSION) > 0;
  const releaseUrl = `https://github.com/${REPO}/releases/tag/v${tag}`;

  // 2) best-effort enrichment via the API (rate-limited → optional)
  let apkUrl = '/JixOne-latest.apk'; // same-origin stable path, refreshed by CI
  let apkSizeMB: number | undefined;
  let publishedAt: string | undefined;
  let notes: string | undefined;
  try {
    const r = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
      headers: {
        'user-agent': 'JixOne-UpdateCheck',
        accept: 'application/vnd.github+json',
      },
      signal: AbortSignal.timeout(4500),
      cache: 'no-store',
    } as RequestInit);
    if (r.ok) {
      const j: any = await r.json();
      const apkAsset = (j.assets ?? []).find((a: any) =>
        String(a.name ?? '').toLowerCase().endsWith('.apk') && a.state === 'uploaded',
      );
      if (apkAsset?.browser_download_url) apkUrl = apkAsset.browser_download_url;
      const size = Number(apkAsset?.size ?? 0);
      if (size > 0) apkSizeMB = Math.round((size / 1024 / 1024) * 10) / 10;
      publishedAt = j.published_at ?? undefined;
      notes = String(j.body ?? '').slice(0, 1500) || undefined;
    }
  } catch { /* enrichment is optional */ }

  return NextResponse.json({
    ok: true,
    current: APP_VERSION,
    latest: tag,
    hasUpdate,
    apkUrl,
    apkSizeMB,
    releaseUrl,
    publishedAt,
    notes,
  });
}
