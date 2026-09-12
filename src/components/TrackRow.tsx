'use client';
import { memo } from 'react';
import { Play, Heart, Download, MoreHorizontal, ListPlus, Trash2, CheckCircle2, Loader2, Pause } from 'lucide-react';
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

  return (
    <div
      onDoubleClick={play}
      className={`group flex items-center gap-2 rounded-xl px-2 py-2 transition-colors sm:gap-3 sm:px-3 sm:py-1.5 ${isCurrent ? 'bg-surface2' : 'hover:bg-surface2'}`}
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
