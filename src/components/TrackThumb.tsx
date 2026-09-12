'use client';
import { memo, useId } from 'react';
import type { Track } from '@/lib/types';
import { cn } from '@/lib/utils';

/** deterministic hash → palette pair */
const PALETTES: [string, string][] = [
  ['#ff2d55', '#8b5cf6'],
  ['#22d3ee', '#6366f1'],
  ['#ff2fa0', '#a855f7'],
  ['#1db954', '#14b8a6'],
  ['#ff6b35', '#f59e0b'],
  ['#3b82f6', '#ec4899'],
  ['#f43f5e', '#fb923c'],
  ['#8b5cf6', '#0ea5e9'],
  ['#f59e0b', '#7c3aed'],
  ['#14b8a6', '#f97316'],
  ['#6366f1', '#d946ef'],
  ['#059669', '#3b82f6'],
];
function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return h;
}
export function initialsOf(title: string): string {
  const words = title.replace(/[^\p{L}\p{N} ]/gu, '').trim().split(/\s+/).filter(Boolean);
  if (!words.length) return '♫';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

/**
 * Track artwork: real thumbnail when available, otherwise a designed
 * gradient cover (SVG initials — scales to any size) — guarantees zero
 * broken images, offline included.
 */
export const TrackThumb = memo(function TrackThumb({
  track,
  className,
  imgClassName,
  rounded = 'rounded-lg',
}: {
  track: Track;
  className?: string;
  imgClassName?: string;
  rounded?: string;
}) {
  const uid = useId().replace(/[:]/g, '');
  const gid = `tg${uid}`;
  const hid = `th${uid}`;
  if (track.thumb) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={track.thumb} alt="" loading="lazy" className={cn(rounded, 'object-cover', className, imgClassName)} />
    );
  }
  const [c1, c2] = PALETTES[hash(track.videoId) % PALETTES.length];
  const initials = initialsOf(track.title);
  return (
    <svg
      aria-hidden
      viewBox="0 0 100 100"
      preserveAspectRatio="xMidYMid slice"
      className={cn(rounded, 'block', className)}
    >
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={c1} />
          <stop offset="100%" stopColor={c2} />
        </linearGradient>
        <radialGradient id={hid} cx="0.25" cy="0" r="1.2">
          <stop offset="0%" stopColor="rgba(255,255,255,0.32)" />
          <stop offset="55%" stopColor="rgba(255,255,255,0)" />
        </radialGradient>
      </defs>
      <rect width="100" height="100" fill={`url(#${gid})`} />
      <rect width="100" height="100" fill={`url(#${hid})`} />
      <circle cx="78" cy="86" r="34" fill="rgba(255,255,255,0.08)" />
      <text
        x="50" y="50"
        textAnchor="middle" dominantBaseline="central"
        fontFamily="Vazirmatn, sans-serif" fontWeight="800" fontSize="34"
        fill="rgba(255,255,255,0.95)"
      >
        {initials}
      </text>
    </svg>
  );
});
