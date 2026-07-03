import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { Scale } from "@/lib/types/database";
import { ScaleForm } from "../ScaleForm";

export default async function EditarEscalaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const supabase = await createClient();
  const { data } = await supabase.from("scales").select("*").eq("id", id).single();

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar escala</h1>
      <ScaleForm existing={data as Scale} />
    </div>
  );
}
