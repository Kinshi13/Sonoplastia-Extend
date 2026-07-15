"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Calendar, BookOpen, Megaphone, FileText, Images, FolderOpen, Sparkles, LucideIcon } from "lucide-react";

const adminLinks: { href: string; label: string; icon: LucideIcon }[] = [
  { href: "/admin", label: "Painel", icon: LayoutDashboard },
  { href: "/admin/escalas", label: "Escalas", icon: Calendar },
  { href: "/admin/doxologia", label: "Doxologia", icon: BookOpen },
  { href: "/admin/anuncios", label: "Anúncios", icon: Megaphone },
  { href: "/admin/boletins", label: "Boletins", icon: FileText },
  { href: "/admin/retrospectiva", label: "Retrospectiva", icon: Images },
  { href: "/admin/sonoplastia", label: "Sonoplastia", icon: FolderOpen },
  { href: "/admin/planos", label: "Planos", icon: Sparkles },
];

/**
 * Fase 11.5 (Etapa 6): the admin area used to share the same flat top-nav-row treatment as the
 * public site. A back office reads as more professional with a persistent rail instead of a row
 * that just wraps onto a second line as items grow - this renders as a sticky sidebar on desktop
 * (lg+) and collapses to the same horizontal pill-scroll pattern as the public ChurchNav on
 * mobile, so neither surface fights for the same treatment.
 */
export function AdminNav({ planName }: { planName?: string }) {
  const pathname = usePathname();

  return (
    <>
      <nav className="hidden lg:flex lg:sticky lg:top-24 lg:h-fit lg:w-56 lg:shrink-0 lg:flex-col lg:gap-1">
        {planName && (
          <div className="mb-3 flex items-center gap-2 rounded-[var(--radius-md)] border border-border-soft bg-surface px-3 py-2 text-xs">
            <span className="text-accent-star" aria-hidden="true">✦</span>
            <span className="text-text-secondary">Plano</span>
            <span className="ml-auto font-medium text-foreground">{planName}</span>
          </div>
        )}
        {adminLinks.map((link) => {
          const active = pathname === link.href;
          const Icon = link.icon;
          return (
            <Link
              key={link.href}
              href={link.href}
              className={`flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5 text-sm font-medium transition-colors ${
                active
                  ? "bg-primary-container text-on-primary-container"
                  : "text-foreground/75 hover:bg-surface hover:text-foreground"
              }`}
            >
              <Icon size={17} className="shrink-0" />
              {link.label}
            </Link>
          );
        })}
      </nav>

      <nav className="lg:hidden -mx-1 flex gap-2 overflow-x-auto px-1 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {adminLinks.map((link) => {
          const active = pathname === link.href;
          return (
            <Link
              key={link.href}
              href={link.href}
              className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                active ? "bg-primary text-white" : "bg-primary-container/40 text-foreground/80 hover:text-primary"
              }`}
            >
              {link.label}
            </Link>
          );
        })}
      </nav>
    </>
  );
}
