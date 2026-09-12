/** Pure helpers to flatten YTMusic innertube responses — shared by client & server */

export interface RawSong {
  videoId: string;
  title: string;
  artist: string;
  duration?: string;
  thumb: string;
}

export function flattenYTM(node: any, out: RawSong[], depth = 0): RawSong[] {
  if (!node || depth > 7 || out.length > 60) return out;
  if (Array.isArray(node)) {
    for (const n of node) flattenYTM(n, out, depth + 1);
    return out;
  }
  if (typeof node !== 'object') return out;

  const vid =
    node.videoId ??
    node.video_id ??
    node?.title?.endpoint?.payload?.videoId ??
    node?.on_tap?.payload?.videoId;
  const title = node.title?.text ?? node.title?.runs?.[0]?.text;

  if (vid && typeof vid === 'string' && title && node.type !== 'MusicCardShelfHeader') {
    const sub: string =
      node.subtitle?.text ??
      node?.flex_columns?.[1]?.title?.runs?.map((r: any) => r.text).join(' ') ??
      node?.flex_columns?.[1]?.text?.runs?.map((r: any) => r.text).join(' ') ??
      node?.authors?.map((a: any) => a.name).join(', ') ??
      '';
    let artist = sub;
    let dur: string | undefined;
    if (sub.includes('•')) {
      const parts = sub.split('•').map((s: string) => s.trim());
      parts.forEach((p) => { if (/^\d+:\d\d(:\d\d)?$/.test(p)) dur = p; });
      artist = parts.filter((p) => p !== dur && !/^(song|video|آهنگ|ویدیو)$/i.test(p)).slice(-2).join(', ') || sub;
    }
    const thumb =
      node.thumbnail?.contents?.[0]?.url ??
      node.thumbnails?.[0]?.url ??
      node?.thumbnail_renderer?.music_thumbnail_renderer?.data?.thumbnail?.contents?.[0]?.url;
    if (thumb) {
      if (!out.some((s) => s.videoId === vid)) {
        out.push({ videoId: vid, title, artist: artist || 'Unknown', duration: dur, thumb });
      }
    }
    return out;
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
