import { Calendar, BookOpen, Megaphone, FolderOpen, Images } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { CardLink } from "@/components/Card";

export const revalidate = 0;

export default async function AdminDashboardPage() {
  const supabase = await createClient();
  const [
    { count: scalesCount },
    { count: doxologiesCount },
    { count: announcementsCount },
    { count: retrospectiveCount },
  ] = await Promise.all([
    supabase.from("scales").select("*", { count: "exact", head: true }),
    supabase.from("doxologies").select("*", { count: "exact", head: true }),
    supabase.from("announcements").select("*", { count: "exact", head: true }).eq("is_active", true),
    supabase.from("retrospective_items").select("*", { count: "exact", head: true }).eq("is_active", true),
  ]);

  const cards = [
    { href: "/admin/escalas", label: "Escalas", count: scalesCount ?? 0, icon: Calendar },
    { href: "/admin/doxologia", label: "Doxologia", count: doxologiesCount ?? 0, icon: BookOpen },
    { href: "/admin/anuncios", label: "Anúncios", count: announcementsCount ?? 0, icon: Megaphone },
    { href: "/admin/retrospectiva", label: "Retrospectiva", count: retrospectiveCount ?? 0, icon: Images },
    { href: "/admin/sonoplastia", label: "Arquivos (Sonoplastia)", count: null, icon: FolderOpen },
  ];

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Painel administrativo</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {cards.map((card) => (
          <CardLink key={card.href} href={card.href} className="p-5 flex items-center gap-4">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-container text-on-primary-container">
              <card.icon size={20} />
            </span>
            <div>
              <p className="text-sm text-text-secondary">{card.label}</p>
              {card.count !== null && <p className="text-2xl font-semibold mt-0.5">{card.count}</p>}
            </div>
          </CardLink>
        ))}
      </div>
    </div>
  );
}
