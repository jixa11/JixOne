'use client';
import { memo } from 'react';
import { Play, Pause } from 'lucide-react';
import { Track } from '@/lib/types';
import { usePlayer } from '@/store/player';
import { TrackThumb } from '@/components/TrackThumb';

function CardInner({ track, queueContext }: { track: Track; queueContext: Track[] }) {
  const isCurrent = usePlayer((s) => s.queue[s.index]?.videoId === track.videoId);
  const isPlaying = usePlayer((s) => s.playing);
  const play = () => {
    const idx = Math.max(0, queueContext.findIndex((x) => x.videoId === track.videoId));
    if (isCurrent) usePlayer.getState().toggle();
    else usePlayer.getState().setQueue(queueContext, idx);
  };
  return (
    <div className="group card-hover w-full cursor-pointer rounded-2xl bg-surface p-3" onClick={play}>
      <div className="relative mb-3 aspect-square w-full overflow-hidden rounded-xl shadow-lg">
        <TrackThumb
          track={track}
          rounded="rounded-xl"
          className="h-full w-full transition-transform duration-500 ease-out group-hover:scale-[1.07]"
        />
        <span
          className={`neon-play absolute bottom-2 left-2 grid h-10 w-10 place-items-center rounded-full transition-all duration-200 ${
            isCurrent ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100'
          }`}
        >
          {isCurrent && isPlaying ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" className="ms-0.5" />}
        </span>
      </div>
      <div className={`truncate text-[13.5px] font-semibold leading-snug ${isCurrent ? 'neon-text' : ''}`}>{track.title}</div>
      <div className="mt-0.5 truncate text-xs text-dim">{track.artist}</div>
    </div>
  );
}
export const TrackCard = memo(CardInner);
