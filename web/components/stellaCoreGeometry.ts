/**
 * Fase 11.8.2 (Bloco A): the Stella Core menu is a compact micro-constellation, not a wide arc.
 * The 11.8.1 arc geometry fixed overlap with the dock but still spread nodes far enough sideways
 * to read as a big "V", and on tall pages its connectors could still reach cards well above the
 * star. This module now lays out up to 4 node slots in a small fixed grid directly above the
 * star - one row nearest the star (row 0), a second row above it only when there are 3-4 actions
 * (row 1) - so the whole menu stays inside a short, constant-height zone regardless of what the
 * page above it contains.
 */

export const MAX_GRID_SLOTS = 4;
const SMALL_WIDTH_BREAKPOINT = 360;
const MARGIN_X = 20;
const NODE_DIAMETER = 48;

export type StellaCoreNodeGeometry = { x: number; y: number; labelMaxWidth: number };
export type StellaCoreGeometry = { nodes: StellaCoreNodeGeometry[]; zoneWidth: number; zoneHeight: number };

type Slot = { col: -1 | 0 | 1; row: 0 | 1 };

// Row 0 = nearest the star. Row 1 = one tier further up, only used for 3-4 nodes. Matches the
// spec's ASCII layouts exactly: 3 items -> apex + pair; 4 items -> two even pairs.
const LAYOUTS: Record<number, Slot[]> = {
  1: [{ col: 0, row: 0 }],
  2: [
    { col: -1, row: 0 },
    { col: 1, row: 0 },
  ],
  3: [
    { col: -1, row: 0 },
    { col: 1, row: 0 },
    { col: 0, row: 1 },
  ],
  4: [
    { col: -1, row: 0 },
    { col: 1, row: 0 },
    { col: -1, row: 1 },
    { col: 1, row: 1 },
  ],
};

/** Maximum real (non-"Mais") action nodes to show before collapsing the rest into an overflow
 *  node - lower on narrow phones so the grid never has to widen past a comfortable thumb reach. */
export function maxRealActions(viewportWidth: number): number {
  return viewportWidth > 0 && viewportWidth < SMALL_WIDTH_BREAKPOINT ? 3 : MAX_GRID_SLOTS;
}

/** [nodeCount] is the number of node slots to render, already capped at MAX_GRID_SLOTS by the
 *  caller (real actions, or real actions minus one plus a trailing "Mais" node). */
export function computeStellaCoreGeometry(nodeCount: number, viewportWidth: number): StellaCoreGeometry {
  const count = Math.min(Math.max(nodeCount, 0), MAX_GRID_SLOTS);
  if (count === 0) return { nodes: [], zoneWidth: 0, zoneHeight: 0 };

  const narrow = viewportWidth > 0 && viewportWidth < SMALL_WIDTH_BREAKPOINT;
  const rowGapNear = narrow ? 66 : 78;
  const rowGapFar = rowGapNear + (narrow ? 52 : 60);

  const maxHalfWidth = viewportWidth > 0 ? viewportWidth / 2 - MARGIN_X - NODE_DIAMETER / 2 : 120;
  const colOffset = clamp(narrow ? 40 : 46, 32, Math.max(32, maxHalfWidth));

  const labelMaxWidth = clamp(colOffset * 2 - 12, 64, 96);

  const layout = LAYOUTS[count];
  const nodes = layout.map((slot) => ({
    x: slot.col * colOffset,
    y: -(slot.row === 0 ? rowGapNear : rowGapFar),
    labelMaxWidth,
  }));

  return {
    nodes,
    zoneWidth: colOffset * 2 + NODE_DIAMETER + 24,
    zoneHeight: rowGapFar + NODE_DIAMETER / 2 + 24,
  };
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
