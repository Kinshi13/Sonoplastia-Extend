/**
 * Fase 11.7 (Parte 3-5): a small original constellation per "kind" of day, used as a low-opacity
 * watermark + a tiny recognizable mark next to a card's title - never the only way to tell the
 * day apart (the date/label text is always present too, see Parte 21/Acessibilidade). Pure SVG,
 * no raster assets, so it stays crisp at any size and costs near nothing to render.
 *
 * These are original geometric compositions (points + connecting lines), not a copy of any
 * existing game or app's constellation art - see the Master Plan's explicit instruction not to
 * reuse another product's assets/icons.
 */
export type ConstellationKind = "BEACON" | "CROWN" | "DAWN" | "PILGRIM";

const KIND_LABEL: Record<ConstellationKind, string> = {
  BEACON: "Constelação do Farol",
  CROWN: "Constelação da Coroa",
  DAWN: "Constelação da Aurora",
  PILGRIM: "Constelação Peregrina",
};

/** Weekday (0=Sunday) -> kind, with `isSpecial` always winning (Parte 3: "dias especiais"). */
export function constellationForDay(weekday: number, isSpecial: boolean): ConstellationKind {
  if (isSpecial) return "PILGRIM";
  if (weekday === 3) return "BEACON"; // quarta
  if (weekday === 6) return "CROWN"; // sábado
  if (weekday === 0) return "DAWN"; // domingo
  return "BEACON"; // any other weekday reads as a quieter mid-week beacon, not a fifth identity
}

/** Quarta: one bright point over a small base, orientation lines - "direção, continuidade". */
function Beacon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M32 8 L32 34" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <path d="M18 50 L32 34 L46 50" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="32" cy="8" r="2.6" fill="currentColor" />
      <circle cx="18" cy="50" r="1.6" fill="currentColor" fillOpacity="0.7" />
      <circle cx="46" cy="50" r="1.6" fill="currentColor" fillOpacity="0.7" />
      <circle cx="32" cy="34" r="1.4" fill="currentColor" fillOpacity="0.6" />
    </svg>
  );
}

/** Sábado: central four-point star with symmetric side stars forming an abstract crown -
 *  "solenidade, centralidade" - the most ornamented of the four, on purpose. */
function Crown({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M10 44 L22 20 L32 34 L42 20 L54 44" stroke="currentColor" strokeWidth="1" strokeOpacity="0.55" />
      <path d="M10 44 L54 44" stroke="currentColor" strokeWidth="1" strokeOpacity="0.35" />
      <path d="M32 10 L34.5 16.5 L41 17 L36 21.3 L37.6 27.7 L32 24 L26.4 27.7 L28 21.3 L23 17 L29.5 16.5 Z" fill="currentColor" />
      <circle cx="10" cy="44" r="1.6" fill="currentColor" fillOpacity="0.75" />
      <circle cx="22" cy="20" r="1.8" fill="currentColor" fillOpacity="0.85" />
      <circle cx="42" cy="20" r="1.8" fill="currentColor" fillOpacity="0.85" />
      <circle cx="54" cy="44" r="1.6" fill="currentColor" fillOpacity="0.75" />
    </svg>
  );
}

/** Domingo: a crescent arc with ascending points and a horizon spark - "início, renovação". */
function Dawn({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M8 46 Q32 26 56 46" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="8" cy="46" r="1.4" fill="currentColor" fillOpacity="0.7" />
      <circle cx="24" cy="32" r="1.6" fill="currentColor" fillOpacity="0.8" />
      <circle cx="40" cy="28" r="1.8" fill="currentColor" fillOpacity="0.9" />
      <circle cx="56" cy="46" r="1.4" fill="currentColor" fillOpacity="0.7" />
      <path d="M40 28 L40 15" stroke="currentColor" strokeWidth="1" strokeOpacity="0.4" />
      <circle cx="40" cy="12" r="2.2" fill="currentColor" />
    </svg>
  );
}

/** Especial: an asymmetric trajectory line with a bright final node - "trajetória, evento". */
function Pilgrim({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M6 52 L20 40 L26 44 L38 24 L50 20" stroke="currentColor" strokeWidth="1" strokeOpacity="0.55" />
      <circle cx="6" cy="52" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="20" cy="40" r="1.5" fill="currentColor" fillOpacity="0.7" />
      <circle cx="26" cy="44" r="1.2" fill="currentColor" fillOpacity="0.6" />
      <circle cx="38" cy="24" r="1.6" fill="currentColor" fillOpacity="0.75" />
      <circle cx="50" cy="20" r="2.6" fill="currentColor" />
    </svg>
  );
}

const KIND_COMPONENT: Record<ConstellationKind, (props: { className?: string }) => React.JSX.Element> = {
  BEACON: Beacon,
  CROWN: Crown,
  DAWN: Dawn,
  PILGRIM: Pilgrim,
};

/** Small mark placed next to a title/date - Parte 4: "uma pessoa deve reconhecer o dia observando
 *  apenas o desenho", always paired with real text (never the sole label). */
export function DayConstellationMark({ kind, className = "h-5 w-5" }: { kind: ConstellationKind; className?: string }) {
  const Shape = KIND_COMPONENT[kind];
  return <Shape className={className} />;
}

/** Large, very low-opacity watermark for a card's background layer. */
export function DayConstellationBackground({ kind, className = "h-40 w-40" }: { kind: ConstellationKind; className?: string }) {
  const Shape = KIND_COMPONENT[kind];
  return <Shape className={className} />;
}

export function constellationLabel(kind: ConstellationKind): string {
  return KIND_LABEL[kind];
}
