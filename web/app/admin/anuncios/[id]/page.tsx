import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { Announcement } from "@/lib/types/database";
import { AnnouncementForm } from "../AnnouncementForm";

export default async function EditarAnuncioPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const supabase = await createClient();
  const { data } = await supabase.from("announcements").select("*").eq("id", id).single();

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar anúncio</h1>
      <AnnouncementForm existing={data as Announcement} />
    </div>
  );
}
