"use client";

import { useEffect, useRef, useState } from "react";
import { LucideIcon, MoreHorizontal } from "lucide-react";
import { computeConstellationGeometry, maxRealActions, StellaConstellationNode } from "./stellaCoreGeometry";
import { StellaConstellationCard } from "./StellaConstellationCard";
import { useViewportWidth } from "./useViewportWidth";
import { emitStellaCoreOpenChange } from "./stellaCoreOpenBus";

export type StellaCoreAction = {
  id: string;
  label: string;
  /** Short description under the title in the mini card (Bloco D) - e.g. "Ver e editar". Omitted
   *  entirely on narrow phones, never truncated into something unreadable. */
  subtitle?: string;
  icon: LucideIcon;
  locked?: boolean;
  onClick: () => void;
};

const NODE_SIZE = 56;

/** React's CSSProperties types `zIndex` as a number, but these are CSS custom properties (see
 *  globals.css's --z-* scale) - this is just a typed pass-through, not a real cast concern. */
function zVar(name: string): number {
  return name as unknown as number;
}

/**
 * Fase 11.8.3 (Bloco A): the "Constellation Action Map" - two branches of connected star-nodes
 * and mini cards growing up from the central star, replacing the single fan of small circular
 * nodes with bare labels. Screens hand it a small contextual action list; this component only
 * knows how to split it into branches, lay them out, and animate the reveal/dismiss sequence.
 *
 * HOTFIX (11.8.3): the dim/blur effect is no longer a `backdrop-filter` overlay. That approach
 * let Chromium/Safari's compositor sweep the Stella Core's own star/cards into the blur sampling
 * whenever an ancestor established `isolation: isolate` (here, <body>, needed for the starfield
 * background) - confirmed visually, not just a spec-reading guess. The blur now applies directly
 * to the page's own content wrapper (see PageBlurWrapper in the root layout) via a small event
 * bus; this backdrop button is just a plain dark scrim + click-to-close target, never blurred
 * itself and never able to blur anything outside its own flat color.
 */
export function StellaCore({
  actions,
  embedded = false,
  onOpenChange,
}: {
  actions: StellaCoreAction[];
  embedded?: boolean;
  onOpenChange?: (open: boolean) => void;
}) {
  const [open, setOpenState] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const firstActionRef = useRef<HTMLButtonElement>(null);
  const viewportWidth = useViewportWidth();

  function setOpen(next: boolean) {
    setOpenState(next);
    if (!next) setMoreOpen(false);
    onOpenChange?.(next);
    emitStellaCoreOpenChange(next);
  }

  useEffect(() => {
    if (!open) return;
    firstActionRef.current?.focus();
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Bloco K: force-close if this instance unmounts/hides while open (route change etc.) so the
  // page-blur bus never gets stuck "on" with nothing left to turn it off.
  useEffect(() => {
    return () => {
      if (open) emitStellaCoreOpenChange(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (actions.length === 0) return null;

  const cap = maxRealActions(viewportWidth || 360);
  const needsOverflow = actions.length > cap;
  const visibleActions = needsOverflow ? actions.slice(0, cap - 1) : actions.slice(0, cap);
  const overflowActions = needsOverflow ? actions.slice(visibleActions.length) : [];
  const nodeCount = visibleActions.length + (overflowActions.length > 0 ? 1 : 0);
  const geometry = computeConstellationGeometry(nodeCount, viewportWidth || 360);
  const svgWidth = geometry.zoneWidth || NODE_SIZE;
  const svgHeight = geometry.zoneHeight + NODE_SIZE;
  const moreNode: StellaConstellationNode | undefined = overflowActions.length > 0 ? geometry.nodes[visibleActions.length] : undefined;

  return (
    <>
      {/* Bloco G: a plain flat scrim, on purpose - no backdrop-filter here anymore (see the
          HOTFIX note above). It only darkens/blocks clicks; the actual blur lives on the page's
          own content wrapper, which this button visually sits above but never touches. */}
      {open && (
        <button
          aria-label="Fechar menu de ações"
          onClick={() => setOpen(false)}
          className="fixed inset-0 cursor-default"
          style={{ background: "rgba(4, 6, 16, 0.55)", zIndex: zVar("var(--z-dock-backdrop)") }}
        />
      )}

      <div className={embedded ? "relative flex justify-center pointer-events-none" : "fixed inset-x-0 bottom-6 flex justify-center pointer-events-none"} style={{ zIndex: embedded ? undefined : zVar("var(--z-core-trigger)") }}>
        {/* StellaConstellationViewport (Bloco H): a local, bounded layer anchored to the star.
            Its size comes only from the geometry's own branch math, never from page content. */}
        <div className="relative pointer-events-auto" style={{ isolation: "isolate" }}>
          {open && (
            <>
              <svg
                className="absolute left-1/2 bottom-1/2 -translate-x-1/2 pointer-events-none"
                width={svgWidth}
                height={svgHeight}
                viewBox={`${-svgWidth / 2} ${-(svgHeight - NODE_SIZE / 2)} ${svgWidth} ${svgHeight}`}
                style={{ overflow: "visible", zIndex: zVar("var(--z-core-connectors)") }}
                aria-hidden="true"
              >
                {geometry.nodes.map((node, i) => (
                  <path
                    key={i}
                    d={`M 0 ${-NODE_SIZE / 2} L ${node.starX} ${node.starY}`}
                    stroke="var(--cc-polaris)"
                    strokeWidth={1.5}
                    strokeLinecap="round"
                    opacity={0.55}
                    className="stella-line"
                    pathLength={1}
                    style={{ animationDelay: `${i * 45}ms` }}
                  />
                ))}
              </svg>

              {visibleActions.map((action, i) => (
                <StellaConstellationCard
                  key={action.id}
                  ref={i === 0 ? firstActionRef : undefined}
                  action={{
                    id: action.id,
                    title: action.label,
                    subtitle: action.subtitle,
                    icon: action.icon,
                    locked: action.locked,
                    onClick: action.onClick,
                  }}
                  node={geometry.nodes[i]}
                  index={i}
                  onSelect={() => {
                    setOpen(false);
                    action.onClick();
                  }}
                />
              ))}

              {moreNode && (
                <StellaConstellationCard
                  action={{ id: "more", title: "Mais", icon: MoreHorizontal, onClick: () => setMoreOpen((v) => !v) }}
                  node={moreNode}
                  index={visibleActions.length}
                  onSelect={() => setMoreOpen((v) => !v)}
                />
              )}

              {moreNode && moreOpen && (
                <div
                  role="menu"
                  className="absolute flex w-max max-w-[220px] flex-col gap-1 rounded-[var(--radius-md)] border p-1.5 shadow-lg"
                  style={{
                    left: "50%",
                    bottom: "50%",
                    transform: `translate(${moreNode.x - 110}px, ${moreNode.y - moreNode.cardHeight / 2 - 8}px) translateY(-100%)`,
                    background: "var(--cc-nebula)",
                    borderColor: "var(--cc-horizon)",
                    zIndex: zVar("var(--z-modal)"),
                  }}
                >
                  {overflowActions.map((action) => {
                    const Icon = action.icon;
                    return (
                      <button
                        key={action.id}
                        role="menuitem"
                        onClick={() => {
                          setOpen(false);
                          action.onClick();
                        }}
                        className="flex items-center gap-2 rounded-[var(--radius-sm)] px-2.5 py-2 text-sm text-left hover:bg-primary-container/30"
                        style={{ color: "var(--foreground)" }}
                      >
                        <Icon size={16} className="shrink-0" style={{ color: "var(--cc-polaris)" }} />
                        {action.label}
                      </button>
                    );
                  })}
                </div>
              )}
            </>
          )}

          <button
            ref={triggerRef}
            onClick={() => setOpen(!open)}
            aria-label={open ? "Fechar menu de ações" : "Abrir menu de ações (Stella Core)"}
            aria-haspopup="menu"
            aria-expanded={open}
            className={`stella-trigger relative flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform active:scale-90 ${open ? "stella-trigger-open" : ""}`}
            style={{ background: "var(--cc-nebula)", border: "1px solid var(--cc-horizon)", zIndex: zVar("var(--z-core-trigger)") }}
          >
            <FourPointStar open={open} />
          </button>
        </div>

        <style jsx>{`
          .stella-node {
            animation: stella-appear var(--cc-duration-standard, 280ms) var(--cc-ease-stellar, ease) both;
          }
          .stella-line {
            stroke-dasharray: 1;
            stroke-dashoffset: 1;
            animation: stella-draw 260ms var(--cc-ease-stellar, ease) both;
          }
          @keyframes stella-appear {
            from {
              opacity: 0;
              transform: translateY(6px) scale(0.85);
            }
            to {
              opacity: 1;
              transform: translateY(0) scale(1);
            }
          }
          @keyframes stella-draw {
            to {
              stroke-dashoffset: 0;
            }
          }
          .stella-trigger-open {
            animation: stella-pulse 900ms var(--cc-ease-stellar, ease) 1;
          }
          @keyframes stella-pulse {
            0% {
              box-shadow: 0 0 0 0 var(--cc-polaris)55;
            }
            70% {
              box-shadow: 0 0 0 10px var(--cc-polaris)00;
            }
            100% {
              box-shadow: 0 0 0 0 var(--cc-polaris)00;
            }
          }
          @media (prefers-reduced-motion: reduce) {
            .stella-node,
            .stella-line,
            .stella-trigger-open {
              animation: none !important;
              opacity: 1 !important;
              stroke-dashoffset: 0 !important;
            }
          }
        `}</style>
      </div>
    </>
  );
}

function FourPointStar({ open }: { open: boolean }) {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      style={{
        transform: `rotate(${open ? 45 : 0}deg)`,
        transition: "transform var(--cc-duration-standard, 280ms) var(--cc-ease-stellar, ease)",
      }}
    >
      <path
        d="M12 2 L14.2 9.8 L22 12 L14.2 14.2 L12 22 L9.8 14.2 L2 12 L9.8 9.8 Z"
        fill="var(--cc-polaris)"
        stroke="var(--cc-comet)"
        strokeOpacity={0.25}
        strokeWidth={1.5}
      />
    </svg>
  );
}
