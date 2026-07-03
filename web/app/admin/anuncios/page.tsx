import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { Announcement } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
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
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          Novo anúncio
        </Link>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex items-center justify-between gap-3 rounded-xl bg-surface p-4 border border-divider"
          >
            <div>
              <p className="font-medium">
                {item.title} {!item.is_active && <span className="text-xs text-text-secondary">(inativo)</span>}
              </p>
              <p className="text-sm text-text-secondary">{formatPublishedAt(item.published_at)}</p>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              <Link href={`/admin/anuncios/${item.id}`} className="text-sm text-primary">
                Editar
              </Link>
              <DeleteButton id={item.id} action={deleteAnnouncementAction} />
            </div>
          </div>
        ))}
        {items.length === 0 && <p className="text-text-secondary">Nenhum anúncio cadastrado.</p>}
      </div>
    </div>
  );
}
