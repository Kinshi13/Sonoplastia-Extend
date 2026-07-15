"use client";

import { useEffect, useRef, useState } from "react";
import { LucideIcon, Lock, MoreHorizontal } from "lucide-react";
import { computeStellaCoreGeometry, MAX_VISIBLE_ACTIONS } from "./stellaCoreGeometry";
import { useViewportWidth } from "./useViewportWidth";

export type StellaCoreAction = {
  id: string;
  label: string;
  /** Shown instead of `label` on narrow phones (Parte 7) - falls back to `label` when omitted.
   *  `label` is always used for aria-label/tooltip regardless. */
  shortLabel?: string;
  icon: LucideIcon;
  locked?: boolean;
  onClick: () => void;
};

const NODE_SIZE = 48;

/** React's CSSProperties types `zIndex` as a number, but these are CSS custom properties (see
 *  globals.css's --z-* scale) - this is just a typed pass-through, not a real cast concern. */
function zVar(name: string): number {
  return name as unknown as number;
}

/**
 * Web mobile/desktop equivalent of the Android Stella Core (Fase 5) - same idea, not a pixel
 * copy: a four-point star that opens into an upward fan of contextual actions with constellation
 * lines connecting them, instead of a generic FAB "+" or a dropdown menu. Screens hand it their
 * own small action list; this component only knows how to lay them out and animate.
 *
 * Fase 11.8.1 stabilization: geometry now comes from computeStellaCoreGeometry (radius clamped to
 * both the viewport width and [reserveBottom], the dock's own measured height) instead of a
 * hardcoded radius - see stellaCoreGeometry.ts for the bug this fixes. More than
 * MAX_VISIBLE_ACTIONS actions collapse into a "Mais" node instead of crowding the arc further.
 * Escape closes the menu and returns focus to the trigger; the first action receives focus on
 * open. [onOpenChange] lets the hosting StellaDock dim its own icons while the menu is open.
 */
export function StellaCore({
  actions,
  embedded = false,
  reserveBottom = 0,
  onOpenChange,
}: {
  actions: StellaCoreAction[];
  embedded?: boolean;
  reserveBottom?: number;
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

  if (actions.length === 0) return null;

  const visibleActions = actions.slice(0, MAX_VISIBLE_ACTIONS - (actions.length > MAX_VISIBLE_ACTIONS ? 1 : 0));
  const overflowActions = actions.length > MAX_VISIBLE_ACTIONS ? actions.slice(visibleActions.length) : [];
  const nodeCount = visibleActions.length + (overflowActions.length > 0 ? 1 : 0);
  const geometry = computeStellaCoreGeometry(nodeCount, viewportWidth || 360, reserveBottom || 96);

  return (
    <div className={embedded ? "relative flex justify-center pointer-events-none" : "fixed inset-x-0 bottom-6 flex justify-center pointer-events-none"} style={{ zIndex: embedded ? undefined : zVar("var(--z-core-trigger)") }}>
      {/* Parte 9: local, bounded menu layer anchored to the star - everything inside it (backdrop
          excluded, which intentionally covers the full page) moves as one unit and never lays
          out relative to the page. */}
      <div className="relative pointer-events-auto" style={{ isolation: "isolate" }}>
        {open && (
          <>
            <button
              aria-label="Fechar menu de ações"
              onClick={() => setOpen(false)}
              className="fixed inset-0 bg-black/25 cursor-default"
              style={{ zIndex: zVar("var(--z-dock-backdrop)") }}
            />

            <svg
              className="absolute left-1/2 bottom-1/2 -translate-x-1/2 pointer-events-none"
              width={geometry.radius * 2 + NODE_SIZE}
              height={geometry.radius * 2 + NODE_SIZE}
              viewBox={`${-(geometry.radius + NODE_SIZE / 2)} ${-(geometry.radius + NODE_SIZE / 2)} ${(geometry.radius + NODE_SIZE / 2) * 2} ${(geometry.radius + NODE_SIZE / 2) * 2}`}
              style={{ overflow: "visible", zIndex: zVar("var(--z-core-connectors)") }}
              aria-hidden="true"
            >
              {geometry.nodes.map((node, i) => (
                <line
                  key={i}
                  x1={0}
                  y1={0}
                  x2={node.x}
                  y2={node.y}
                  stroke="var(--cc-polaris)"
                  strokeWidth={2}
                  opacity={0.4}
                  className="stella-line"
                  style={{ animationDelay: `${i * 40}ms` }}
                />
              ))}
            </svg>

            {visibleActions.map((action, i) => (
              <ActionNode
                key={action.id}
                ref={i === 0 ? firstActionRef : undefined}
                action={action}
                node={geometry.nodes[i]}
                index={i}
                onSelect={() => {
                  setOpen(false);
                  action.onClick();
                }}
              />
            ))}

            {overflowActions.length > 0 && (
              <div
                className="stella-node absolute flex flex-col items-center gap-1.5"
                style={{
                  left: "50%",
                  bottom: "50%",
                  transform: `translate(${geometry.nodes[visibleActions.length].x - NODE_SIZE / 2}px, ${geometry.nodes[visibleActions.length].y}px)`,
                  animationDelay: `${visibleActions.length * 55}ms`,
                  zIndex: zVar("var(--z-core-actions)"),
                }}
              >
                <button
                  onClick={() => setMoreOpen((v) => !v)}
                  aria-label="Mais ações"
                  aria-expanded={moreOpen}
                  role="menuitem"
                  className="relative flex items-center justify-center rounded-full border shadow-sm"
                  style={{ width: NODE_SIZE, height: NODE_SIZE, background: "var(--cc-nebula)", borderColor: "var(--cc-horizon)", color: "var(--cc-polaris)" }}
                >
                  <MoreHorizontal size={20} />
                </button>
                <span className="text-xs font-medium text-center leading-tight" style={{ color: "var(--foreground)", maxWidth: geometry.nodes[visibleActions.length].labelMaxWidth }}>
                  Mais
                </span>

                {moreOpen && (
                  <div
                    role="menu"
                    className="absolute bottom-full mb-2 flex w-max max-w-[220px] flex-col gap-1 rounded-[var(--radius-md)] border p-1.5 shadow-lg"
                    style={{ background: "var(--cc-nebula)", borderColor: "var(--cc-horizon)", zIndex: zVar("var(--z-modal)") }}
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
          className="relative flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform active:scale-90"
          style={{ background: "var(--cc-nebula)", border: "1px solid var(--cc-horizon)", zIndex: zVar("var(--z-core-trigger)") }}
        >
          <FourPointStar open={open} />
        </button>
      </div>

      <style jsx>{`
        .stella-node,
        .stella-line {
          animation: stella-appear var(--cc-duration-standard, 280ms) var(--cc-ease-stellar, ease) both;
        }
        @keyframes stella-appear {
          from {
            opacity: 0;
            transform: scale(0.6);
          }
          to {
            opacity: 1;
            transform: scale(1);
          }
        }
        .stella-line {
          transform-origin: 0 0;
        }
        @media (prefers-reduced-motion: reduce) {
          .stella-node,
          .stella-line {
            animation: none !important;
            opacity: 1 !important;
          }
        }
      `}</style>
    </div>
  );
}

function ActionNode({
  action,
  node,
  index,
  onSelect,
  ref,
}: {
  action: StellaCoreAction;
  node: { x: number; y: number; labelMaxWidth: number };
  index: number;
  onSelect: () => void;
  ref?: React.Ref<HTMLButtonElement>;
}) {
  const Icon = action.icon;
  return (
    <div
      className="stella-node absolute flex flex-col items-center gap-1.5"
      style={{
        left: "50%",
        bottom: "50%",
        transform: `translate(${node.x - NODE_SIZE / 2}px, ${node.y}px)`,
        animationDelay: `${index * 55}ms`,
        zIndex: zVar("var(--z-core-actions)"),
      }}
    >
      <button
        ref={ref}
        role="menuitem"
        onClick={onSelect}
        aria-label={action.label + (action.locked ? " (recurso do plano superior)" : "")}
        title={action.label}
        className="relative flex items-center justify-center rounded-full border shadow-sm"
        style={{
          width: NODE_SIZE,
          height: NODE_SIZE,
          background: "var(--cc-nebula)",
          borderColor: "var(--cc-horizon)",
          color: action.locked ? "var(--cc-comet)" : "var(--cc-polaris)",
        }}
      >
        <Icon size={19} />
        {action.locked && (
          <span
            className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full"
            style={{ background: "var(--cc-comet)", color: "var(--cc-nebula)" }}
          >
            <Lock size={9} />
          </span>
        )}
      </button>
      <span
        className="text-[11px] font-medium text-center leading-tight [display:-webkit-box] [-webkit-line-clamp:2] [-webkit-box-orient:vertical] overflow-hidden"
        style={{ color: "var(--foreground)", maxWidth: node.labelMaxWidth }}
      >
        {action.shortLabel ?? action.label}
      </span>
    </div>
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
