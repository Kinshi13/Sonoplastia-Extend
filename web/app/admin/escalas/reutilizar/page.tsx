import Link from "next/link";
import { ArrowLeft, Calendar, RotateCcw } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

/** Fase 11.8.4 (Parte 11): "Reutilizar escala anterior" - picks a source scale to base a new one
 *  on. Structure only (title, type, roles, order) - never copies who was assigned. */
export default async function ReutilizarEscalaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("scales")
    .select("*")
    .eq("church_id", churchId)
    .order("date", { ascending: false })
    .limit(60);
  const items = (data as Scale[]) ?? [];

  return (
    <div>
      <Link href="/admin/escalas/nova" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Voltar
      </Link>
      <h1 className="font-display text-2xl mb-1">Reutilizar escala anterior</h1>
      <p className="text-sm text-text-secondary mb-6">
        Escolha uma escala já cadastrada para reaproveitar sua estrutura (título, funções e ordem)
        em uma nova data - as pessoas ficam em branco para você preencher.
      </p>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma escala recente disponível para reutilização." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <Card key={item.id} className="p-5 flex flex-col gap-3">
              <div>
                <p className="font-semibold leading-tight">{item.title}</p>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
                  <Calendar size={14} className="shrink-0" />
                  {formatDatePt(item.date)} às {formatTimePt(item.start_time)}
                </p>
              </div>
              <Link
                href={`/admin/escalas/reutilizar/${item.id}`}
                className="mt-auto flex items-center justify-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90 transition-opacity"
              >
                <RotateCcw size={14} /> Usar esta
              </Link>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
