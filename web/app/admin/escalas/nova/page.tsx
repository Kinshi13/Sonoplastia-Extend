import Link from "next/link";
import { RotateCcw } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { OrganizationRole, OrganizationPerson, PersonTeamMembership, ScaleAssignment } from "@/lib/types/database";
import { ScaleForm } from "../ScaleForm";
import { recentPersonIdsForChurch } from "../recentPeople";

export default async function NovaEscalaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const [{ data: roles }, { data: people }, { data: memberships }, recentPersonIds] = await Promise.all([
    supabase.from("organization_roles").select("*").eq("church_id", churchId).eq("is_active", true).order("sort_order", { ascending: true }),
    supabase.from("organization_people").select("*").eq("church_id", churchId).eq("is_active", true).order("full_name", { ascending: true }),
    supabase.from("person_team_memberships").select("*"),
    recentPersonIdsForChurch(churchId!),
  ]);

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Nova escala</h1>
        <Link
          href="/admin/escalas/reutilizar"
          className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-primary hover:bg-primary-container/20 transition-colors"
        >
          <RotateCcw size={14} /> Reutilizar escala anterior
        </Link>
      </div>
      <ScaleForm
        existing={null}
        roles={(roles as OrganizationRole[]) ?? []}
        initialPeople={(people as OrganizationPerson[]) ?? []}
        memberships={(memberships as PersonTeamMembership[]) ?? []}
        existingAssignments={[] as ScaleAssignment[]}
        recentPersonIds={recentPersonIds}
      />
    </div>
  );
}
