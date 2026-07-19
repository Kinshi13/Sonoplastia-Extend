"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Web Fase 6.7 - replaces the old per-frame canvas parallax (dozens of stars, two radial
 * gradients, and an O(n²) line-pairing loop, all redrawn on every scroll/pointermove tick, on a
 * canvas sized to the *full document height* on tall pages) with a static night sky + a small
 * number of highlighted stars. The site's identity comes from *looking* like a considered
 * starfield, not from continuously recomputing one.
 *
 * Layers, back to front:
 *   1. CSS gradient wash (deep navy -> void, two very soft glows) - pure CSS, zero JS.
 *   2. Static starfield + a few faint orbital arcs - one SVG, computed once from a seeded PRNG at
 *      module load (deterministic, so server and client render the same markup - no hydration
 *      mismatch). Never touched again after paint - no scroll listener, no resize remeasurement
 *      (percentage-based viewBox scales with the viewport via CSS alone).
 *   3. A handful of highlighted accent stars - real DOM nodes so they can move: on desktop, a
 *      capped 4-8px drift follows the cursor (`transform: translate3d`, written directly to the
 *      DOM from a single rAF-coalesced pointermove handler - no React state per frame, no
 *      re-render at all); on touch devices, a slow (12-18s) CSS `@keyframes` drift within 1-3px -
 *      no JS driving it whatsoever. `prefers-reduced-motion` removes movement from both without
 *      hiding the sky itself.
 *
 * Mounts once in the root layout and never depends on FPS, a device-tier guess, or a loading state
 * to become visible - the gradient + static SVG are in the very first paint, fixed between routes.
 */

type Point = { x: number; y: number };

function seededRandom(seed: number) {
  let s = seed;
  return () => {
    s = (s * 1103515245 + 12345) & 0x7fffffff;
    return (s % 10000) / 10000;
  };
}

// Percent-based (viewBox 0-100) so the whole field scales with the viewport via CSS alone - no
// resize listener, no re-measurement, unlike the old canvas layer.
const distantRandom = seededRandom(90210);
const DISTANT_STARS: (Point & { r: number })[] = Array.from({ length: 55 }, () => ({
  x: distantRandom() * 100,
  y: distantRandom() * 100,
  r: distantRandom() * 0.12 + 0.05,
}));

// A few faint orbital arcs - static decoration, not a data visualization; hand-placed-looking
// curves rather than a grid so the sky doesn't read as a repeating texture.
const ORBIT_ARCS = ["M -10 24 Q 30 8 70 20 T 130 12", "M -5 68 Q 25 82 55 66 T 118 74", "M 20 -5 Q 15 40 32 78"];

const ACCENT_SEED = 4242;
const accentRandom = seededRandom(ACCENT_SEED);
// Fase 6.7 - counts capped per spec: 5-8 desktop, 3-5 mobile. The mobile set is a stable prefix of
// the desktop one (same first N stars) rather than an independent random set, so the sky doesn't
// visibly rearrange itself at the breakpoint.
const ACCENT_STARS: (Point & { depth: number; driftMs: number; dx: number; dy: number })[] = Array.from(
  { length: 8 },
  () => ({
    x: accentRandom() * 96 + 2,
    y: accentRandom() * 92 + 4,
    depth: accentRandom() * 0.6 + 0.4, // 0.4-1.0 - how much of the cursor amplitude this star gets
    driftMs: 12000 + accentRandom() * 6000,
    dx: (accentRandom() * 2 - 1) * 2.5, // spec: mobile amplitude max 1-3px
    dy: (accentRandom() * 2 - 1) * 2.5,
  })
);
const DESKTOP_ACCENT_COUNT = 8;
const MOBILE_ACCENT_COUNT = 4;
const DESKTOP_AMPLITUDE_PX = 6; // spec: desktop amplitude max 4-8px

export function ConstellationScene() {
  const [mounted, setMounted] = useState(false);
  const [isFinePointer, setIsFinePointer] = useState(false);
  const starRefs = useRef<(HTMLSpanElement | null)[]>([]);

  useEffect(() => {
    // One-time client-only mount gate (accent stars are decorative-only, no SSR value) - same
    // pattern as DockPortal for other browser-only checks.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
    setIsFinePointer(window.matchMedia("(pointer: fine)").matches);
  }, []);

  useEffect(() => {
    if (!mounted || !isFinePointer) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) return;

    let px = 0;
    let py = 0;
    let raf = 0;
    let ticking = false;

    function apply() {
      for (const el of starRefs.current) {
        if (!el) continue;
        const depth = Number(el.dataset.depth || "1");
        el.style.transform = `translate3d(${px * DESKTOP_AMPLITUDE_PX * depth}px, ${py * DESKTOP_AMPLITUDE_PX * depth}px, 0)`;
      }
      ticking = false;
    }

    function onPointerMove(event: PointerEvent) {
      if (document.hidden) return;
      px = (event.clientX / window.innerWidth) * 2 - 1;
      py = (event.clientY / window.innerHeight) * 2 - 1;
      if (ticking) return;
      ticking = true;
      raf = requestAnimationFrame(apply);
    }

    window.addEventListener("pointermove", onPointerMove, { passive: true });
    return () => {
      window.removeEventListener("pointermove", onPointerMove);
      cancelAnimationFrame(raf);
    };
  }, [mounted, isFinePointer]);

  const accentStars = ACCENT_STARS.slice(0, isFinePointer ? DESKTOP_ACCENT_COUNT : MOBILE_ACCENT_COUNT);

  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 overflow-hidden night-sky">
      <div className="absolute inset-0 night-sky-wash" />

      {/* xMidYMid slice (not "none") - stars are circles, and "none" non-uniformly stretches the
          0-100 viewBox to the viewport's aspect ratio, turning every circle into an oval on any
          non-square screen. "slice" scales uniformly and crops instead, so stars stay round; a
          little gets cropped at the very edges on extreme aspect ratios, which is fine for a
          full-bleed decorative sky. */}
      <svg className="absolute inset-0 h-full w-full" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid slice">
        {ORBIT_ARCS.map((d, i) => (
          <path key={i} d={d} fill="none" stroke="var(--cc-polaris)" strokeWidth={0.06} opacity={0.12} vectorEffect="non-scaling-stroke" />
        ))}
        {DISTANT_STARS.map((star, i) => (
          <circle key={i} cx={star.x} cy={star.y} r={star.r} fill="var(--cc-stardust)" opacity={0.4} />
        ))}
      </svg>

      {mounted &&
        accentStars.map((star, i) => (
          <span
            key={i}
            ref={(el) => {
              starRefs.current[i] = el;
            }}
            data-depth={star.depth}
            className={isFinePointer ? "accent-star" : "accent-star accent-star-drift"}
            style={
              {
                left: `${star.x}%`,
                top: `${star.y}%`,
                "--drift-ms": `${star.driftMs}ms`,
                "--drift-dx": `${star.dx}px`,
                "--drift-dy": `${star.dy}px`,
              } as React.CSSProperties
            }
          />
        ))}

      <style jsx>{`
        .night-sky-wash {
          background:
            radial-gradient(ellipse 60% 45% at 14% 12%, color-mix(in srgb, var(--cc-polaris) 12%, transparent), transparent 70%),
            radial-gradient(ellipse 55% 40% at 86% 62%, color-mix(in srgb, var(--cc-aurora) 10%, transparent), transparent 70%),
            linear-gradient(180deg, var(--cc-void) 0%, color-mix(in srgb, var(--cc-void) 90%, var(--cc-nebula) 10%) 100%);
        }
        .accent-star {
          position: absolute;
          width: 3px;
          height: 3px;
          border-radius: 9999px;
          background: var(--cc-polaris);
          box-shadow: 0 0 8px 2px color-mix(in srgb, var(--cc-polaris) 70%, transparent);
          will-change: transform;
        }
        .accent-star-drift {
          animation: accent-drift var(--drift-ms, 14s) ease-in-out infinite;
        }
        @keyframes accent-drift {
          0%,
          100% {
            transform: translate3d(0, 0, 0);
          }
          50% {
            transform: translate3d(var(--drift-dx, 2px), var(--drift-dy, 2px), 0);
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .accent-star-drift {
            animation: none;
          }
        }
      `}</style>
    </div>
  );
}
