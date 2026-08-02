import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { OrganizationRole, ScaleTemplate } from "@/lib/types/database";
import { ScaleTemplateForm } from "../../ScaleTemplateForm";
import { ensureDefaultRolesAction } from "../../../pessoas/ensureDefaultRoles";

export default async function EditarModeloPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  await ensureDefaultRolesAction(churchId!);
  const supabase = await createClient();
  const [{ data }, { data: roles }] = await Promise.all([
    supabase.from("scale_templates").select("*").eq("id", id).eq("church_id", churchId).single(),
    supabase.from("organization_roles").select("*").eq("church_id", churchId).eq("is_active", true).order("sort_order", { ascending: true }),
  ]);

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar modelo</h1>
      <ScaleTemplateForm existing={data as ScaleTemplate} churchId={churchId!} roles={(roles as OrganizationRole[]) ?? []} />
    </div>
  );
}
