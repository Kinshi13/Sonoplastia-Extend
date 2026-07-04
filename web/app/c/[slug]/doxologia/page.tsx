import { notFound } from "next/navigation";
import { Calendar, Clock } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

export default async function DoxologiaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const { data } = await supabase
    .from("doxologies")
    .select("*")
    .eq("church_id", church.id)
    .gte("date", today)
    .order("date", { ascending: true })
    .limit(10);

  const items = (data as Doxology[]) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Doxologia</h2>
        <p className="mt-1 text-sm text-text-secondary">Ordem do culto para os próximos cultos.</p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma programação futura cadastrada." />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {items.map((doxology) => (
            <DoxologyCard key={doxology.id} doxology={doxology} />
          ))}
        </div>
      )}
    </div>
  );
}

function DoxologyCard({ doxology }: { doxology: Doxology }) {
  const steps = doxology.program_order.slice().sort((a, b) => a.order - b.order);

  return (
    <Card className="p-6 flex flex-col gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      <div>
        <h3 className="text-lg font-semibold">{doxology.title}</h3>
        <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
          <Calendar size={14} />
          {formatDatePt(doxology.date)} às {formatTimePt(doxology.start_time)}
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
