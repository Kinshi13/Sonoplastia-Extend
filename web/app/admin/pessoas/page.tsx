import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { OrganizationPerson, OrganizationRole, OrganizationTeam, PersonTeamMembership } from "@/lib/types/database";
import { PeopleTeamsManager } from "./PeopleTeamsManager";

export const revalidate = 0;

export default async function PessoasPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();

  const [{ data: people }, { data: teams }, { data: roles }, { data: memberships }] = await Promise.all([
    supabase.from("organization_people").select("*").eq("church_id", churchId).order("full_name", { ascending: true }),
    supabase.from("organization_teams").select("*").eq("church_id", churchId).order("sort_order", { ascending: true }),
    supabase.from("organization_roles").select("*").eq("church_id", churchId).order("sort_order", { ascending: true }),
    supabase.from("person_team_memberships").select("*"),
  ]);

  return (
    <div>
      <h1 className="font-display text-2xl mb-1">Pessoas e Equipes</h1>
      <p className="text-sm text-text-secondary mb-6">
        Cadastre as pessoas da organização, agrupe-as em equipes e configure quais funções existem
        nas suas escalas.
      </p>
      <PeopleTeamsManager
        people={(people as OrganizationPerson[]) ?? []}
        teams={(teams as OrganizationTeam[]) ?? []}
        roles={(roles as OrganizationRole[]) ?? []}
        memberships={(memberships as PersonTeamMembership[]) ?? []}
      />
    </div>
  );
}
