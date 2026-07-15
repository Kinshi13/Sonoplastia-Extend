"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LucideIcon } from "lucide-react";
import { StellaCore, StellaCoreAction } from "./StellaCore";
import { useScrollDirection } from "./useScrollDirection";

export type StellaDockItem = { href: string; label: string; icon: LucideIcon };

/**
 * Fase 11.8 (Parte 2-4) / 11.8.1 stabilization: the site's adaptive navigation core - Stella Core
 * sits center, elevated, flanked by two nav items per side. Fixed full-width at the bottom on
 * phones (safe-area aware); a centered floating dock with a max-width from `sm` up, so it never
 * stretches edge-to-edge on a tablet or desktop window. Compacts (shrinks, drops labels) on
 * continued downward scroll and expands again on upward scroll/idle.
 *
 * Fase 11.8.2 (Bloco A): StellaCore's menu is now a compact grid anchored to the star itself, so
 * it no longer needs to know the dock's measured height to stay clear of it. While the Core is
 * open: labels hide, non-active icons dim and stop accepting clicks, and the compact/expand
 * scroll toggle freezes at whatever state it was in - and the menu force-closes on every route
 * change so it can never linger over a new page.
 */
export function StellaDock({
  items,
  stellaCoreActions,
}: {
  items: [StellaDockItem, StellaDockItem, StellaDockItem, StellaDockItem];
  stellaCoreActions: StellaCoreAction[];
}) {
  const pathname = usePathname();
  const scrollCompact = useScrollDirection();
  const [coreOpen, setCoreOpen] = useState(false);
  const [left, leftInner, rightInner, right] = items;

  // Freeze whatever compact state the dock was in the moment the Core opens - Parte 11: "evitar
  // que o dock mude de altura durante a interação". React's documented "adjusting state during
  // render" pattern (compare against a tracked previous value, setState conditionally in the
  // render body) instead of an effect - this transition only happens on the render where
  // coreOpen actually flips, so it's a single extra render, not a cascade.
  const [prevCoreOpen, setPrevCoreOpen] = useState(false);
  const [frozenCompact, setFrozenCompact] = useState<boolean | null>(null);
  if (coreOpen !== prevCoreOpen) {
    setPrevCoreOpen(coreOpen);
    setFrozenCompact(coreOpen ? scrollCompact : null);
  }
  const compact = coreOpen ? frozenCompact ?? scrollCompact : scrollCompact;

  // Same pattern: close the menu the moment the route actually changes, not via an effect.
  const [prevPathname, setPrevPathname] = useState(pathname);
  if (pathname !== prevPathname) {
    setPrevPathname(pathname);
    if (coreOpen) setCoreOpen(false);
  }

  return (
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 flex justify-center px-3 pointer-events-none sm:bottom-4"
      style={{ paddingBottom: "max(env(safe-area-inset-bottom), 0px)", zIndex: zVar("var(--z-dock)") }}
    >
      {/* No backdrop-blur here on purpose (Bloco A7/A9 fix): `backdrop-filter` on this bar was
          creating a new CSS containing block for every `position: fixed` descendant - including
          StellaCore's own full-screen dim/blur overlay when the menu opens, which as a result
          rendered clipped to this bar's own small box instead of covering the viewport. */}
      <div
        className={`pointer-events-auto flex w-full items-center justify-between gap-1 border border-border-soft bg-surface-glass transition-[height,padding] duration-300 sm:w-auto sm:min-w-[320px] sm:gap-2 sm:px-2 sm:rounded-[var(--radius-hero)] ${
          compact ? "h-14 px-2" : "h-[68px] px-3"
        }`}
        style={{
          borderRadius: "var(--radius-hero) var(--radius-hero) 0 0",
          boxShadow: "var(--elevation-floating)",
        }}
      >
        <DockLink item={left} active={pathname === left.href} compact={compact} dimmed={coreOpen} />
        <DockLink item={leftInner} active={pathname === leftInner.href} compact={compact} dimmed={coreOpen} />

        <div className="relative -mt-8 shrink-0 sm:-mt-9">
          <StellaCore actions={stellaCoreActions} embedded onOpenChange={setCoreOpen} />
        </div>

        <DockLink item={rightInner} active={pathname === rightInner.href} compact={compact} dimmed={coreOpen} />
        <DockLink item={right} active={pathname === right.href} compact={compact} dimmed={coreOpen} />
      </div>
    </nav>
  );
}

function zVar(name: string): number {
  return name as unknown as number;
}

function DockLink({
  item,
  active,
  compact,
  dimmed,
}: {
  item: StellaDockItem;
  active: boolean;
  compact: boolean;
  dimmed: boolean;
}) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      aria-hidden={dimmed}
      tabIndex={dimmed ? -1 : undefined}
      className={`flex flex-col items-center justify-center gap-0.5 rounded-[var(--radius-md)] px-3 py-1.5 text-[11px] font-medium transition-[color,opacity] duration-200 ${
        dimmed ? "opacity-30 pointer-events-none" : "opacity-100"
      } ${active ? "text-primary" : "text-text-secondary hover:text-foreground"}`}
    >
      <Icon size={20} strokeWidth={active ? 2.4 : 2} />
      {!compact && !dimmed && <span>{item.label}</span>}
    </Link>
  );
}
