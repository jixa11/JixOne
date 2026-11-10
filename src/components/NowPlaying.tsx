'use client';
import { useEffect, useRef, useState } from 'react';
import {
  Play, Pause, SkipBack, SkipForward, Repeat, Repeat1, Shuffle,
  Heart, ListMusic, Download, ChevronDown, Video,
} from 'lucide-react';
import { usePlayer } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useLibrary } from '@/store/library';
import { useDownloads } from '@/store/downloads';
import { enqueueDownload } from '@/engine/downloadsRunner';
import { fmtTime, t } from '@/lib/i18n';
import { toast } from 'sonner';
import { TrackThumb } from '@/components/TrackThumb';
import { cn } from '@/lib/utils';

/**
 * Full-screen now-playing sheet. Reached by tapping the player bar rather than
 * from the nav, so it behaves like a sheet over the app instead of a route.
 */
export default function NowPlaying() {
  const p = usePlayer();
  const open = usePlayer((s) => s.npOpen);
  const setOpen = usePlayer((s) => s.setNpOpen);
  const lang = useSettings((s) => s.lang);
  const track = p.queue[p.index];
  const liked = useLibrary((s) => s.liked.some((x) => x.videoId === track?.videoId));
  const toggleLike = useLibrary((s) => s.toggleLike);
  const dl = useDownloads((s) => (track ? s.items[track.videoId] : undefined));

  const [dragVal, setDragVal] = useState<number | null>(null);
  // drag-down-to-dismiss
  const [dragY, setDragY] = useState(0);
  const startY = useRef<number | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false); };
    window.addEventListener('keydown', onKey);
    // the sheet covers the app; stop the page behind it from scrolling
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  }, [open, setOpen]);

  useEffect(() => { if (!track) setOpen(false); }, [track, setOpen]);

  if (!track) return null;

  const pos = dragVal ?? p.position;
  const dur = p.duration || track.durationSec || 0;
  const pct = dur ? Math.min(100, (pos / dur) * 100) : 0;
  const RepeatIcon = p.repeat === 'one' ? Repeat1 : Repeat;

  const onPointerDown = (e: React.PointerEvent) => {
    if ((e.target as HTMLElement).closest('input,button')) return;
    startY.current = e.clientY;
  };
  const onPointerMove = (e: React.PointerEvent) => {
    if (startY.current === null) return;
    setDragY(Math.max(0, e.clientY - startY.current));
  };
  const endDrag = () => {
    if (dragY > 110) setOpen(false);
    startY.current = null;
    setDragY(0);
  };

  return (
    <div
      className={cn('np-sheet', open && 'np-open')}
      aria-hidden={!open}
      role="dialog"
      aria-modal="true"
      aria-label={t(lang, 'nowPlaying')}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      style={dragY ? { transform: `translateY(${dragY}px)`, transition: 'none' } : undefined}
    >
      {/* the art, blurred, as the sheet's own ground */}
      <div className="np-bg" style={{ backgroundImage: `url("${track.thumb}")` }} aria-hidden />

      {/* one centred column: on a wide screen the controls would otherwise
          spread to the far edges and stop reading as a single player */}
      <div className="relative mx-auto flex h-full w-full max-w-[460px] flex-col">
        {/* header */}
        <div className="flex items-center gap-2 px-4 pt-4">
          <button
            onClick={() => setOpen(false)}
            className="grid h-10 w-10 place-items-center rounded-full bg-surface2 text-dim transition-transform active:scale-90"
            aria-label={t(lang, 'close')}
          >
            <ChevronDown size={20} />
          </button>
          <div className="flex-1 text-center text-[11px] font-semibold uppercase tracking-[0.14em] text-dim">
            {t(lang, 'nowPlaying')}
          </div>
          <button
            onClick={() => p.cycleVideo()}
            className="grid h-10 w-10 place-items-center rounded-full bg-surface2 transition-transform active:scale-90"
            aria-label="video"
          >
            <Video size={18} className={p.videoMode !== 'hidden' ? 'text-[var(--accent)]' : 'text-dim'} />
          </button>
        </div>

        {/* grab handle — the affordance for dragging the sheet back down */}
        <div className="mx-auto mt-3 h-1 w-10 shrink-0 rounded-full bg-[var(--text-dim)] opacity-40" />

        {/* cover */}
        <div className="flex flex-1 items-center justify-center px-7 py-5">
          <TrackThumb
            track={track}
            rounded="rounded-2xl"
            className="np-art aspect-square w-full max-w-[min(74vw,340px)] shadow-2xl"
            imgClassName="h-full w-full"
          />
        </div>

        {/* title + like */}
        <div className="flex items-center gap-3 px-7">
          <div className="min-w-0 flex-1">
            <div className="truncate text-[19px] font-black leading-tight">{track.title}</div>
            <div className="truncate text-[13px] text-dim">{track.artist}</div>
          </div>
          <button
            onClick={() => { const now = toggleLike(track); toast(t(lang, now ? 'addedToLiked' : 'removedFromLiked')); }}
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full transition-transform active:scale-90"
            aria-label={t(lang, 'like')}
          >
            <Heart key={String(liked)} size={22} className={liked ? 'heart-pop fill-[var(--accent)] text-[var(--accent)]' : 'text-dim'} />
          </button>
        </div>

        {/* seek */}
        <div dir="ltr" className="px-7 pt-5">
          <div className="slider-wrap">
            <input
              type="range" min={0} max={dur || 1} step={1} value={pos}
              style={{ ['--fill' as string]: `${pct}%` }}
              onChange={(e) => setDragVal(+e.target.value)}
              onPointerUp={(e) => { p.seek(+(e.target as HTMLInputElement).value); setDragVal(null); }}
              onKeyUp={(e) => { p.seek(+(e.target as HTMLInputElement).value); setDragVal(null); }}
              className="slider w-full" aria-label="seek"
            />
          </div>
          <div className="mt-1.5 flex justify-between text-[11px] tabular-nums text-dim">
            <span>{fmtTime(pos)}</span>
            <span>{fmtTime(dur)}</span>
          </div>
        </div>

        {/* transport */}
        <div dir="ltr" className="flex items-center justify-center gap-5 px-7 pt-3 sm:gap-7">
          <button
            onClick={p.toggleShuffle}
            className={cn('grid h-11 w-11 place-items-center rounded-full transition-transform active:scale-90', p.shuffle ? 'text-[var(--accent)]' : 'text-dim')}
            aria-label="shuffle"
          >
            <Shuffle size={20} />
          </button>
          <button onClick={p.prev} className="grid h-12 w-12 place-items-center rounded-full transition-transform active:scale-90" aria-label="previous">
            <SkipBack size={26} fill="currentColor" />
          </button>
          <button
            onClick={() => { if (p.engine === 'pending' || p.loading) return; p.toggle(); }}
            className={cn('neon-play grid h-[68px] w-[68px] place-items-center rounded-full transition-transform active:scale-95', p.playing && 'pulse-glow')}
            aria-label="play/pause"
          >
            {p.loading || p.engine === 'pending' ? (
              <span className="h-6 w-6 animate-spin rounded-full border-2 border-black/30 border-t-black" />
            ) : p.playing ? <Pause size={30} fill="currentColor" /> : <Play size={30} fill="currentColor" className="ms-1" />}
          </button>
          <button onClick={() => p.next()} className="grid h-12 w-12 place-items-center rounded-full transition-transform active:scale-90" aria-label="next">
            <SkipForward size={26} fill="currentColor" />
          </button>
          <button
            onClick={p.cycleRepeat}
            className={cn('grid h-11 w-11 place-items-center rounded-full transition-transform active:scale-90', p.repeat !== 'off' ? 'text-[var(--accent)]' : 'text-dim')}
            aria-label="repeat"
          >
            <RepeatIcon size={20} />
          </button>
        </div>

        {/* secondary actions */}
        <div className="flex items-center justify-center gap-3 px-7 pb-[calc(22px+env(safe-area-inset-bottom))] pt-5">
          <button
            onClick={() => {
              enqueueDownload(track.videoId, { title: track.title, artist: track.artist, thumb: track.thumb, durationSec: track.durationSec ?? 210 });
              toast(t(lang, 'addedToDl'));
            }}
            className="flex items-center gap-2 rounded-full border border-line bg-surface px-4 py-2.5 text-[12px] font-semibold transition-colors hover:bg-surface2"
          >
            <Download size={15} className={dl?.status === 'done' ? 'text-[var(--accent)]' : 'text-dim'} />
            {dl?.status === 'done' ? t(lang, 'downloaded') : t(lang, 'download')}
          </button>
          <button
            onClick={p.toggleQueue}
            className="flex items-center gap-2 rounded-full border border-line bg-surface px-4 py-2.5 text-[12px] font-semibold transition-colors hover:bg-surface2"
          >
            <ListMusic size={15} className="text-dim" />
            {t(lang, 'queue')}
          </button>
        </div>
      </div>
    </div>
  );
}
