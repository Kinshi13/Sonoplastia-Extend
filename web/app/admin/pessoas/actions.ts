"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { requireAdmin, ActionResult } from "../actions";

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
    if (error) return { error: error.message };
    await syncTeamMemberships(id, formData);
    revalidatePath("/admin/pessoas");
    return { id };
  }

  const { data: inserted, error } = await supabase
    .from("organization_people")
    .insert({ ...payload, church_id: admin.churchId, created_at: Date.now() })
    .select("id")
    .single();
  if (error) return { error: error.message };
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
