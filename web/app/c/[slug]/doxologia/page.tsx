import { notFound } from "next/navigation";
import { Calendar, Clock } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { isDoxologyLiveNow } from "@/lib/currentSession";
import { RETROSPECTIVE_WINDOW_DAYS } from "@/lib/expiredAnnouncements";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

/**
 * Fase 11.8.2 (Bloco F): before this fix, the query only fetched `date >= today` with no fallback
 * - a doxologia stayed visible right up until its own day, then vanished from the site forever
 * with no equivalent to the announcements' Retrospectiva window. Live data confirmed this: a
 * church whose only 2 doxologias had already happened showed a completely empty page. Recently
 * concluded services (same RETROSPECTIVE_WINDOW_DAYS used for announcements) now stay visible in
 * a separate "Recentes" section instead of disappearing the moment the date passes.
 */
export default async function DoxologiaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const today = new Date();
  const todayStr = today.toISOString().slice(0, 10);
  const windowStart = new Date(today);
  windowStart.setDate(windowStart.getDate() - RETROSPECTIVE_WINDOW_DAYS);
  const windowStartStr = windowStart.toISOString().slice(0, 10);

  const { data } = await supabase
    .from("doxologies")
    .select("*")
    .eq("church_id", church.id)
    .gte("date", windowStartStr)
    .order("date", { ascending: true })
    .order("start_time", { ascending: true })
    .limit(20);

  const items = (data as Doxology[]) ?? [];
  const upcoming = items.filter((item) => item.date >= todayStr);
  const recent = items
    .filter((item) => item.date < todayStr)
    .sort((a, b) => (a.date === b.date ? 0 : a.date < b.date ? 1 : -1));
  const now = new Date();

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Doxologia</h2>
        <p className="mt-1 text-sm text-text-secondary">Ordem do culto para os próximos cultos.</p>
      </div>

      {upcoming.length === 0 && recent.length === 0 ? (
        <EmptyState message="Nenhuma programação cadastrada." />
      ) : (
        <>
          {upcoming.length === 0 ? (
            <EmptyState message="Nenhuma programação futura cadastrada." />
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-5">
              {upcoming.map((doxology) => (
                <DoxologyCard key={doxology.id} doxology={doxology} isLive={isDoxologyLiveNow(doxology, now)} />
              ))}
            </div>
          )}

          {recent.length > 0 && (
            <div className="flex flex-col gap-4">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-text-muted">Recentes</h3>
              <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-5">
                {recent.map((doxology) => (
                  <DoxologyCard key={doxology.id} doxology={doxology} isLive={false} concluded />
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function DoxologyCard({
  doxology,
  isLive,
  concluded = false,
}: {
  doxology: Doxology;
  isLive: boolean;
  concluded?: boolean;
}) {
  const steps = (doxology.program_order ?? []).slice().sort((a, b) => a.order - b.order);

  return (
    <Card
      className={`p-6 flex flex-col gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 ${
        isLive ? "ring-2 ring-primary" : ""
      } ${concluded ? "opacity-70" : ""}`}
    >
      <div>
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-lg font-semibold">{doxology.title}</h3>
          {isLive && (
            <span className="flex shrink-0 items-center gap-1.5 rounded-full bg-primary px-2.5 py-1 text-xs font-medium text-white">
              <span className="h-1.5 w-1.5 rounded-full bg-white animate-pulse" /> Agora
            </span>
          )}
          {concluded && (
            <span className="flex shrink-0 items-center rounded-full bg-background-secondary px-2.5 py-1 text-xs font-medium text-text-secondary">
              Concluída
            </span>
          )}
        </div>
        <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
          <Calendar size={14} />
          {formatDatePt(doxology.date)} às {formatTimePt(doxology.start_time)}
          {doxology.end_time ? ` - ${formatTimePt(doxology.end_time)}` : ""}
        </p>
      </div>

      {steps.length > 0 && (
        <ol className="relative flex flex-col gap-4 pl-1">
          {steps.map((step, index) => (
            <li key={index} className="relative pl-7">
              {index !== steps.length - 1 && (
                <span className="absolute left-[9px] top-6 bottom-[-16px] w-px bg-divider" />
              )}
              <span className="absolute left-0 top-0.5 flex h-[19px] w-[19px] items-center justify-center rounded-full bg-primary-container text-[10px] font-semibold text-on-primary-container">
                {step.order}
              </span>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-medium leading-tight">{step.title}</p>
                  {step.responsible_person && (
                    <p className="text-sm text-text-secondary">{step.responsible_person}</p>
                  )}
                  {step.description && (
                    <p className="text-xs text-text-secondary mt-0.5">{step.description}</p>
                  )}
                </div>
                {step.estimated_duration_minutes != null && (
                  <span className="shrink-0 flex items-center gap-1 text-xs text-text-secondary">
                    <Clock size={12} />
                    {step.estimated_duration_minutes} min
                  </span>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}

      {doxology.notes && (
        <p className="text-sm text-text-secondary italic border-t border-divider pt-3">
          {doxology.notes}
        </p>
      )}
    </Card>
  );
}
