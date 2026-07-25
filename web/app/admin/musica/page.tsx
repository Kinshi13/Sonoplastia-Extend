import Link from "next/link";
import { Plus, Music2 } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { WorshipSong } from "@/lib/types/database";
import { formatDatePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../DeleteButton";
import { deleteWorshipSongAction } from "../actions";
import { PublishToggleButton } from "./PublishToggleButton";
import { ReorderButtons } from "./ReorderButtons";

export const revalidate = 0;

export default async function AdminMusicaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("worship_songs")
    .select("*")
    .eq("church_id", churchId)
    .order("program_date", { ascending: false, nullsFirst: false })
    .order("order_index", { ascending: true });
  const items = (data as WorshipSong[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Música e Louvor</h1>
        <Link
          href="/admin/musica/nova"
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Nova música
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma música cadastrada. Adicione as músicas que serão tocadas ou cantadas na programação." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {items.map((item) => (
            <Card key={item.id} className="overflow-hidden flex flex-col hover:shadow-md hover:-translate-y-0.5">
              <div className="relative aspect-video w-full bg-background">
                {item.thumbnail_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={item.thumbnail_url}
                    alt={`Thumbnail da música ${item.title}`}
                    loading="lazy"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-text-secondary">
                    <Music2 size={24} />
                  </div>
                )}
              </div>
              <div className="p-4 flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  <p className="font-medium leading-tight truncate flex-1">{item.title}</p>
                  {!item.is_published && (
                    <span className="shrink-0 rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-medium text-amber-600">
                      Rascunho
                    </span>
                  )}
                </div>
                <p className="text-xs text-text-secondary">
                  {item.artist || "Artista/canal não informado"}
                  {item.moment_label && ` · ${item.moment_label}`}
                </p>
                {item.program_date && (
                  <p className="text-xs text-text-secondary">{formatDatePt(item.program_date)}</p>
                )}
                <div className="mt-1 flex items-center gap-2">
                  <ReorderButtons id={item.id} />
                  <span className="text-xs text-text-secondary">Ordem {item.order_index}</span>
                </div>
                <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-2 border-t border-divider pt-3">
                  <Link href={`/admin/musica/${item.id}`} className="text-sm font-medium text-primary">
                    Editar
                  </Link>
                  <PublishToggleButton id={item.id} isPublished={item.is_published} />
                  <DeleteButton id={item.id} action={deleteWorshipSongAction} />
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
