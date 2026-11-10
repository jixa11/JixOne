'use client';
import { usePlayer } from '@/store/player';
import { useView } from '@/store/view';

/**
 * The Android hardware back button used to go through WebView.canGoBack(),
 * which is always false here: the app is a single page with its own nav stack,
 * so back always fell through to moveTaskToBack and closed the whole app.
 *
 * MainActivity now asks the page first. Return true when we consumed the press;
 * only a false answer lets Android leave the app.
 */
export function registerNativeBack() {
  if (typeof window === 'undefined') return;

  (window as any).__jixBack = () => {
    const p = usePlayer.getState();

    // Innermost layer first, the way a phone user expects.
    if (p.queueOpen) {
      p.toggleQueue();
      return true;
    }
    if (p.npOpen) {
      p.setNpOpen(false);
      return true;
    }

    const v = useView.getState();
    if (v.canBack()) {
      v.back();
      return true;
    }
    // On the home view there is nothing left to pop — let Android take it.
    return false;
  };
}
