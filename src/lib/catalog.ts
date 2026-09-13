import type { Track } from '@/lib/types';

/**
 * Curated international catalog — famous hits with real YouTube videoIds.
 * Used as: (1) offline/unreachable-network fallback for Home,
 * (2) content of the demo YouTube Music playlists on sign-in.
 * Thumbs intentionally empty → rendered with generated gradient cover art.
 */

export interface CatalogEntry {
  id: string; // youtube video id
  title: string;
  artist: string;
  duration: string; // "m:ss"
}

export const INTERNATIONAL_HITS: CatalogEntry[] = [
  { id: '4NRXx6U8ABQ', title: 'Blinding Lights', artist: 'The Weeknd', duration: '3:20' },
  { id: 'TUVcZfQe-Kw', title: 'Levitating', artist: 'Dua Lipa', duration: '3:23' },
  { id: 'JGwWNGJdvx8', title: 'Shape of You', artist: 'Ed Sheeran', duration: '4:24' },
  { id: 'dvgZkm1xWPE', title: 'Viva La Vida', artist: 'Coldplay', duration: '4:03' },
  { id: 'DyDfgMOUjCI', title: 'bad guy', artist: 'Billie Eilish', duration: '3:14' },
  { id: '7wtfhZwyrcc', title: 'Believer', artist: 'Imagine Dragons', duration: '3:37' },
  { id: 'fJ9rUzIMcZQ', title: 'Bohemian Rhapsody', artist: 'Queen', duration: '5:59' },
  { id: 'Zi_XLOBDo_Y', title: 'Billie Jean', artist: 'Michael Jackson', duration: '4:54' },
  { id: 'YQHsXMglC9A', title: 'Hello', artist: 'Adele', duration: '6:07' },
  { id: '60ItHLz5WEA', title: 'Faded', artist: 'Alan Walker', duration: '3:32' },
  { id: 'RgKAFK5djSk', title: 'See You Again', artist: 'Wiz Khalifa, Charlie Puth', duration: '3:58' },
  { id: '9bZkp7q19f0', title: 'Gangnam Style', artist: 'PSY', duration: '4:13' },
  { id: 'hT_nvWreIhg', title: 'Counting Stars', artist: 'OneRepublic', duration: '4:44' },
  { id: 'e-ORhEE9VVg', title: 'Blank Space', artist: 'Taylor Swift', duration: '4:33' },
  { id: 'lWA2pjMjpBs', title: 'Diamonds', artist: 'Rihanna', duration: '4:48' },
  { id: '09R8_2nJtjg', title: 'Sugar', artist: 'Maroon 5', duration: '5:01' },
  { id: 'nYh-n7EOtMA', title: 'Cheap Thrills', artist: 'Sia', duration: '3:46' },
  { id: 'oygrmJFKYZY', title: "Don't Start Now", artist: 'Dua Lipa', duration: '3:03' },
  { id: '2Vv-BfVoq4g', title: 'Perfect', artist: 'Ed Sheeran', duration: '4:40' },
  { id: 'fKopy74weus', title: 'Thunder', artist: 'Imagine Dragons', duration: '3:07' },
  { id: 'YykjpeuMNEk', title: 'Hymn for the Weekend', artist: 'Coldplay', duration: '4:19' },
  { id: 'FTQbiNvZqaY', title: 'Africa', artist: 'Toto', duration: '4:56' },
  { id: 'V1Pl8CzNzCw', title: 'lovely', artist: 'Billie Eilish, Khalid', duration: '3:20' },
  { id: '34Na4j8AVgA', title: 'Starboy', artist: 'The Weeknd', duration: '3:50' },
];

export function catalogTrack(e: CatalogEntry): Track {
  const [m, s] = e.duration.split(':').map(Number);
  return {
    videoId: e.id,
    title: e.title,
    artist: e.artist,
    duration: e.duration,
    durationSec: m * 60 + s,
    thumb: '', // empty → generated gradient cover art (never a broken image)
  };
}

export const CURATED_TRACKS: Track[] = INTERNATIONAL_HITS.map(catalogTrack);

export interface ArtistDef {
  name: string;
  query: string;
  hue: [string, string]; // gradient for the avatar
}

export const POPULAR_ARTISTS: ArtistDef[] = [
  { name: 'The Weeknd', query: 'The Weeknd', hue: ['#ff2d55', '#8b5cf6'] },
  { name: 'Taylor Swift', query: 'Taylor Swift', hue: ['#be185d', '#7c3aed'] },
  { name: 'Drake', query: 'Drake', hue: ['#0ea5e9', '#1e293b'] },
  { name: 'Dua Lipa', query: 'Dua Lipa', hue: ['#f43f5e', '#fb923c'] },
  { name: 'Billie Eilish', query: 'Billie Eilish', hue: ['#22d3ee', '#166534'] },
  { name: 'Coldplay', query: 'Coldplay', hue: ['#3b82f6', '#fbbf24'] },
  { name: 'Ed Sheeran', query: 'Ed Sheeran', hue: ['#f97316', '#b91c1c'] },
  { name: 'Imagine Dragons', query: 'Imagine Dragons', hue: ['#6366f1', '#0ea5e9'] },
  { name: 'Rihanna', query: 'Rihanna', hue: ['#ec4899', '#6366f1'] },
  { name: 'Eminem', query: 'Eminem', hue: ['#64748b', '#111827'] },
  { name: 'Queen', query: 'Queen', hue: ['#f59e0b', '#7c3aed'] },
  { name: 'Adele', query: 'Adele', hue: ['#8b5cf6', '#be123c'] },
];
