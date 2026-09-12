'use client';
import { useEffect, useState, useRef } from 'react';
import { Play, Pause, SkipBack, SkipForward, Repeat, Repeat1, Shuffle, Heart, ListMusic, Download, Volume2, VolumeX, Video } from 'lucide-react';
import { usePlayer } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useLibrary } from '@/store/library';
import { useDownloads } from '@/store/downloads';
import { enqueueDownload } from '@/engine/downloadsRunner';
import { fmtTime, t, num } from '@/lib/i18n';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { TrackThumb } from '@/components/TrackThumb';
import { cn } from '@/lib/utils';

export default function PlayerBar() {
  const p = usePlayer();
  const lang = useSettings((s) => s.lang);
  const volume = useSettings((s) => s.volume);
  const setS = useSettings((s) => s.set);
  const liked = useLibrary((s) => s.liked.some((x) => x.videoId === p.queue[p.index]?.videoId));
  const toggleLike = useLibrary((s) => s.toggleLike);
  const dl = useDownloads((s) => (p.queue[p.index] ? s.items[p.queue[p.index]!.videoId] : undefined));
  const track = p.queue[p.index];
  const barRef = useRef<HTMLDivElement>(null);

  // seek slider local state for smooth dragging
  const [dragVal, setDragVal] = useState<number | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;
      if (e.code === 'Space') { e.preventDefault(); p.toggle(); }
      if (e.code === 'ArrowRight' && e.ctrlKey) p.next();
      if (e.code === 'ArrowLeft' && e.ctrlKey) p.prev();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!track) {
    return (
      <div ref={barRef} className="glass fixed bottom-0 z-30 flex h-[76px] w-full items-center justify-center border-t border-line text-xs text-dim md:ps-[264px]">
        {t(lang, 'nothingPlaying')} — {t(lang, 'tapToPlay')}
      </div>
    );
  }
  const pos = dragVal ?? p.position;
  const dur = p.duration || track.durationSec || 0;
  const pct = dur ? Math.min(100, (pos / dur) * 100) : 0;
  const RepeatIcon = p.repeat === 'one' ? Repeat1 : Repeat;

  return (
    <div ref={barRef} className="glass player-in fixed bottom-0 z-30 w-full border-t border-line md:ps-[264px]">
      {/* animated progress on top edge (mobile) */}
      <div className="h-[2px] w-full bg-surface2 md:hidden">
        <div className="sheen-line h-full" style={{ width: `${pct}%` }} />
      </div>

      <div className="flex h-[72px] items-center gap-2 px-3 sm:gap-4 sm:px-4">
        {/* track info */}
        <div className="flex min-w-0 items-center gap-3 md:w-[26%]">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <TrackThumb track={track} rounded="rounded-lg" className="h-11 w-11 shadow-md" />
          <div className="min-w-0 flex-1">
            <div className="truncate text-[13px] font-semibold">{track.title}</div>
            <div className="truncate text-[11px] text-dim">{track.artist}</div>
          </div>
          <Button
            variant="ghost" size="icon"
            className="hidden h-9 w-9 sm:grid sm:place-items-center"
            onClick={() => { const now = toggleLike(track); toast(t(lang, now ? 'addedToLiked' : 'removedFromLiked')); }}
            aria-label={t(lang, 'like')}
          >
            <Heart
              key={String(liked)}
              size={16}
              className={liked ? 'heart-pop fill-[var(--accent)] text-[var(--accent)]' : 'text-dim'}
            />
          </Button>
        </div>

        {/* transport (LTR zone) */}
        <div dir="ltr" className="flex flex-1 flex-col items-center gap-1">
          <div className="flex items-center gap-2 sm:gap-4">
            <Button variant="ghost" size="icon" className={cn('hidden h-8 w-8 sm:grid sm:place-items-center', p.shuffle && 'text-[var(--accent)]')} onClick={p.toggleShuffle} aria-label="shuffle">
              <Shuffle size={16} />
            </Button>
            <Button variant="ghost" size="icon" className="h-9 w-9" onClick={p.prev} aria-label="prev">
              <SkipBack size={18} fill="currentColor" />
            </Button>
            <button
              onClick={() => { if (p.engine === 'pending' || p.loading) return; p.toggle(); }}
              className={`neon-play grid h-11 w-11 place-items-center rounded-full transition-transform active:scale-95 ${p.playing ? 'pulse-glow' : ''}`}
              aria-label="play/pause"
            >
              {p.loading || p.engine === 'pending' ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-black/30 border-t-black" />
              ) : p.playing ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" className="ms-0.5" />}
            </button>
            <Button variant="ghost" size="icon" className="h-9 w-9" onClick={() => p.next()} aria-label="next">
              <SkipForward size={18} fill="currentColor" />
            </Button>
            <Button variant="ghost" size="icon" className={cn('hidden h-8 w-8 sm:grid sm:place-items-center', p.repeat !== 'off' && 'text-[var(--accent)]')} onClick={p.cycleRepeat} aria-label="repeat">
              <RepeatIcon size={16} />
            </Button>
          </div>
          {/* seek */}
          <div className="hidden w-full max-w-[520px] items-center gap-2 md:flex" dir="ltr">
            <span className="w-9 text-end text-[10px] tabular-nums text-dim">{fmtTime(pos)}</span>
            <div className="slider-wrap relative flex-1">
              <input
                type="range" min={0} max={dur || 1} step={1} value={pos}
                style={{ ['--fill' as string]: `${pct}%` }}
                onChange={(e) => setDragVal(+e.target.value)}
                onPointerUp={(e) => { p.seek(+(e.target as HTMLInputElement).value); setDragVal(null); }}
                onKeyUp={(e) => { p.seek(+(e.target as HTMLInputElement).value); setDragVal(null); }}
                className="slider w-full" aria-label="seek"
              />
            </div>
            <span className="w-9 text-[10px] tabular-nums text-dim">{fmtTime(dur)}</span>
          </div>
        </div>

        {/* right controls */}
        <div className="flex items-center gap-1 md:w-[26%] md:justify-end">
          <Button
            variant="ghost" size="icon" className="hidden h-8 w-8 lg:grid lg:place-items-center"
            onClick={() => { setS({ volume: volume > 0 ? 0 : 0.9 }); }}
            aria-label="mute"
          >
            {volume > 0 ? <Volume2 size={16} className="text-dim" /> : <VolumeX size={16} className="text-dim" />}
          </Button>
          <div dir="ltr" className="slider-wrap hidden w-24 lg:block">
            <input
              type="range" min={0} max={1} step={0.01} value={volume}
              style={{ ['--fill' as string]: `${volume * 100}%` }}
              onChange={(e) => setS({ volume: +e.target.value })}
              className="slider w-full" aria-label="volume"
            />
          </div>
          <Button variant="ghost" size="icon" className="h-10 w-10 sm:h-9 sm:w-9" aria-label="video" onClick={() => { p.cycleVideo(); }}>
            <Video size={17} className={p.videoMode !== 'hidden' ? 'text-[var(--accent)]' : 'text-dim'} />
          </Button>
          <Button
            variant="ghost" size="icon" className="h-10 w-10 sm:h-9 sm:w-9" aria-label="download current"
            onClick={() => {
              enqueueDownload(track.videoId, { title: track.title, artist: track.artist, thumb: track.thumb, durationSec: track.durationSec ?? 210 });
              toast(t(lang, 'addedToDl'));
            }}
          >
            {dl?.status === 'done' ? <Download size={17} className="text-[var(--accent)]" /> : <Download size={17} className="text-dim" />}
          </Button>
          <Button variant="ghost" size="icon" className="relative h-10 w-10 sm:h-9 sm:w-9" onClick={p.toggleQueue} aria-label="queue">
            <ListMusic size={18} className="text-dim" />
            {p.queue.length > 0 && (
              <span className="absolute -end-0.5 -top-0.5 min-w-[16px] rounded-full bg-[var(--accent)] px-1 text-center text-[9px] font-bold leading-4 text-[var(--on-accent)]">
                {num(p.queue.length, lang)}
              </span>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
