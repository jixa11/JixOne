'use client';
import { X, Play, Pause, ListX } from 'lucide-react';
import { usePlayer } from '@/store/player';
import { EqBars } from '@/components/TrackRow';
import { TrackThumb } from '@/components/TrackThumb';
import { t, fmtTime } from '@/lib/i18n';
import { useSettings } from '@/store/settings';
import { Button } from '@/components/ui/button';

export default function QueuePanel() {
  const open = usePlayer((s) => s.queueOpen);
  const p = usePlayer();
  const lang = useSettings((s) => s.lang);
  if (!open) return null;

  return (
    <div className="glass fixed bottom-[84px] end-3 top-16 z-40 flex w-[340px] max-w-[92vw] flex-col rounded-2xl border border-line shadow-2xl view-in">
      <div className="flex items-center justify-between border-b border-line px-4 py-3">
        <div>
          <div className="text-sm font-bold">{t(lang, 'queue')}</div>
          <div className="text-[11px] text-dim">{p.queue.length} {t(lang, 'songs')}</div>
        </div>
        <div className="flex gap-1">
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => usePlayer.setState({ queue: [], index: -1, playing: false, engine: 'pending' })} aria-label="clear">
            <ListX size={16} className="text-dim" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={p.toggleQueue} aria-label="close">
            <X size={16} />
          </Button>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto p-2">
        {p.queue.map((tr, i) => {
          const isCur = i === p.index;
          return (
            <div
              key={`${tr.videoId}-${i}`}
              onClick={() => p.jumpTo(i)}
              className={`flex cursor-pointer items-center gap-3 rounded-xl p-2 ${isCur ? 'bg-surface2' : 'hover:bg-surface2'}`}
            >
              <TrackThumb track={tr} rounded="rounded-md" className="h-9 w-9 shrink-0" />
              <div className="min-w-0 flex-1">
                <div className={`truncate text-xs font-semibold ${isCur ? 'neon-text' : ''}`}>{tr.title}</div>
                <div className="truncate text-[10px] text-dim">{tr.artist}</div>
              </div>
              {isCur ? <EqBars playing={p.playing} /> : <span className="text-[10px] tabular-nums text-dim">{tr.duration ?? fmtTime(tr.durationSec)}</span>}
              {isCur && (
                <button onClick={(e) => { e.stopPropagation(); p.toggle(); }} className="text-dim" aria-label="toggle">
                  {p.playing ? <Pause size={13} /> : <Play size={13} />}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
