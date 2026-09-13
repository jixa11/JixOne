'use client';
import { useEffect, useRef } from 'react';
import { usePlayer, setController, getController, EngineController } from '@/store/player';
import { useSettings } from '@/store/settings';
import { useDownloads } from '@/store/downloads';
import { useLibrary } from '@/store/library';
import { YTController, AudioController, SyncPlayer } from '@/engine/players';
import { extractStream, extractCodeOf } from '@/engine/extractor';
import { getLocalFileURL } from '@/engine/downloader';
import { t } from '@/lib/i18n';
import { toast } from 'sonner';

function bridge(): Window['AndroidBridge'] | undefined {
  return typeof window !== 'undefined' ? window.AndroidBridge : undefined;
}

/** YouTube's bot gate is the one failure the user can actually fix — name it. */
function signInHint(code: ReturnType<typeof extractCodeOf>): 'needLogin' | 'loginStaleToast' | null {
  if (code === 'LOGIN_REQUIRED') return 'needLogin';
  if (code === 'LOGIN_STALE') return 'loginStaleToast';
  return null;
}

/** dedupe toasts — the old code re-toasted on every retry (user: «رگباری میاد پشت سر هم») */
const lastToastAt: Record<string, number> = {};
function toastOnce(key: string, msg: string) {
  const now = Date.now();
  if (now - (lastToastAt[key] ?? 0) < 6000) return;
  lastToastAt[key] = now;
  toast(msg);
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

  // ---- toast helpers (lang-aware) ----
  const msg = (key: Parameters<typeof t>[1]) => t(useSettings.getState().lang, key);

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
      onError: (m: string) => {
        usePlayer.getState().setLoading(false);
        if (m === 'PLAY_BLOCKED') {
          // autoplay policy — wait for a real user gesture; don't switch engines
          usePlayer.getState().setPlaying(false);
          return;
        }
        handleEngineError(m);
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
      load: async (t2, autoplay) => {
        if (!audioRef.current) audioRef.current = new AudioController(hooks);
        activeKind.current = 'audio';
        const dl = useDownloads.getState().items[t2.videoId];
        if (dl?.status === 'done') {
          const f = await getLocalFileURL(t2.videoId);
          if (f) { audioRef.current.load(f.url, autoplay); audioRef.current.setVolume(useSettings.getState().volume); return true; }
        }
        try {
          abortRef.current?.abort();
          abortRef.current = new AbortController();
          const s = await extractStream(t2.videoId, 'high');
          if (s.durationSec) usePlayer.getState().setDuration(s.durationSec);
          audioRef.current.load(s.url, autoplay);
          audioRef.current.setVolume(useSettings.getState().volume);
          return true;
        } catch {
          return false;
        }
      },
      play: () => audioRef.current?.play(),
      pause: () => audioRef.current?.pause(),
      seek: (sec) => audioRef.current?.seek(sec),
      setVolume: (v) => audioRef.current?.setVolume(v),
      stop: () => audioRef.current?.stop(),
    };
    const ytCtl: EngineController = {
      load: async (t2, autoplay) => {
        const c = makeYT();
        activeKind.current = 'yt';
        return c.load(t2.videoId, autoplay);
      },
      play: () => playerRef.current?.play(),
      pause: () => playerRef.current?.pause(),
      seek: (sec) => playerRef.current?.seek(sec),
      setVolume: (v) => playerRef.current?.setVolume(v),
      stop: () => playerRef.current?.stop(),
    };

    const dispatch: EngineController = {
      load: async (t2, autoplay) => {
        const kind = usePlayer.getState().engine;
        if (kind === 'local' || kind === 'custom') return audioCtl.load(t2, autoplay);
        if (kind === 'yt') return ytCtl.load(t2, autoplay);
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

  // ---- engine error handling (mid-play failures) ----
  const handleEngineError = (msgRaw: string) => {
    const st = usePlayer.getState();
    const mode = useSettings.getState().playbackMode;
    if (st.engine === 'custom' && mode !== 'adfree' && msgRaw === 'AUDIO_ERROR') {
      // the ad-free stream died mid-play → try the official player once
      toastOnce('adfreeFail', msg('adfreeFail'));
      st.setEngine('yt');
      st.setLoading(true);
    } else if (st.engine === 'custom' && mode === 'adfree') {
      toastOnce('extractFail', msg('extractFail'));
      st.setPlaying(false);
    } else if (st.engine === 'yt') {
      toastOnce('officialFail', msg('officialUnavailable'));
      st.setPlaying(false);
    }
  };

  // ---- load orchestration: one custom attempt + one official attempt per track ----
  const attemptsRef = useRef<{ videoId: string; officialTried: boolean }>({ videoId: '', officialTried: false });

  useEffect(() => {
    if (!track) return;
    let cancelled = false;

    if (attemptsRef.current.videoId !== track.videoId) {
      attemptsRef.current = { videoId: track.videoId, officialTried: false };
    }
    // set by the custom-engine attempt so the failure message can name the cause
    let lastCode: ReturnType<typeof extractCodeOf> = null;

    const stopWith = (key: Parameters<typeof t>[1] | null) => {
      if (cancelled) return;
      usePlayer.getState().setLoading(false);
      usePlayer.getState().setPlaying(false);
      if (key) toastOnce(key, msg(key));
    };

    const loadKind = async (kind: 'local' | 'custom' | 'yt'): Promise<boolean> => {
      const ctl = getController();
      if (!ctl || cancelled) return false;
      usePlayer.getState().setEngine(kind);
      usePlayer.getState().setLoading(true);
      // route directly to the right controller
      if (kind === 'yt') {
        if (!playerRef.current && videoDivRef.current) {
          ytMountRef.current = document.createElement('div');
          videoDivRef.current.appendChild(ytMountRef.current);
          playerRef.current = new YTController(ytMountRef.current, makeHooks());
        }
        activeKind.current = 'yt';
        try { return await playerRef.current!.load(track.videoId, true); }
        catch { return false; }
      }
      if (!audioRef.current) audioRef.current = new AudioController(makeHooks());
      activeKind.current = 'audio';
      if (kind === 'local') {
        const f = await getLocalFileURL(track.videoId);
        if (f) { audioRef.current.load(f.url, true); audioRef.current.setVolume(useSettings.getState().volume); return true; }
        return false;
      }
      try {
        abortRef.current?.abort();
        abortRef.current = new AbortController();
        const s = await extractStream(track.videoId, 'high');
        if (cancelled) return false;
        if (s.durationSec) usePlayer.getState().setDuration(s.durationSec);
        audioRef.current.load(s.url, true);
        audioRef.current.setVolume(useSettings.getState().volume);
        return true;
      } catch (e) {
        lastCode = extractCodeOf(e);
        return false;
      }
    };

    (async () => {
      const mode = useSettings.getState().playbackMode;
      const dl = useDownloads.getState().items[track.videoId];

      if (dl?.status === 'done') {
        if (await loadKind('local')) { usePlayer.getState().setLoading(false); return; }
      }

      if (mode !== 'official') {
        if (await loadKind('custom')) { usePlayer.getState().setLoading(false); return; }
        if (cancelled) return;
        // ad-free failed → fall back to the official player ONCE
        if (!attemptsRef.current.officialTried) {
          attemptsRef.current.officialTried = true;
          toastOnce('adfreeFail', msg('adfreeFail'));
          if (await loadKind('yt')) { usePlayer.getState().setLoading(false); return; }
        }
        stopWith(signInHint(lastCode) ?? 'playFail');
        return;
      }

      // official-only mode
      if (await loadKind('yt')) { usePlayer.getState().setLoading(false); return; }
      stopWith('officialUnavailable');
    })();

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [track?.videoId]);

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
          artwork: [{ src: track.thumb, sizes: '512x512', type: 'image/jpeg' }],
        });
      }
      navigator.mediaSession?.setActionHandler?.('play', () => usePlayer.getState().explicitPlay());
      navigator.mediaSession?.setActionHandler?.('pause', () => usePlayer.getState().explicitPause());
      navigator.mediaSession?.setActionHandler?.('previoustrack', () => usePlayer.getState().prev());
      navigator.mediaSession?.setActionHandler?.('nexttrack', () => usePlayer.getState().next());
      navigator.mediaSession?.setActionHandler?.('seekto', (d: any) => { if (d.seekTime != null) usePlayer.getState().seek(d.seekTime); });
    } catch { /* noop */ }
    bridge()?.updateMedia(JSON.stringify({
      title: track.title, artist: track.artist, artwork: track.thumb,
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
