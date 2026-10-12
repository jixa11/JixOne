import type { NextConfig } from "next";
import { fileURLToPath } from "url";
import { dirname } from "path";

/**
 * STATIC_EXPORT=1 → fully static build (out/) for embedding inside the Android APK.
 * Default → standalone server build (web deployment / optional custom server mode).
 */
const isStatic = process.env.STATIC_EXPORT === "1";

/**
 * Pin the standalone layout to this repo. Without it, Next infers the
 * "workspace root" by walking up for a lockfile — clone the repo inside any
 * parent workspace and the standalone output lands at
 * .next/standalone/<relative-path-to-that-root>/server.js, which silently
 * breaks the build/start scripts (they expect .next/standalone/server.js).
 */
const repoRoot = dirname(fileURLToPath(import.meta.url));

const nextConfig: NextConfig = {
  outputFileTracingRoot: repoRoot,
  ...(isStatic
    ? { output: "export" as const, basePath: "/assets/web" } // embedded in APK under WebViewAssetLoader /assets/ handler
    : { output: "standalone" as const }),
  images: { unoptimized: true },
  typescript: {
    ignoreBuildErrors: true,
  },
  reactStrictMode: false,
};

export default nextConfig;
