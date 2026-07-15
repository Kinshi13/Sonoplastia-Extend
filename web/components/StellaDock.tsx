"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LucideIcon } from "lucide-react";
import { StellaCore, StellaCoreAction } from "./StellaCore";
import { useScrollDirection } from "./useScrollDirection";

export type StellaDockItem = { href: string; label: string; icon: LucideIcon };

/**
 * Fase 11.8 (Parte 2-4): the site's adaptive navigation core - Stella Core sits center, elevated,
 * flanked by two nav items per side. Fixed full-width at the bottom on phones (safe-area aware);
 * a centered floating dock with a max-width from `sm` up, so it never stretches edge-to-edge on
 * a tablet or desktop window. Compacts (shrinks, drops labels) on continued downward scroll and
 * expands again on upward scroll/idle - see useScrollDirection for the threshold/rAF details.
 */
export function StellaDock({
  items,
  stellaCoreActions,
}: {
  items: [StellaDockItem, StellaDockItem, StellaDockItem, StellaDockItem];
  stellaCoreActions: StellaCoreAction[];
}) {
  const pathname = usePathname();
  const compact = useScrollDirection();
  const [left, leftInner, rightInner, right] = items;

  return (
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 z-30 flex justify-center px-3 pointer-events-none sm:bottom-4"
      style={{ paddingBottom: "max(env(safe-area-inset-bottom), 0px)" }}
    >
      <div
        className={`pointer-events-auto flex w-full items-center justify-between gap-1 border border-border-soft bg-surface-glass backdrop-blur-md transition-[height,padding] duration-300 sm:w-auto sm:min-w-[320px] sm:gap-2 sm:px-2 ${
          compact ? "h-14 px-2" : "h-[68px] px-3"
        }`}
        style={{
          borderRadius: "var(--radius-hero) var(--radius-hero) 0 0",
          boxShadow: "var(--elevation-floating)",
        }}
      >
        <DockLink item={left} active={pathname === left.href} compact={compact} />
        <DockLink item={leftInner} active={pathname === leftInner.href} compact={compact} />

        <div className="relative -mt-8 shrink-0 sm:-mt-9">
          <StellaCore actions={stellaCoreActions} embedded />
        </div>

        <DockLink item={rightInner} active={pathname === rightInner.href} compact={compact} />
        <DockLink item={right} active={pathname === right.href} compact={compact} />
      </div>

      {/* Desktop/tablet gets the same dock, just floating and rounded on every corner instead of
          docked to the screen edge - handled purely by the sm:bottom-4 + sm:rounded-full below. */}
      <style jsx>{`
        @media (min-width: 640px) {
          nav > div {
            border-radius: var(--radius-hero) !important;
          }
        }
      `}</style>
    </nav>
  );
}

function DockLink({ item, active, compact }: { item: StellaDockItem; active: boolean; compact: boolean }) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={`flex flex-col items-center justify-center gap-0.5 rounded-[var(--radius-md)] px-3 py-1.5 text-[11px] font-medium transition-colors ${
        active ? "text-primary" : "text-text-secondary hover:text-foreground"
      }`}
    >
      <Icon size={20} strokeWidth={active ? 2.4 : 2} />
      {!compact && <span>{item.label}</span>}
    </Link>
  );
}
