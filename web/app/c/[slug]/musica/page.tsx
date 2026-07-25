import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { WorshipSong } from "@/lib/types/database";
import { formatDatePt } from "@/lib/format";
import { EmptyState } from "@/components/EmptyState";
import { AnunciosRetrospectivaTabs } from "@/components/AnunciosRetrospectivaTabs";
import { WorshipSongCard } from "@/components/WorshipSongCard";

export const revalidate = 0;

/**
 * Música e Louvor (público) - prioriza a lista da próxima programação (Bloco 11: "priorizar
 * músicas da próxima programação... músicas da escala vigente"). Se a próxima escala não tiver
 * músicas cadastradas ainda, cai para a data publicada mais recente em vez de mostrar uma tela
 * vazia quando na verdade existe conteúdo (só não é o do dia mais próximo).
 */
export default async function MusicaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);

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

  const items = (data as WorshipSong[]) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <AnunciosRetrospectivaTabs slug={slug} active="musica" />

      <div>
        <h2 className="text-3xl font-bold tracking-tight">Música e Louvor</h2>
        <p className="mt-1 text-sm text-text-secondary">
          {targetDate ? `Programação de ${formatDatePt(targetDate)}` : "Músicas da programação da igreja."}
        </p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Ainda não há músicas cadastradas para esta programação." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
          {items.map((song, index) => (
            <WorshipSongCard key={song.id} song={song} position={index + 1} churchName={church.name} churchSlug={slug} />
          ))}
        </div>
      )}
    </div>
  );
}
