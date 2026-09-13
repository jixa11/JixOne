'use client';

/** OPFS-backed persistent audio storage (works in Chrome WebView / modern browsers) */

function extForItag(itag: number): string {
  if (itag === 140 || itag === 139) return 'm4a';
  return 'weba';
}
const nameOf = (videoId: string, itag: number) => `${videoId}.${extForItag(itag)}`;

async function root(): Promise<FileSystemDirectoryHandle> {
  return navigator.storage.getDirectory();
}

/** googlevideo stops sending partway through an un-ranged request — a 3.4 MB
 *  track arrived about 55% complete and the write failed on the length
 *  mismatch. Playback never showed it because <audio> issues its own ranges.
 *  Downloads therefore walk the file in explicit byte ranges. */
const CHUNK = 2 * 1024 * 1024;

export async function downloadToOPFS(
  videoId: string,
  itag: number,
  url: string,
  onProgress: (got: number, total: number) => void,
  signal?: AbortSignal
): Promise<{ size: number }> {
  const dir = await root();
  const fh = await dir.getFileHandle(nameOf(videoId, itag), { create: true });
  const writable = await (fh as any).createWritable();
  try {
    let got = 0;
    let total = 0;

    for (;;) {
      const stop = total ? Math.min(got + CHUNK - 1, total - 1) : got + CHUNK - 1;
      const res = await fetch(url, { signal, headers: { range: `bytes=${got}-${stop}` } });
      if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`);

      if (!total) {
        // content-range carries the true total; a server that ignored the range
        // answers 200 and content-length is the whole file instead
        const declared = res.headers.get('content-range')?.split('/')[1];
        total = declared && declared !== '*'
          ? Number(declared)
          : Number(res.headers.get('content-length') ?? 0);
      }

      const reader = res.body.getReader();
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;
        await writable.write(value);
        got += value.byteLength;
        if (total) onProgress(got, total);
      }

      if (!total || got >= total) break;
    }

    await writable.close();
    return { size: got };
  } catch (e) {
    try { await writable.abort?.(); } catch { /* noop */ }
    try { await dir.removeEntry(nameOf(videoId, itag)); } catch { /* noop */ }
    throw e;
  }
}

export async function getLocalFileURL(videoId: string): Promise<{ url: string; size: number } | null> {
  try {
    const dir = await root();
    for (const ext of ['weba', 'm4a']) {
      try {
        const fh = await dir.getFileHandle(`${videoId}.${ext}`);
        const file = await fh.getFile();
        if (file.size > 0) return { url: URL.createObjectURL(file), size: file.size };
      } catch { /* try next */ }
    }
    return null;
  } catch {
    return null;
  }
}

export async function deleteLocalFile(videoId: string): Promise<void> {
  try {
    const dir = await root();
    await dir.removeEntry(`${videoId}.weba`).catch(() => {});
    await dir.removeEntry(`${videoId}.m4a`).catch(() => {});
  } catch { /* noop */ }
}

export async function storageUsage(): Promise<{ usage: number; quota: number }> {
  try {
    const est = await navigator.storage.estimate();
    return { usage: est.usage ?? 0, quota: est.quota ?? 0 };
  } catch {
    return { usage: 0, quota: 0 };
  }
}
