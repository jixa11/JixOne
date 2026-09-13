'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Playlist, Track } from '@/lib/types';

interface LibraryState {
  playlists: Playlist[];
  liked: Track[];
  history: Track[];
  offlinePlays: { videoId: string; at: number }[]; // for play-sync feature
  createPlaylist: (name: string) => string;
  deletePlaylist: (id: string) => void;
  renamePlaylist: (id: string, name: string) => void;
  addToPlaylist: (id: string, t: Track) => void;
  removeFromPlaylist: (id: string, videoId: string) => void;
  setAutoDownload: (id: string, on: boolean) => void;
  toggleLike: (t: Track) => boolean;
  pushHistory: (t: Track) => void;
  pushOfflinePlay: (videoId: string) => void;
  clearOfflinePlays: () => void;
}

const uid = () => Math.random().toString(36).slice(2, 10);

export const useLibrary = create<LibraryState>()(
  persist(
    (set, get) => ({
      playlists: [],
      liked: [],
      history: [],
      offlinePlays: [],
      createPlaylist: (name) => {
        const id = uid();
        set((s) => ({ playlists: [{ id, name, createdAt: Date.now(), tracks: [] }, ...s.playlists] }));
        return id;
      },
      deletePlaylist: (id) => set((s) => ({ playlists: s.playlists.filter((p) => p.id !== id) })),
      renamePlaylist: (id, name) =>
        set((s) => ({ playlists: s.playlists.map((p) => (p.id === id ? { ...p, name } : p)) })),
      addToPlaylist: (id, t) =>
        set((s) => ({
          playlists: s.playlists.map((p) =>
            p.id === id && !p.tracks.some((x) => x.videoId === t.videoId)
              ? { ...p, tracks: [...p.tracks, t] }
              : p
          ),
        })),
      removeFromPlaylist: (id, videoId) =>
        set((s) => ({
          playlists: s.playlists.map((p) =>
            p.id === id ? { ...p, tracks: p.tracks.filter((x) => x.videoId !== videoId) } : p
          ),
        })),
      setAutoDownload: (id, on) =>
        set((s) => ({ playlists: s.playlists.map((p) => (p.id === id ? { ...p, autoDownload: on } : p)) })),
      toggleLike: (t) => {
        const has = get().liked.some((x) => x.videoId === t.videoId);
        set((s) => ({ liked: has ? s.liked.filter((x) => x.videoId !== t.videoId) : [t, ...s.liked] }));
        return !has;
      },
      pushHistory: (t) =>
        set((s) => ({ history: [t, ...s.history.filter((x) => x.videoId !== t.videoId)].slice(0, 60) })),
      pushOfflinePlay: (videoId) =>
        set((s) => ({ offlinePlays: [...s.offlinePlays.filter((x) => x.videoId !== videoId), { videoId, at: Date.now() }].slice(-30) })),
      clearOfflinePlays: () => set({ offlinePlays: [] }),
    }),
    { name: 'np-library' }
  )
);
