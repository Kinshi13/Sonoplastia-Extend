import { createClient } from "@/lib/supabase/server";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";

export const revalidate = 0;

const ROLE_FIELDS: { key: keyof Scale; label: string }[] = [
  { key: "sound_person", label: "Sonoplastia" },
  { key: "conducting_person", label: "Regência" },
  { key: "musical_message_person", label: "Mensagem musical" },
  { key: "preaching_person", label: "Pregação" },
  { key: "reception_person", label: "Recepção" },
];

export default async function HomePage() {
  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const { data: scales } = await supabase
    .from("scales")
    .select("*")
    .gte("date", today)
    .order("date", { ascending: true })
    .order("start_time", { ascending: true })
    .limit(20);

  const items = (scales as Scale[]) ?? [];

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold">Próximas Escalas</h1>

      {items.length === 0 ? (
        <p className="text-text-secondary">Nenhuma escala futura cadastrada.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {items.map((scale) => (
            <ScaleCard key={scale.id} scale={scale} />
          ))}
        </div>
      )}
    </div>
  );
}

function ScaleCard({ scale }: { scale: Scale }) {
  return (
    <div className="rounded-2xl bg-surface p-5 shadow-sm border border-divider">
      <div className="flex items-baseline justify-between gap-2">
        <h2 className="text-lg font-semibold">{scale.title}</h2>
        {scale.is_special_event && (
          <span className="rounded-full bg-primary-container px-3 py-0.5 text-xs font-medium text-on-primary-container">
            Especial
          </span>
        )}
      </div>
      <p className="mt-1 text-sm text-text-secondary capitalize">
        {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
        {scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}
      </p>

      <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
        {ROLE_FIELDS.map(({ key, label }) => {
          const value = scale[key];
          if (!value || typeof value !== "string") return null;
          return (
            <div key={key} className="flex justify-between border-b border-divider/60 pb-1">
              <span className="text-text-secondary">{label}</span>
              <span className="font-medium">{value}</span>
            </div>
          );
        })}
      </div>

      {scale.notes && (
        <p className="mt-3 text-sm text-text-secondary italic">{scale.notes}</p>
      )}
    </div>
  );
}
