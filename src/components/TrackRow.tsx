'use client';
import { memo, useRef, useState } from 'react';
import { Play, Heart, Download, MoreHorizontal, ListPlus, Trash2, CheckCircle2, Loader2, Pause, SkipForward } from 'lucide-react';
import { Track } from '@/lib/types';
import { usePlayer } from '@/store/player';
import { useLibrary } from '@/store/library';
import { useDownloads } from '@/store/downloads';
import { enqueueDownload } from '@/engine/downloadsRunner';
import { useSettings } from '@/store/settings';
import { useView } from '@/store/view';
import { fmtTime, num, t } from '@/lib/i18n';
import { toast } from 'sonner';
import { TrackThumb } from '@/components/TrackThumb';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator, DropdownMenuSub, DropdownMenuSubTrigger, DropdownMenuSubContent,
} from '@/components/ui/dropdown-menu';

interface Props {
  track: Track;
  i?: number;
  queueContext: Track[];
  onRemove?: () => void;
  showIndex?: boolean;
}

function RowInner({ track, i, queueContext, onRemove, showIndex = true }: Props) {
  const lang = useSettings((s) => s.lang);
  const player = usePlayer();
  const isCurrent = player.queue[player.index]?.videoId === track.videoId;
  const isPlaying = isCurrent && player.playing;
  const liked = useLibrary((s) => s.liked.some((x) => x.videoId === track.videoId));
  const dl = useDownloads((s) => s.items[track.videoId]);
  const playlists = useLibrary((s) => s.playlists);

  const play = () => {
    if (isCurrent) { usePlayer.getState().toggle(); return; }
    const idx = Math.max(0, queueContext.findIndex((x) => x.videoId === track.videoId));
    usePlayer.getState().setQueue(queueContext, idx);
  };

  const downloadNow = () => {
    enqueueDownload(track.videoId, { title: track.title, artist: track.artist, thumb: track.thumb, durationSec: track.durationSec ?? 210 });
    toast(t(lang, 'addedToDl'));
    useView.getState().push('library');
  };

  // ---- swipe actions (touch only) ----
  // left  → skip to the track after this one
  // right → drop this track at the end of the play queue
  const [dx, setDx] = useState(0);
  const swipe = useRef<{ x: number; y: number; active: boolean } | null>(null);
  const THRESHOLD = 72;

  const swipeLeft = () => {
    const idx = queueContext.findIndex((x) => x.videoId === track.videoId);
    const nextIdx = idx + 1;
    if (nextIdx > 0 && nextIdx < queueContext.length) {
      usePlayer.getState().setQueue(queueContext, nextIdx);
      toast(`${t(lang, 'upNext')}: ${queueContext[nextIdx].title}`);
    } else {
      usePlayer.getState().next();
    }
  };
  const swipeRight = () => {
    usePlayer.getState().enqueue(track);
    toast(t(lang, 'addedToQueue'));
  };

  // The thumbnail and the title are both buttons, so the gesture has to start on
  // them — skipping buttons here left nowhere on the row to swipe from. Instead
  // the drag arms everywhere and the click it would otherwise fire is swallowed.
  const swiped = useRef(false);

  const onPointerDown = (e: React.PointerEvent) => {
    if (e.pointerType === 'mouse') return; // pointer users have the row menu
    swiped.current = false;
    swipe.current = { x: e.clientX, y: e.clientY, active: false };
    // the row slides out from under the finger, so without capture the rest of
    // the move events land on whatever is beneath it and the gesture dies
    try { e.currentTarget.setPointerCapture(e.pointerId); } catch { /* unsupported */ }
  };
  const onPointerMove = (e: React.PointerEvent) => {
    const s = swipe.current;
    if (!s) return;
    const mx = e.clientX - s.x;
    const my = e.clientY - s.y;
    if (!s.active) {
      if (Math.abs(mx) < 10 && Math.abs(my) < 10) return;
      // a mostly-vertical drag is the list scrolling, so let it go
      if (Math.abs(my) >= Math.abs(mx)) { swipe.current = null; setDx(0); return; }
      s.active = true;
    }
    setDx(Math.max(-130, Math.min(130, mx)));
  };
  const endSwipe = (e: React.PointerEvent) => {
    try { e.currentTarget.releasePointerCapture(e.pointerId); } catch { /* already released */ }
    if (swipe.current?.active) {
      swiped.current = true;
      if (dx <= -THRESHOLD) swipeLeft();
      else if (dx >= THRESHOLD) swipeRight();
    }
    swipe.current = null;
    setDx(0);
  };
  /** a drag must not also count as a tap on the row's play button */
  const onClickCapture = (e: React.MouseEvent) => {
    if (!swiped.current) return;
    swiped.current = false;
    e.preventDefault();
    e.stopPropagation();
  };

  const armed = Math.abs(dx) >= THRESHOLD;

  return (
    <div className="relative">
      {/* what the swipe will do, revealed under the row as it moves */}
      {dx !== 0 && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-between rounded-xl px-4" aria-hidden>
          <span className={`flex items-center gap-1.5 text-[11px] font-bold transition-opacity ${dx > 0 ? 'opacity-100' : 'opacity-0'} ${armed && dx > 0 ? 'text-[var(--accent)]' : 'text-dim'}`}>
            <ListPlus size={16} /> {t(lang, 'addedToQueue')}
          </span>
          <span className={`flex items-center gap-1.5 text-[11px] font-bold transition-opacity ${dx < 0 ? 'opacity-100' : 'opacity-0'} ${armed && dx < 0 ? 'text-[var(--accent)]' : 'text-dim'}`}>
            {t(lang, 'upNext')} <SkipForward size={16} />
          </span>
        </div>
      )}
      <div
        onDoubleClick={play}
        onClickCapture={onClickCapture}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endSwipe}
        onPointerCancel={endSwipe}
        style={dx ? { transform: `translateX(${dx}px)`, transition: 'none' } : undefined}
        className={`group relative flex touch-pan-y items-center gap-2 rounded-xl px-2 py-2 transition-[background-color,transform] sm:gap-3 sm:px-3 sm:py-1.5 ${isCurrent ? 'bg-surface2' : 'bg-[var(--app-bg)] hover:bg-surface2'}`}
      >
      {showIndex && (
        <div className="relative hidden w-7 shrink-0 items-center justify-center text-xs text-dim sm:flex">
          <span className="group-hover:hidden">{isCurrent ? <EqBars playing={isPlaying} /> : num((i ?? 0) + 1, lang)}</span>
          <button onClick={play} aria-label={t(lang, 'playAll')} className="hidden text-foreground group-hover:block">
            {isPlaying ? <Pause size={15} /> : <Play size={15} />}
          </button>
        </div>
      )}
      <button onClick={play} className="relative shrink-0 overflow-hidden rounded-lg sm:hidden" aria-label="play">
        <TrackThumb track={track} rounded="rounded-lg" className="h-10 w-10" />
        <span className="absolute inset-0 grid h-10 w-10 place-items-center bg-black/45 opacity-0 transition-opacity active:opacity-100">
          <Play size={16} className="text-white" />
        </span>
      </button>
      <button onClick={play} className="hidden shrink-0 sm:block" aria-label="play">
        <TrackThumb track={track} rounded="rounded-lg" className="h-10 w-10" />
      </button>

      <button onClick={play} className="min-w-0 flex-1 text-start">
        <div className={`truncate text-[13px] font-medium sm:text-sm ${isCurrent ? 'neon-text' : ''}`}>
          {track.title}
        </div>
        <div className="truncate text-[11px] text-dim sm:text-xs">{track.artist}</div>
      </button>

      {dl && (
        <span className="shrink-0" title={dl.status}>
          {dl.status === 'done' && <CheckCircle2 size={15} className="text-[var(--accent)]" />}
          {(dl.status === 'downloading' || dl.status === 'extracting' || dl.status === 'pending' || dl.status === 'waiting-net') && (
            <span className="text-[10px] text-dim">{Math.round(dl.progress * 100)}%</span>
          )}
          {dl.status === 'error' && <Download size={14} className="text-red-400" />}
        </span>
      )}
      <span className="hidden w-10 shrink-0 text-end text-xs tabular-nums text-dim sm:block">{track.duration ?? fmtTime(track.durationSec)}</span>

      <button
        onClick={() => { const now = useLibrary.getState().toggleLike(track); toast(t(lang, now ? 'addedToLiked' : 'removedFromLiked')); }}
        aria-label={t(lang, 'like')}
        className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-dim transition-colors hover:bg-surface2 hover:text-foreground sm:h-9 sm:w-9"
      >
        <Heart size={17} key={String(liked)} className={`heart-pop ${liked ? 'fill-[var(--accent)] text-[var(--accent)]' : ''}`} />
      </button>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            aria-label="menu"
            className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-dim transition-colors hover:bg-surface2 hover:text-foreground sm:h-9 sm:w-9"
          >
            <MoreHorizontal size={17} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-52 border-line bg-[var(--app-bg-2)]">
          <DropdownMenuItem onClick={downloadNow} className="gap-2">
            <Download size={14} /> {t(lang, 'download')}
            {dl?.status === 'done' && <CheckCircle2 size={12} className="ms-auto text-[var(--accent)]" />}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => { usePlayer.getState().enqueue(track); toast(t(lang, 'addedToQueue')); }} className="gap-2">
            <ListPlus size={14} /> {t(lang, 'queue')}
          </DropdownMenuItem>
          <DropdownMenuSub>
            <DropdownMenuSubTrigger className="gap-2"><ListPlus size={14} /> {t(lang, 'addToPlaylist')}</DropdownMenuSubTrigger>
            <DropdownMenuSubContent className="max-h-64 overflow-auto border-line bg-[var(--app-bg-2)]">
              {playlists.length === 0 && <div className="px-3 py-2 text-xs text-dim">{t(lang, 'emptyLibrary')}</div>}
              {playlists.map((p) => (
                <DropdownMenuItem
                  key={p.id}
                  onClick={() => { useLibrary.getState().addToPlaylist(p.id, track); toast(t(lang, 'addedTo') + ': ' + p.name); }}
                >
                  {p.name}
                </DropdownMenuItem>
              ))}
            </DropdownMenuSubContent>
          </DropdownMenuSub>
          {onRemove && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={onRemove} className="gap-2 text-red-400">
                <Trash2 size={14} /> {t(lang, 'remove')}
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      </div>
    </div>
  );
}

export function EqBars({ playing }: { playing: boolean }) {
  return (
    <span className={`inline-flex h-3.5 items-end gap-[2px] ${playing ? 'eq-play' : 'opacity-60'}`}>
      <span className="eq-bar" style={{ height: '60%' }} />
      <span className="eq-bar" style={{ height: '100%' }} />
      <span className="eq-bar" style={{ height: '40%' }} />
    </span>
  );
}

export const TrackRow = memo(RowInner);
