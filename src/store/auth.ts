'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface AuthUser {
  name: string;
  email: string;
  picture?: string;
  demo?: boolean; // true when signed in without real OAuth (no client id configured)
}

interface AuthState {
  user: AuthUser | null;
  setUser: (u: AuthUser | null) => void;
  signOut: () => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      setUser: (u) => set({ user: u }),
      signOut: () => set({ user: null }),
    }),
    { name: 'np-auth' }
  )
);
