'use client';
import { create } from 'zustand';

/** Install-as-app state, shared by PWARegister (events) and SettingsView (UI). */
export interface PwaState {
  /** the browser fired beforeinstallprompt and the native dialog is available */
  canInstall: boolean;
  /** running as an installed app (standalone display mode) */
  installed: boolean;
  /** ios needs the manual Share → Add to Home Screen path */
  platform: 'ios' | 'generic';
}

export const usePwa = create<PwaState>(() => ({
  canInstall: false,
  installed: false,
  platform: 'generic',
}));

let deferredPrompt: any = null;

export function pwaSetDeferredPrompt(e: unknown) {
  deferredPrompt = e;
  usePwa.setState({ canInstall: !!e });
}

export function pwaIsStandalone(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    return (
      window.matchMedia?.('(display-mode: standalone)').matches ||
      (navigator as any).standalone === true
    );
  } catch {
    return false;
  }
}

export function pwaDetectPlatform(): 'ios' | 'generic' {
  if (typeof navigator === 'undefined') return 'generic';
  const ua = navigator.userAgent;
  // iPadOS 13+ masquerades as desktop Safari but has no :hover state
  const isIOS =
    /iPad|iPhone|iPod/.test(ua) ||
    (ua.includes('Macintosh') && typeof document !== 'undefined' && 'ontouchend' in document);
  return isIOS ? 'ios' : 'generic';
}

/** Opens the browser's install dialog. Resolves true when the user accepted. */
export async function pwaPromptInstall(): Promise<boolean> {
  if (!deferredPrompt) return false;
  try {
    deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    deferredPrompt = null;
    usePwa.setState({ canInstall: false });
    return choice?.outcome === 'accepted';
  } catch {
    return false;
  }
}
