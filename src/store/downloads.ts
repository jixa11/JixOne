'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { DownloadItem, QualityId } from '@/lib/types';

interface DownloadsState {
  items: Record<string, DownloadItem>; // by videoId
  add: (item: DownloadItem) => void;
  update: (videoId: string, patch: Partial<DownloadItem>) => void;
  remove: (videoId: string) => void;
  clearAll: () => void;
  has: (videoId: string) => boolean;
  doneCount: () => number;
}

export const useDownloads = create<DownloadsState>()(
  persist(
    (set, get) => ({
      items: {},
      add: (item) => set((s) => ({ items: { ...s.items, [item.videoId]: item } })),
      update: (videoId, patch) =>
        set((s) => (s.items[videoId] ? { items: { ...s.items, [videoId]: { ...s.items[videoId], ...patch } } } : s)),
      remove: (videoId) =>
        set((s) => {
          const items = { ...s.items };
          delete items[videoId];
          return { items };
        }),
      clearAll: () => set({ items: {} }),
      has: (videoId) => !!get().items[videoId],
      doneCount: () => Object.values(get().items).filter((i) => i.status === 'done').length,
    }),
    { name: 'np-downloads' }
  )
);

export type { DownloadItem, QualityId };
