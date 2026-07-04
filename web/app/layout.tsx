import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import { RegisterServiceWorker } from "@/components/RegisterServiceWorker";
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
    statusBarStyle: "default",
    title: "Escala Church",
  },
  icons: {
    icon: [{ url: "/icons/favicon-32.png", sizes: "32x32", type: "image/png" }],
    apple: [{ url: "/icons/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
  },
};

export const viewport: Viewport = {
  themeColor: "#3B7DDD",
  width: "device-width",
  initialScale: 1,
};

const navLinks = [
  { href: "/", label: "Escala" },
  { href: "/doxologia", label: "Doxologia" },
  { href: "/anuncios", label: "Anúncios" },
];

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
      <body className="min-h-full flex flex-col bg-background text-foreground">
        <RegisterServiceWorker />
        <header className="border-b border-divider bg-surface">
          <div className="mx-auto flex max-w-4xl items-center justify-between px-5 py-4">
            <Link href="/" className="text-lg font-semibold text-primary">
              Escala Church
            </Link>
            <nav className="flex items-center gap-5 text-sm">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="text-foreground/80 hover:text-primary transition-colors"
                >
                  {link.label}
                </Link>
              ))}
              <Link
                href="/admin"
                className="rounded-full bg-primary px-4 py-1.5 text-white hover:opacity-90 transition-opacity"
              >
                Admin
              </Link>
            </nav>
          </div>
        </header>
        <main className="flex-1 mx-auto w-full max-w-4xl px-5 py-8">{children}</main>
        <footer className="border-t border-divider py-6 text-center text-xs text-text-secondary">
          Escala Church
        </footer>
      </body>
    </html>
  );
}
