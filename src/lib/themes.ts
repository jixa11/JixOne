export interface ThemeDef {
  id: string;
  name: { fa: string; en: string };
  /** [bg, accent, accent2, meshHighlight] — preview swatches */
  swatch: [string, string, string, string];
  cyber?: boolean;
}

export const THEMES: ThemeDef[] = [
  { id: 'cyber-red', name: { fa: 'کریمسون نئون', en: 'Neon Crimson' }, swatch: ['#08070c', '#ff2d55', '#8b5cf6', '#22d3ee'], cyber: true },
  { id: 'cyber-blue', name: { fa: 'نئو توکیو', en: 'Neo Tokyo' }, swatch: ['#04060e', '#22d3ee', '#6366f1', '#d946ef'], cyber: true },
  { id: 'synthwave', name: { fa: 'سینث‌ویو', en: 'Synthwave' }, swatch: ['#0c0416', '#ff2fa0', '#a855f7', '#fb923c'], cyber: true },
  { id: 'spotify', name: { fa: 'اسپاتیفای کلاسیک', en: 'Classic Spotify' }, swatch: ['#0e0e0e', '#1db954', '#14b8a6', '#3b82f6'] },
  { id: 'oled', name: { fa: 'OLED خالص', en: 'Pure OLED' }, swatch: ['#000000', '#ffffff', '#a1a1aa', '#3f3f46'] },
  { id: 'dubai', name: { fa: 'غروب دبی', en: 'Dubai Sunset' }, swatch: ['#0e0910', '#ff6b35', '#f59e0b', '#ec4899'] },
  { id: 'yt-light', name: { fa: 'روشن یوتیوب', en: 'YouTube Light' }, swatch: ['#f7f7f9', '#ff0033', '#ff5e3a', '#8b5cf6'] },
  { id: 'yt-original', name: { fa: 'یوتیوب میوزیک', en: 'YT Music Original' }, swatch: ['#0d0d0d', '#ff0033', '#ff6b6b', '#7c2d12'] },
];
