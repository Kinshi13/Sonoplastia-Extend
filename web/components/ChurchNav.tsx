"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/**
 * Fase 8 (web mobile redesign): the public church nav used to be a `flex-wrap` row of plain text
 * links - fine on desktop, but on a phone it wraps into a ragged second line and gives no sense
 * of which page you're on. This is a horizontally scrollable pill-tab strip instead (no wrap, one
 * thumb swipe reaches every tab), with the active page highlighted - something the old nav never
 * showed at all.
 */
export function ChurchNav({ links }: { links: { href: string; label: string }[] }) {
  const pathname = usePathname();

  return (
    <nav className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {links.map((link) => {
        const active = pathname === link.href;
        return (
          <Link
            key={link.href}
            href={link.href}
            className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-colors ${
              active
                ? "bg-primary text-white"
                : "bg-primary-container/40 text-foreground/80 hover:text-primary"
            }`}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}
