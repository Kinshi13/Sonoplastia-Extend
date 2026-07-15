"use client";

import { useEffect, useState } from "react";

/** Reactive viewport width for the Stella Core geometry - recomputes on resize/orientation
 *  change (covers the iOS Safari url-bar show/hide case too, since that fires a resize/visualViewport
 *  event). Starts at 0 (server) and settles on first client paint, matching Next's hydration model. */
export function useViewportWidth(): number {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    function update() {
      setWidth(window.visualViewport?.width ?? window.innerWidth);
    }
    update();
    window.addEventListener("resize", update);
    window.visualViewport?.addEventListener("resize", update);
    return () => {
      window.removeEventListener("resize", update);
      window.visualViewport?.removeEventListener("resize", update);
    };
  }, []);

  return width;
}
