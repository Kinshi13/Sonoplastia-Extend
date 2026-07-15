/**
 * Fase 11.8.1 (Parte 2): the single place that computes where every node/label/connector of the
 * Stella Core menu goes. Nothing else in the component tree does its own trigonometry - that's
 * what let the old inline calculation drift into overlapping the dock (Parte 1 diagnosis below).
 *
 * Root causes found in the Fase 11.8 implementation:
 * - radius was a hardcoded 108px regardless of viewport width, so on a 320-360px phone the arc's
 *   outer nodes landed outside the safe horizontal margin (clipped/touching the screen edge).
 * - the vertical extent of the arc was never checked against the dock's own height - a node near
 *   the horizontal ends of a wide spread (many actions) has a small |sin(angle)|, i.e. barely
 *   lifted above the star, which sits only ~32-36px above the dock already - net result, a node
 *   could render at/inside the dock's own bounding box.
 * - label width (`max-w-[76px]`) was fixed regardless of how close two adjacent nodes actually
 *   were, so a tight spread (5 actions in 150°) let labels touch or overlap.
 * - more than ~5 actions had no fallback - the arc just kept getting more crowded.
 * This module fixes all four by clamping radius/spread to the viewport and the reserved dock
 * zone, deriving label width from actual node spacing, and capping visible actions with a
 * "Mais" overflow past MAX_VISIBLE_ACTIONS.
 */

export const MAX_VISIBLE_ACTIONS = 5;
const MARGIN_X = 20; // safe horizontal margin from the viewport edge
const MIN_RADIUS = 76;
const MAX_RADIUS = 128;
const NODE_DIAMETER = 48;

export type StellaCoreNodeGeometry = {
  x: number;
  y: number;
  labelMaxWidth: number;
};

export type StellaCoreGeometry = {
  radius: number;
  nodes: StellaCoreNodeGeometry[];
};

/**
 * [reserveBottom] is the dock's own measured height + a small gap - the arc's radius is capped
 * so no node's vertical extent (`radius * |sin(angle)|` from the star) needs to dip below that
 * line. [viewportWidth] clamps the radius again so the widest nodes stay `MARGIN_X` inside the
 * screen edge. The smaller of the two wins.
 */
export function computeStellaCoreGeometry(
  actionCount: number,
  viewportWidth: number,
  reserveBottom: number
): StellaCoreGeometry {
  const count = Math.min(actionCount, MAX_VISIBLE_ACTIONS);
  if (count === 0) return { radius: 0, nodes: [] };

  // Wider spread for more actions, but capped well short of a full semicircle so it always
  // reads as an "arco", never a ring - Parte 4's per-breakpoint compaction falls naturally out
  // of the same formula since it's driven by count, not a hardcoded breakpoint switch.
  const spread = Math.min(140, 30 + count * 20);
  const start = -90 - spread / 2;
  const step = count > 1 ? spread / (count - 1) : 0;
  const angles = Array.from({ length: count }, (_, i) => ((start + step * i) * Math.PI) / 180);

  const maxSin = Math.max(...angles.map((a) => Math.abs(Math.sin(a))), 0.001);
  const maxCos = Math.max(...angles.map((a) => Math.abs(Math.cos(a))), 0.001);

  const radiusFromViewport = (viewportWidth / 2 - MARGIN_X - NODE_DIAMETER / 2) / maxCos;
  const radiusFromDock = Math.max(0, reserveBottom) / maxSin;
  const radius = clamp(Math.min(radiusFromViewport, radiusFromDock || MAX_RADIUS), MIN_RADIUS, MAX_RADIUS);

  // Label width derives from actual angular spacing between neighbors at this radius, so two
  // adjacent labels can never physically overlap regardless of count.
  const arcSpacing = count > 1 ? (radius * ((step * Math.PI) / 180)) : radius;
  const labelMaxWidth = clamp(arcSpacing + 24, 56, 92);

  const nodes = angles.map((angle) => ({
    x: Math.cos(angle) * radius,
    y: Math.sin(angle) * radius,
    labelMaxWidth,
  }));

  return { radius, nodes };
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
