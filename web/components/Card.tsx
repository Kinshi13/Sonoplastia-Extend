import { ReactNode } from "react";
import Link from "next/link";

const baseCardClass =
  "rounded-2xl bg-surface border border-divider shadow-sm transition-all duration-200";
const interactiveCardClass =
  "hover:shadow-lg hover:-translate-y-0.5 hover:border-primary/40 cursor-pointer";

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
