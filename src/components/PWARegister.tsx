'use client';
import { useEffect } from 'react';
import { pwaSetDeferredPrompt, pwaIsStandalone, pwaDetectPlatform, usePwa } from '@/lib/pwa';
import { useSettings } from '@/store/settings';
import { t } from '@/lib/i18n';
import { toast } from 'sonner';

/**
 * Registers the service worker and wires up the install prompt.
 *
 * Skipped inside the Android APK: its WebView is served from
 * appassets.androidplatform.net under a basePath, the shell is already on
 * device, and a service worker there would only ever 404.
 */
export default function PWARegister() {
  useEffect(() => {
    const inApk = !!(window as any).AndroidBridge;

    usePwa.setState({ installed: pwaIsStandalone(), platform: pwaDetectPlatform() });

    if (!inApk && 'serviceWorker' in navigator && /^https?:$/.test(window.location.protocol)) {
      navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(() => {
        /* dev server without sw, or unsupported — the app works without it */
      });
    }

    const onPrompt = (e: Event) => {
      e.preventDefault();
      pwaSetDeferredPrompt(e);
    };
    const onInstalled = () => {
      pwaSetDeferredPrompt(null);
      usePwa.setState({ installed: true, canInstall: false });
      toast(t(useSettings.getState().lang, 'pwaDone'));
    };
    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  return null;
}
