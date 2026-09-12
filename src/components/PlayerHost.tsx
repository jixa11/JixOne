'use client';
import { useEffect, useRef } from 'react';
import { usePlayer, setController, getController, EngineController } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useDownloads } from '@/store/downloads';
import { useLibrary } from '@/store/library';
import { YTController, AudioController, SyncPlayer } from '@/engine/players';
import { extractStream } from '@/engine/extractor';
import { getLocalFileURL } from '@/engine/downloader';
import { toast } from 'sonner';

function bridge(): Window['AndroidBridge'] | undefined {
  return typeof window !== 'undefined' ? window.AndroidBridge : undefined;
}

export default function PlayerHost() {
  const playerRef = useRef<YTController | null>(null);
  const audioRef = useRef<AudioController | null>(null);
  const activeKind = useRef<'yt' | 'audio'>('audio');
  const abortRef = useRef<AbortController | null>(null);
  const videoDivRef = useRef<HTMLDivElement>(null);
  const ytMountRef = useRef<HTMLDivElement | null>(null);
  const syncMountRef = useRef<HTMLDivElement>(null);
  const syncRef = useRef<SyncPlayer | null>(null);

  const queue = usePlayer((s) => s.queue);
  const index = usePlayer((s) => s.index);
  const playing = usePlayer((s) => s.playing);
  const engine = usePlayer((s) => s.engine);
  const videoMode = usePlayer((s) => s.videoMode);
  const volume = useSettings((s) => s.volume);

  const track = queue[index];

  // ---- init controllers ----
  useEffect(() => {
    const hooks = {
      onPosition: (sec: number) => {
        usePlayer.getState().setPosition(sec);
        try { navigator.mediaSession?.setPositionState?.({ position: sec, duration: usePlayer.getState().duration || sec, playbackRate: 1 }); } catch { /* noop */ }
        bridge()?.updateMedia(JSON.stringify({ pos: Math.round(sec) }));
      },
      onDuration: (sec: number) => usePlayer.getState().setDuration(sec),
      onPlayState: (b: boolean) => usePlayer.getState().setPlaying(b),
      onEnded: () => usePlayer.getState().next(true),
      onError: (msg: string) => {
        usePlayer.getState().setLoading(false);
        if (msg === 'PLAY_BLOCKED') {
          // autoplay policy — wait for real user gesture; don't switch engines
          usePlayer.getState().setPlaying(false);
          return;
        }
        handleEngineError(msg);
      },
    };

    const makeYT = () => {
      if (!playerRef.current && videoDivRef.current) {
        ytMountRef.current = document.createElement('div');
        videoDivRef.current.appendChild(ytMountRef.current);
        playerRef.current = new YTController(ytMountRef.current, hooks);
      }
      return playerRef.current!;
    };

    const audioCtl: EngineController = {
      load: async (t, autoplay) => {
        if (!audioRef.current) audioRef.current = new AudioController(hooks);
        activeKind.current = 'audio';
        const dl = useDownloads.getState().items[t.videoId];
        if (dl?.status === 'done') {
          const f = await getLocalFileURL(t.videoId);
          if (f) { audioRef.current.load(f.url, autoplay); audioRef.current.setVolume(useSettings.getState().volume); return true; }
        }
        setControllerLoading(true);
        try {
          abortRef.current?.abort();
          abortRef.current = new AbortController();
          const s = await extractStream(t.videoId, 'high');
          audioRef.current.load(s.url, autoplay);
          audioRef.current.setVolume(useSettings.getState().volume);
          if (s.durationSec) usePlayer.getState().setDuration(s.durationSec);
          return true;
        } catch (e: any) {
          if (e?.name === 'AbortError') return false;
          return false;
        } finally {
          setControllerLoading(false);
        }
      },
      play: () => audioRef.current?.play(),
      pause: () => audioRef.current?.pause(),
      seek: (sec) => audioRef.current?.seek(sec),
      setVolume: (v) => audioRef.current?.setVolume(v),
      stop: () => audioRef.current?.stop(),
    };
    const ytCtl: EngineController = {
      load: async (t, autoplay) => {
        const c = makeYT();
        activeKind.current = 'yt';
        return c.load(t.videoId, autoplay);
      },
      play: () => playerRef.current?.play(),
      pause: () => playerRef.current?.pause(),
      seek: (sec) => playerRef.current?.seek(sec),
      setVolume: (v) => playerRef.current?.setVolume(v),
      stop: () => playerRef.current?.stop(),
    };

    // dispatching controller that routes by current engine kind
    const dispatch: EngineController = {
      load: async (t, autoplay) => {
        const kind = usePlayer.getState().engine;
        if (kind === 'local' || kind === 'custom') return audioCtl.load(t, autoplay);
        if (kind === 'yt') return ytCtl.load(t, autoplay);
        return false;
      },
      play: () => (activeKind.current === 'yt' ? ytCtl.play() : audioCtl.play()),
      pause: () => (activeKind.current === 'yt' ? ytCtl.pause() : audioCtl.pause()),
      seek: (sec) => (activeKind.current === 'yt' ? ytCtl.seek(sec) : audioCtl.seek(sec)),
      setVolume: (v) => (activeKind.current === 'yt' ? ytCtl.setVolume(v) : audioCtl.setVolume(v)),
      stop: () => { ytCtl.stop(); audioCtl.stop(); },
    };
    setController(dispatch);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setControllerLoading = (b: boolean) => usePlayer.getState().setLoading(b);

  // ---- engine error handling with fallback ----
  const handleEngineError = (msg: string) => {
    const st = usePlayer.getState();
    const mode = useSettings.getState().playbackMode;
    if (st.engine === 'custom' && (mode === 'auto' || msg === 'AUDIO_ERROR') && mode !== 'adfree') {
      toast(t_fa('adfreeFail'));
      usePlayer.getState().setEngine('yt');
      usePlayer.getState().setLoading(true);
    } else if (st.engine === 'custom' && mode === 'adfree') {
      toast(t_fa('extractFail'));
      usePlayer.getState().setPlaying(false);
    } else if (msg === 'AUDIO_ERROR' && st.engine === 'local') {
      toast(t_fa('extractFail'));
    }
  };

  // ---- engine selection when track/engine changes ----
  const lastLoadRef = useRef('');
  const fallbackDoneRef = useRef('');
  useEffect(() => {
    if (!track) return;
    const key = `${track.videoId}:${usePlayer.getState().engine}`;
    if (lastLoadRef.current === key && usePlayer.getState().loading) return; // anti-loop guard
    let cancelled = false;
    (async () => {
      const st = usePlayer.getState();
      const mode = useSettings.getState().playbackMode;
      const dl = useDownloads.getState().items[track.videoId];

      let kind: 'local' | 'custom' | 'yt' = 'yt';
      if (dl?.status === 'done') kind = 'local';
      else if (mode === 'official') kind = 'yt';
      else kind = 'custom'; // auto & adfree both try adfree first; auto falls back on error

      lastLoadRef.current = `${track.videoId}:${kind}`;
      st.setEngine(kind);
      st.setLoading(true);
      const ok = await getControllerSafe(kind, track);
      if (cancelled) return;
      if (!ok && kind === 'custom' && mode === 'auto' && fallbackDoneRef.current !== track.videoId) {
        fallbackDoneRef.current = track.videoId;
        toast(t_fa('adfreeFail'));
        lastLoadRef.current = `${track.videoId}:yt`;
        usePlayer.getState().setEngine('yt');
        const ok2 = await getControllerSafe('yt', track);
        if (!ok2 && !cancelled) { usePlayer.getState().setLoading(false); usePlayer.getState().setPlaying(false); }
      } else if (!ok && !cancelled) {
        usePlayer.getState().setLoading(false);
        usePlayer.getState().setPlaying(false);
        if (kind === 'custom' && useSettings.getState().playbackMode === 'adfree') toast(t_fa('extractFail'));
      }
      if (ok) usePlayer.getState().setLoading(false);
      else if (!cancelled) {
        usePlayer.getState().setLoading(false);
        usePlayer.getState().setPlaying(false);
        if (kind === 'custom' && mode === 'auto' && fallbackDoneRef.current === track.videoId) {
          // already tried fallback
        }
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [track?.videoId, engine]);

  const getControllerSafe = async (kind: string, t: NonNullable<typeof track>) => {
    const autoplay = true;
    const ctl = getController();
    if (!ctl) return false;
    // route directly to right controller
    const st = { ...usePlayer.getState(), engine: kind as any };
    void st;
    if (kind === 'yt') {
      if (!playerRef.current && videoDivRef.current) {
        ytMountRef.current = document.createElement('div');
        videoDivRef.current.appendChild(ytMountRef.current);
        playerRef.current = new YTController(ytMountRef.current, makeHooks());
      }
      activeKind.current = 'yt';
      const r = await playerRef.current!.load(t.videoId, autoplay);
      if (r) usePlayer.getState().setLoading(false);
      return r;
    }
    // audio path
    if (!audioRef.current) audioRef.current = new AudioController(makeHooks());
    activeKind.current = 'audio';
    const dl = useDownloads.getState().items[t.videoId];
    if (dl?.status === 'done') {
      const f = await getLocalFileURL(t.videoId);
      if (f) { audioRef.current.load(f.url, autoplay); audioRef.current.setVolume(volume); usePlayer.getState().setLoading(false); return true; }
    }
    try {
      abortRef.current?.abort();
      abortRef.current = new AbortController();
      const s = await extractStream(t.videoId, 'high');
      if (s.durationSec) usePlayer.getState().setDuration(s.durationSec);
      audioRef.current.load(s.url, autoplay);
      audioRef.current.setVolume(volume);
      usePlayer.getState().setLoading(false);
      return true;
    } catch {
      return false;
    }
  };

  const hooksRef = useRef({} as ReturnType<typeof makeHooks>);
  function makeHooks() {
    if (!hooksRef.current.onPosition) {
      hooksRef.current = {
        onPosition: (sec: number) => {
          usePlayer.getState().setPosition(sec);
          try { navigator.mediaSession?.setPositionState?.({ position: sec, duration: usePlayer.getState().duration || sec, playbackRate: 1 }); } catch { /* noop */ }
          bridge()?.updateMedia(JSON.stringify({ pos: Math.round(sec) }));
        },
        onDuration: (sec: number) => usePlayer.getState().setDuration(sec),
        onPlayState: (b: boolean) => usePlayer.getState().setPlaying(b),
        onEnded: () => usePlayer.getState().next(true),
        onError: () => handleEngineError('AUDIO_ERROR'),
      };
    }
    return hooksRef.current;
  }

  // ---- play/pause reaction ----
  useEffect(() => {
    const ctl = getController();
    if (!ctl || engine === 'pending' || !track) return;
    if (playing) ctl.play();
    else ctl.pause();
    bridge()?.setPlaying(playing);
    try { if (navigator.mediaSession) navigator.mediaSession.playbackState = playing ? 'playing' : 'paused'; } catch { /* noop */ }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playing, engine, track?.videoId]);

  // ---- volume ----
  useEffect(() => { getController()?.setVolume(volume); }, [volume]);

  // ---- history + offline play record + mediaSession + native bridge ----
  useEffect(() => {
    if (!track) return;
    useLibrary.getState().pushHistory(track);
    if (useDownloads.getState().items[track.videoId]?.status === 'done') {
      useLibrary.getState().pushOfflinePlay(track.videoId);
    }
    try {
      if (navigator.mediaSession) {
        navigator.mediaSession.metadata = new MediaMetadata({
          title: track.title,
          artist: track.artist,
          album: 'JixOne',
          artwork: [{ src: `/api/art?u=${encodeURIComponent(track.thumb)}`, sizes: '512x512', type: 'image/jpeg' }],
        });
      }
      navigator.mediaSession?.setActionHandler?.('play', () => usePlayer.getState().explicitPlay());
      navigator.mediaSession?.setActionHandler?.('pause', () => usePlayer.getState().explicitPause());
      navigator.mediaSession?.setActionHandler?.('previoustrack', () => usePlayer.getState().prev());
      navigator.mediaSession?.setActionHandler?.('nexttrack', () => usePlayer.getState().next());
      navigator.mediaSession?.setActionHandler?.('seekto', (d: any) => { if (d.seekTime != null) usePlayer.getState().seek(d.seekTime); });
    } catch { /* noop */ }
    bridge()?.updateMedia(JSON.stringify({
      title: track.title, artist: track.artist, artwork: `/api/art?u=${encodeURIComponent(track.thumb)}`,
      duration: usePlayer.getState().duration, videoId: track.videoId,
    }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [track?.videoId, engine]);
  useEffect(() => {
    window.NativeAction = {
      play: () => usePlayer.getState().explicitPlay(),
      pause: () => usePlayer.getState().explicitPause(),
      next: () => usePlayer.getState().next(),
      prev: () => usePlayer.getState().prev(),
      seek: (sec: number) => usePlayer.getState().seek(sec),
    };
  }, []);

  // ---- play-sync: register offline plays when online (optional feature) ----
  useEffect(() => {
    if (!syncRef.current) {
      syncRef.current = new SyncPlayer((videoId, ok) => {
        if (ok) {
          const lib = useLibrary.getState();
          lib.offlinePlays; // touch
          useLibrary.setState((s) => ({ offlinePlays: s.offlinePlays.filter((p) => p.videoId !== videoId) }));
        }
        scheduleNextSync();
      });
    }
    function scheduleNextSync() {
      const lib = useLibrary.getState();
      const s = useSettings.getState();
      if (!s.playSync || !navigator.onLine) return;
      const next = lib.offlinePlays[0];
      if (next) {
        setTimeout(() => syncRef.current?.syncOne(next.videoId), 1500);
      }
    }
    const onOnline = () => scheduleNextSync();
    window.addEventListener('online', onOnline);
    const iv = setInterval(scheduleNextSync, 60000);
    return () => { window.removeEventListener('online', onOnline); clearInterval(iv); };
  }, []);

  const miniVisible = videoMode !== 'hidden';
  return (
    <>
      {/* official YT player host — mini chip above player bar (collapsible to theater) */}
      <div
        ref={videoDivRef}
        className={videoMode === 'theater'
          ? 'fixed inset-0 z-[60] bg-black'
          : miniVisible
            ? 'fixed bottom-[92px] left-3 z-40 h-[92px] w-[164px] overflow-hidden rounded-xl border border-line shadow-2xl'
            : 'pointer-events-none fixed -bottom-[2000px] left-0 h-[180px] w-[320px] opacity-0'}
        aria-label="video-player-host"
      >
        {videoMode === 'theater' && (
          <button
            onClick={() => usePlayer.getState().setVideoMode('mini')}
            className="absolute left-3 top-3 z-50 rounded-full bg-black/60 px-3 py-1.5 text-xs text-white"
          >
            ✕
          </button>
        )}
      </div>
      {/* hidden sync player host */}
      <div ref={syncMountRef} className="pointer-events-none fixed -left-[3000px] top-0 h-[120px] w-[214px] opacity-0" aria-hidden />
    </>
  );
}

function t_fa(key: string): string {
  // tiny helper to avoid circular import with i18n in error paths
  const map: Record<string, string> = {
    adfreeFail: 'موتور بدون تبلیغ در دسترس نیست — پلیر رسمی جایگزین شد',
    extractFail: 'استخراج ناموفق بود — VPN روشن است؟',
  };
  return map[key] ?? key;
}
