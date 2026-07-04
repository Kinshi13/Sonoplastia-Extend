import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { RetrospectiveItem } from "@/lib/types/database";
import { RetrospectivaForm } from "../RetrospectivaForm";

export default async function EditarRetrospectivaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const supabase = await createClient();
  const { data } = await supabase.from("retrospective_items").select("*").eq("id", id).single();

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar publicação</h1>
      <RetrospectivaForm existing={data as RetrospectiveItem} />
    </div>
  );
}
