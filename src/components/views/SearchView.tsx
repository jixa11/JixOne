'use client';
import { useEffect, useRef, useState } from 'react';
import { Track } from '@/lib/types';
import { TrackRow } from '@/components/TrackRow';
import { usePlayer } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useView } from '@/store/view';
import { t } from '@/lib/i18n';
import { searchTracks } from '@/engine/ytclient';
import { readCache, writeCache, cacheKey } from '@/engine/fastCache';
import { Skeleton } from '@/components/ui/skeleton';
import { Search as SearchIcon, Play, SearchX, Music4, CloudOff, RefreshCw } from 'lucide-react';

export default function SearchView() {
  const lang = useSettings((s) => s.lang);
  const seed = useView((s) => s.searchSeed);
  const clearSeed = useView((s) => s.setSearchSeed);
  const [q, setQ] = useState(seed ?? '');
  const [tracks, setTracks] = useState<Track[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const [tick, setTick] = useState(0);
  const debounce = useRef<any>(null);

  // consume artist seed once
  useEffect(() => {
    if (seed) { setQ(seed); clearSeed(''); }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    clearTimeout(debounce.current);
    if (!q.trim()) {
      const id = setTimeout(() => { setTracks(null); setLoading(false); setFailed(false); }, 0);
      return () => clearTimeout(id);
    }
    // instant cached results while the fresh search runs (stale-while-revalidate)
    const cached = readCache<Track[]>(cacheKey('search', q.trim().toLowerCase()));
    if (cached && Array.isArray(cached) && cached.length > 0) {
      setTracks(cached);
      setFailed(false);
    }
    debounce.current = setTimeout(() => {
      setLoading(true);
      // device-side search (primary) → helper-server fallback (searchTracks handles both)
      searchTracks(q)
        .then((t2) => {
          const list = (t2 ?? []) as Track[];
          setTracks(list);
          setFailed(false);
          setLoading(false);
          if (list.length > 0) writeCache(cacheKey('search', q.trim().toLowerCase()), list);
        })
        .catch(() => {
          // keep cached rows visible if we have them; otherwise show the error state
          if (!cached || cached.length === 0) setTracks([]);
          setFailed(true);
          setLoading(false);
        });
    }, 450);
    return () => clearTimeout(debounce.current);
  }, [q, tick]);

  const retry = () => { setFailed(false); setLoading(true); setTick((x) => x + 1); };

  return (
    <div className="view-in mx-auto max-w-[900px] px-4 pb-8">
      <div className="sticky top-0 z-10 -mx-4 mb-4 bg-[var(--app-bg)]/80 px-4 pb-3 pt-1 backdrop-blur">
        <div className="flex items-center gap-3 rounded-2xl border border-line bg-surface px-4 py-3 transition-all duration-200 focus-within:border-[color-mix(in_srgb,var(--accent)_55%,transparent)] focus-within:shadow-[var(--glow-soft)]">
          <SearchIcon size={18} className="text-dim" />
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={t(lang, 'searchPlaceholder')}
            className="w-full bg-transparent text-sm outline-none placeholder:text-dim"
            aria-label={t(lang, 'search')}
          />
          {loading && <span className="h-4 w-4 animate-spin rounded-full border-2 border-dim/40 border-t-[var(--accent)]" />}
        </div>
      </div>

      {tracks === null ? (
        <div className="flex flex-col items-center gap-3 py-24 text-center">
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-surface2 text-dim">
            <Music4 size={24} />
          </span>
          <p className="max-w-[320px] text-[13px] leading-relaxed text-dim">{t(lang, 'searchHint')}</p>
        </div>
      ) : loading && tracks.length === 0 ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3 rounded-xl p-2">
              <Skeleton className="h-10 w-10 rounded-lg" />
              <div className="flex-1 space-y-1.5">
                <Skeleton className="h-3.5 w-2/5 rounded" />
                <Skeleton className="h-3 w-1/4 rounded" />
              </div>
            </div>
          ))}
        </div>
      ) : tracks.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-24 text-center">
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-surface2 text-dim">
            <SearchX size={24} />
          </span>
          <div className="max-w-[340px] space-y-1">
            <div className="text-sm font-bold">{t(lang, 'noResults')}</div>
            <div className="text-xs leading-relaxed text-dim">{t(lang, 'checkConn')}</div>
          </div>
        </div>
      ) : (
        <>
          {failed && (
            <div className="mb-3 flex items-center justify-between gap-3 rounded-xl border border-[var(--warning)]/40 bg-[var(--warning)]/10 px-4 py-2.5">
              <div className="flex min-w-0 items-center gap-2 text-[11.5px] leading-relaxed">
                <CloudOff size={15} className="shrink-0 text-[var(--warning)]" />
                <span className="text-dim">{t(lang, 'searchStale')}</span>
              </div>
              <button
                onClick={retry}
                className="flex shrink-0 items-center gap-1.5 rounded-full border border-line bg-surface px-3.5 py-1.5 text-[10.5px] font-semibold text-dim transition-colors hover:bg-surface2 hover:text-foreground"
              >
                <RefreshCw size={12} /> {t(lang, 'retry')}
              </button>
            </div>
          )}
          {tracks.length > 1 && (
            <button
              onClick={() => usePlayer.getState().setQueue(tracks, 0, { kind: 'search' })}
              className="neon-play mb-4 flex items-center gap-2 rounded-full px-5 py-2.5 text-sm font-bold transition-transform hover:scale-[1.03] active:scale-95"
            >
              <Play size={14} fill="currentColor" /> {t(lang, 'playAll')}
            </button>
          )}
          <div className="stagger flex flex-col gap-1">
            {tracks.map((tr, i) => (
              <TrackRow key={tr.videoId} track={tr} i={i} queueContext={tracks} />
            ))}
          </div>
        </>
      )}

      {!loading && failed && tracks.length === 0 && (
        <div className="mt-4 flex flex-col items-center gap-3 text-center">
          <div className="max-w-[360px] rounded-2xl border border-line bg-surface p-4 text-start">
            <div className="mb-1 text-[13px] font-bold">{t(lang, 'searchFailTitle')}</div>
            <div className="text-[11.5px] leading-relaxed text-dim">{t(lang, 'searchFailDesc')}</div>
          </div>
          <button
            onClick={retry}
            className="neon-play flex items-center gap-2 rounded-full px-5 py-2.5 text-[13px] font-bold transition-transform active:scale-95"
          >
            <RefreshCw size={14} /> {t(lang, 'retry')}
          </button>
        </div>
      )}
    </div>
  );
}
