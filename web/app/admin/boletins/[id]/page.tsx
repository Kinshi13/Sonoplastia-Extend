import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Announcement, Bulletin } from "@/lib/types/database";
import { BulletinForm } from "../BulletinForm";

export default async function EditarBoletimPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const [{ data }, { data: announcements }] = await Promise.all([
    supabase.from("bulletins").select("*").eq("id", id).eq("church_id", churchId).single(),
    supabase.from("announcements").select("*").eq("church_id", churchId).order("published_at", { ascending: false }),
  ]);

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar boletim</h1>
      <BulletinForm existing={data as Bulletin} announcements={(announcements as Announcement[]) ?? []} />
    </div>
  );
}
