import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Doxology } from "@/lib/types/database";
import { EmptyState } from "@/components/EmptyState";
import { ReuseDoxologyList } from "./ReuseDoxologyList";

export const revalidate = 0;

/**
 * Fase 11.8.3 (Bloco N-P): "Reutilizar programação recente" - lists this church's Doxologias
 * (old and new alike, Bloco U: no date filter here, unlike the public page) so an admin can base
 * a new one on a past program instead of retyping it. Sorting (favorites, then most recent, then
 * most reused) and search happen client-side in ReuseDoxologyList - the admin list is typically
 * small enough that a second round-trip for search isn't worth it.
 */
export default async function ReutilizarDoxologiaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data, error } = await supabase
    .from("doxologies")
    .select("*")
    .eq("church_id", churchId)
    .order("date", { ascending: false })
    .limit(60);

  const items = (data as Doxology[]) ?? [];

  return (
    <div>
      <Link href="/admin/doxologia/nova" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Voltar
      </Link>
      <h1 className="font-display text-2xl mb-1">Reutilizar programação recente</h1>
      <p className="text-sm text-text-secondary mb-6">
        Escolha uma Doxologia já cadastrada para usar como base de uma nova - a original nunca é
        alterada.
      </p>

      {error ? (
        <EmptyState message="Não foi possível carregar as programações recentes. Tente novamente." />
      ) : items.length === 0 ? (
        <EmptyState message="Nenhuma programação recente disponível para reutilização." />
      ) : (
        <ReuseDoxologyList items={items} />
      )}
    </div>
  );
}
