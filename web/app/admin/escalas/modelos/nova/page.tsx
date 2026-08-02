import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { OrganizationRole } from "@/lib/types/database";
import { ScaleTemplateForm } from "../../ScaleTemplateForm";
import { ensureDefaultRolesAction } from "../../../pessoas/ensureDefaultRoles";

export default async function NovoModeloPage() {
  const { churchId } = await getAdminStatus();
  await ensureDefaultRolesAction(churchId!);
  const supabase = await createClient();
  const { data: roles } = await supabase
    .from("organization_roles")
    .select("*")
    .eq("church_id", churchId)
    .eq("is_active", true)
    .order("sort_order", { ascending: true });

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Novo modelo</h1>
      <ScaleTemplateForm existing={null} churchId={churchId!} roles={(roles as OrganizationRole[]) ?? []} />
    </div>
  );
}
