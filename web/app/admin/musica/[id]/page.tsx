import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { WorshipSong } from "@/lib/types/database";
import { MusicaForm } from "../MusicaForm";

export default async function EditarMusicaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("worship_songs")
    .select("*")
    .eq("id", id)
    .eq("church_id", churchId)
    .single();

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar música</h1>
      <MusicaForm existing={data as WorshipSong} />
    </div>
  );
}
