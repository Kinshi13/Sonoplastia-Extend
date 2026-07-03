import Link from "next/link";
import { Calendar, Plus } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
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
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Nova doxologia
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma doxologia cadastrada." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <Card
              key={item.id}
              className="p-5 flex flex-col gap-3 hover:shadow-md hover:-translate-y-0.5"
            >
              <div>
                <p className="font-semibold leading-tight">{item.title}</p>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
                  <Calendar size={14} className="shrink-0" />
                  {formatDatePt(item.date)} às {formatTimePt(item.start_time)}
                </p>
              </div>
              <div className="mt-auto flex items-center justify-between gap-3 border-t border-divider pt-3">
                <Link href={`/admin/doxologia/${item.id}`} className="text-sm font-medium text-primary">
                  Editar
                </Link>
                <DeleteButton id={item.id} action={deleteDoxologyAction} />
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
