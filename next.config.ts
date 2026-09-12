import type { NextConfig } from "next";

/**
 * STATIC_EXPORT=1 → fully static build (out/) for embedding inside the Android APK.
 * Default → standalone server build (web deployment / optional custom server mode).
 */
const isStatic = process.env.STATIC_EXPORT === "1";

const nextConfig: NextConfig = {
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
