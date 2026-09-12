export interface Track {
  videoId: string;
  title: string;
  artist: string;
  artistId?: string;
  duration?: string; // "3:45"
  durationSec?: number;
  thumb: string; // proxied url
  album?: string;
}

export interface Playlist {
  id: string;
  name: string;
  createdAt: number;
  tracks: Track[];
  autoDownload?: boolean;
  source?: 'local' | 'ytmusic';
  ytId?: string;
}

export type DownloadStatus = 'pending' | 'extracting' | 'downloading' | 'done' | 'error' | 'paused' | 'waiting-net';

export interface DownloadItem {
  videoId: string;
  title: string;
  artist: string;
  thumb: string;
  durationSec: number;
  quality: QualityId;
  status: DownloadStatus;
  progress: number; // 0..1
  sizeBytes?: number;
  error?: string;
  addedAt: number;
}

export type QualityId = 'high' | 'mid' | 'low';

export interface QualityInfo {
  id: QualityId;
  label: { fa: string; en: string };
  itag: number; // preferred itag
  fallbackItags: number[];
  kbps: number;
  bytesPerSec: number;
}

export const QUALITIES: Record<QualityId, QualityInfo> = {
  high: { id: 'high', label: { fa: 'بالا', en: 'High' }, itag: 251, fallbackItags: [140, 250], kbps: 160, bytesPerSec: 20000 },
  mid: { id: 'mid', label: { fa: 'متوسط', en: 'Medium' }, itag: 250, fallbackItags: [249, 140], kbps: 70, bytesPerSec: 8750 },
  low: { id: 'low', label: { fa: 'کم‌حجم', en: 'Low' }, itag: 249, fallbackItags: [250, 139], kbps: 50, bytesPerSec: 6250 },
};

export function estimateSize(durationSec: number, q: QualityId): number {
  return Math.round((durationSec || 210) * QUALITIES[q].bytesPerSec);
}
