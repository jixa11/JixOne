'use client';
import { create } from 'zustand';

export type ViewName = 'search' | 'explore' | 'library' | 'playlist' | 'liked' | 'history' | 'settings';

interface ViewState {
  stack: { name: ViewName; id?: string }[];
  current: { name: ViewName; id?: string };
  push: (name: ViewName, id?: string) => void;
  back: () => void;
  canBack: () => boolean;
  searchSeed: string;
  setSearchSeed: (s: string) => void;
}

/** Explore is the landing view. Search and Library follow it; Settings lives
 *  in the top-corner gear. */
export const useView = create<ViewState>((set, get) => ({
  stack: [],
  current: { name: 'explore' },
  push: (name, id) => set((s) => ({ stack: [...s.stack, s.current].slice(-25), current: { name, id } })),
  back: () =>
    set((s) => {
      const stack = [...s.stack];
      const prev = stack.pop() ?? { name: 'explore' as ViewName };
      return { stack, current: prev };
    }),
  canBack: () => get().stack.length > 0,
  searchSeed: '',
  setSearchSeed: (s) => set({ searchSeed: s }),
}));
