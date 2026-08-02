import Link from "next/link";
import { Calendar, Plus, RotateCcw, LayoutTemplate, Shuffle } from "lucide-react";
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
import { LEGACY_ROLE_DEFS } from "@/lib/legacyRoles";

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
            href="/admin/escalas/modelos"
            className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-primary hover:bg-primary-container/20 transition-colors"
          >
            <LayoutTemplate size={14} /> Modelos
          </Link>
          <Link
            href="/admin/escalas/reutilizar"
            className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-primary hover:bg-primary-container/20 transition-colors"
          >
            <RotateCcw size={14} /> Reutilizar
          </Link>
          <Link
            href="/admin/escalas/aleatoria"
            className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-primary hover:bg-primary-container/20 transition-colors"
          >
            <Shuffle size={14} /> Aleatória
          </Link>
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
            // Hotfix: this list never showed who's assigned, so a successful save was invisible
            // without opening "Editar" again - reads straight off the legacy columns, which is
            // what saveScaleAction always keeps up to date regardless of migration state.
            const assigned = LEGACY_ROLE_DEFS.map((d) => scale[d.field]).filter((v) => v && v.trim().length > 0);
            return (
              <CelestialAdminCard key={scale.id} kind={kind} className="p-5 flex flex-col gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <CelestialOfficialHeader kind={kind} org="" showOrg={false} />
                    {scale.is_temporary && (
                      <span className="shrink-0 rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-medium text-amber-600">
                        Temporária
                      </span>
                    )}
                  </div>
                  <p className="font-semibold leading-tight mt-1.5">{scale.title}</p>
                  <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
                    <Calendar size={14} className="shrink-0" />
                    {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
                  </p>
                  {assigned.length > 0 ? (
                    <p className="mt-2 text-xs text-text-secondary line-clamp-2">{assigned.join(" · ")}</p>
                  ) : (
                    <p className="mt-2 text-xs text-text-muted italic">Ninguém escalado ainda</p>
                  )}
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
