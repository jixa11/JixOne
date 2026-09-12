'use client';

/** Unified engines: official YT iframe player + html audio (custom stream / local OPFS file) */

declare global {
  interface Window {
    YT?: any;
    onYouTubeIframeAPIReady?: () => void;
    AndroidBridge?: {
      updateMedia: (json: string) => void;
      setPlaying: (b: boolean) => void;
      notify: (msg: string) => void;
    };
    NativeAction?: {
      play?: () => void; pause?: () => void; next?: () => void; prev?: () => void; seek?: (sec: number) => void;
    };
  }
}

let apiPromise: Promise<void> | null = null;
export function loadYTApi(): Promise<void> {
  if (apiPromise) return apiPromise;
  apiPromise = new Promise<void>((resolve) => {
    if (window.YT?.Player) return resolve();
    const prev = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => { prev?.(); resolve(); };
    const s = document.createElement('script');
    s.src = 'https://www.youtube.com/iframe_api';
    document.head.appendChild(s);
  });
  return apiPromise;
}

export interface EngineHooks {
  onPosition: (sec: number) => void;
  onDuration: (sec: number) => void;
  onPlayState: (playing: boolean) => void;
  onEnded: () => void;
  onError: (msg: string) => void;
}

/** -------- Official YT IFrame engine -------- */
export class YTController {
  private player: any = null;
  private timer: any = null;
  private currentId: string | null = null;
  private pendingLoad: { id: string; autoplay: boolean; resolve: (ok: boolean) => void } | null = null;

  constructor(private host: HTMLElement, private hooks: EngineHooks) {}

  async ensure(): Promise<void> {
    await loadYTApi();
    if (this.player) return;
    await new Promise<void>((resolve) => {
      this.player = new window.YT.Player(this.host, {
        height: '100%',
        width: '100%',
        playerVars: { playsinline: 1, rel: 0, modestbranding: 1 },
        events: {
          onReady: () => resolve(),
          onStateChange: (e: any) => {
            const st = e.data;
            if (st === 1) { this.hooks.onPlayState(true); this.startPoll(); const d = this.player.getDuration?.() ?? 0; if (d) this.hooks.onDuration(d); }
            else if (st === 2) this.hooks.onPlayState(false);
            else if (st === 0) { this.hooks.onPlayState(false); this.hooks.onEnded(); }
          },
          onError: () => {
            if (this.pendingLoad) { const r = this.pendingLoad.resolve; this.pendingLoad = null; r(false); }
            else this.hooks.onError('YT_ERROR');
          },
        },
      });
    });
  }

  private startPoll() {
    clearInterval(this.timer);
    this.timer = setInterval(() => {
      try { this.hooks.onPosition(this.player.getCurrentTime() ?? 0); } catch { /* noop */ }
    }, 500);
  }

  async load(id: string, autoplay: boolean): Promise<boolean> {
    await this.ensure();
    const ok = await new Promise<boolean>((resolve) => {
      this.pendingLoad = { id, autoplay, resolve };
      this.currentId = id;
      try { this.player.loadVideoById(id); } catch { resolve(false); }
      setTimeout(() => { if (this.pendingLoad?.resolve === resolve) { this.pendingLoad = null; resolve(true); } }, 3500);
    });
    if (ok && !autoplay) setTimeout(() => { try { this.player.pauseVideo(); } catch { /* noop */ } }, 120);
    return ok;
  }

  play() { try { this.player?.playVideo(); } catch { /* noop */ } }
  pause() { try { this.player?.pauseVideo(); } catch { /* noop */ } }
  seek(sec: number) { try { this.player?.seekTo(sec, true); } catch { /* noop */ } }
  setVolume(v: number) { try { this.player?.setVolume(Math.round(v * 100)); } catch { /* noop */ } }
  stop() { clearInterval(this.timer); try { this.player?.stopVideo(); } catch { /* noop */ } }
  destroy() { clearInterval(this.timer); try { this.player?.destroy?.(); } catch { /* noop */ } this.player = null; }
}

/** -------- HTML audio engine (custom ad-free stream / local OPFS file) -------- */
export class AudioController {
  private audio: HTMLAudioElement;
  private timer: any = null;

  constructor(private hooks: EngineHooks) {
    this.audio = new Audio();
    this.audio.preload = 'auto';
    this.audio.addEventListener('loadedmetadata', () => { if (isFinite(this.audio.duration)) this.hooks.onDuration(this.audio.duration); });
    this.audio.addEventListener('timeupdate', () => this.hooks.onPosition(this.audio.currentTime));
    this.audio.addEventListener('playing', () => this.hooks.onPlayState(true));
    this.audio.addEventListener('pause', () => this.hooks.onPlayState(false));
    this.audio.addEventListener('ended', () => { this.hooks.onPlayState(false); this.hooks.onEnded(); });
    this.audio.addEventListener('error', () => this.hooks.onError('AUDIO_ERROR'));
  }

  load(url: string, autoplay: boolean): boolean {
    this.audio.src = url;
    this.audio.load();
    if (autoplay) this.audio.play().catch(() => this.hooks.onError('PLAY_BLOCKED'));
    return true;
  }
  play() { this.audio.play().catch(() => { /* noop */ }); }
  pause() { this.audio.pause(); }
  seek(sec: number) { try { this.audio.currentTime = sec; } catch { /* noop */ } }
  setVolume(v: number) { this.audio.volume = Math.max(0, Math.min(1, v)); }
  stop() { this.audio.pause(); this.audio.removeAttribute('src'); this.audio.load(); }
}

/** -------- Hidden muted player for "play-sync" (offline play registration) -------- */
export class SyncPlayer {
  private player: any = null;
  private host: HTMLElement | null = null;
  private timer: any = null;
  private busy = false;

  constructor(private onDone: (videoId: string, ok: boolean) => void) {}

  setHost(el: HTMLElement | null) { this.host = el; }

  private async ensure(): Promise<any> {
    if (this.player) return this.player;
    await loadYTApi();
    this.player = await new Promise((resolve) => {
      this.player = new window.YT.Player(this.host, {
        height: '120', width: '214',
        playerVars: { playsinline: 1, disablekb: 1, controls: 0 },
        events: { onReady: () => resolve(this.player) },
      });
    });
    return this.player;
  }

  async syncOne(videoId: string) {
    if (this.busy) return;
    this.busy = true;
    let ok = false;
    try {
      const p = await this.ensure();
      await new Promise<void>((res) => { p.loadVideoById(videoId); setTimeout(res, 2500); });
      p.mute();
      p.playVideo();
      await new Promise<void>((res) => {
        let elapsed = 0;
        this.timer = setInterval(() => {
          elapsed += 5;
          try { if (p.getPlayerState() === 1) ok = true; } catch { /* noop */ }
          if (elapsed >= 30) { clearInterval(this.timer); res(); }
        }, 5000);
      });
      try { p.stopVideo(); p.unMute(); } catch { /* noop */ }
    } catch { ok = false; }
    this.busy = false;
    this.onDone(videoId, ok);
  }
}
