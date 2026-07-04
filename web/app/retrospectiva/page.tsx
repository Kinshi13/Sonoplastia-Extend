import { createClient } from "@/lib/supabase/server";
import { RetrospectiveItem } from "@/lib/types/database";
import { EmptyState } from "@/components/EmptyState";
import { RetrospectivaGrid } from "@/components/RetrospectivaGrid";

export const revalidate = 0;

export default async function RetrospectivaPage() {
  const supabase = await createClient();
  const { data } = await supabase
    .from("retrospective_items")
    .select("*")
    .eq("is_active", true)
    .order("published_at", { ascending: false });

  const items = (data as RetrospectiveItem[]) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Retrospectiva</h1>
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
