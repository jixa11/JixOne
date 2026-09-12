'use client';
import { useDownloads } from '@/store/downloads';
import { useSettings } from '@/store/settings';
import { extractStream } from '@/engine/extractor';
import { downloadToOPFS } from '@/engine/downloader';

let started = false;
let processing = false;

/** Sequential download queue processor (device-side extraction + OPFS write) */
export function startDownloadRunner() {
  if (started) return;
  started = true;

  // on boot: resume interrupted items
  const st = useDownloads.getState();
  Object.values(st.items).forEach((it) => {
    if (it.status === 'downloading' || it.status === 'extracting') st.update(it.videoId, { status: 'pending', progress: 0 });
  });

  window.addEventListener('online', () => tick());
  window.addEventListener('offline', () => {
    const s = useDownloads.getState();
    Object.values(s.items).forEach((it) => {
      if (it.status === 'pending' || it.status === 'extracting' || it.status === 'downloading') s.update(it.videoId, { status: 'waiting-net' });
    });
  });
  setInterval(tick, 4000);
  setTimeout(tick, 800);
}

async function tick() {
  if (processing) return;
  if (typeof navigator !== 'undefined' && !navigator.onLine) return;
  const s = useDownloads.getState();
  const queue = Object.values(s.items).filter((i) => i.status === 'pending' || i.status === 'waiting-net');
  if (!queue.length) return;
  processing = true;
  try {
    for (const item of queue) {
      const cur = useDownloads.getState().items[item.videoId];
      if (!cur || (cur.status !== 'pending' && cur.status !== 'waiting-net')) continue;
      await processOne(cur.videoId);
    }
  } finally {
    processing = false;
  }
}

async function processOne(videoId: string) {
  const d = useDownloads.getState();
  const q = useSettings.getState().downloadQuality;
  const item = d.items[videoId];
  if (!item) return;
  d.update(videoId, { status: 'extracting', progress: 0 });
  try {
    const stream = await extractStream(videoId, q);
    d.update(videoId, { status: 'downloading', sizeBytes: stream.size });
    const { size } = await downloadToOPFS(videoId, stream.itag, stream.url, (got, total) => {
      useDownloads.getState().update(videoId, { progress: total ? got / total : 0, sizeBytes: total || got });
    });
    useDownloads.getState().update(videoId, { status: 'done', progress: 1, sizeBytes: size });
  } catch (e: any) {
    const msg = e?.message === 'NO_STREAM' ? 'no stream' : String(e?.message ?? e);
    useDownloads.getState().update(videoId, { status: 'error', error: msg });
  }
}

export function enqueueDownload(videoId: string, meta: { title: string; artist: string; thumb: string; durationSec: number }) {
  const d = useDownloads.getState();
  if (d.items[videoId]?.status === 'done') return;
  d.add({
    videoId,
    title: meta.title,
    artist: meta.artist,
    thumb: meta.thumb,
    durationSec: meta.durationSec,
    quality: useSettings.getState().downloadQuality,
    status: 'pending',
    progress: 0,
    addedAt: Date.now(),
  });
  setTimeout(tick, 100);
}

export function enqueueMany(tracks: { videoId: string; title: string; artist: string; thumb: string; durationSec?: number }[]) {
  tracks.forEach((t) => enqueueDownload(t.videoId, { title: t.title, artist: t.artist, thumb: t.thumb, durationSec: t.durationSec ?? 210 }));
}
