import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { getEntitlements } from "@/lib/entitlements";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { constellationForDay, constellationLabel } from "@/components/celestial/DayConstellation";
import { PrintTrigger } from "./PrintTrigger";

export const revalidate = 0;

const ROLE_FIELDS: { key: keyof Scale; label: string }[] = [
  { key: "reception_person", label: "Recepção" },
  { key: "sound_person", label: "Sonoplastia" },
  { key: "preaching_person", label: "Pregação" },
  { key: "conducting_person", label: "Regência" },
  { key: "musical_message_person", label: "Mensagem musical" },
];

/**
 * Fase 11.7 (Parte 10, "impressão"): a dedicated print-optimized route instead of a print
 * stylesheet bolted onto the interactive admin list - the browser's own "Salvar como PDF" in the
 * print dialog is the PDF path for this phase (see export-actions.ts's comment on why a
 * dedicated PDF-generation library wasn't added blind). Constellations appear only as a discrete
 * watermark, per Parte 21 - the weekday text is always the real label.
 */
export default async function PrintGeneralScalePage({
  searchParams,
}: {
  searchParams: Promise<{ month?: string }>;
}) {
  const { month } = await searchParams;
  const { isAdmin, churchId } = await getAdminStatus();
  if (!isAdmin || !churchId) redirect("/login");

  const entitlements = await getEntitlements(churchId);
  if (!entitlements.features.has("EXPORT_GENERAL_SCALE")) {
    return (
      <div className="mx-auto max-w-md text-center py-16">
        <h1 className="text-lg font-semibold mb-2">Exportação não disponível</h1>
        <p className="text-sm text-text-secondary">
          Exportar a Escala Geral faz parte de um plano superior. Veja Planos e recursos.
        </p>
      </div>
    );
  }

  const supabase = await createClient();
  const { data: church } = await supabase.from("churches").select("name").eq("id", churchId).single();
  let query = supabase.from("scales").select("*").eq("church_id", churchId).order("date", { ascending: true });
  if (month) {
    const [y, m] = month.split("-").map(Number);
    const nextMonth = m === 12 ? `${y + 1}-01-01` : `${y}-${String(m + 1).padStart(2, "0")}-01`;
    query = query.gte("date", `${month}-01`).lt("date", nextMonth);
  }
  const { data } = await query;
  const items = (data as Scale[]) ?? [];

  return (
    <div className="mx-auto max-w-3xl bg-white text-black p-8 print:p-0">
      <PrintTrigger month={month} />
      <style>{`
        @media print {
          @page { margin: 16mm; }
          body { background: white !important; }
        }
      `}</style>

      <header className="flex items-center justify-between border-b border-black/20 pb-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold">{church?.name ?? "Escala Church"}</h1>
          <p className="text-sm text-black/60">Escala Geral{month ? ` · ${month}` : ""}</p>
        </div>
        <p className="text-xs text-black/50">Gerado em {new Date().toLocaleString("pt-BR")}</p>
      </header>

      <div className="flex flex-col gap-4">
        {items.map((scale) => {
          const kind = constellationForDay(new Date(`${scale.date}T00:00:00`).getDay(), scale.is_special_event);
          const roles = ROLE_FIELDS.filter(({ key }) => typeof scale[key] === "string" && (scale[key] as string).trim());
          return (
            <div key={scale.id} className="relative overflow-hidden rounded-lg border border-black/15 p-4 break-inside-avoid">
              <p className="text-[10px] uppercase tracking-wider text-black/40">{constellationLabel(kind)}</p>
              <p className="font-semibold">{scale.title}</p>
              <p className="text-sm text-black/60 capitalize">
                {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
                {scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}
                {scale.is_special_event ? " · Especial" : ""}
              </p>
              {roles.length > 0 && (
                <ul className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
                  {roles.map(({ key, label }) => (
                    <li key={key}>
                      <span className="text-black/50">{label}: </span>
                      {scale[key] as string}
                    </li>
                  ))}
                </ul>
              )}
              {scale.notes && <p className="mt-2 text-sm italic text-black/60">{scale.notes}</p>}
            </div>
          );
        })}
        {items.length === 0 && <p className="text-sm text-black/50">Nenhuma escala para o período selecionado.</p>}
      </div>
    </div>
  );
}
