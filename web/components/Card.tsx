import { ReactNode } from "react";
import Link from "next/link";

const baseCardClass =
  "rounded-[var(--radius-lg)] bg-surface border border-border-soft transition-all duration-200 [box-shadow:var(--elevation-raised)]";
// Web Fase 6.7 - no translate on hover ("não mover o card"); elevation + border alone signal
// interactivity, without shifting layout under a moving cursor.
const interactiveCardClass = "hover:[box-shadow:var(--elevation-elevated)] hover:border-primary/40 cursor-pointer";

/** Static card - use for content that isn't itself a link/action. */
export function Card({ className = "", children }: { className?: string; children: ReactNode }) {
  return <div className={`${baseCardClass} ${className}`}>{children}</div>;
}

/** Card that behaves like a link, with hover elevation - the "interactive card" building block
 *  used across admin lists and public pages instead of plain rows. */
export function CardLink({
  href,
  className = "",
  children,
}: {
  href: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Link href={href} className={`${baseCardClass} ${interactiveCardClass} block ${className}`}>
      {children}
    </Link>
  );
}
