"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

/**
 * Fase 11.8.3 HOTFIX (real root cause, not the earlier isolation/backdrop-filter guess): the
 * Stella Dock is rendered by each route's own layout (app/c/[slug]/layout.tsx, app/admin/layout
 * .tsx) as a DOM sibling of `{children}` *inside* the root layout's `<main>` - which is now
 * wrapped by PageBlurWrapper. Applying `filter: blur()` to an ancestor unconditionally blurs
 * every descendant, dock and Stella Core included - that's correct, unambiguous CSS, not a bug to
 * work around with layer tricks. Portaling the dock to <body> makes it a true sibling of
 * PageBlurWrapper instead of a descendant, so it can never be swept into that blur.
 */
export function DockPortal({ children }: { children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    // One-time client-only mount gate - portaling is inherently a browser-only concern (no
    // `document` during SSR), same pattern used by IosInstallHint for other browser-only checks.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  if (!mounted) return null;
  return createPortal(children, document.body);
}
