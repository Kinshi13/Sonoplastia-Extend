"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { requireAdmin, ActionResult } from "../actions";
import { LEGACY_ROLE_DEFS } from "@/lib/legacyRoles";

const MISSING_SCHEMA_CODES = new Set(["PGRST205", "42P01", "42703"]);
const PEOPLE_UNAVAILABLE_MESSAGE =
  "O banco de pessoas ainda não está disponível nesta igreja (aguardando uma atualização do sistema) - por enquanto, use \"Nome manual\" para preencher a função.";

/**
 * Hotfix: self-heals a church that has zero `organization_roles` rows - either because migrations
 * 006-010 haven't run yet (this silently no-ops, since `organization_roles` doesn't exist to
 * insert into - the client-side fallback in lib/legacyRoles.ts covers the form in the meantime),
 * or because it's a church created *after* those migrations ran (010's own seed only covers
 * churches that existed at the time it ran, so anything newer needs this). Never duplicates - the
 * `not exists` guard mirrors the one in 010_organization_roles_people.sql exactly.
 */
export async function ensureDefaultRolesAction(churchId: string): Promise<void> {
  const supabase = await createClient();
  const { data: existing, error } = await supabase.from("organization_roles").select("legacy_field_key").eq("church_id", churchId);
  if (error) return; // table doesn't exist yet - nothing to seed into.

  const covered = new Set((existing ?? []).map((r) => r.legacy_field_key));
  const missing = LEGACY_ROLE_DEFS.filter((d) => !covered.has(d.field));
  if (missing.length === 0) return;

  const now = Date.now();
  await supabase.from("organization_roles").insert(
    missing.map((d) => ({
      church_id: churchId,
      name: d.name,
      icon_key: d.iconKey,
      sort_order: d.sortOrder,
      is_required: true,
      legacy_field_key: d.field,
      created_at: now,
      updated_at: now,
    }))
  );
}

// Roles --------------------------------------------------------------------

export async function saveRoleAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const name = String(formData.get("name") ?? "").trim();
  if (!name) return { error: "Informe o nome da função." };

  const payload = {
    name,
    short_name: formData.get("short_name") ? String(formData.get("short_name")) : null,
    description: String(formData.get("description") ?? ""),
    icon_key: String(formData.get("icon_key") ?? "users"),
    allows_multiple_people: formData.get("allows_multiple_people") === "on",
    team_id: formData.get("team_id") ? String(formData.get("team_id")) : null,
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("organization_roles").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { data: existing } = await supabase
      .from("organization_roles")
      .select("sort_order")
      .eq("church_id", admin.churchId)
      .order("sort_order", { ascending: false })
      .limit(1);
    const nextSort = (existing?.[0]?.sort_order ?? -1) + 1;
    const { error } = await supabase.from("organization_roles").insert({
      ...payload,
      church_id: admin.churchId,
      sort_order: nextSort,
      created_at: Date.now(),
    });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/pessoas");
  revalidatePath("/admin/escalas");
  return {};
}

export async function setRoleActiveAction(id: string, isActive: boolean): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  // Arquivar (Parte 2): desativar, nunca apagar quando há histórico - a exclusão real fica só
  // para uma função recém-criada sem nenhuma escala usando ela (deleteRoleAction abaixo).
  const { error } = await supabase
    .from("organization_roles")
    .update({ is_active: isActive, updated_at: Date.now() })
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}

export async function deleteRoleAction(id: string): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const { count } = await supabase
    .from("scale_assignments")
    .select("id", { count: "exact", head: true })
    .eq("role_id", id);
  if (count && count > 0) {
    return { error: "Esta função já foi usada em escalas - arquive em vez de excluir para preservar o histórico." };
  }

  const { error } = await supabase.from("organization_roles").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}

// People ---------------------------------------------------------------------

export async function savePersonAction(id: string | null, formData: FormData): Promise<ActionResult & { id?: string }> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const fullName = String(formData.get("full_name") ?? "").trim();
  if (!fullName) return { error: "Informe o nome completo." };

  const payload = {
    full_name: fullName,
    display_name: formData.get("display_name") ? String(formData.get("display_name")) : null,
    email: formData.get("email") ? String(formData.get("email")) : null,
    phone: formData.get("phone") ? String(formData.get("phone")) : null,
    notes: String(formData.get("notes") ?? ""),
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("organization_people").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: MISSING_SCHEMA_CODES.has(error.code) ? PEOPLE_UNAVAILABLE_MESSAGE : error.message };
    await syncTeamMemberships(id, formData);
    revalidatePath("/admin/pessoas");
    return { id };
  }

  const { data: inserted, error } = await supabase
    .from("organization_people")
    .insert({ ...payload, church_id: admin.churchId, created_at: Date.now() })
    .select("id")
    .single();
  // Hotfix: organization_people doesn't exist yet on a database where migrations 006-010 haven't
  // been applied - a clear, actionable message here instead of a raw Postgres error, and pointing
  // at "Nome manual" (which works today, with zero dependency on this table) as the real fallback.
  if (error) return { error: MISSING_SCHEMA_CODES.has(error.code) ? PEOPLE_UNAVAILABLE_MESSAGE : error.message };
  await syncTeamMemberships(inserted.id, formData);
  revalidatePath("/admin/pessoas");
  return { id: inserted.id };
}

async function syncTeamMemberships(personId: string, formData: FormData) {
  const supabase = await createClient();
  const teamIds = formData.getAll("team_ids").map(String).filter(Boolean);
  await supabase.from("person_team_memberships").delete().eq("person_id", personId);
  if (teamIds.length > 0) {
    await supabase.from("person_team_memberships").insert(
      teamIds.map((teamId) => ({ person_id: personId, team_id: teamId, is_primary: false, created_at: Date.now() }))
    );
  }
}

export async function setPersonFavoriteAction(id: string, isFavorite: boolean): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  const { error } = await supabase
    .from("organization_people")
    .update({ is_favorite: isFavorite })
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}

export async function setPersonActiveAction(id: string, isActive: boolean): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  const { error } = await supabase
    .from("organization_people")
    .update({ is_active: isActive, updated_at: Date.now() })
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}

export async function deletePersonAction(id: string): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const { count } = await supabase
    .from("scale_assignments")
    .select("id", { count: "exact", head: true })
    .eq("person_id", id);
  if (count && count > 0) {
    return { error: "Esta pessoa já foi escalada antes - desative em vez de excluir para preservar o histórico." };
  }

  const { error } = await supabase.from("organization_people").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}

// Teams ------------------------------------------------------------------

export async function saveTeamAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const name = String(formData.get("name") ?? "").trim();
  if (!name) return { error: "Informe o nome da equipe." };

  const payload = {
    name,
    description: String(formData.get("description") ?? ""),
    icon_key: String(formData.get("icon_key") ?? "users"),
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("organization_teams").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase.from("organization_teams").insert({ ...payload, church_id: admin.churchId, created_at: Date.now() });
    if (error) return { error: error.message };
  }
  revalidatePath("/admin/pessoas");
  return {};
}

export async function deleteTeamAction(id: string): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  const { error } = await supabase.from("organization_teams").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/pessoas");
  return {};
}
