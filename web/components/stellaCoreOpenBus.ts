"use client";

/**
 * Fase 11.8.3 HOTFIX: a tiny cross-tree signal so the root layout's page content (a sibling part
 * of the DOM, not an ancestor/descendant of StellaCore) can react to the Core opening/closing
 * without a React context provider threaded through every layout. Plain DOM CustomEvents on
 * `document` - no external state library needed for one boolean.
 */
const EVENT_NAME = "stella-core-open-change";

export function emitStellaCoreOpenChange(open: boolean): void {
  if (typeof document === "undefined") return;
  document.dispatchEvent(new CustomEvent<boolean>(EVENT_NAME, { detail: open }));
}

export function subscribeStellaCoreOpenChange(callback: (open: boolean) => void): () => void {
  if (typeof document === "undefined") return () => {};
  function handler(event: Event) {
    callback((event as CustomEvent<boolean>).detail);
  }
  document.addEventListener(EVENT_NAME, handler);
  return () => document.removeEventListener(EVENT_NAME, handler);
}
