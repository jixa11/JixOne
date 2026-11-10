/**
 * Regenerates the PWA icons in public/icons from the app icon (src/app/icon.png).
 *   bun scripts/make-pwa-icons.mjs
 * - icon-192 / icon-512: plain squares for the manifest
 * - maskable-*: the mark at 72% on the app's dark base, inside the launcher
 *   safe zone (Android masks may crop up to ~25% at the edges)
 */
import sharp from 'sharp';
import { mkdirSync } from 'node:fs';

const SRC = 'src/app/icon.png';
const OUT = 'public/icons';
mkdirSync(OUT, { recursive: true });

const BG = { r: 10, g: 10, b: 15, alpha: 1 }; // #0a0a0f — the app's base color

await sharp(SRC).resize(512, 512, { fit: 'cover' }).png().toFile(`${OUT}/icon-512.png`);
await sharp(SRC).resize(192, 192, { fit: 'cover' }).png().toFile(`${OUT}/icon-192.png`);

async function maskable(size) {
  const inner = Math.round(size * 0.72);
  const content = await sharp(SRC)
    .resize(inner, inner, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer();
  await sharp({ create: { width: size, height: size, channels: 4, background: BG } })
    .composite([{ input: content, gravity: 'center' }])
    .png()
    .toFile(`${OUT}/maskable-${size}.png`);
}

await maskable(192);
await maskable(512);
console.log('PWA icons written to', OUT);
