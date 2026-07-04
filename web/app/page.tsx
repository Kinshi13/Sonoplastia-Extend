import { Calendar, Mic2, Music2, BookOpen, Users, Sparkles } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

const ROLE_FIELDS: { key: keyof Scale; label: string; icon: typeof Mic2 }[] = [
  { key: "sound_person", label: "Sonoplastia", icon: Mic2 },
  { key: "conducting_person", label: "Regência", icon: Music2 },
  { key: "musical_message_person", label: "Mensagem musical", icon: Music2 },
  { key: "preaching_person", label: "Pregação", icon: BookOpen },
  { key: "reception_person", label: "Recepção", icon: Users },
];

export default async function HomePage() {
  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const { data: scales, error } = await supabase
    .from("scales")
    .select("*")
    .gte("date", today)
    .order("date", { ascending: true })
    .order("start_time", { ascending: true })
    .limit(20);

  const items = (scales as Scale[]) ?? [];

  const debugInfo = {
    url: process.env.NEXT_PUBLIC_SUPABASE_URL ?? "(undefined)",
    keyLength: (process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ?? "").length,
    error: error ? { message: error.message, code: error.code, hint: error.hint } : null,
  };

  return (
    <>
    {(error || items.length === 0) && (
      <pre className="mb-4 whitespace-pre-wrap rounded-xl border border-red-500 bg-red-50 p-4 text-xs text-red-900">
        DEBUG (remover depois): {JSON.stringify(debugInfo, null, 2)}
      </pre>
    )}
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Próximas Escalas</h1>
        <p className="mt-1 text-sm text-text-secondary">
          Quem está escalado nos próximos cultos e eventos.
        </p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma escala futura cadastrada." />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {items.map((scale) => (
            <ScaleCard key={scale.id} scale={scale} />
          ))}
        </div>
      )}
    </div>
    </>
  );
}

function ScaleCard({ scale }: { scale: Scale }) {
  const roles = ROLE_FIELDS.filter(({ key }) => {
    const value = scale[key];
    return typeof value === "string" && value.trim().length > 0;
  });

  return (
    <Card className="p-6 flex flex-col gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold leading-tight">{scale.title}</h2>
          <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
            <Calendar size={14} className="shrink-0" />
            {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
            {scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}
          </p>
        </div>
        {scale.is_special_event && (
          <span className="flex items-center gap-1 shrink-0 rounded-full bg-primary-container px-3 py-1 text-xs font-medium text-on-primary-container">
            <Sparkles size={12} /> Especial
          </span>
        )}
      </div>

      {roles.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {roles.map(({ key, label, icon: Icon }) => (
            <div
              key={key}
              className="flex items-center gap-2 rounded-xl bg-background px-3 py-2 text-sm border border-divider"
            >
              <Icon size={15} className="text-primary shrink-0" />
              <div className="leading-tight">
                <p className="text-[11px] text-text-secondary">{label}</p>
                <p className="font-medium">{scale[key] as string}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {scale.notes && (
        <p className="text-sm text-text-secondary italic border-t border-divider pt-3">
          {scale.notes}
        </p>
      )}
    </Card>
  );
}
