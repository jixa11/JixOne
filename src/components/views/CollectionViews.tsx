'use client';
import { useLibrary } from '@/store/library';
import { useSettings } from '@/store/settings';
import { useView } from '@/store/view';
import { TrackRow } from '@/components/TrackRow';
import { t, num } from '@/lib/i18n';
import { Heart, Clock3, Search } from 'lucide-react';

function EmptyState({ icon: Icon, message, actionLabel, onAction }: { icon: any; message: string; actionLabel?: string; onAction?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-2xl border border-line bg-surface py-14 text-center">
      <span className="grid h-14 w-14 place-items-center rounded-2xl bg-surface2 text-dim">
        <Icon size={24} />
      </span>
      <p className="max-w-[320px] text-[13px] leading-relaxed text-dim">{message}</p>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="flex items-center gap-2 rounded-full border border-line bg-surface2 px-5 py-2.5 text-xs font-bold transition-colors hover:bg-surface-hover"
        >
          <Search size={14} /> {actionLabel}
        </button>
      )}
    </div>
  );
}

export function LikedView() {
  const lang = useSettings((s) => s.lang);
  const liked = useLibrary((s) => s.liked);
  return (
    <div className="view-in mx-auto max-w-[900px] px-4 pb-8">
      <div className="mb-6 flex items-center gap-4">
        <span className="neon-play grid h-16 w-16 place-items-center rounded-2xl shadow-[var(--glow)]"><Heart size={26} fill="currentColor" /></span>
        <div>
          <h1 className="text-2xl font-black tracking-tight sm:text-3xl">{t(lang, 'likedSongs')}</h1>
          <div className="mt-0.5 text-xs text-dim">{num(liked.length, lang)} {t(lang, 'songs')}</div>
        </div>
      </div>
      {liked.length === 0 ? (
        <EmptyState
          icon={Heart}
          message={t(lang, 'emptyLiked')}
          actionLabel={t(lang, 'goSearch')}
          onAction={() => useView.getState().push('search')}
        />
      ) : (
        <div className="flex flex-col gap-1">
          {liked.map((tr, i) => <TrackRow key={tr.videoId} track={tr} i={i} queueContext={liked} />)}
        </div>
      )}
    </div>
  );
}

export function HistoryView() {
  const lang = useSettings((s) => s.lang);
  const history = useLibrary((s) => s.history);
  return (
    <div className="view-in mx-auto max-w-[900px] px-4 pb-8">
      <div className="mb-6 flex items-center gap-4">
        <span className="grid h-16 w-16 place-items-center rounded-2xl bg-surface2 text-dim"><Clock3 size={24} /></span>
        <div>
          <h1 className="text-2xl font-black tracking-tight sm:text-3xl">{t(lang, 'recentPlayed')}</h1>
          <div className="mt-0.5 text-xs text-dim">{num(history.length, lang)} {t(lang, 'songs')}</div>
        </div>
      </div>
      {history.length === 0 ? (
        <EmptyState
          icon={Clock3}
          message={t(lang, 'emptyHistory')}
          actionLabel={t(lang, 'goSearch')}
          onAction={() => useView.getState().push('search')}
        />
      ) : (
        <div className="flex flex-col gap-1">
          {history.map((tr, i) => <TrackRow key={tr.videoId} track={tr} i={i} queueContext={history} />)}
        </div>
      )}
    </div>
  );
}
