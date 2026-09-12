'use client';
import { create } from 'zustand';
import type { Track } from '@/lib/types';

export type EngineKind = 'pending' | 'yt' | 'custom' | 'local';
export type RepeatMode = 'off' | 'all' | 'one';

export interface EngineController {
  load: (track: Track, autoplay: boolean) => Promise<boolean>; // false = failed
  play: () => void;
  pause: () => void;
  seek: (sec: number) => void;
  setVolume: (v: number) => void;
  stop: () => void;
}

const controller: { current: EngineController | null } = { current: null };
export const setController = (c: EngineController | null) => { controller.current = c; };
export const getController = () => controller.current;

interface PlayerState {
  queue: Track[];
  index: number;
  playing: boolean;
  loading: boolean;
  engine: EngineKind;
  position: number;
  duration: number;
  shuffle: boolean;
  repeat: RepeatMode;
  videoMode: 'hidden' | 'mini' | 'theater';
  queueOpen: boolean;
  lastContext?: { kind: 'playlist' | 'search' | 'home' | 'liked' | 'queue'; id?: string };

  current: () => Track | undefined;
  setQueue: (tracks: Track[], startIdx: number, ctx?: PlayerState['lastContext']) => void;
  enqueue: (t: Track) => void;
  playIndex: (i: number) => void;
  jumpTo: (i: number) => void;
  removeFromQueue: (i: number) => void;
  toggle: () => void;
  explicitPlay: () => void;
  explicitPause: () => void;
  next: (auto?: boolean) => void;
  prev: () => void;
  seek: (sec: number) => void;
  setPosition: (sec: number) => void;
  setDuration: (sec: number) => void;
  setLoading: (b: boolean) => void;
  setEngine: (e: EngineKind) => void;
  setPlaying: (b: boolean) => void;
  setVideoMode: (m: PlayerState['videoMode']) => void;
  cycleVideo: () => void;
  toggleQueue: () => void;
  toggleShuffle: () => void;
  cycleRepeat: () => void;
  stopAll: () => void;
}

export const usePlayer = create<PlayerState>((set, get) => ({
  queue: [],
  index: -1,
  playing: false,
  loading: false,
  engine: 'pending',
  position: 0,
  duration: 0,
  shuffle: false,
  repeat: 'off',
  videoMode: 'hidden',
  queueOpen: false,

  current: () => {
    const { queue, index } = get();
    return index >= 0 && index < queue.length ? queue[index] : undefined;
  },
  setQueue: (tracks, startIdx, ctx) =>
    set({ queue: Array.isArray(tracks) ? tracks : [], index: startIdx, engine: 'pending', loading: true, position: 0, duration: 0, lastContext: ctx }),
  enqueue: (t) => set((s) => ({ queue: [...s.queue, t] })),
  playIndex: (i) => {
    if (i < 0 || i >= get().queue.length) return;
    set({ index: i, engine: 'pending', loading: true, position: 0, duration: 0 });
  },
  jumpTo: (i) => get().playIndex(i),
  removeFromQueue: (i) =>
    set((s) => {
      const q = [...s.queue];
      q.splice(i, 1);
      let index = s.index;
      if (i < s.index) index--;
      return { queue: q, index };
    }),
  toggle: () => set((s) => ({ playing: !s.playing })),
  explicitPlay: () => set({ playing: true }),
  explicitPause: () => set({ playing: false }),
  next: (auto = false) => {
    const { queue, index, shuffle, repeat } = get();
    if (!queue.length) return;
    if (auto && repeat === 'one') { set({ engine: 'pending', loading: true, position: 0 }); return; }
    let ni: number;
    if (shuffle && queue.length > 1) {
      do { ni = Math.floor(Math.random() * queue.length); } while (ni === index);
    } else {
      ni = index + 1;
      if (ni >= queue.length) {
        if (repeat === 'all' || !auto) ni = 0;
        else { set({ playing: false }); return; }
      }
    }
    set({ index: ni, engine: 'pending', loading: true, position: 0, duration: 0 });
  },
  prev: () => {
    const { queue, index, position } = get();
    if (position > 4) { get().seek(0); return; }
    const ni = index - 1 < 0 ? queue.length - 1 : index - 1;
    set({ index: ni, engine: 'pending', loading: true, position: 0, duration: 0 });
  },
  seek: (sec) => {
    set({ position: sec });
    controller.current?.seek(sec);
  },
  setPosition: (sec) => set({ position: sec }),
  setDuration: (sec) => set({ duration: sec }),
  setLoading: (b) => set({ loading: b }),
  setEngine: (e) => set({ engine: e }),
  setPlaying: (b) => set({ playing: b }),
  setVideoMode: (m) => set({ videoMode: m }),
  cycleVideo: () =>
    set((s) => ({ videoMode: s.videoMode === 'hidden' ? 'mini' : s.videoMode === 'mini' ? 'theater' : 'hidden' })),
  toggleQueue: () => set((s) => ({ queueOpen: !s.queueOpen })),
  toggleShuffle: () => set((s) => ({ shuffle: !s.shuffle })),
  cycleRepeat: () =>
    set((s) => ({ repeat: s.repeat === 'off' ? 'all' : s.repeat === 'all' ? 'one' : 'off' })),
  stopAll: () => set({ playing: false }),
}));
