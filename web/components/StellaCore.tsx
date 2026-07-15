"use client";

import { useEffect, useRef, useState } from "react";
import { LucideIcon, Lock } from "lucide-react";

export type StellaCoreAction = {
  id: string;
  label: string;
  icon: LucideIcon;
  locked?: boolean;
  onClick: () => void;
};

/**
 * Web mobile/desktop equivalent of the Android Stella Core (Fase 5) - same idea, not a pixel
 * copy: a four-point star that opens into an upward fan of contextual actions with constellation
 * lines connecting them, instead of a generic FAB "+" or a dropdown menu. Screens hand it their
 * own small action list (see getStellaCoreActions below); this component only knows how to lay
 * them out and animate.
 *
 * Fase 11.8 (Parte 9): Escape closes the menu and returns focus to the trigger; the first action
 * receives focus on open so keyboard users don't have to tab past the backdrop.
 */
export function StellaCore({ actions, embedded = false }: { actions: StellaCoreAction[]; embedded?: boolean }) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const firstActionRef = useRef<HTMLButtonElement>(null);

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
  }, [open]);

  if (actions.length === 0) return null;

  const spread = Math.min(150, 34 + actions.length * 24);
  const start = -90 - spread / 2;
  const step = actions.length > 1 ? spread / (actions.length - 1) : 0;
  const radius = 108;

  return (
    <div className={embedded ? "relative flex justify-center pointer-events-none" : "fixed inset-x-0 bottom-6 z-40 flex justify-center pointer-events-none"}>
      <div className="relative pointer-events-auto">
        {open && (
          <>
            <button
              aria-label="Fechar menu de ações"
              onClick={() => setOpen(false)}
              className="fixed inset-0 bg-black/25 cursor-default"
              style={{ zIndex: -1 }}
            />
            <svg
              className="absolute left-1/2 bottom-1/2 -translate-x-1/2"
              width={radius * 2}
              height={radius * 2}
              style={{ overflow: "visible" }}
              aria-hidden="true"
            >
              {actions.map((_, i) => {
                const angle = ((start + step * i) * Math.PI) / 180;
                const x = radius + Math.cos(angle) * radius;
                const y = radius + Math.sin(angle) * radius;
                return (
                  <line
                    key={i}
                    x1={radius}
                    y1={radius}
                    x2={x}
                    y2={y}
                    stroke="var(--cc-polaris)"
                    strokeWidth={2}
                    opacity={0.45}
                    className="stella-line"
                    style={{ animationDelay: `${i * 40}ms` }}
                  />
                );
              })}
            </svg>

            {actions.map((action, i) => {
              const angle = ((start + step * i) * Math.PI) / 180;
              const x = Math.cos(angle) * radius;
              const y = Math.sin(angle) * radius;
              const Icon = action.icon;
              return (
                <button
                  key={action.id}
                  ref={i === 0 ? firstActionRef : undefined}
                  role="menuitem"
                  onClick={() => {
                    setOpen(false);
                    action.onClick();
                  }}
                  aria-label={action.label + (action.locked ? " (recurso do plano superior)" : "")}
                  className="stella-node absolute flex flex-col items-center gap-1.5"
                  style={{
                    left: "50%",
                    bottom: "50%",
                    transform: `translate(${x - 26}px, ${y}px)`,
                    animationDelay: `${i * 55}ms`,
                  }}
                >
                  <span
                    className="relative flex h-12 w-12 items-center justify-center rounded-full border shadow-sm"
                    style={{
                      background: "var(--cc-nebula)",
                      borderColor: "var(--cc-horizon)",
                      color: action.locked ? "var(--cc-comet)" : "var(--cc-polaris)",
                    }}
                  >
                    <Icon size={20} />
                    {action.locked && (
                      <span
                        className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full"
                        style={{ background: "var(--cc-comet)", color: "var(--cc-nebula)" }}
                      >
                        <Lock size={9} />
                      </span>
                    )}
                  </span>
                  <span className="text-xs font-medium text-center max-w-[76px] leading-tight" style={{ color: "var(--foreground)" }}>
                    {action.label}
                  </span>
                </button>
              );
            })}
          </>
        )}

        <button
          ref={triggerRef}
          onClick={() => setOpen((v) => !v)}
          aria-label={open ? "Fechar menu de ações" : "Abrir menu de ações (Stella Core)"}
          aria-haspopup="menu"
          aria-expanded={open}
          className="relative flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform active:scale-90"
          style={{ background: "var(--cc-nebula)", border: "1px solid var(--cc-horizon)" }}
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
          transform-origin: center;
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
