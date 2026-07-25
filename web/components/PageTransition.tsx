"use client";

import { usePathname } from "next/navigation";

/**
 * Transições entre telas (Bloco 23-28) - a light, entry-only fade + 8px vertical slide on every
 * route change, remounting via `key={pathname}` (which App Router already does implicitly for the
 * page component itself; this just gives the remount a visible, deliberate animation instead of a
 * dry cut). CSS-only, no Framer Motion added (the project doesn't already depend on it - Bloco 27:
 * "não adicionar Framer Motion apenas por causa disso"). Only `opacity`/`transform` animate, both
 * GPU-composited and cheap regardless of how much content is inside (Bloco 28).
 *
 * Exit animations aren't implemented (Bloco 24 explicitly allows this: "se o framework dificultar
 * animação de saída, implementar apenas animação de entrada") - App Router unmounts the outgoing
 * page synchronously on navigation, so there's no window to animate it out without a routing
 * library this project doesn't have.
 */
export function PageTransition({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div key={pathname} className="page-transition">
      {children}
      <style jsx>{`
        .page-transition {
          animation: page-transition-enter 220ms cubic-bezier(0.3, 0, 0.2, 1) both;
        }
        @keyframes page-transition-enter {
          from {
            opacity: 0;
            transform: translateY(8px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .page-transition {
            animation: none;
          }
        }
      `}</style>
    </div>
  );
}
