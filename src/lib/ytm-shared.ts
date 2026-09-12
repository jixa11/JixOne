/** Pure helpers to flatten YTMusic innertube responses — shared by client & server.
 *  Parser understands BOTH:
 *  - real youtubei.js v18 parsed nodes (MusicResponsiveListItem with flex_columns/id)
 *  - legacy/simple shapes (videoId + title.text) used by E2E fixtures */

export interface RawSong {
  videoId: string;
  title: string;
  artist: string;
  duration?: string;
  thumb: string;
}

const VID_RE = /^[A-Za-z0-9_-]{11}$/;      // real YouTube video ids
const VID_LOOSE_RE = /^[A-Za-z0-9_-]{9,16}$/; // explicit videoId fields (incl. E2E fixtures)
const DUR_RE = /^\d+:\d\d(:\d\d)?$/;
/** filler words removed from subtitles when guessing the artist */
const FILLER_RE = /^(song|video|episode|single|album|ep|audio|official|music|آهنگ|ویدیو|تک‌آهنگ|تک اهنگ)$/i;

function vid11(v: any): string | undefined {
  return typeof v === 'string' && VID_RE.test(v) ? v : undefined;
}

function vidAny(v: any): string | undefined {
  return typeof v === 'string' && VID_LOOSE_RE.test(v) ? v : undefined;
}

function textOf(t: any): string | undefined {
  if (typeof t === 'string') return t;
  return t?.text ?? t?.runs?.[0]?.text ?? undefined;
}

function thumbOf(node: any): string | undefined {
  return (
    node?.thumbnail?.contents?.[0]?.url ??
    node?.thumbnails?.[0]?.url ??
    node?.thumbnail_renderer?.music_thumbnail_renderer?.data?.thumbnail?.contents?.[0]?.url ??
    undefined
  );
}

/** "Artist • 3:20" / "Song • Artist • 879M views" / "Episode • date • Podcast" */
function artistFromSubtitle(sub: string, fallbackDuration?: string): { artist: string; dur?: string } {
  if (!sub) return { artist: '' };
  let dur = fallbackDuration;
  if (sub.includes('•')) {
    const parts = sub.split('•').map((s: string) => s.trim());
    parts.forEach((p) => { if (!dur && DUR_RE.test(p)) dur = p; });
    const names = parts.filter(
      (p) =>
        p !== dur &&
        !FILLER_RE.test(p) &&
        !DUR_RE.test(p) &&
        !/views?$/i.test(p) &&
        !/^\d{4}$/.test(p) &&
        !/^[A-Z][a-z]{2} \d{1,2}, \d{4}$/.test(p)
    );
    return { artist: names.slice(-2).join(', '), dur };
  }
  return { artist: sub, dur };
}

function pushSong(out: RawSong[], song: RawSong) {
  if (!out.some((s) => s.videoId === song.videoId)) out.push(song);
}

export function flattenYTM(node: any, out: RawSong[], depth = 0): RawSong[] {
  if (!node || depth > 9 || out.length > 60) return out;
  if (Array.isArray(node)) {
    for (const n of node) flattenYTM(n, out, depth + 1);
    return out;
  }
  if (typeof node !== 'object') return out;

  // ── 1) MusicResponsiveListItem — real YTM search rows (youtubei.js v18) ──
  if (node.type === 'MusicResponsiveListItem' || (Array.isArray(node.flex_columns) && node.flex_columns.length)) {
    const vid =
      vidAny(node.playlist_item_data?.videoId) ??
      vid11(node.id) ??
      vidAny(node.videoId) ??
      vid11(node?.flex_columns?.[0]?.title?.runs?.[0]?.endpoint?.payload?.videoId);
    const title = textOf(node.flex_columns?.[0]?.title) ?? textOf(node.title) ?? textOf(node.name);
    if (vid && title && node.type !== 'MusicCardShelfHeader') {
      const subRaw = node.flex_columns?.[1]?.title ?? node.subtitle ?? node.flex_columns?.[2]?.title;
      const sub = textOf(subRaw) ?? '';
      const explicitDur = typeof node.duration === 'string' ? node.duration : node.duration?.text;
      const fixedDur = textOf(node.fixed_columns?.[0]?.title);
      const artistsExplicit = Array.isArray(node.artists) || Array.isArray(node.authors)
        ? (node.artists ?? node.authors)?.map((a: any) => a?.name).filter(Boolean).join(', ')
        : '';
      const { artist, dur } = artistFromSubtitle(sub, explicitDur ?? fixedDur);
      const artistFallback = (!artistsExplicit && !artist) ? (textOf(node.flex_columns?.[2]?.title) ?? '') : '';
      const thumb = thumbOf(node);
      if (thumb) {
        pushSong(out, { videoId: vid, title, artist: artistsExplicit || artist || artistFallback || 'Unknown', duration: dur, thumb });
        return out;
      }
    }
  }

  // ── 2) generic walker — MusicCardShelf, simple rows, fixtures ──
  const vid =
    vidAny(node.videoId) ??
    vidAny(node.video_id) ??
    vidAny(node.playlist_item_data?.videoId) ??
    vid11(node?.endpoint?.payload?.videoId) ??
    vid11(node?.on_tap?.payload?.videoId) ??
    vid11(node?.title?.endpoint?.payload?.videoId);
  const title = textOf(node.title) ?? textOf(node.name);
  if (vid && title && node.type !== 'MusicCardShelfHeader') {
    const sub = textOf(node.subtitle) ?? '';
    const explicitDur = typeof node.duration === 'string' ? node.duration : node.duration?.text;
    const artistsExplicit = Array.isArray(node.artists) || Array.isArray(node.authors)
      ? (node.artists ?? node.authors)?.map((a: any) => a?.name).filter(Boolean).join(', ')
      : '';
    const { artist, dur } = artistFromSubtitle(sub, explicitDur);
    const thumb = thumbOf(node);
    if (thumb) {
      pushSong(out, { videoId: vid, title, artist: artistsExplicit || artist || 'Unknown', duration: dur, thumb });
      return out;
    }
  }

  for (const k of Object.keys(node)) {
    if (k === 'menu') continue;
    const v = node[k];
    if (v && typeof v === 'object') flattenYTM(v, out, depth + 1);
  }
  return out;
}

export const MOOD_QUERIES: Record<string, string[]> = {
  iran_now: ['آهنگ جدید پاپ ایرانی', 'آهنگ ایرانی پرطرفدار', 'persian pop hits'],
  pop: ['top pop hits 2026', 'pop international hits'],
  classic: ['persian classic hits 70s 80s', 'timeless persian songs'],
  electronic: ['electronic chill mix', 'edm top hits'],
  hiphop: ['hip hop hits 2026', 'رپ فارسی جدید'],
  lofi: ['lofi chill beats', 'lofi study music'],
  rock: ['rock classics', 'best rock songs'],
  ambient: ['ambient relax music', 'piano calm music'],
};
