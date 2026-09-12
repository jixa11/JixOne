'use client';

/**
 * Google Identity Services (implicit flow) — real OAuth path.
 * Requires NEXT_PUBLIC_GOOGLE_CLIENT_ID to be configured at build time.
 * Scope: youtube.readonly → lets us list the user's YouTube Music playlists.
 */

export function googleClientId(): string | null {
  return (process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID as string | undefined) || null;
}

declare global {
  interface Window {
    google?: any;
  }
}

let gsiLoading: Promise<void> | null = null;

function loadGsi(): Promise<void> {
  if (typeof window === 'undefined') return Promise.reject(new Error('no window'));
  if (window.google?.accounts?.oauth2) return Promise.resolve();
  if (gsiLoading) return gsiLoading;
  gsiLoading = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://accounts.google.com/gsi/client';
    s.async = true;
    s.onload = () => resolve();
    s.onerror = () => { gsiLoading = null; reject(new Error('Failed to load Google Identity Services')); };
    document.head.appendChild(s);
  });
  return gsiLoading;
}

/** Opens the Google consent popup and resolves with an access token */
export async function requestGoogleToken(): Promise<string> {
  const clientId = googleClientId();
  if (!clientId) throw new Error('NO_CLIENT_ID');
  await loadGsi();
  return new Promise((resolve, reject) => {
    try {
      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: 'https://www.googleapis.com/auth/youtube.readonly',
        callback: (resp: any) => {
          if (resp?.access_token) resolve(resp.access_token);
          else reject(new Error(resp?.error || 'OAuth failed'));
        },
      });
      client.requestAccessToken({ prompt: '' });
    } catch (e) {
      reject(e);
    }
  });
}

export interface GoogleProfile {
  name: string;
  email: string;
  picture?: string;
}

/** GET /oauth2/v3/userinfo with the access token */
export async function fetchGoogleProfile(accessToken: string): Promise<GoogleProfile> {
  const r = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!r.ok) throw new Error('userinfo failed');
  const d = await r.json();
  return { name: d.name || d.email, email: d.email, picture: d.picture };
}
