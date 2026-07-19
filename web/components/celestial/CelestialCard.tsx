"use client";

import { ReactNode, useEffect, useRef, useState } from "react";
import { ConstellationKind, DayConstellationBackground, DayConstellationMark } from "./DayConstellation";
import { CardConstellation, pickCardConstellation } from "./CardConstellation";

type Accent = "beacon" | "crown" | "dawn" | "pilgrim" | "neutral";

// Parte 5: variations of the Constellation Calm palette per day, not a disconnected new palette.
const ACCENT_COLOR: Record<Accent, string> = {
  beacon: "var(--cc-polaris)", // quarta - azul frio
  crown: "var(--accent-star)", // sábado - dourado/luz, mais marcante
  dawn: "var(--cc-aurora)", // domingo - lavanda/aurora
  pilgrim: "#8b6cf0", // especial - violeta estelar
  neutral: "var(--accent-constellation)",
};

const KIND_TO_ACCENT: Record<ConstellationKind, Accent> = {
  BEACON: "beacon",
  CROWN: "crown",
  DAWN: "dawn",
  PILGRIM: "pilgrim",
};

// Web Fase 6.7 (section 9) - standardized card states. Success has no existing Constellation Calm
// token (the palette only has polaris/aurora/comet/nova) - #3fb87f is a one-off addition in the
// same spirit as `pilgrim`'s #8b6cf0 two lines up, kept deliberately muted ("tonalidade verde
// discreta"), not a saturated/neon green.
type CardState = "success" | "warning" | "error" | "disabled" | "selected";
const STATE_COLOR: Record<Exclude<CardState, "disabled" | "selected">, string> = {
  success: "#3fb87f",
  warning: "var(--cc-comet)",
  error: "var(--cc-nova)",
};

type CelestialCardProps = {
  children: ReactNode;
  className?: string;
  /** Which day-constellation to watermark/mark this card with - omit for a neutral admin/plan card. */
  kind?: ConstellationKind;
  /** Sábado (CROWN) gets the "mais solene" treatment automatically unless overridden. */
  solemn?: boolean;
  /** Corner ornament + glow only, no watermark - use for compact/list rows. */
  compact?: boolean;
  glow?: "none" | "hover" | "active";
  /** Bloco B5: a small luminous dot near the corner mark, for a card whose content just changed
   *  without needing to redesign the whole card to say so. */
  updated?: boolean;
  /** Stable id (or index) used to deterministically pick a CardConstellation pattern for cards
   *  without a `kind` - same card always gets the same pattern, never random per render. */
  id?: string | number;
  /** Web Fase 6.7 (section 9) - success/warning/error tint the glow/border; disabled removes both
   *  interaction and glow entirely; selected is a persistent moderate glow (distinct from the
   *  hover-only "active" glow value above). */
  state?: CardState;
  /** Web Fase 6.7 (section 8) - opt-in touch "observed" state: a tap lights the card up for ~1.8s
   *  then fades, without delaying whatever the tap itself does (no preventDefault, no stopPropagation).
   *  Only wired on coarse-pointer devices; desktop keeps its existing hover/focus behavior. */
  interactive?: boolean;
};

/**
 * Fase 11.7 (Parte 1-2): "Celestial Frame" - the site's own card language instead of a plain
 * rounded rectangle. One base implementation (this component) with a handful of named exports
 * below matching the requested variants, so callers don't each hand-roll the corner ornaments,
 * watermark, and inner line separately, and every card in the app stays visually related instead
 * of drifting into six unrelated designs.
 *
 * Layers, back to front: ambient shadow (elevation token) → external glow (only on hover/active,
 * never idle) → main frame (border + two cut corners) → inner decorative line → gradient
 * background → day-constellation watermark → content → corner star ornament. Content sits in a
 * plain div on top, unaffected by any of the decoration below it - legibility first.
 */
export function CelestialCard({
  children,
  className = "",
  kind,
  solemn,
  compact,
  glow = "none",
  updated,
  id,
  state,
  interactive = false,
}: CelestialCardProps) {
  const accent = kind ? KIND_TO_ACCENT[kind] : "neutral";
  const stateColor = state && state !== "disabled" && state !== "selected" ? STATE_COLOR[state] : undefined;
  const color = stateColor ?? ACCENT_COLOR[accent];
  const isCrownSolemn = solemn ?? kind === "CROWN";
  const disabled = state === "disabled";
  const selected = state === "selected";
  const pattern = !kind && id !== undefined ? pickCardConstellation(id) : undefined;

  const [observed, setObserved] = useState(false);
  const observedTimeout = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => () => clearTimeout(observedTimeout.current), []);

  function handleTouchObserve() {
    if (!interactive || disabled) return;
    if (!window.matchMedia("(pointer: coarse)").matches) return;
    setObserved(true);
    clearTimeout(observedTimeout.current);
    observedTimeout.current = setTimeout(() => setObserved(false), 1800);
  }

  const showActiveGlow = glow === "active" || selected || observed;

  return (
    <div
      onPointerDown={interactive ? handleTouchObserve : undefined}
      aria-disabled={disabled || undefined}
      // Web Fase 6.7 - no translate on hover ("não mover o card"); glow/border alone signal
      // interactivity. Transition tightened to the requested 180-260ms window (was 300ms).
      className={`group relative overflow-hidden border transition-[box-shadow] duration-[220ms] focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-offset-background ${
        disabled ? "pointer-events-none opacity-60" : ""
      } ${className}`}
      style={{
        // @ts-expect-error -- CSS custom property, not a real color token
        "--tw-ring-color": color,
        borderColor: disabled ? "var(--border-soft)" : "var(--border-soft)",
        borderRadius: "var(--radius-lg)",
        clipPath: isCrownSolemn
          ? "polygon(0 14px, 14px 0, 100% 0, 100% calc(100% - 14px), calc(100% - 14px) 100%, 0 100%)"
          : "polygon(0 10px, 10px 0, 100% 0, 100% 100%, 0 100%)",
        background: `linear-gradient(155deg, var(--surface) 0%, var(--surface-elevated) 100%)`,
        boxShadow: showActiveGlow
          ? `var(--elevation-elevated), 0 0 0 1px ${color}55, 0 0 32px -8px ${color}55`
          : "var(--elevation-raised)",
      }}
    >
      {/* Layer 1: external glow, hover only (desktop) / observed (touch) */}
      {(glow === "hover" || interactive) && (
        <div
          aria-hidden="true"
          className={`pointer-events-none absolute inset-0 transition-opacity duration-[220ms] group-hover:opacity-100 ${
            observed ? "opacity-100" : "opacity-0"
          }`}
          style={{ boxShadow: `0 0 32px -6px ${color}66`, borderRadius: "inherit" }}
        />
      )}

      {/* Layer 3: inner decorative line - a second, brighter copy fades in on hover (Bloco B4:
          "borda interna recebe brilho") instead of animating the border color directly. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-[5px] rounded-[calc(var(--radius-lg)-6px)] border"
        style={{ borderColor: `${color}2a` }}
      />
      {(glow === "hover" || interactive) && (
        <div
          aria-hidden="true"
          className={`pointer-events-none absolute inset-[5px] rounded-[calc(var(--radius-lg)-6px)] border transition-opacity duration-[220ms] group-hover:opacity-100 ${
            observed ? "opacity-100" : "opacity-0"
          }`}
          style={{ borderColor: `${color}70` }}
        />
      )}

      {/* Layer 5: day-constellation (or generic CardConstellation) watermark - Bloco B3: base
          opacity in the 12-24% range, brightens further on hover/selection/observed. */}
      {(kind || pattern) && !compact && (
        <div
          aria-hidden="true"
          className={`pointer-events-none absolute -right-6 -bottom-6 opacity-[0.18] transition-opacity duration-[220ms] group-hover:opacity-[0.3] ${
            observed ? "opacity-[0.3]" : ""
          }`}
          style={{ color }}
        >
          {kind ? (
            <DayConstellationBackground kind={kind} className={isCrownSolemn ? "h-56 w-56" : "h-40 w-40"} />
          ) : (
            <CardConstellation pattern={pattern!} className="h-40 w-40" />
          )}
        </div>
      )}

      {/* Layer 7: corner ornament (top-left only - two corners would compete with content) */}
      <div aria-hidden="true" className="pointer-events-none absolute left-3 top-3 opacity-70" style={{ color }}>
        <svg viewBox="0 0 12 12" className="h-2.5 w-2.5" fill="currentColor">
          <path d="M6 0 L7 5 L12 6 L7 7 L6 12 L5 7 L0 6 L5 5 Z" />
        </svg>
      </div>
      {updated && (
        <span
          aria-hidden="true"
          className="absolute right-3 top-3 h-2 w-2 rounded-full animate-pulse"
          style={{ background: "var(--accent-star)" }}
        />
      )}

      {/* Layer 6: content */}
      <div className="relative">{children}</div>
    </div>
  );
}

/** Corner mark + label helper for a card's header - pairs the SVG with real text, per Parte 21
 *  (constellations are a complement, never the only label). */
export function CelestialDayBadge({ kind, label }: { kind: ConstellationKind; label: string }) {
  const accent = KIND_TO_ACCENT[kind];
  const color = ACCENT_COLOR[accent];
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium uppercase tracking-[0.1em]" style={{ color }}>
      <DayConstellationMark kind={kind} className="h-4 w-4" />
      {label}
    </span>
  );
}

/**
 * Fase 11.8.2 (Bloco B1-B2): replaces the old "Constelação do Farol · Igreja" text badge. The
 * constellation is a visual identifier - it doesn't need its invented name spelled out for a
 * visitor to recognize it (the mark + watermark on the card already do that job). What a person
 * actually needs to read is the organization and that this is the official schedule; `org` is
 * only rendered once even when the card is reused elsewhere on the same page.
 */
export function CelestialOfficialHeader({
  kind,
  org,
  showOrg = true,
}: {
  kind: ConstellationKind;
  org: string;
  showOrg?: boolean;
}) {
  const accent = KIND_TO_ACCENT[kind];
  const color = ACCENT_COLOR[accent];
  return (
    <span
      className="inline-flex items-center gap-1.5 text-xs font-medium uppercase tracking-[0.1em]"
      style={{ color }}
      title={`Escala oficial · ${org}`}
    >
      <DayConstellationMark kind={kind} className="h-4 w-4" />
      {showOrg ? (
        <>
          <span className="text-foreground/90 normal-case tracking-normal font-semibold">{org}</span>
          <span aria-hidden="true">·</span> Escala oficial
        </>
      ) : (
        "Escala oficial"
      )}
    </span>
  );
}

// Named variants (Parte 2) - thin, semantically-named wrappers over the same base so every card
// in the app stays one system instead of six independent designs.
export function CelestialHeroCard(props: Omit<CelestialCardProps, "glow">) {
  return <CelestialCard {...props} glow="none" />;
}
export function CelestialCompactCard(props: Omit<CelestialCardProps, "compact">) {
  return <CelestialCard {...props} compact glow="hover" />;
}
export function CelestialAnnouncementCard(props: CelestialCardProps) {
  return <CelestialCard {...props} glow="hover" />;
}
export function CelestialAdminCard(props: CelestialCardProps) {
  return <CelestialCard {...props} glow="hover" compact={props.compact ?? true} />;
}
export function CelestialPlanCard(props: CelestialCardProps) {
  return <CelestialCard {...props} glow={props.glow ?? "none"} />;
}
