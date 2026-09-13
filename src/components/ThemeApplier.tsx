'use client';
import { useEffect } from 'react';
import { useSettings } from '@/store/settings';
import { THEMES } from '@/lib/themes';
import { assetUrl } from '@/lib/asset';

/** Applies theme + glow + language direction to <html> reactively, with a smooth color morph on theme switch */
export default function ThemeApplier() {
  const theme = useSettings((s) => s.theme);
  const glow = useSettings((s) => s.glow);
  const lang = useSettings((s) => s.lang);

  useEffect(() => {
    const html = document.documentElement;
    html.dataset.theme = theme;
    html.dataset.glow = glow;
    html.lang = lang;
    html.dir = lang === 'fa' ? 'rtl' : 'ltr';

    // Artwork is set here rather than in CSS because the APK serves the app
    // under a basePath that CSS url() knows nothing about.
    const def = THEMES.find((t) => t.id === theme);
    html.style.setProperty('--theme-art', def?.art ? `url("${assetUrl(def.art)}")` : 'none');
    html.style.setProperty('--theme-tile', def?.tile ? `url("${assetUrl(def.tile)}")` : 'none');
    // notify native shell (widget accent)
    const styles = getComputedStyle(html);
    window.AndroidBridge?.updateMedia(
      JSON.stringify({ themeAccent: styles.getPropertyValue('--accent').trim() })
    );
  }, [theme, glow, lang]);

  // brief cross-fade of colors when the theme changes
  useEffect(() => {
    const html = document.documentElement;
    html.classList.add('theme-anim');
    const id = setTimeout(() => html.classList.remove('theme-anim'), 500);
    return () => clearTimeout(id);
  }, [theme]);

  return null;
}
