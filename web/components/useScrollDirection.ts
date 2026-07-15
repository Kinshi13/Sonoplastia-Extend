"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Fase 11.8 (Parte 4): drives the Stella Dock's compact/expanded state. A single passive scroll
 * listener, throttled via rAF (same pattern as ConstellationScene) - direction only flips after
 * `threshold` px of continuous movement, so a small wobble doesn't flicker the dock. Always
 * reports "expanded" when reduced motion is on, since the compacting transition is itself motion.
 */
export function useScrollDirection(threshold = 24) {
  const [compact, setCompact] = useState(false);
  const lastY = useRef(0);
  const accum = useRef(0);

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) return;

    lastY.current = window.scrollY;
    let ticking = false;

    function onScroll() {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        const y = window.scrollY;
        const dy = y - lastY.current;
        if (Math.sign(dy) !== Math.sign(accum.current)) accum.current = 0;
        accum.current += dy;
        lastY.current = y;

        if (y < 40) {
          setCompact(false);
        } else if (accum.current > threshold) {
          setCompact(true);
        } else if (accum.current < -threshold) {
          setCompact(false);
        }
        ticking = false;
      });
    }

    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [threshold]);

  return compact;
}
