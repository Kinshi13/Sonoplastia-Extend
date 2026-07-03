import { createClient } from "@/lib/supabase/server";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";

export const revalidate = 0;

export default async function DoxologiaPage() {
  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const { data } = await supabase
    .from("doxologies")
    .select("*")
    .gte("date", today)
    .order("date", { ascending: true })
    .limit(10);

  const items = (data as Doxology[]) ?? [];

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold">Doxologia</h1>

      {items.length === 0 ? (
        <p className="text-text-secondary">Nenhuma programação futura cadastrada.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {items.map((doxology) => (
            <DoxologyCard key={doxology.id} doxology={doxology} />
          ))}
        </div>
      )}
    </div>
  );
}

function DoxologyCard({ doxology }: { doxology: Doxology }) {
  return (
    <div className="rounded-2xl bg-surface p-5 shadow-sm border border-divider">
      <h2 className="text-lg font-semibold">{doxology.title}</h2>
      <p className="mt-1 text-sm text-text-secondary capitalize">
        {formatDatePt(doxology.date)} às {formatTimePt(doxology.start_time)}
      </p>

      {doxology.program_order.length > 0 && (
        <ol className="mt-4 flex flex-col gap-2">
          {doxology.program_order
            .slice()
            .sort((a, b) => a.order - b.order)
            .map((step, index) => (
              <li
                key={index}
                className="flex items-start justify-between gap-3 border-b border-divider/60 pb-2 text-sm"
              >
                <div>
                  <span className="font-medium">{step.title}</span>
                  {step.responsible_person && (
                    <span className="text-text-secondary"> — {step.responsible_person}</span>
                  )}
                  {step.description && (
                    <p className="text-text-secondary text-xs mt-0.5">{step.description}</p>
                  )}
                </div>
                {step.estimated_duration_minutes != null && (
                  <span className="shrink-0 text-text-secondary">
                    {step.estimated_duration_minutes} min
                  </span>
                )}
              </li>
            ))}
        </ol>
      )}

      {doxology.notes && (
        <p className="mt-3 text-sm text-text-secondary italic">{doxology.notes}</p>
      )}
    </div>
  );
}
