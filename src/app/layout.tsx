import type { Metadata, Viewport } from "next";
import "./globals.css";
import "vazirmatn/Vazirmatn-font-face.css";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "JixOne — Music Player",
  description: "JixOne: a beautiful music player with YouTube Music catalog — Persian & English",
  applicationName: "JixOne",
};

export const viewport: Viewport = {
  themeColor: "#0a0a0f",
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fa" dir="rtl" data-theme="cyber-red" data-glow="mid" suppressHydrationWarning>
      <body className="antialiased">
        {children}
        <Toaster position="bottom-center" theme="dark" richColors closeButton />
      </body>
    </html>
  );
}
