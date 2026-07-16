"use client";

import { usePathname, useRouter } from "next/navigation";
import { LayoutDashboard, Calendar, Megaphone, Sparkles, CalendarPlus, Music, FileText, Images, FolderOpen, Users } from "lucide-react";
import { StellaDock } from "./StellaDock";
import { StellaCoreAction } from "./StellaCore";

const DOCK_ITEMS: [
  { href: string; label: string; icon: typeof Calendar },
  { href: string; label: string; icon: typeof Calendar },
  { href: string; label: string; icon: typeof Calendar },
  { href: string; label: string; icon: typeof Calendar },
] = [
  { href: "/admin", label: "Painel", icon: LayoutDashboard },
  { href: "/admin/escalas", label: "Escalas", icon: Calendar },
  { href: "/admin/anuncios", label: "Anúncios", icon: Megaphone },
  { href: "/admin/planos", label: "Planos", icon: Sparkles },
];

/** Same contextual mapping as the previous AdminStellaCore (Fase 5), routes not covered by the
 *  dock's 4 fixed icons (Doxologia, Boletins, Retrospectiva, Sonoplastia) stay reachable through
 *  the star instead of needing a 5th/6th dock slot. */
function actionsForPath(pathname: string, push: (href: string) => void, hasAdvancedMedia: boolean): StellaCoreAction[] {
  if (pathname === "/admin") {
    return [
      { id: "new_scale", label: "Nova escala", icon: CalendarPlus, onClick: () => push("/admin/escalas/nova") },
      { id: "new_announcement", label: "Novo anúncio", icon: Megaphone, onClick: () => push("/admin/anuncios/nova") },
    ];
  }
  if (pathname.startsWith("/admin/escalas")) {
    return [{ id: "new_scale", label: "Nova escala", icon: CalendarPlus, onClick: () => push("/admin/escalas/nova") }];
  }
  if (pathname.startsWith("/admin/doxologia")) {
    return [{ id: "new_doxology", label: "Nova doxologia", icon: Music, onClick: () => push("/admin/doxologia/nova") }];
  }
  if (pathname.startsWith("/admin/anuncios")) {
    return [
      { id: "new_announcement", label: "Novo anúncio", icon: Megaphone, onClick: () => push("/admin/anuncios/nova") },
      {
        id: "media",
        label: "Gerenciar mídia",
        icon: FolderOpen,
        locked: !hasAdvancedMedia,
        onClick: () => push(hasAdvancedMedia ? "/admin/sonoplastia" : "/admin/planos"),
      },
    ];
  }
  if (pathname.startsWith("/admin/boletins")) {
    return [{ id: "new_bulletin", label: "Novo boletim", icon: FileText, onClick: () => push("/admin/boletins/nova") }];
  }
  if (pathname.startsWith("/admin/retrospectiva")) {
    return [{ id: "new_retro", label: "Novo item", icon: Images, onClick: () => push("/admin/retrospectiva/nova") }];
  }
  return [
    { id: "doxologia", label: "Doxologia", icon: Music, onClick: () => push("/admin/doxologia") },
    { id: "boletins", label: "Boletins", icon: FileText, onClick: () => push("/admin/boletins") },
    { id: "people", label: "Pessoas e equipes", icon: Users, onClick: () => push("/admin/pessoas") },
  ];
}

export function AdminStellaDock({ hasAdvancedMedia }: { hasAdvancedMedia: boolean }) {
  const pathname = usePathname();
  const router = useRouter();
  const actions = actionsForPath(pathname, (href) => router.push(href), hasAdvancedMedia);
  return <StellaDock items={DOCK_ITEMS} stellaCoreActions={actions} />;
}
