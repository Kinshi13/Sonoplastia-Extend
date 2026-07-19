"use client";

import { LucideIcon, Lock } from "lucide-react";
import { StellaConstellationNode } from "./stellaCoreGeometry";

/** React's CSSProperties types `zIndex` as a number, but these are CSS custom properties (see
 *  globals.css's --z-* scale) - this is just a typed pass-through, not a real cast concern. */
function zVar(name: string): number {
  return name as unknown as number;
}

export type StellaConstellationAction = {
  id: string;
  title: string;
  subtitle?: string;
  icon: LucideIcon;
  locked?: boolean;
  badge?: string;
  onClick: () => void;
};

/**
 * Fase 11.8.3 (Bloco C) / HOTFIX (11.8.3.1): a full mini card for each Stella Core action -
 * Celestial Frame styling, its own star-node, and a real title/subtitle instead of a bare label
 * floating next to a circle. Star-node and card are one component on purpose (Bloco C: "não
 * posicionar label separadamente com coordenadas independentes").
 *
 * HOTFIX: this card previously had no explicit z-index, so it sat below the backdrop's z=35
 * scrim (see StellaCore.tsx) - visually darkening it and swallowing every click. Now explicit
 * (z-core-actions) and paired with a dedicated star-glow layer so the card reads clearly even
 * against a dimmed, blurred background. `onActive` reports hover/focus up to StellaCore so it can
 * light up only this node's own connector (Bloco 11) instead of all of them at once.
 */
export function StellaConstellationCard({
  action,
  node,
  index,
  onSelect,
  onActive,
  ref,
}: {
  action: StellaConstellationAction;
  node: StellaConstellationNode;
  index: number;
  onSelect: () => void;
  onActive?: (active: boolean) => void;
  ref?: React.Ref<HTMLButtonElement>;
}) {
  if (process.env.NODE_ENV !== "production" && typeof action.onClick !== "function") {
    console.error(`Stella Core action "${action.id}" has no onAction.`);
  }

  const Icon = action.icon;
  const dir = node.branch === "R" ? 1 : -1;
  // Star-node position relative to the card's own center (card is offset outward from the star).
  const starOffsetX = node.starX - node.x;
  const starOffsetY = node.starY - node.y;
  const disabled = typeof action.onClick !== "function";

  return (
    <div
      className="stella-node absolute flex items-center"
      style={{
        left: "50%",
        bottom: "50%",
        transform: `translate(${node.x - node.cardWidth / 2}px, ${node.y - node.cardHeight / 2}px)`,
        animationDelay: `${index * 65}ms`,
        width: node.cardWidth,
        height: Math.max(node.cardHeight, 44),
        zIndex: zVar("var(--z-core-actions)"),
      }}
    >
      {/* StellaCardStarGlow (Bloco 7-8): a soft halo + a brighter point near the star-node, behind
          the card's own frame/content - never a flat neon rectangle. Kept as plain radial
          gradients (no blur() filter) so it stays cheap even with several cards open at once. */}
      <span
        aria-hidden="true"
        className="stella-glow-halo pointer-events-none absolute rounded-full"
        style={{
          left: `calc(50% + ${starOffsetX * 0.5}px)`,
          top: `calc(50% + ${starOffsetY * 0.5}px)`,
          transform: "translate(-50%, -50%)",
          width: node.cardWidth * 1.6,
          height: node.cardWidth * 1.6,
          background: "radial-gradient(circle, var(--cc-polaris) 0%, transparent 68%)",
          opacity: 0.16,
        }}
      />

      {/* Star-node: a small glowing point where the connector ends, overlapping the card's near
          corner (Bloco E: "parcialmente sobreposta à moldura"). */}
      <span
        aria-hidden="true"
        className="stella-star-node absolute rounded-full"
        style={{
          left: `calc(50% + ${starOffsetX}px)`,
          top: `calc(50% + ${starOffsetY}px)`,
          transform: "translate(-50%, -50%)",
          width: 7,
          height: 7,
          background: "var(--cc-comet)",
          boxShadow: "0 0 6px 1px var(--cc-comet)",
        }}
      />

      <button
        ref={ref}
        role="menuitem"
        onClick={disabled ? undefined : onSelect}
        onPointerEnter={() => onActive?.(true)}
        onPointerLeave={() => onActive?.(false)}
        onFocus={() => onActive?.(true)}
        onBlur={() => onActive?.(false)}
        disabled={disabled}
        aria-label={action.title + (action.subtitle ? `, ${action.subtitle}` : "") + (action.locked ? " (recurso do plano superior)" : "")}
        title={action.subtitle ? `${action.title} - ${action.subtitle}` : action.title}
        className="stella-card group/card relative flex h-full min-h-[44px] w-full items-center gap-2.5 overflow-hidden border px-3 text-left transition-transform duration-100 hover:-translate-y-[2px] active:scale-[0.97] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
        style={{
          borderColor: "var(--border-soft)",
          borderRadius: "var(--radius-md)",
          background: `linear-gradient(155deg, var(--cc-nebula) 0%, var(--cc-nebula-elevated) 100%)`,
          boxShadow: "var(--elevation-elevated)",
          outlineColor: "var(--cc-polaris)",
          clipPath:
            dir === 1
              ? "polygon(0 0, calc(100% - 9px) 0, 100% 9px, 100% 100%, 0 100%)"
              : "polygon(0 9px, 9px 0, 100% 0, 100% 100%, 0 100%)",
        }}
      >
        {/* Bloco 10: border brightens + halo intensifies on hover/focus, not just an outer glow
            the low-contrast border alone would be easy to miss against a blurred backdrop. */}
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-200 group-hover/card:opacity-100 group-focus-visible/card:opacity-100"
          style={{ boxShadow: "0 0 0 1.5px var(--cc-polaris), 0 0 20px -4px var(--cc-polaris)" }}
        />
        <span
          className="relative flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
          style={{
            background: action.locked ? "var(--cc-nebula)" : "var(--primary-container)",
            color: action.locked ? "var(--cc-comet)" : "var(--on-primary-container)",
          }}
        >
          <Icon size={16} />
          {action.locked && (
            <span
              className="absolute -right-1 -top-1 flex h-3.5 w-3.5 items-center justify-center rounded-full"
              style={{ background: "var(--cc-comet)", color: "var(--cc-nebula)" }}
            >
              <Lock size={8} />
            </span>
          )}
        </span>
        <span className="relative min-w-0 flex-1">
          <span className="block truncate text-[13px] font-semibold leading-tight" style={{ color: "var(--foreground)" }}>
            {action.title}
          </span>
          {node.showSubtitle && action.subtitle && (
            <span className="block truncate text-[11px] leading-tight" style={{ color: "var(--text-secondary)" }}>
              {action.subtitle}
            </span>
          )}
        </span>
        {action.badge && (
          <span
            className="relative shrink-0 rounded-full px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide"
            style={{ background: "var(--accent-star)", color: "var(--cc-nebula)" }}
          >
            {action.badge}
          </span>
        )}
      </button>

      <style jsx>{`
        /* Web Fase 6.7 - the star-node/halo used to "breathe" continuously (infinite animation)
           for as long as Stella Core stayed open, which is exactly the kind of always-on cost
           section 10 asks to remove. Both now sit at a static resting value and only change on
           the card's own hover/focus (via .group/card, same mechanism the border glow already
           uses two blocks up) - a state change, not a loop. */
        .stella-star-node {
          opacity: 0.75;
          transition: opacity 200ms var(--cc-ease-stellar, ease);
        }
        :global(.stella-card:hover) .stella-star-node,
        :global(.stella-card:focus-visible) .stella-star-node {
          opacity: 1;
        }
        .stella-glow-halo {
          opacity: 0.16;
          transition: opacity 200ms var(--cc-ease-stellar, ease);
        }
        :global(.stella-card:hover) .stella-glow-halo,
        :global(.stella-card:focus-visible) .stella-glow-halo {
          opacity: 0.22;
        }
      `}</style>
    </div>
  );
}
