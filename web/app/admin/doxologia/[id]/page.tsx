import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Doxology } from "@/lib/types/database";
import { DoxologyForm } from "../DoxologyForm";

export default async function EditarDoxologiaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("doxologies")
    .select("*")
    .eq("id", id)
    .eq("church_id", churchId)
    .single();

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar doxologia</h1>
      <DoxologyForm existing={data as Doxology} />
    </div>
  );
}
