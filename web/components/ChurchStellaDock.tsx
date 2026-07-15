"use client";

import { usePathname, useRouter } from "next/navigation";
import { Calendar, Music2, Megaphone, FileText, Images, ShieldCheck } from "lucide-react";
import { StellaDock } from "./StellaDock";
import { StellaCoreAction } from "./StellaCore";

/**
 * Fase 11.8 (Parte 2-7): the public church site's primary navigation - four icons (Escala,
 * Doxologia, Anúncios, Boletins) flank the Stella Core; Retrospectiva and the admin entry point
 * move into the star's contextual menu instead of competing for a fifth dock slot, per "reduzir
 * a quantidade de informação permanente na tela".
 */
export function ChurchStellaDock({ slug }: { slug: string }) {
  const pathname = usePathname();
  const router = useRouter();
  const base = `/c/${slug}`;

  const items: [
    { href: string; label: string; icon: typeof Calendar },
    { href: string; label: string; icon: typeof Calendar },
    { href: string; label: string; icon: typeof Calendar },
    { href: string; label: string; icon: typeof Calendar },
  ] = [
    { href: base, label: "Escala", icon: Calendar },
    { href: `${base}/doxologia`, label: "Doxologia", icon: Music2 },
    { href: `${base}/anuncios`, label: "Anúncios", icon: Megaphone },
    { href: `${base}/boletins`, label: "Boletins", icon: FileText },
  ];

  const actions: StellaCoreAction[] = [
    { id: "retro", label: "Retrospectiva", icon: Images, onClick: () => router.push(`${base}/retrospectiva`) },
  ];
  if (pathname !== base) {
    actions.push({ id: "escala", label: "Escala", icon: Calendar, onClick: () => router.push(base) });
  }
  actions.push({ id: "admin", label: "Área administrativa", icon: ShieldCheck, onClick: () => router.push("/admin") });

  return <StellaDock items={items} stellaCoreActions={actions} />;
}
