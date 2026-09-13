export interface ThemeDef {
  id: string;
  name: { fa: string; en: string };
  /** [bg, accent, accent2, meshHighlight] — preview swatches */
  swatch: [string, string, string, string];
  /** artwork shown on the theme card, so the motif is visible before picking */
  thumb?: string;
  /** single piece of artwork, anchored and faded behind the app */
  art?: string;
  /** seamless motif repeated across the background */
  tile?: string;
  cyber?: boolean;
}

export const THEMES: ThemeDef[] = [
  // ——— Persian themes (Iranian carpets, tilework & Achaemenid art) ———
  { id: 'persian-carpet', name: { fa: 'فرش ایرانی', en: 'Persian Carpet' }, swatch: ['#170b0e', '#d34a5e', '#2aa9a2', '#e9a832'], thumb: '/themes/thumb-carpet.webp', art: '/themes/carpet-night.webp' },
  { id: 'persian-tile', name: { fa: 'کاشی اصفهان', en: 'Isfahan Tile' }, swatch: ['#081226', '#33c5d6', '#4f7fe6', '#d8aa50'], thumb: '/themes/thumb-tile.webp', tile: '/themes/tile-isfahan.webp' },
  { id: 'derafsh', name: { fa: 'درفش کاویانی', en: 'Kaviani Banner' }, swatch: ['#1b0a2a', '#e0b24a', '#9a5be0', '#c83250'], thumb: '/themes/thumb-derafsh.webp', art: '/themes/derafsh-banner.webp' },
  { id: 'cyrus', name: { fa: 'کوروش هخامنشی', en: 'Cyrus the Great' }, swatch: ['#f5eede', '#b08a2e', '#2a5d9f', '#c46240'], thumb: '/themes/thumb-cyrus.webp', art: '/themes/cyrus-portrait.webp' },
  // ——— previous themes ———
  { id: 'cyber-red', name: { fa: 'کریمسون نئون', en: 'Neon Crimson' }, swatch: ['#08070c', '#ff2d55', '#8b5cf6', '#22d3ee'], cyber: true },
  { id: 'cyber-blue', name: { fa: 'نئو توکیو', en: 'Neo Tokyo' }, swatch: ['#04060e', '#22d3ee', '#6366f1', '#d946ef'], cyber: true },
  { id: 'synthwave', name: { fa: 'سینث‌ویو', en: 'Synthwave' }, swatch: ['#0c0416', '#ff2fa0', '#a855f7', '#fb923c'], cyber: true },
  { id: 'spotify', name: { fa: 'اسپاتیفای کلاسیک', en: 'Classic Spotify' }, swatch: ['#0e0e0e', '#1db954', '#14b8a6', '#3b82f6'] },
  { id: 'oled', name: { fa: 'OLED خالص', en: 'Pure OLED' }, swatch: ['#000000', '#ffffff', '#a1a1aa', '#3f3f46'] },
  { id: 'dubai', name: { fa: 'غروب دبی', en: 'Dubai Sunset' }, swatch: ['#0e0910', '#ff6b35', '#f59e0b', '#ec4899'] },
  { id: 'yt-light', name: { fa: 'روشن یوتیوب', en: 'YouTube Light' }, swatch: ['#f7f7f9', '#ff0033', '#ff5e3a', '#8b5cf6'] },
  { id: 'yt-original', name: { fa: 'یوتیوب میوزیک', en: 'YT Music Original' }, swatch: ['#0d0d0d', '#ff0033', '#ff6b6b', '#7c2d12'] },
];
