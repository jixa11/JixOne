'use client';
import { useAuth } from '@/store/auth';
import { useLibrary } from '@/store/library';
import { googleClientId, requestGoogleToken, fetchGoogleProfile } from '@/lib/google-auth';
import { fetchYTMusicPlaylists, demoYTMusicPlaylists } from '@/lib/ytmusic-import';
import { CURATED_TRACKS } from '@/lib/catalog';
import type { Lang } from '@/lib/i18n';

export type SignInResult =
  | { ok: true; mode: 'real' | 'demo'; imported: number; name: string }
  | { ok: false; reason: 'no-playlists' | 'oauth-error' };

/** index of curated tracks by videoId (for the demo library) */
let demoIndex: Record<string, import('@/lib/types').Track> | null = null;
function getDemoIndex() {
  if (!demoIndex) {
    demoIndex = {};
    for (const tr of CURATED_TRACKS) demoIndex[tr.videoId] = tr;
  }
  return demoIndex;
}

/**
 * Full sign-in + YouTube Music playlist import.
 * Real OAuth when NEXT_PUBLIC_GOOGLE_CLIENT_ID exists, demo account otherwise.
 */
export async function signInAndImport(lang: Lang): Promise<SignInResult> {
  const clientId = googleClientId();

  if (clientId) {
    // ---- REAL PATH ----
    try {
      const token = await requestGoogleToken();
      const [profile, playlists] = await Promise.all([
        fetchGoogleProfile(token).catch(() => null),
        fetchYTMusicPlaylists(token),
      ]);
      if (!playlists.length) return { ok: false, reason: 'no-playlists' };
      const count = useLibrary.getState().importYTMusic(playlists);
      useAuth.getState().setUser(
        profile ?? { name: lang === 'fa' ? 'کاربر گوگل' : 'Google user', email: '' }
      );
      return { ok: true, mode: 'real', imported: count, name: profile?.name ?? '' };
    } catch {
      return { ok: false, reason: 'oauth-error' };
    }
  }

  // ---- DEMO PATH ----
  const playlists = demoYTMusicPlaylists(getDemoIndex());
  const count = useLibrary.getState().importYTMusic(playlists);
  useAuth.getState().setUser({
    name: lang === 'fa' ? 'سارا محمدی' : 'Alex Carter',
    email: lang === 'fa' ? 'sara.mohammadi@gmail.com' : 'alex.carter@gmail.com',
    demo: true,
  });
  return { ok: true, mode: 'demo', imported: count, name: useAuth.getState().user?.name ?? '' };
}

/** re-sync (signed in) */
export async function resyncPlaylists(lang: Lang): Promise<SignInResult> {
  return signInAndImport(lang);
}
