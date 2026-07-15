"use client";

import { useEffect, useState } from "react";
import { subscribeStellaCoreOpenChange } from "./stellaCoreOpenBus";

/**
 * Fase 11.8.3 HOTFIX: wraps the page's own content (header + main) so it - and only it - gets
 * blurred/desaturated while the Stella Core is open. This replaces the previous approach (a
 * `backdrop-filter` overlay meant to blur only what's "behind" it) after confirming visually that
 * Chromium/Safari can bleed that blur onto content painted *after* the backdrop too, once an
 * ancestor establishes `isolation: isolate` (here, <body>, needed for the starfield background).
 * `filter: blur()` applied directly to this wrapper can't leak onto the Stella Core, which lives
 * in a completely separate part of the tree (rendered by the dock, never inside this wrapper).
 */
export function PageBlurWrapper({ children }: { children: React.ReactNode }) {
  const [blurred, setBlurred] = useState(false);

  useEffect(() => subscribeStellaCoreOpenChange(setBlurred), []);

  return (
    <div
      className="flex flex-1 flex-col transition-[filter,opacity] duration-300 motion-reduce:transition-none"
      style={{
        filter: blurred ? "blur(6px) saturate(0.85)" : "none",
        opacity: blurred ? 0.6 : 1,
        pointerEvents: blurred ? "none" : undefined,
      }}
    >
      {children}
    </div>
  );
}
