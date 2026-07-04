import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Announcement } from "@/lib/types/database";
import { BulletinForm } from "../BulletinForm";

export default async function NovoBoletimPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("announcements")
    .select("*")
    .eq("church_id", churchId)
    .order("published_at", { ascending: false });

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Novo boletim</h1>
      <BulletinForm existing={null} announcements={(data as Announcement[]) ?? []} />
    </div>
  );
}
