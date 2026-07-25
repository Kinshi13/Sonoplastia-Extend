import Link from "next/link";
import { Plus } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Announcement } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { AnnouncementShareButton } from "@/components/AnnouncementShareButton";
import { DeleteButton } from "../DeleteButton";
import { deleteAnnouncementAction } from "../actions";
import { PublishToggleButton } from "./PublishToggleButton";

export const revalidate = 0;

export default async function AdminAnunciosPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const [{ data }, { data: church }] = await Promise.all([
    supabase
      .from("announcements")
      .select("*")
      .eq("church_id", churchId)
      .order("published_at", { ascending: false }),
    supabase.from("churches").select("name, slug").eq("id", churchId).single(),
  ]);
  const items = (data as Announcement[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Anúncios</h1>
        <Link
          href="/admin/anuncios/nova"
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Novo anúncio
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhum anúncio cadastrado." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {items.map((item) => (
            <Card
              key={item.id}
              className="p-5 flex flex-col gap-3 hover:shadow-md hover:-translate-y-0.5"
            >
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-semibold leading-tight">{item.title}</p>
                  {!item.is_active && (
                    <span className="shrink-0 rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-medium text-amber-600">
                      Rascunho
                    </span>
                  )}
                </div>
                <p className="mt-1 text-sm text-text-secondary">
                  {formatPublishedAt(item.published_at)}
                </p>
              </div>
              <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-2 border-t border-divider pt-3">
                <Link href={`/admin/anuncios/${item.id}`} className="text-sm font-medium text-primary">
                  Editar
                </Link>
                {church && item.is_active && (
                  <AnnouncementShareButton announcement={item} churchName={church.name} churchSlug={church.slug} />
                )}
                <PublishToggleButton id={item.id} isActive={item.is_active} />
                <DeleteButton id={item.id} action={deleteAnnouncementAction} />
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
