import Link from "next/link";
import { Plus, Video } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { RetrospectiveItem } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../DeleteButton";
import { deleteRetrospectiveItemAction } from "../actions";

export const revalidate = 0;

export default async function AdminRetrospectivaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("retrospective_items")
    .select("*")
    .eq("church_id", churchId)
    .order("published_at", { ascending: false });
  const items = (data as RetrospectiveItem[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Retrospectiva</h1>
        <Link
          href="/admin/retrospectiva/nova"
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Nova publicação
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma foto ou vídeo publicado ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <Card key={item.id} className="overflow-hidden flex flex-col hover:shadow-md hover:-translate-y-0.5">
              <div className="relative aspect-4/3 w-full bg-background">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={item.media_type === "VIDEO" ? item.poster_url ?? item.media_url : item.media_url}
                  alt={item.title || "Retrospectiva"}
                  className="h-full w-full object-cover"
                />
                {item.media_type === "VIDEO" && (
                  <span className="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-black/50 text-white">
                    <Video size={14} />
                  </span>
                )}
              </div>
              <div className="p-4 flex flex-col gap-3">
                <p className="font-medium leading-tight truncate">{item.title || "(sem título)"}</p>
                <div className="mt-auto flex items-center justify-between gap-3 border-t border-divider pt-3">
                  <Link href={`/admin/retrospectiva/${item.id}`} className="text-sm font-medium text-primary">
                    Editar
                  </Link>
                  <DeleteButton id={item.id} action={deleteRetrospectiveItemAction} />
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
