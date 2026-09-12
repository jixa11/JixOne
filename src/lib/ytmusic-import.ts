'use client';
import type { Track } from '@/lib/types';
import { DEMO_YT_PLAYLISTS } from '@/lib/catalog';

export interface ImportedPlaylist {
  name: string;
  ytId?: string;
  tracks: Track[];
}

const YT = 'https://www.googleapis.com/youtube/v3';

/**
 * Real path: list the signed-in user's YouTube Music playlists via YouTube Data API v3,
 * then fetch each playlist's first page of items and map them to Track.
 */
export async function fetchYTMusicPlaylists(accessToken: string): Promise<ImportedPlaylist[]> {
  const r = await fetch(`${YT}/playlists?part=snippet,contentDetails&mine=true&maxResults=50`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!r.ok) throw new Error(`playlists.list failed (${r.status})`);
  const data = await r.json();
  const items: any[] = data.items ?? [];
  if (!items.length) return [];

  const out: ImportedPlaylist[] = [];
  await Promise.all(
    items.slice(0, 20).map(async (pl) => {
      const name: string = pl?.snippet?.title || 'Untitled';
      const ytId: string | undefined = pl?.id;
      let tracks: Track[] = [];
      try {
        const r2 = await fetch(
          `${YT}/playlistItems?part=snippet,contentDetails&playlistId=${pl.id}&maxResults=50`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        if (r2.ok) {
          const d2 = await r2.json();
          tracks = (d2.items ?? [])
            .map((it: any) => {
              const vid: string | undefined = it?.contentDetails?.videoId;
              const title: string = it?.snippet?.title || '';
              const owner: string = it?.snippet?.videoOwnerChannelTitle || '';
              const thumb: string =
                it?.snippet?.thumbnails?.medium?.url || it?.snippet?.thumbnails?.default?.url || '';
              if (!vid || !title || title === 'Private video' || title === 'Deleted video') return null;
              return { videoId: vid, title, artist: owner || 'YouTube', thumb } as Track;
            })
            .filter(Boolean);
        }
      } catch {
        /* keep whatever we have */
      }
      out.push({ name, ytId, tracks });
    })
  );
  return out;
}

/** Demo path (no OAuth client id configured): realistic YTMusic-shaped library from the curated catalog */
export function demoYTMusicPlaylists(trackIndex: Record<string, Track>): ImportedPlaylist[] {
  return DEMO_YT_PLAYLISTS.map((pl) => ({
    name: pl.name,
    ytId: `demo-${pl.name.toLowerCase().replace(/\s+/g, '-')}`,
    tracks: pl.ids.map((id) => trackIndex[id]).filter(Boolean),
  })).filter((pl) => pl.tracks.length > 0);
}
