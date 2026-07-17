import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Scale, OrganizationRole, OrganizationPerson, PersonTeamMembership, ScaleAssignment } from "@/lib/types/database";
import { ScaleForm } from "../ScaleForm";
import { recentPersonIdsForChurch } from "../recentPeople";
import { ensureDefaultRolesAction } from "../../pessoas/actions";

export default async function EditarEscalaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  await ensureDefaultRolesAction(churchId!);
  const supabase = await createClient();
  const [{ data }, { data: roles }, { data: people }, { data: memberships }, { data: assignments }, recentPersonIds] = await Promise.all([
    supabase.from("scales").select("*").eq("id", id).eq("church_id", churchId).single(),
    supabase.from("organization_roles").select("*").eq("church_id", churchId).eq("is_active", true).order("sort_order", { ascending: true }),
    supabase.from("organization_people").select("*").eq("church_id", churchId).eq("is_active", true).order("full_name", { ascending: true }),
    supabase.from("person_team_memberships").select("*"),
    supabase.from("scale_assignments").select("*").eq("scale_id", id).order("position", { ascending: true }),
    recentPersonIdsForChurch(churchId!),
  ]);

  if (!data) notFound();

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Editar escala</h1>
      <ScaleForm
        existing={data as Scale}
        churchId={churchId!}
        roles={(roles as OrganizationRole[]) ?? []}
        initialPeople={(people as OrganizationPerson[]) ?? []}
        memberships={(memberships as PersonTeamMembership[]) ?? []}
        existingAssignments={(assignments as ScaleAssignment[]) ?? []}
        recentPersonIds={recentPersonIds}
      />
    </div>
  );
}
