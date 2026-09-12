'use client';
import { useMemo, useState } from 'react';
import { useLibrary } from '@/store/library';
import { usePlayer } from '@/store/player';
import { useView } from '@/store/view';
import { useSettings } from '@/store/settings';
import { useDownloads } from '@/store/downloads';
import { TrackRow } from '@/components/TrackRow';
import { Track } from '@/lib/types';
import { t, num, fmtSize } from '@/lib/i18n';
import { estimateSize } from '@/lib/types';
import { enqueueMany } from '@/engine/downloadsRunner';
import { Play, Shuffle, MoreVertical, Trash2, Pencil, Download, Search, X, Check, ListMusic, ArrowDownToLine } from 'lucide-react';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Switch } from '@/components/ui/switch';
import { TrackThumb } from '@/components/TrackThumb';
import { Youtube } from 'lucide-react';

export default function PlaylistView({ id }: { id: string }) {
  const lang = useSettings((s) => s.lang);
  const dq = useSettings((s) => s.downloadQuality);
  const playlist = useLibrary((s) => s.playlists.find((p) => p.id === id));
  const rename = useLibrary((s) => s.renamePlaylist);
  const del = useLibrary((s) => s.deletePlaylist);
  const removeTrack = useLibrary((s) => s.removeFromPlaylist);
  const autoDl = useLibrary((s) => s.setAutoDownload);
  const doneCount = useDownloads((s) => Object.values(s.items).filter((i) => i.status === 'done' && playlist?.tracks.some((t2) => t2.videoId === i.videoId)).length);

  const [menu, setMenu] = useState(false);
  const [renaming, setRenaming] = useState(false);
  const [newName, setNewName] = useState('');
  const [confirmDl, setConfirmDl] = useState(false);
  const [adding, setAdding] = useState(false);

  const tracks = playlist?.tracks ?? [];
  const totalEst = useMemo(() => tracks.reduce((a, t2) => a + estimateSize(t2.durationSec ?? 210, dq), 0), [tracks, dq]);

  if (!playlist) return <div className="p-10 text-center text-dim">not found</div>;

  const coverTracks = tracks.slice(0, 4);

  return (
    <div className="view-in mx-auto max-w-[1000px] px-4 pb-8">
      {/* header */}
      <div className="mb-6 flex flex-col items-center gap-5 sm:flex-row sm:items-end">
        <div className="neon-ring relative grid h-44 w-44 shrink-0 place-items-center overflow-hidden rounded-2xl border bg-surface2 shadow-2xl">
          {coverTracks.length ? (
            <div className="grid h-full w-full grid-cols-2 grid-rows-2">
              {coverTracks.map((t2) => (
                <TrackThumb key={t2.videoId} track={t2} rounded="" className="h-full w-full" />
              ))}
            </div>
          ) : (
            <span className="grid h-full w-full place-items-center text-dim"><ListMusic size={40} /></span>
          )}
          {playlist.source === 'ytmusic' && (
            <span className="absolute bottom-2 end-2 grid h-7 w-7 place-items-center rounded-full bg-[#ff0033] text-white shadow-lg ring-2 ring-black/20">
              <Youtube size={14} strokeWidth={2.5} />
            </span>
          )}
        </div>
        <div className="min-w-0 flex-1 text-center sm:text-start">
          <div className="text-[11px] font-bold uppercase tracking-wider text-dim">
            {playlist.source === 'ytmusic' ? 'YouTube Music' : 'Playlist'}
          </div>
          <h1 className="mb-2 text-3xl font-black leading-tight sm:text-4xl">{playlist.name}</h1>
          <div className="text-xs text-dim">
            {num(tracks.length, lang)} {t(lang, 'songs')} · ~{fmtSize(totalEst, lang)}
            {doneCount > 0 && <> · <span className="text-[var(--accent)]">{num(doneCount, lang)} {t(lang, 'offlineReady')}</span></>}
          </div>
          <div className="mt-4 flex flex-wrap items-center justify-center gap-2 sm:justify-start">
            <button
              disabled={!tracks.length}
              onClick={() => tracks.length && usePlayer.getState().setQueue(tracks, 0, { kind: 'playlist', id })}
              className="neon-play flex items-center gap-2 rounded-full px-6 py-2.5 text-sm font-bold disabled:opacity-40"
            >
              <Play size={15} fill="currentColor" /> {t(lang, 'playAll')}
            </button>
            <button
              disabled={!tracks.length}
              onClick={() => tracks.length && usePlayer.getState().setQueue(tracks, Math.floor(Math.random() * tracks.length), { kind: 'playlist', id })}
              className="flex items-center gap-2 rounded-full border border-line bg-surface px-5 py-2.5 text-sm hover:bg-surface2 disabled:opacity-40"
            >
              <Shuffle size={15} /> {t(lang, 'shufflePlay')}
            </button>
            <button
              disabled={!tracks.length}
              onClick={() => {
                if (useSettings.getState().askBeforeBatch) setConfirmDl(true);
                else { enqueueMany(tracks); toast(t(lang, 'dlStart')); useView.getState().push('library'); }
              }}
              className="flex items-center gap-2 rounded-full border border-line bg-surface px-5 py-2.5 text-sm hover:bg-surface2 disabled:opacity-40"
            >
              <Download size={15} /> {t(lang, 'downloadAll')}
            </button>
            <button onClick={() => setAdding(true)} className="flex items-center gap-2 rounded-full border border-line bg-surface px-5 py-2.5 text-sm hover:bg-surface2">
              <Search size={15} /> {t(lang, 'addSongs')}
            </button>
            <div className="relative">
              {menu && <button aria-hidden className="fixed inset-0 z-20 cursor-default" onClick={() => setMenu(false)} tabIndex={-1} />}
              <button onClick={() => setMenu(!menu)} className="relative z-30 grid h-11 w-11 place-items-center rounded-full bg-surface transition-colors hover:bg-surface2" aria-label="menu">
                <MoreVertical size={17} />
              </button>
              {menu && (
                <div className="absolute end-0 top-12 z-30 w-44 overflow-hidden rounded-xl border border-line bg-[var(--app-bg-2)] shadow-xl view-in">
                  <button className="flex w-full items-center gap-2 px-4 py-3 text-xs transition-colors hover:bg-surface2" onClick={() => { setRenaming(true); setNewName(playlist.name); setMenu(false); }}>
                    <Pencil size={13} /> {t(lang, 'rename')}
                  </button>
                  <label className="flex w-full cursor-pointer items-center justify-between gap-2 px-4 py-3 text-xs transition-colors hover:bg-surface2">
                    <span className="flex items-center gap-2"><Download size={13} /> {t(lang, 'autoDownload')}</span>
                    <Switch checked={!!playlist.autoDownload} onCheckedChange={(v) => autoDl(playlist.id, v)} />
                  </label>
                  <button
                    className="flex w-full items-center gap-2 px-4 py-3 text-xs text-red-400 transition-colors hover:bg-surface2"
                    onClick={() => { if (confirm(t(lang, 'confirmDelete'))) { del(playlist.id); useView.getState().back(); toast(t(lang, 'deleted')); } }}
                  >
                    <Trash2 size={13} /> {t(lang, 'deletePlaylist')}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* tracks */}
      {tracks.length === 0 ? (
        <div className="rounded-2xl border border-line bg-surface py-16 text-center text-sm text-dim">{t(lang, 'emptyPlaylist')}</div>
      ) : (
        <div className="flex flex-col gap-1">
          {tracks.map((tr: Track, i) => (
            <TrackRow key={tr.videoId} track={tr} i={i} queueContext={tracks} onRemove={() => removeTrack(playlist.id, tr.videoId)} />
          ))}
        </div>
      )}

      {/* batch download confirm */}
      <Dialog open={confirmDl} onOpenChange={setConfirmDl}>
        <DialogContent className="border-line bg-[var(--app-bg-2)] sm:max-w-sm">
          <DialogHeader><DialogTitle className="flex items-center gap-2"><ArrowDownToLine size={16} /> {t(lang, 'dnTitle')}</DialogTitle></DialogHeader>
          <div className="text-sm">
            {num(tracks.length, lang)} {t(lang, 'songs')} — {t(lang, 'totalSize')}: <b className="text-[var(--accent)]">{fmtSize(totalEst, lang)}</b>
          </div>
          <div className="text-[11px] text-dim">{t(lang, 'quality')}: {dq === 'high' ? t(lang, 'high') : dq === 'mid' ? t(lang, 'mid') : t(lang, 'low')} ({dq === 'high' ? '160' : dq === 'mid' ? '70' : '50'}kbps)</div>
          <div className="flex gap-2">
            <button className="flex-1 rounded-xl border border-line py-3 text-sm text-dim transition-colors hover:bg-surface" onClick={() => setConfirmDl(false)}>{t(lang, 'cancel')}</button>
            <button
              className="neon-play flex-1 rounded-xl py-3 text-sm font-bold transition-transform active:scale-[0.98]"
              onClick={() => { setConfirmDl(false); enqueueMany(tracks); toast(t(lang, 'dlStart')); useView.getState().push('library'); }}
            >
              {t(lang, 'startDownload')}
            </button>
          </div>
        </DialogContent>
      </Dialog>

      {/* rename */}
      <Dialog open={renaming} onOpenChange={setRenaming}>
        <DialogContent className="border-line bg-[var(--app-bg-2)] sm:max-w-sm">
          <DialogHeader><DialogTitle>{t(lang, 'rename')}</DialogTitle></DialogHeader>
          <input autoFocus value={newName} onChange={(e) => setNewName(e.target.value)} className="w-full rounded-xl border border-line bg-surface px-4 py-3 text-sm outline-none focus:border-[var(--accent)]" />
          <button
            className="neon-play rounded-xl py-2.5 text-sm font-bold"
            onClick={() => { if (newName.trim()) { rename(playlist.id, newName.trim()); setRenaming(false); } }}
          >
            <Check size={16} className="inline" /> {t(lang, 'done')}
          </button>
        </DialogContent>
      </Dialog>

      {/* add songs inline search */}
      <AddSongsDialog open={adding} onClose={() => setAdding(false)} onAdd={(tr) => {
        useLibrary.getState().addToPlaylist(playlist.id, tr);
        if (playlist.autoDownload) {
          enqueueMany([tr]);
        }
      }} />
    </div>
  );
}

function AddSongsDialog({ open, onClose, onAdd }: { open: boolean; onClose: () => void; onAdd: (t: Track) => void }) {
  const lang = useSettings((s) => s.lang);
  const [q, setQ] = useState('');
  const [tracks, setTracks] = useState<Track[] | null>(null);
  const [added, setAdded] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!open) {
      const id = setTimeout(() => { setTracks(null); setQ(''); setAdded(new Set()); }, 0);
      return () => clearTimeout(id);
    }
  }, [open]);

  useEffect2Inline(q, open, setTracks);

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-line bg-[var(--app-bg-2)] sm:max-w-lg">
        <DialogHeader><DialogTitle className="flex items-center gap-2"><Search size={15} /> {t(lang, 'addSongs')}</DialogTitle></DialogHeader>
        <input
          autoFocus value={q} onChange={(e) => setQ(e.target.value)}
          placeholder={t(lang, 'searchPlaceholder')}
          className="w-full rounded-xl border border-line bg-surface px-4 py-3 text-sm outline-none focus:border-[var(--accent)]"
        />
        <div className="max-h-[320px] overflow-y-auto">
          {tracks?.map((tr) => (
            <div key={tr.videoId} className="flex items-center gap-3 rounded-xl p-2 hover:bg-surface2">
              <TrackThumb track={tr} rounded="rounded-md" className="h-9 w-9 shrink-0" />
              <div className="min-w-0 flex-1">
                <div className="truncate text-xs font-semibold">{tr.title}</div>
                <div className="truncate text-[10px] text-dim">{tr.artist}</div>
              </div>
              <button
                onClick={() => { onAdd(tr); setAdded((s) => new Set(s).add(tr.videoId)); }}
                className="neon-play grid h-10 w-10 shrink-0 place-items-center rounded-full transition-transform active:scale-90 disabled:opacity-40"
                disabled={added.has(tr.videoId)}
                aria-label="add"
              >
                {added.has(tr.videoId) ? <Check size={15} /> : <Plus size={16} />}
              </button>
            </div>
          ))}
        </div>
        <button onClick={onClose} className="rounded-xl border border-line py-2 text-sm text-dim">{t(lang, 'done')}</button>
      </DialogContent>
    </Dialog>
  );
}

import { Plus } from 'lucide-react';
import { useEffect } from 'react';

function useEffect2Inline(q: string, open: boolean, setTracks: (t: Track[] | null) => void) {
  useEffect(() => {
    if (!open || !q.trim()) {
      const id = setTimeout(() => setTracks(null), 0);
      return () => clearTimeout(id);
    }
    const id = setTimeout(() => {
      import('@/engine/ytclient').then(({ searchTracks }) => searchTracks(q))
        .catch(() => fetch(`/api/yt/search?q=${encodeURIComponent(q)}`).then((r) => r.json()).then((d) => d.tracks ?? []))
        .then((t2) => setTracks(t2 ?? []))
        .catch(() => setTracks([]));
    }, 450);
    return () => clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, open]);
}
