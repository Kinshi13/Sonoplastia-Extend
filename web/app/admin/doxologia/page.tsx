import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { DeleteButton } from "../DeleteButton";
import { deleteDoxologyAction } from "../actions";

export const revalidate = 0;

export default async function AdminDoxologiaPage() {
  const supabase = await createClient();
  const { data } = await supabase.from("doxologies").select("*").order("date", { ascending: true });
  const items = (data as Doxology[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Doxologia</h1>
        <Link
          href="/admin/doxologia/nova"
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          Nova doxologia
        </Link>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex items-center justify-between gap-3 rounded-xl bg-surface p-4 border border-divider"
          >
            <div>
              <p className="font-medium">{item.title}</p>
              <p className="text-sm text-text-secondary capitalize">
                {formatDatePt(item.date)} às {formatTimePt(item.start_time)}
              </p>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              <Link href={`/admin/doxologia/${item.id}`} className="text-sm text-primary">
                Editar
              </Link>
              <DeleteButton id={item.id} action={deleteDoxologyAction} />
            </div>
          </div>
        ))}
        {items.length === 0 && <p className="text-text-secondary">Nenhuma doxologia cadastrada.</p>}
      </div>
    </div>
  );
}
