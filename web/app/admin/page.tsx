import Link from "next/link";
import { createClient } from "@/lib/supabase/server";

export const revalidate = 0;

export default async function AdminDashboardPage() {
  const supabase = await createClient();
  const [{ count: scalesCount }, { count: doxologiesCount }, { count: announcementsCount }] =
    await Promise.all([
      supabase.from("scales").select("*", { count: "exact", head: true }),
      supabase.from("doxologies").select("*", { count: "exact", head: true }),
      supabase.from("announcements").select("*", { count: "exact", head: true }).eq("is_active", true),
    ]);

  const cards = [
    { href: "/admin/escalas", label: "Escalas", count: scalesCount ?? 0 },
    { href: "/admin/doxologia", label: "Doxologia", count: doxologiesCount ?? 0 },
    { href: "/admin/anuncios", label: "Anúncios", count: announcementsCount ?? 0 },
    { href: "/admin/sonoplastia", label: "Arquivos (Sonoplastia)", count: null },
  ];

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Painel administrativo</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {cards.map((card) => (
          <Link
            key={card.href}
            href={card.href}
            className="rounded-2xl bg-surface p-5 shadow-sm border border-divider hover:border-primary transition-colors"
          >
            <p className="text-sm text-text-secondary">{card.label}</p>
            {card.count !== null && <p className="text-3xl font-semibold mt-1">{card.count}</p>}
          </Link>
        ))}
      </div>
    </div>
  );
}
