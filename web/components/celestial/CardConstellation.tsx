/**
 * Web Fase 6.7 - decorative constellation for cards that don't already have one (DayConstellation
 * covers escala cards with a real weekday `kind`; admin/plan/generic cards had none at all). Same
 * spirit as DayConstellation.tsx - pure SVG, 4/5-point compositions, no raster assets, no canvas -
 * just a broader, non-weekday-tied set so a page full of these cards doesn't repeat one pattern.
 *
 * 6 named patterns, picked deterministically from a card's own id/index (see
 * [pickCardConstellation]) so the same card always renders the same pattern across re-renders/
 * navigations, without needing to persist anything.
 */
export type CardConstellationPattern = "arc" | "diagonal" | "triangle" | "diamond" | "ascending" | "radiant";

const PATTERNS: CardConstellationPattern[] = ["arc", "diagonal", "triangle", "diamond", "ascending", "radiant"];

/** Deterministic pick from any stable string/number (a card's id, or its index in a list) - same
 *  input always yields the same pattern, so a card's decoration doesn't shuffle on re-render. */
export function pickCardConstellation(seed: string | number): CardConstellationPattern {
  const s = String(seed);
  let hash = 0;
  for (let i = 0; i < s.length; i++) hash = (hash * 31 + s.charCodeAt(i)) & 0x7fffffff;
  return PATTERNS[hash % PATTERNS.length];
}

function Arc({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M8 40 Q32 14 56 40" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="8" cy="40" r="1.4" fill="currentColor" fillOpacity="0.7" />
      <circle cx="24" cy="21" r="1.6" fill="currentColor" fillOpacity="0.8" />
      <circle cx="40" cy="21" r="1.6" fill="currentColor" fillOpacity="0.8" />
      <circle cx="56" cy="40" r="1.4" fill="currentColor" fillOpacity="0.7" />
    </svg>
  );
}

function Diagonal({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M10 54 L26 38 L38 42 L54 10" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="10" cy="54" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="26" cy="38" r="1.6" fill="currentColor" fillOpacity="0.75" />
      <circle cx="38" cy="42" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="54" cy="10" r="2.2" fill="currentColor" />
    </svg>
  );
}

function Triangle({ className }: { className?: string }) {
  // Deliberately incomplete - one side left open ("triângulo incompleto"), not a closed shape.
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M14 48 L32 14 L50 48" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="14" cy="48" r="1.5" fill="currentColor" fillOpacity="0.7" />
      <circle cx="32" cy="14" r="2" fill="currentColor" />
      <circle cx="50" cy="48" r="1.5" fill="currentColor" fillOpacity="0.7" />
    </svg>
  );
}

function Diamond({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M32 12 L48 32 L32 52 L16 32 Z" stroke="currentColor" strokeWidth="1" strokeOpacity="0.45" />
      <circle cx="32" cy="12" r="1.6" fill="currentColor" fillOpacity="0.75" />
      <circle cx="48" cy="32" r="1.4" fill="currentColor" fillOpacity="0.65" />
      <circle cx="32" cy="52" r="1.4" fill="currentColor" fillOpacity="0.65" />
      <circle cx="16" cy="32" r="1.4" fill="currentColor" fillOpacity="0.65" />
    </svg>
  );
}

function Ascending({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path d="M8 52 L22 42 L34 30 L48 20 L58 10" stroke="currentColor" strokeWidth="1" strokeOpacity="0.5" />
      <circle cx="8" cy="52" r="1.2" fill="currentColor" fillOpacity="0.55" />
      <circle cx="22" cy="42" r="1.3" fill="currentColor" fillOpacity="0.62" />
      <circle cx="34" cy="30" r="1.5" fill="currentColor" fillOpacity="0.7" />
      <circle cx="48" cy="20" r="1.7" fill="currentColor" fillOpacity="0.8" />
      <circle cx="58" cy="10" r="2.2" fill="currentColor" />
    </svg>
  );
}

function Radiant({ className }: { className?: string }) {
  // Central star with short branches - the only pattern with a true center node.
  return (
    <svg viewBox="0 0 64 64" className={className} fill="none" aria-hidden="true">
      <path
        d="M32 32 L32 14 M32 32 L48 22 M32 32 L50 40 M32 32 L38 54 M32 32 L14 42"
        stroke="currentColor"
        strokeWidth="1"
        strokeOpacity="0.45"
      />
      <circle cx="32" cy="32" r="2.4" fill="currentColor" />
      <circle cx="32" cy="14" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="48" cy="22" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="50" cy="40" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="38" cy="54" r="1.3" fill="currentColor" fillOpacity="0.6" />
      <circle cx="14" cy="42" r="1.3" fill="currentColor" fillOpacity="0.6" />
    </svg>
  );
}

const PATTERN_COMPONENT: Record<CardConstellationPattern, (props: { className?: string }) => React.JSX.Element> = {
  arc: Arc,
  diagonal: Diagonal,
  triangle: Triangle,
  diamond: Diamond,
  ascending: Ascending,
  radiant: Radiant,
};

/** Same usage shape as DayConstellationBackground/Mark - drop-in for a card's decorative layer. */
export function CardConstellation({
  pattern,
  className = "h-40 w-40",
}: {
  pattern: CardConstellationPattern;
  className?: string;
}) {
  const Shape = PATTERN_COMPONENT[pattern];
  return <Shape className={className} />;
}
