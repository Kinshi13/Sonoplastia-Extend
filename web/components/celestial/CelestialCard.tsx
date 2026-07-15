import { ReactNode } from "react";
import { ConstellationKind, DayConstellationBackground, DayConstellationMark } from "./DayConstellation";

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
export function CelestialCard({ children, className = "", kind, solemn, compact, glow = "none", updated }: CelestialCardProps) {
  const accent = kind ? KIND_TO_ACCENT[kind] : "neutral";
  const color = ACCENT_COLOR[accent];
  const isCrownSolemn = solemn ?? kind === "CROWN";

  return (
    <div
      className={`group relative overflow-hidden border transition-[box-shadow,transform] duration-300 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-offset-background ${
        glow === "hover" ? "hover:-translate-y-[3px]" : ""
      } ${className}`}
      style={{
        // @ts-expect-error -- CSS custom property, not a real color token
        "--tw-ring-color": color,
        borderColor: "var(--border-soft)",
        borderRadius: "var(--radius-lg)",
        clipPath: isCrownSolemn
          ? "polygon(0 14px, 14px 0, 100% 0, 100% calc(100% - 14px), calc(100% - 14px) 100%, 0 100%)"
          : "polygon(0 10px, 10px 0, 100% 0, 100% 100%, 0 100%)",
        background: `linear-gradient(155deg, var(--surface) 0%, var(--surface-elevated) 100%)`,
        boxShadow:
          glow === "active"
            ? `var(--elevation-elevated), 0 0 0 1px ${color}55, 0 0 32px -8px ${color}55`
            : "var(--elevation-raised)",
      }}
    >
      {/* Layer 1: external glow, hover only */}
      {glow === "hover" && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
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
      {glow === "hover" && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-[5px] rounded-[calc(var(--radius-lg)-6px)] border opacity-0 transition-opacity duration-300 group-hover:opacity-100"
          style={{ borderColor: `${color}70` }}
        />
      )}

      {/* Layer 5: day-constellation watermark - Bloco B3: base opacity raised into the 12-24%
          range (was 9%, read as too faint) and brightens further on hover/selection. */}
      {kind && !compact && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-6 -bottom-6 opacity-[0.18] transition-opacity duration-300 group-hover:opacity-[0.3]"
          style={{ color }}
        >
          <DayConstellationBackground kind={kind} className={isCrownSolemn ? "h-56 w-56" : "h-40 w-40"} />
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
