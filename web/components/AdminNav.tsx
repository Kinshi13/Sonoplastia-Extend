"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Calendar, BookOpen, Megaphone, FileText, Images, FolderOpen, Sparkles, Users, Music, LucideIcon } from "lucide-react";

const adminLinks: { href: string; label: string; icon: LucideIcon }[] = [
  { href: "/admin", label: "Painel", icon: LayoutDashboard },
  { href: "/admin/escalas", label: "Escalas", icon: Calendar },
  { href: "/admin/musica", label: "Música e Louvor", icon: Music },
  { href: "/admin/doxologia", label: "Doxologia", icon: BookOpen },
  { href: "/admin/anuncios", label: "Anúncios", icon: Megaphone },
  { href: "/admin/pessoas", label: "Pessoas e Equipes", icon: Users },
  { href: "/admin/boletins", label: "Boletins", icon: FileText },
  { href: "/admin/retrospectiva", label: "Retrospectiva", icon: Images },
  { href: "/admin/sonoplastia", label: "Sonoplastia", icon: FolderOpen },
  { href: "/admin/planos", label: "Planos", icon: Sparkles },
];

/**
 * Fase 11.5 (Etapa 6) / Fase 11.8 (Parte 3): desktop-only persistent sidebar - Stella Core stays
 * a floating contextual núcleo on desktop (see AdminStellaCore), per the Master Plan's explicit
 * allowance that it "pode ficar integrada à sidebar... ou como núcleo contextual flutuante" and
 * doesn't have to sit centered at the bottom like the mobile dock. Mobile no longer uses this
 * component at all - see AdminStellaDock, which replaces both this pill row and the standalone
 * floating star with one bottom dock.
 */
export function AdminNav({ planName }: { planName?: string }) {
  const pathname = usePathname();

  return (
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
  );
}
