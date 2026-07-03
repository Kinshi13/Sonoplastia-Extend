import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { Scale } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { DeleteButton } from "../DeleteButton";
import { deleteScaleAction } from "../actions";

export const revalidate = 0;

export default async function AdminEscalasPage() {
  const supabase = await createClient();
  const { data } = await supabase.from("scales").select("*").order("date", { ascending: true });
  const items = (data as Scale[]) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Escalas</h1>
        <Link
          href="/admin/escalas/nova"
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          Nova escala
        </Link>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((scale) => (
          <div
            key={scale.id}
            className="flex items-center justify-between gap-3 rounded-xl bg-surface p-4 border border-divider"
          >
            <div>
              <p className="font-medium">{scale.title}</p>
              <p className="text-sm text-text-secondary capitalize">
                {formatDatePt(scale.date)} às {formatTimePt(scale.start_time)}
              </p>
            </div>
            <div className="flex items-center gap-3 shrink-0">
              <Link href={`/admin/escalas/${scale.id}`} className="text-sm text-primary">
                Editar
              </Link>
              <DeleteButton id={scale.id} action={deleteScaleAction} />
            </div>
          </div>
        ))}
        {items.length === 0 && <p className="text-text-secondary">Nenhuma escala cadastrada.</p>}
      </div>
    </div>
  );
}
