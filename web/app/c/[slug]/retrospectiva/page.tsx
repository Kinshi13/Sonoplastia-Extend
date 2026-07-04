import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { RetrospectiveItem } from "@/lib/types/database";
import { EmptyState } from "@/components/EmptyState";
import { RetrospectivaGrid } from "@/components/RetrospectivaGrid";

export const revalidate = 0;

export default async function RetrospectivaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const { data } = await supabase
    .from("retrospective_items")
    .select("*")
    .eq("church_id", church.id)
    .eq("is_active", true)
    .order("published_at", { ascending: false });

  const items = (data as RetrospectiveItem[]) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Retrospectiva</h2>
        <p className="mt-1 text-sm text-text-secondary">Fotos e vídeos dos últimos cultos e eventos.</p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma foto ou vídeo publicado ainda." />
      ) : (
        <RetrospectivaGrid items={items} />
      )}
    </div>
  );
}
