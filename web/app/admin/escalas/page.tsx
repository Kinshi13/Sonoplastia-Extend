import Link from "next/link";
import { Calendar, Plus } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { EmptyState } from "@/components/EmptyState";
import { CelestialAdminCard, CelestialOfficialHeader } from "@/components/celestial/CelestialCard";
import { constellationForDay } from "@/components/celestial/DayConstellation";
import { DeleteButton } from "../DeleteButton";
import { deleteScaleAction } from "../actions";
import { ExportStudio } from "./ExportStudio";

export const revalidate = 0;

export default async function AdminEscalasPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("scales")
    .select("*")
    .eq("church_id", churchId)
    .order("date", { ascending: true });
  const items = (data as Scale[]) ?? [];

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <h1 className="font-display text-2xl">Escalas</h1>
        <div className="flex items-center gap-2">
          <ExportStudio />
          <Link
            href="/admin/escalas/nova"
            className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
          >
            <Plus size={16} /> Nova escala
          </Link>
        </div>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma escala cadastrada." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {items.map((scale) => {
            const kind = constellationForDay(new Date(`${scale.date}T00:00:00`).getDay(), scale.is_special_event);
            return (
              <CelestialAdminCard key={scale.id} kind={kind} className="p-5 flex flex-col gap-3">
                <div>
                  <CelestialOfficialHeader kind={kind} org="" showOrg={false} />
                  <p className="font-semibold leading-tight mt-1.5">{scale.title}</p>
                  <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
                    <Calendar size={14} className="shrink-0" />
                    {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
                  </p>
                </div>
                <div className="mt-auto flex items-center justify-between gap-3 border-t border-border-soft pt-3">
                  <Link href={`/admin/escalas/${scale.id}`} className="text-sm font-medium text-primary">
                    Editar
                  </Link>
                  <DeleteButton id={scale.id} action={deleteScaleAction} />
                </div>
              </CelestialAdminCard>
            );
          })}
        </div>
      )}
    </div>
  );
}
