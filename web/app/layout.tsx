import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import { RegisterServiceWorker } from "@/components/RegisterServiceWorker";
import { IosInstallHint } from "@/components/IosInstallHint";
import { ConstellationScene } from "@/components/ConstellationScene";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Escala Church",
  description: "Escala, doxologia e anúncios da igreja",
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: "Escala Church",
  },
  formatDetection: {
    telephone: false,
  },
  icons: {
    icon: [{ url: "/icons/favicon-32.png", sizes: "32x32", type: "image/png" }],
    apple: [{ url: "/icons/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#4A5FE0" },
    { media: "(prefers-color-scheme: dark)", color: "#090B14" },
  ],
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="pt-BR"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="isolate min-h-full flex flex-col bg-background text-foreground">
        <ConstellationScene />
        <RegisterServiceWorker />
        <header
          className="sticky top-0 z-20 border-b border-border-soft bg-surface-glass backdrop-blur-md"
          style={{ paddingTop: "env(safe-area-inset-top)" }}
        >
          <div className="mx-auto flex max-w-[1400px] items-center justify-between px-5 py-4">
            <Link href="/" className="flex items-center gap-2 text-lg font-semibold tracking-tight text-primary">
              <span aria-hidden="true" className="text-accent-star">✦</span>
              Escala Church
            </Link>
            <nav className="flex items-center gap-5 text-sm">
              <Link
                href="/admin"
                className="rounded-full bg-primary px-4 py-1.5 text-white shadow-[var(--elevation-raised)] hover:opacity-90 transition-opacity"
              >
                Admin
              </Link>
            </nav>
          </div>
        </header>
        <main className="flex-1 mx-auto w-full max-w-[1400px] px-5 py-8">{children}</main>
        <footer className="border-t border-border-soft py-6 text-center text-xs text-text-muted">
          Escala Church
        </footer>
        <IosInstallHint />
      </body>
    </html>
  );
}
