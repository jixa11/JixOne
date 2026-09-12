'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Lang } from '@/lib/i18n';
import type { QualityId } from '@/lib/types';

export type PlaybackMode = 'auto' | 'official' | 'adfree';
export type GlowLevel = 'low' | 'mid' | 'high';

interface SettingsState {
  theme: string;
  glow: GlowLevel;
  lang: Lang;
  playbackMode: PlaybackMode;
  downloadQuality: QualityId;
  askBeforeBatch: boolean;
  playSync: boolean; // register offline plays (off by default)
  serverUrl: string; // advanced: base url for APK
  volume: number;
  set: (p: Partial<SettingsState>) => void;
}

export const useSettings = create<SettingsState>()(
  persist(
    (set) => ({
      theme: 'cyber-red',
      glow: 'mid',
      lang: 'en', // app default language is English; users can switch to fa in Settings
      playbackMode: 'auto',
      downloadQuality: 'high',
      askBeforeBatch: true,
      playSync: false,
      serverUrl: '',
      volume: 0.9,
      set: (p) => set(p),
    }),
    { name: 'np-settings' }
  )
);
