"use client";

import { LucideIcon, Lock } from "lucide-react";
import { StellaConstellationNode } from "./stellaCoreGeometry";

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
 * Fase 11.8.3 (Bloco C): a full mini card for each Stella Core action - Celestial Frame styling,
 * its own star-node, and a real title/subtitle instead of a bare label floating next to a circle.
 * Star-node and card are one component on purpose (Bloco C: "não posicionar label separadamente
 * com coordenadas independentes").
 */
export function StellaConstellationCard({
  action,
  node,
  index,
  onSelect,
  ref,
}: {
  action: StellaConstellationAction;
  node: StellaConstellationNode;
  index: number;
  onSelect: () => void;
  ref?: React.Ref<HTMLButtonElement>;
}) {
  const Icon = action.icon;
  const dir = node.branch === "R" ? 1 : -1;
  // Star-node position relative to the card's own center (card is offset outward from the star).
  const starOffsetX = node.starX - node.x;
  const starOffsetY = node.starY - node.y;

  return (
    <div
      className="stella-node absolute flex items-center"
      style={{
        left: "50%",
        bottom: "50%",
        transform: `translate(${node.x - node.cardWidth / 2}px, ${node.y - node.cardHeight / 2}px)`,
        animationDelay: `${index * 65}ms`,
        width: node.cardWidth,
        height: node.cardHeight,
      }}
    >
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
        onClick={onSelect}
        aria-label={action.title + (action.subtitle ? `, ${action.subtitle}` : "") + (action.locked ? " (recurso do plano superior)" : "")}
        title={action.subtitle ? `${action.title} - ${action.subtitle}` : action.title}
        className="stella-card group/card relative flex h-full w-full items-center gap-2.5 overflow-hidden border px-3 text-left transition-transform duration-200 hover:-translate-y-[2px] active:scale-[0.97]"
        style={{
          borderColor: "var(--border-soft)",
          borderRadius: "var(--radius-md)",
          background: `linear-gradient(155deg, var(--cc-nebula) 0%, var(--cc-nebula-elevated) 100%)`,
          boxShadow: "var(--elevation-raised)",
          clipPath:
            dir === 1
              ? "polygon(0 0, calc(100% - 9px) 0, 100% 9px, 100% 100%, 0 100%)"
              : "polygon(0 9px, 9px 0, 100% 0, 100% 100%, 0 100%)",
        }}
      >
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-200 group-hover/card:opacity-100"
          style={{ boxShadow: "0 0 0 1px var(--cc-polaris)55, 0 0 18px -4px var(--cc-polaris)70" }}
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
        .stella-star-node {
          animation: stella-star-pulse 1.8s ease-in-out infinite;
        }
        @keyframes stella-star-pulse {
          0%,
          100% {
            opacity: 0.65;
            transform: translate(-50%, -50%) scale(1);
          }
          50% {
            opacity: 1;
            transform: translate(-50%, -50%) scale(1.25);
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .stella-star-node {
            animation: none;
          }
        }
      `}</style>
    </div>
  );
}
