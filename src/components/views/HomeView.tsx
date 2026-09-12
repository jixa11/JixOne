'use client';
import { useEffect, useState } from 'react';
import { Track } from '@/lib/types';
import { TrackCard } from '@/components/TrackCard';
import { TrackThumb } from '@/components/TrackThumb';
import { usePlayer } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useLibrary } from '@/store/library';
import { useView } from '@/store/view';
import { t, greetKey, num, type DictKey } from '@/lib/i18n';
import { homeTracks } from '@/engine/ytclient';
import { CURATED_TRACKS, POPULAR_ARTISTS } from '@/lib/catalog';
import { Skeleton } from '@/components/ui/skeleton';
import { Play, Shuffle, Sparkles, WifiOff, RefreshCw } from 'lucide-react';

const MOODS = [
  { id: 'pop', label: { fa: 'پاپ جهانی', en: 'Global Pop' } },
  { id: 'hiphop', label: { fa: 'هیپ‌هاپ', en: 'Hip-Hop' } },
  { id: 'electronic', label: { fa: 'الکترونیک', en: 'Electronic' } },
  { id: 'rock', label: { fa: 'راک', en: 'Rock' } },
  { id: 'lofi', label: { fa: 'لو-فای', en: 'Lo-Fi' } },
  { id: 'ambient', label: { fa: 'آرامش', en: 'Ambient' } },
  { id: 'iran_now', label: { fa: 'داغ ایران', en: 'Persian now' } },
  { id: 'classic', label: { fa: 'کلاسیک', en: 'Classics' } },
];

export default function HomeView() {
  const lang = useSettings((s) => s.lang);
  const liked = useLibrary((s) => s.liked);
  const history = useLibrary((s) => s.history);
  const [mood, setMood] = useState('pop');
  const [tracks, setTracks] = useState<Track[] | null>(null);
  const [curated, setCurated] = useState(false);
  const [reloadTick, setReloadTick] = useState(0);
  // hydration-safe greeting: stable on first render (server+client match),
  // real time-based value applied only after mount using the device clock
  const [greet, setGreet] = useState<DictKey>('goodNight');

  useEffect(() => {
    setGreet(greetKey());
  }, []);

  useEffect(() => {
    let dead = false;
    const id = setTimeout(() => setTracks(null), 0);
    // device-side search (primary) → server fallback → curated offline catalog
    homeTracks(mood)
      .catch(() => fetch(`/api/yt/home?mood=${mood}`).then((r) => r.json()).then((d) => d.tracks ?? []))
      .then((t2) => {
        if (dead) return;
        if (t2 && t2.length > 0) { setTracks(t2); setCurated(false); }
        else { setTracks(CURATED_TRACKS); setCurated(true); }
      })
      .catch(() => { if (!dead) { setTracks(CURATED_TRACKS); setCurated(true); } });
    return () => { dead = true; clearTimeout(id); };
  }, [mood, reloadTick]);

  const playAll = (shuffle = false) => {
    if (!tracks?.length) return;
    usePlayer.getState().setQueue(tracks, shuffle ? Math.floor(Math.random() * tracks.length) : 0, { kind: 'home' });
  };

  return (
    <div className="view-in mx-auto max-w-[1280px] px-4 pb-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="flex items-center gap-2 text-[26px] font-black leading-tight tracking-tight sm:text-[32px]">
            {t(lang, greet)}
            <Sparkles size={22} className="text-[var(--accent)] drop-shadow-[var(--glow-soft)]" />
          </h1>
          <p className="mt-1 text-[12.5px] text-dim">{t(lang, 'quickPicks')} · {t(lang, 'trending')}</p>
        </div>
        {tracks?.length ? (
          <div className="flex gap-2">
            <button onClick={() => playAll(false)} className="neon-play flex items-center gap-2 rounded-full px-5 py-2.5 text-sm font-bold transition-transform hover:scale-[1.03] active:scale-95">
              <Play size={15} fill="currentColor" /> {t(lang, 'playAll')}
            </button>
            <button onClick={() => playAll(true)} className="flex items-center gap-2 rounded-full border border-line bg-surface px-5 py-2.5 text-sm transition-colors hover:bg-surface2">
              <Shuffle size={15} /> {t(lang, 'shufflePlay')}
            </button>
          </div>
        ) : null}
      </div>

      {/* quick access */}
      <div className="stagger mb-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        <button onClick={() => useView.getState().push('liked')} className="card-hover flex min-h-[64px] cursor-pointer items-center gap-3 overflow-hidden rounded-xl bg-surface text-start">
          <span className="neon-play grid h-[64px] w-[64px] shrink-0 place-items-center"><Play size={20} fill="currentColor" /></span>
          <span className="min-w-0 pe-2">
            <span className="line-clamp-2 text-[13px] font-bold leading-snug">{t(lang, 'likedSongs')}</span>
            <span className="text-[11px] text-dim">{num(liked.length, lang)} {t(lang, 'songs')}</span>
          </span>
        </button>
        {history.slice(0, 3).map((h) => (
          <button
            key={h.videoId}
            onClick={() => usePlayer.getState().setQueue([h], 0)}
            className="card-hover flex min-h-[64px] cursor-pointer items-center gap-3 overflow-hidden rounded-xl bg-surface text-start"
          >
            <TrackThumb track={h} rounded="" className="h-[64px] w-[64px] shrink-0" />
            <span className="min-w-0 pe-2">
              <span className="line-clamp-2 text-[13px] font-bold leading-snug">{h.title}</span>
              <span className="line-clamp-1 text-[11px] text-dim">{h.artist}</span>
            </span>
          </button>
        ))}
      </div>

      {/* popular artists (international) */}
      <div className="mb-6">
        <h2 className="mb-3 px-1 text-base font-black tracking-tight">{t(lang, 'popularArtists')}</h2>
        <div className="stagger flex gap-4 overflow-x-auto pb-2 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {POPULAR_ARTISTS.map((a) => (
            <button
              key={a.name}
              onClick={() => { useView.getState().setSearchSeed(a.query); useView.getState().push('search'); }}
              className="group flex w-[84px] shrink-0 flex-col items-center gap-2"
              title={a.name}
            >
              <span
                className="grid h-[76px] w-[76px] place-items-center rounded-full text-xl font-black text-white/95 shadow-lg transition-transform duration-300 group-hover:scale-[1.07]"
                style={{ background: `linear-gradient(135deg, ${a.hue[0]}, ${a.hue[1]})` }}
              >
                {a.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}
              </span>
              <span className="w-full truncate text-center text-[11.5px] text-dim transition-colors group-hover:text-foreground">{a.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* mood chips */}
      <div className="mb-5 flex flex-wrap gap-2">
        {MOODS.map((m) => (
          <button
            key={m.id}
            onClick={() => setMood(m.id)}
            className={`rounded-full border px-4 py-2 text-[12.5px] transition-all duration-150 hover:scale-[1.03] ${
              mood === m.id ? 'border-[var(--accent)] bg-surface2 font-bold text-[var(--accent)] neon-ring' : 'border-line bg-surface text-dim hover:text-foreground'
            }`}
          >
            {m.label[lang]}
          </button>
        ))}
      </div>

      {/* section title */}
      {tracks && tracks.length > 0 && (
        <div className="mb-3 flex items-baseline gap-2.5 px-1">
          <h2 className="text-lg font-black tracking-tight">{MOODS.find((m) => m.id === mood)?.label[lang]}</h2>
          <span className="text-[11px] text-dim">{num(tracks.length, lang)} {t(lang, 'songs')}</span>
          {curated && <span className="rounded-full bg-surface2 px-2.5 py-0.5 text-[10px] text-dim">{t(lang, 'curatedBadge')}</span>}
        </div>
      )}

      {/* track grid */}
      {tracks === null ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="rounded-2xl bg-surface p-3">
              <Skeleton className="mb-3 aspect-square w-full rounded-xl" />
              <Skeleton className="mb-1 h-3.5 w-3/4 rounded" />
              <Skeleton className="h-3 w-1/2 rounded" />
            </div>
          ))}
        </div>
      ) : (
        <div className="stagger grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">
          {tracks.map((tr) => <TrackCard key={tr.videoId} track={tr} queueContext={tracks} />)}
        </div>
      )}
    </div>
  );
}
