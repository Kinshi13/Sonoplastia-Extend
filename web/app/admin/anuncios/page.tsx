import Link from "next/link";
import { Plus } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { Announcement } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../DeleteButton";
import { deleteAnnouncementAction } from "../actions";

export const revalidate = 0;

export default async function AdminAnunciosPage() {
  const supabase = await createClient();
  const { data } = await supabase
    .from("announcements")
    .select("*")
    .order("published_at", { ascending: false });
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
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <Card
              key={item.id}
              className="p-5 flex flex-col gap-3 hover:shadow-md hover:-translate-y-0.5"
            >
              <div>
                <p className="font-semibold leading-tight">
                  {item.title}{" "}
                  {!item.is_active && (
                    <span className="text-xs font-normal text-text-secondary">(inativo)</span>
                  )}
                </p>
                <p className="mt-1 text-sm text-text-secondary">
                  {formatPublishedAt(item.published_at)}
                </p>
              </div>
              <div className="mt-auto flex items-center justify-between gap-3 border-t border-divider pt-3">
                <Link href={`/admin/anuncios/${item.id}`} className="text-sm font-medium text-primary">
                  Editar
                </Link>
                <DeleteButton id={item.id} action={deleteAnnouncementAction} />
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
