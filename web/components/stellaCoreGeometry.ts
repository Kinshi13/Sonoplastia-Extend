/**
 * Fase 11.8.3 (Bloco A-H): the Stella Core is now a two-branch "constellation action map" instead
 * of a single fan of small circular nodes. Each action gets its own star-node (a small glowing
 * point where the connector ends) plus a full mini card (StellaConstellationCard) - not a bare
 * label floating in space. This module only computes positions; it knows nothing about rendering.
 */

export type Branch = "L" | "R";

export type StellaConstellationNode = {
  x: number;
  y: number;
  branch: Branch;
  /** Position of the small star-node along the connector, short of the card itself. */
  starX: number;
  starY: number;
  cardWidth: number;
  cardHeight: number;
  showSubtitle: boolean;
};

export type StellaConstellationGeometry = {
  nodes: StellaConstellationNode[];
  zoneWidth: number;
  zoneHeight: number;
};

export const MAX_REAL_ACTIONS = 6;
const SMALL_WIDTH_BREAKPOINT = 380;
const MARGIN_X = 16;

// Bloco B: how many actions go to the right branch vs the left branch, for each total count.
// The heavier branch is filled first (right, arbitrarily but consistently).
const BRANCH_SPLIT: Record<number, [right: number, left: number]> = {
  1: [1, 0],
  2: [1, 1],
  3: [2, 1],
  4: [2, 2],
  5: [3, 2],
  6: [3, 3],
};

/** Real (non-"Mais") action cap for the current viewport - narrower phones fit fewer per branch
 *  before the composition would crowd the screen edges. */
export function maxRealActions(viewportWidth: number): number {
  return viewportWidth > 0 && viewportWidth < SMALL_WIDTH_BREAKPOINT ? 4 : MAX_REAL_ACTIONS;
}

export function computeConstellationGeometry(nodeCount: number, viewportWidth: number): StellaConstellationGeometry {
  const count = Math.min(Math.max(nodeCount, 0), MAX_REAL_ACTIONS);
  if (count === 0) return { nodes: [], zoneWidth: 0, zoneHeight: 0 };

  const narrow = viewportWidth > 0 && viewportWidth < SMALL_WIDTH_BREAKPOINT;
  const [rightCount, leftCount] = BRANCH_SPLIT[count];

  const xBase = narrow ? 58 : 76;
  const xStep = narrow ? 8 : 14;
  const yBase = narrow ? 70 : 86;
  const yStep = narrow ? 62 : 74;
  const cardWidth = narrow ? 108 : 148;
  const cardHeight = narrow ? 48 : 58;
  const showSubtitle = !narrow;

  const maxHalfWidth = viewportWidth > 0 ? viewportWidth / 2 - MARGIN_X : 400;

  function branchNodes(branch: Branch, k: number): StellaConstellationNode[] {
    const dir = branch === "R" ? 1 : -1;
    return Array.from({ length: k }, (_, i) => {
      const starX = dir * (xBase + i * xStep);
      const starY = -(yBase + i * yStep);
      // The card sits further out from the star-node in the branch direction, its near edge
      // just past the star so the connector never has to cross the card itself (Bloco F).
      const cardCenterX = clamp(starX + dir * (cardWidth / 2 + 10), -maxHalfWidth + cardWidth / 2, maxHalfWidth - cardWidth / 2);
      return {
        x: cardCenterX,
        y: starY,
        branch,
        starX,
        starY,
        cardWidth,
        cardHeight,
        showSubtitle,
      };
    });
  }

  // Interleave so DOM/focus order is bottom-to-top on one branch then the other (Bloco L).
  const nodes = [...branchNodes("R", rightCount), ...branchNodes("L", leftCount)];

  const maxK = Math.max(rightCount, leftCount);
  const farStarX = xBase + (maxK - 1) * xStep;
  const zoneWidth = 2 * (farStarX + cardWidth + 20);
  const zoneHeight = yBase + (maxK - 1) * yStep + cardHeight / 2 + 24;

  return { nodes, zoneWidth, zoneHeight };
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}
