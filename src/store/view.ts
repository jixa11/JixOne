'use client';
import { create } from 'zustand';

export type ViewName = 'home' | 'search' | 'library' | 'playlist' | 'liked' | 'history' | 'downloads' | 'settings';

interface ViewState {
  stack: { name: ViewName; id?: string }[];
  current: { name: ViewName; id?: string };
  push: (name: ViewName, id?: string) => void;
  back: () => void;
  canBack: () => boolean;
  searchSeed: string;
  setSearchSeed: (s: string) => void;
}

export const useView = create<ViewState>((set, get) => ({
  stack: [],
  current: { name: 'home' },
  push: (name, id) => set((s) => ({ stack: [...s.stack, s.current].slice(-25), current: { name, id } })),
  back: () =>
    set((s) => {
      const stack = [...s.stack];
      const prev = stack.pop() ?? { name: 'home' as ViewName };
      return { stack, current: prev };
    }),
  canBack: () => get().stack.length > 0,
  searchSeed: '',
  setSearchSeed: (s) => set({ searchSeed: s }),
}));
