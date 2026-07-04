import Link from "next/link";
import { Plus, FileText } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Bulletin } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../DeleteButton";
import { deleteBulletinAction } from "../actions";

export const revalidate = 0;

export default async function AdminBoletinsPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("bulletins")
    .select("*")
    .eq("church_id", churchId)
    .order("published_at", { ascending: false });
  const items = (data as Bulletin[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Boletins</h1>
        <Link
          href="/admin/boletins/nova"
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Novo boletim
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhum boletim publicado ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <Card key={item.id} className="overflow-hidden flex flex-col hover:shadow-md hover:-translate-y-0.5">
              <div className="relative aspect-4/3 w-full bg-background">
                {item.cover_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={item.cover_url} alt={item.title} className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-text-secondary">
                    <FileText size={32} />
                  </div>
                )}
              </div>
              <div className="p-4 flex flex-col gap-3">
                <p className="font-medium leading-tight truncate">{item.title}</p>
                <div className="mt-auto flex items-center justify-between gap-3 border-t border-divider pt-3">
                  <Link href={`/admin/boletins/${item.id}`} className="text-sm font-medium text-primary">
                    Editar
                  </Link>
                  <DeleteButton id={item.id} action={deleteBulletinAction} />
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
