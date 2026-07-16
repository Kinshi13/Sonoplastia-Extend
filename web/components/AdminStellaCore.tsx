"use client";

import { usePathname, useRouter } from "next/navigation";
import { CalendarPlus, Music, Megaphone, FileText, Images, FolderOpen, Users } from "lucide-react";
import { StellaCore, StellaCoreAction } from "@/components/StellaCore";

/**
 * Web mirror of Android's StellaCoreConnector (Fase 5 Section 9): maps the current admin route to
 * a small contextual action list. Every action here navigates to a route that already exists
 * (the "nova"/"[id]" pages under app/admin/*) - no faked/no-op actions.
 */
function actionsForPath(pathname: string, push: (href: string) => void, hasAdvancedMedia: boolean): StellaCoreAction[] {
  if (pathname === "/admin") {
    return [
      { id: "new_scale", label: "Escala", subtitle: "Nova escala", icon: CalendarPlus, onClick: () => push("/admin/escalas/nova") },
      { id: "new_announcement", label: "Anúncios", subtitle: "Novo anúncio", icon: Megaphone, onClick: () => push("/admin/anuncios/nova") },
      { id: "people", label: "Pessoas", subtitle: "Pessoas e equipes", icon: Users, onClick: () => push("/admin/pessoas") },
    ];
  }
  if (pathname.startsWith("/admin/escalas")) {
    return [
      { id: "new_scale", label: "Escala", subtitle: "Nova escala", icon: CalendarPlus, onClick: () => push("/admin/escalas/nova") },
      { id: "people", label: "Pessoas", subtitle: "Pessoas e equipes", icon: Users, onClick: () => push("/admin/pessoas") },
    ];
  }
  if (pathname.startsWith("/admin/pessoas")) {
    return [{ id: "new_scale", label: "Escala", subtitle: "Nova escala", icon: CalendarPlus, onClick: () => push("/admin/escalas/nova") }];
  }
  if (pathname.startsWith("/admin/doxologia")) {
    return [{ id: "new_doxology", label: "Doxologia", subtitle: "Nova doxologia", icon: Music, onClick: () => push("/admin/doxologia/nova") }];
  }
  if (pathname.startsWith("/admin/anuncios")) {
    return [
      { id: "new_announcement", label: "Anúncios", subtitle: "Novo anúncio", icon: Megaphone, onClick: () => push("/admin/anuncios/nova") },
      {
        id: "media",
        label: "Mídia",
        subtitle: "Gerenciar mídia",
        icon: FolderOpen,
        locked: !hasAdvancedMedia,
        // Locked churches land on Planos instead of Sonoplastia - the same "preview, not a dead
        // end" behavior as Android's PremiumPreviewSheet, just without a dedicated sheet here.
        onClick: () => push(hasAdvancedMedia ? "/admin/sonoplastia" : "/admin/planos"),
      },
    ];
  }
  if (pathname.startsWith("/admin/boletins")) {
    return [{ id: "new_bulletin", label: "Boletins", subtitle: "Novo boletim", icon: FileText, onClick: () => push("/admin/boletins/nova") }];
  }
  if (pathname.startsWith("/admin/retrospectiva")) {
    return [{ id: "new_retro", label: "Histórico", subtitle: "Novo item", icon: Images, onClick: () => push("/admin/retrospectiva/nova") }];
  }
  return [];
}

export function AdminStellaCore({ hasAdvancedMedia }: { hasAdvancedMedia: boolean }) {
  const pathname = usePathname();
  const router = useRouter();
  const actions = actionsForPath(pathname, (href) => router.push(href), hasAdvancedMedia);
  return <StellaCore actions={actions} />;
}
