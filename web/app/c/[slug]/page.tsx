import { notFound } from "next/navigation";
import { Calendar, Mic2, Music2, BookOpen, Users, Sparkles, CalendarClock, Radio } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { CelestialHeroCard, CelestialCompactCard, CelestialOfficialHeader } from "@/components/celestial/CelestialCard";
import { constellationForDay } from "@/components/celestial/DayConstellation";

export const revalidate = 0;

const ROLE_FIELDS: { key: keyof Scale; label: string; icon: typeof Mic2 }[] = [
  { key: "sound_person", label: "Sonoplastia", icon: Mic2 },
  { key: "conducting_person", label: "Regência", icon: Music2 },
  { key: "musical_message_person", label: "Mensagem musical", icon: Music2 },
  { key: "preaching_person", label: "Pregação", icon: BookOpen },
  { key: "reception_person", label: "Recepção", icon: Users },
];

function dayKindFor(scale: Scale) {
  return constellationForDay(new Date(`${scale.date}T00:00:00`).getDay(), scale.is_special_event);
}

export default async function ChurchHomePage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const { data: scales } = await supabase
    .from("scales")
    .select("*")
    .eq("church_id", church.id)
    .gte("date", today)
    .order("date", { ascending: true })
    .order("start_time", { ascending: true })
    .limit(20);

  const items = (scales as Scale[]) ?? [];
  const [next, ...rest] = items;

  return (
    <div className="flex flex-col gap-10">
      {next ? (
        <Hero church={church.name} next={next} upcomingCount={items.length} />
      ) : (
        <div>
          <h2 className="font-display text-3xl tracking-tight">Próximas Escalas</h2>
          <p className="mt-1 text-sm text-text-secondary">Quem está escalado nos próximos cultos e eventos.</p>
        </div>
      )}

      {items.length === 0 ? (
        <EmptyState message="Nenhuma escala futura cadastrada." />
      ) : rest.length > 0 ? (
        <div className="flex flex-col gap-4">
          <h3 className="font-display text-lg text-foreground/90">Depois dessa</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-5">
            {rest.map((scale) => (
              <ScaleCard key={scale.id} scale={scale} />
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

/**
 * Fase 11.7 (Parte 8): the hero now carries the day's own constellation identity - Coroa for
 * sábado gets the "solene" cut-corner treatment automatically (CelestialHeroCard maps CROWN to
 * `solemn`), a title set in fontDisplay, and the constellation watermarked low-opacity in the
 * background instead of a generic radial blob.
 */
function Hero({ church, next, upcomingCount }: { church: string; next: Scale; upcomingCount: number }) {
  const roles = ROLE_FIELDS.filter(({ key }) => {
    const value = next[key];
    return typeof value === "string" && value.trim().length > 0;
  });
  const kind = dayKindFor(next);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[1.6fr_1fr] gap-5">
      <CelestialHeroCard kind={kind} className="p-7 sm:p-9">
        <div className="flex flex-col gap-5">
          <div className="flex items-center justify-between gap-3">
            <CelestialOfficialHeader kind={kind} org={church} />
          </div>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <h1 className="font-display text-3xl sm:text-4xl tracking-tight text-balance">{next.title}</h1>
            <div className="flex shrink-0 flex-wrap items-center gap-2">
              {next.is_special_event && (
                <span className="flex items-center gap-1 shrink-0 rounded-full bg-primary-container px-3 py-1 text-xs font-medium text-on-primary-container">
                  <Sparkles size={12} /> Especial
                </span>
              )}
              {next.is_temporary && (
                <span className="flex items-center gap-1 shrink-0 rounded-full bg-amber-500/15 px-3 py-1 text-xs font-medium text-amber-600">
                  Temporária
                </span>
              )}
            </div>
          </div>
          <p className="flex items-center gap-1.5 text-sm text-text-secondary capitalize">
            <Calendar size={14} className="shrink-0" />
            {formatDatePt(next.date)} às {formatTimePt(next.start_time)}
            {next.end_time ? ` - ${formatTimePt(next.end_time)}` : ""}
          </p>

          {roles.length > 0 && (
            <div className="flex flex-wrap gap-2 pt-1">
              {roles.map(({ key, label, icon: Icon }) => (
                <div
                  key={key}
                  className="flex items-center gap-2 rounded-[var(--radius-md)] bg-background px-3 py-2 text-sm border border-border-soft"
                >
                  <Icon size={15} className="text-primary shrink-0" />
                  <div className="leading-tight">
                    <p className="text-[11px] text-text-secondary">{label}</p>
                    <p className="font-medium">{next[key] as string}</p>
                  </div>
                </div>
              ))}
            </div>
          )}

          {next.notes && (
            <p className="text-sm text-text-secondary italic border-t border-border-soft pt-4">{next.notes}</p>
          )}
        </div>
      </CelestialHeroCard>

      <div className="flex flex-col gap-5">
        <Card className="p-6 flex items-center gap-4">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-primary-container text-on-primary-container">
            <CalendarClock size={20} />
          </span>
          <div>
            <p className="text-sm text-text-secondary">Escalas futuras cadastradas</p>
            <p className="text-2xl font-semibold mt-0.5">{upcomingCount}</p>
          </div>
        </Card>
        <Card className="p-6 flex items-center gap-4">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-primary-container text-on-primary-container">
            <Radio size={20} />
          </span>
          <div>
            <p className="text-sm text-text-secondary">Sincronizado com o app</p>
            <p className="text-sm font-medium mt-0.5 text-foreground">Em tempo real</p>
          </div>
        </Card>
      </div>
    </div>
  );
}

function ScaleCard({ scale }: { scale: Scale }) {
  const roles = ROLE_FIELDS.filter(({ key }) => {
    const value = scale[key];
    return typeof value === "string" && value.trim().length > 0;
  });
  const kind = dayKindFor(scale);

  return (
    <CelestialCompactCard kind={kind} id={scale.id} interactive className="p-6 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1.5">
          <CelestialOfficialHeader kind={kind} org="" showOrg={false} />
          <h3 className="text-lg font-semibold leading-tight">{scale.title}</h3>
          <p className="flex items-center gap-1.5 text-sm text-text-secondary capitalize">
            <Calendar size={14} className="shrink-0" />
            {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
            {scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          {scale.is_special_event && (
            <span className="flex items-center gap-1 shrink-0 rounded-full bg-primary-container px-3 py-1 text-xs font-medium text-on-primary-container">
              <Sparkles size={12} /> Especial
            </span>
          )}
          {scale.is_temporary && (
            <span className="flex items-center gap-1 shrink-0 rounded-full bg-amber-500/15 px-3 py-1 text-xs font-medium text-amber-600">
              Temporária
            </span>
          )}
        </div>
      </div>

      {roles.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {roles.map(({ key, label, icon: Icon }) => (
            <div
              key={key}
              className="flex items-center gap-2 rounded-[var(--radius-md)] bg-background px-3 py-2 text-sm border border-border-soft"
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
        <p className="text-sm text-text-secondary italic border-t border-border-soft pt-3">
          {scale.notes}
        </p>
      )}
    </CelestialCompactCard>
  );
}
