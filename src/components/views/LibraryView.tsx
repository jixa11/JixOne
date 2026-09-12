'use client';
import { Heart, Clock3, Plus, Download, Youtube } from 'lucide-react';
import { useLibrary } from '@/store/library';
import { useSettings } from '@/store/settings';
import { useView } from '@/store/view';
import { t, num } from '@/lib/i18n';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { useState } from 'react';
import { TrackCard } from '@/components/TrackCard';
import { TrackThumb } from '@/components/TrackThumb';
import DownloadsView from '@/components/views/DownloadsView';

export default function LibraryView() {
  const lang = useSettings((s) => s.lang);
  const playlists = useLibrary((s) => s.playlists);
  const create = useLibrary((s) => s.createPlaylist);
  const liked = useLibrary((s) => s.liked);
  const history = useLibrary((s) => s.history);
  const [openNew, setOpenNew] = useState(false);
  const [name, setName] = useState('');

  return (
    <div className="view-in mx-auto max-w-[1200px] px-4 pb-8">
      <h1 className="mb-6 text-2xl font-black sm:text-3xl">{t(lang, 'library')}</h1>

      <div className="stagger mb-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <button onClick={() => setOpenNew(true)} className="card-hover flex min-h-[56px] items-center justify-center gap-2 rounded-2xl border border-dashed border-line bg-surface text-sm text-dim">
          <Plus size={16} /> {t(lang, 'newPlaylist')}
        </button>
        <button onClick={() => useView.getState().push('liked')} className="card-hover flex min-h-[56px] items-center gap-3 rounded-2xl bg-surface p-3 text-start">
          <span className="neon-play grid h-10 w-10 place-items-center rounded-lg"><Heart size={17} fill="currentColor" /></span>
          <span><span className="block text-[13px] font-bold">{t(lang, 'likedSongs')}</span><span className="text-[11px] text-dim">{num(liked.length, lang)}</span></span>
        </button>
        <button onClick={() => useView.getState().push('history')} className="card-hover flex min-h-[56px] items-center gap-3 rounded-2xl bg-surface p-3 text-start">
          <span className="grid h-10 w-10 place-items-center rounded-lg bg-surface2 text-dim"><Clock3 size={17} /></span>
          <span><span className="block text-[13px] font-bold">{t(lang, 'recentPlayed')}</span><span className="text-[11px] text-dim">{num(history.length, lang)}</span></span>
        </button>
        <button onClick={() => document.getElementById('jixone-downloads')?.scrollIntoView({ behavior: 'smooth', block: 'start' })} className="card-hover flex min-h-[56px] items-center gap-3 rounded-2xl bg-surface p-3 text-start">
          <span className="grid h-10 w-10 place-items-center rounded-lg bg-surface2 text-dim"><Download size={17} /></span>
          <span><span className="block text-[13px] font-bold">{t(lang, 'downloads')}</span></span>
        </button>
      </div>

      {playlists.length === 0 ? (
        <button
          onClick={() => setOpenNew(true)}
          className="flex w-full cursor-pointer flex-col items-center gap-3 rounded-2xl border border-dashed border-line bg-surface py-14 text-center transition-colors hover:bg-surface2"
        >
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-surface2 text-dim">
            <Plus size={24} />
          </span>
          <span className="max-w-[320px] text-[13px] leading-relaxed text-dim">{t(lang, 'emptyLibrary')}</span>
        </button>
      ) : (
        <div className="stagger grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {playlists.map((p) => (
            <button key={p.id} onClick={() => useView.getState().push('playlist', p.id)} className="card-hover rounded-2xl bg-surface p-3 text-start">
              <div className="relative mb-3 aspect-square w-full overflow-hidden rounded-xl bg-surface2">
                {p.tracks[0] ? (
                  <div className="grid h-full w-full grid-cols-2 grid-rows-2">
                    {p.tracks.slice(0, 4).map((t2) => (
                      <TrackThumb key={t2.videoId} track={t2} rounded="" className="h-full w-full" />
                    ))}
                    {p.tracks.length < 4 && Array.from({ length: 4 - Math.min(4, p.tracks.length) }).map((_, i) => <div key={i} className="bg-surface2" />)}
                  </div>
                ) : (
                  <div className="grid h-full place-items-center text-dim"><Download size={26} /></div>
                )}
              </div>
              <div className="flex items-center gap-1.5">
                {p.source === 'ytmusic' && (
                  <span className="grid h-4 w-4 shrink-0 place-items-center rounded-full bg-[#ff0033] text-white">
                    <Youtube size={9} strokeWidth={2.5} />
                  </span>
                )}
                <div className="truncate text-sm font-bold">{p.name}</div>
              </div>
              <div className="text-[11px] text-dim">{num(p.tracks.length, lang)} {t(lang, 'songs')}</div>
            </button>
          ))}
        </div>
      )}

      {/* liked preview */}
      {liked.length > 0 && (
        <>
          <h2 className="mb-3 mt-8 text-lg font-black">{t(lang, 'likedSongs')}</h2>
          <div className="stagger grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">
            {liked.slice(0, 6).map((tr) => <TrackCard key={tr.videoId} track={tr} queueContext={liked} />)}
          </div>
        </>
      )}

      {/* offline downloads — merged into Library (per user request) */}
      <div id="jixone-downloads" className="scroll-mt-20" />
      <h2 className="mb-3 mt-10 text-lg font-black">{t(lang, 'libraryDownloads')}</h2>
      <DownloadsView embedded />

      <Dialog open={openNew} onOpenChange={setOpenNew}>
        <DialogContent className="border-line bg-[var(--app-bg-2)] sm:max-w-sm">
          <DialogHeader><DialogTitle>{t(lang, 'createPlaylist')}</DialogTitle></DialogHeader>
          <input
            autoFocus value={name} onChange={(e) => setName(e.target.value)}
            placeholder={t(lang, 'playlistName')}
            className="w-full rounded-xl border border-line bg-surface px-4 py-3 text-sm outline-none focus:border-[var(--accent)]"
          />
          <button
            className="neon-play mt-2 rounded-xl py-2.5 text-sm font-bold"
            onClick={() => {
              if (!name.trim()) { toast(t(lang, 'needName')); return; }
              const id = create(name.trim());
              setName(''); setOpenNew(false);
              useView.getState().push('playlist', id);
            }}
          >
            {t(lang, 'createPlaylist')}
          </button>
        </DialogContent>
      </Dialog>
    </div>
  );
}
