import { notFound } from "next/navigation";
import Link from "next/link";
import { Settings } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { getAdminStatus } from "@/lib/supabase/auth";
import { WorshipSong } from "@/lib/types/database";
import { formatDatePt } from "@/lib/format";
import { EmptyState } from "@/components/EmptyState";
import { AnunciosRetrospectivaTabs } from "@/components/AnunciosRetrospectivaTabs";
import { WorshipSongCard } from "@/components/WorshipSongCard";
import { WorshipNotificationOptIn } from "@/components/WorshipNotificationOptIn";

export const revalidate = 0;

/**
 * Música e Louvor (público) - prioriza, nesta ordem: (1) recomendação do dia publicada para hoje,
 * (2) músicas da próxima programação/escala vigente, (3) publicação mais recente, caso a próxima
 * programação ainda não tenha músicas cadastradas. "Gerenciar músicas" só aparece para quem já é
 * Admin desta igreja (getAdminStatus(), verificado no servidor) - a mesma experiência pública,
 * com um controle a mais, em vez de uma área administrativa desconectada.
 */
export default async function MusicaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const { isAdmin, churchId } = await getAdminStatus();
  const isAdminOfThisChurch = isAdmin && churchId === church.id;

  const today = new Date().toISOString().slice(0, 10);

  const { data: recommendation } = await supabase
    .from("worship_songs")
    .select("*")
    .eq("church_id", church.id)
    .eq("is_published", true)
    .eq("is_daily_recommendation", true)
    .eq("recommendation_date", today)
    .order("updated_at", { ascending: false })
    .limit(1)
    .maybeSingle();

  const { data: nextScale } = await supabase
    .from("scales")
    .select("date")
    .eq("church_id", church.id)
    .gte("date", today)
    .order("date", { ascending: true })
    .limit(1)
    .maybeSingle();

  let targetDate: string | null = nextScale?.date ?? null;

  if (targetDate) {
    const { count } = await supabase
      .from("worship_songs")
      .select("id", { count: "exact", head: true })
      .eq("church_id", church.id)
      .eq("is_published", true)
      .eq("program_date", targetDate);
    if (!count) targetDate = null;
  }

  if (!targetDate) {
    const { data: mostRecent } = await supabase
      .from("worship_songs")
      .select("program_date")
      .eq("church_id", church.id)
      .eq("is_published", true)
      .not("program_date", "is", null)
      .order("program_date", { ascending: false })
      .limit(1)
      .maybeSingle();
    targetDate = mostRecent?.program_date ?? null;
  }

  const { data } = targetDate
    ? await supabase
        .from("worship_songs")
        .select("*")
        .eq("church_id", church.id)
        .eq("is_published", true)
        .eq("program_date", targetDate)
        .order("order_index", { ascending: true })
    : { data: [] as WorshipSong[] };

  const recommendationSong = (recommendation as WorshipSong | null) ?? null;
  // The recommendation gets its own spotlight card above - excluded here so it isn't shown twice.
  const items = ((data as WorshipSong[]) ?? []).filter((song) => song.id !== recommendationSong?.id);

  return (
    <div className="flex flex-col gap-6">
      <AnunciosRetrospectivaTabs slug={slug} active="musica" />

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Música e Louvor</h2>
          <p className="mt-1 text-sm text-text-secondary">Canções e louvores da programação da igreja.</p>
        </div>
        {isAdminOfThisChurch && (
          <Link
            href="/admin/musica"
            className="flex shrink-0 items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-sm font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
          >
            <Settings size={14} /> Gerenciar músicas
          </Link>
        )}
      </div>

      <WorshipNotificationOptIn churchId={church.id} />

      {recommendationSong && (
        <div className="flex flex-col gap-2">
          <h3 className="font-display text-lg text-foreground/90">Recomendação do dia</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            <WorshipSongCard song={recommendationSong} position={1} churchName={church.name} churchSlug={slug} />
          </div>
        </div>
      )}

      <div className="flex flex-col gap-2">
        <h3 className="font-display text-lg text-foreground/90">
          {targetDate ? `Programação de ${formatDatePt(targetDate)}` : "Músicas publicadas"}
        </h3>
        {items.length === 0 ? (
          <EmptyState
            message={
              isAdminOfThisChurch
                ? "Adicione as músicas que serão tocadas ou cantadas nesta programação."
                : "Ainda não há músicas publicadas para esta programação."
            }
          />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
            {items.map((song, index) => (
              <WorshipSongCard key={song.id} song={song} position={index + 1} churchName={church.name} churchSlug={slug} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
