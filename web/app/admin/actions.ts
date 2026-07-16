"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { ProgramStep } from "@/lib/types/database";

export type ActionResult = { error?: string };

export async function signOutAction() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}

/** Resolves the current admin's church (id + slug) or an error string - RLS is the real
 *  enforcement, this just avoids a raw Postgres error reaching the form and gives us the slug
 *  to revalidate the right public path after a write. Exported so app/admin/pessoas/actions.ts
 *  can reuse the same check instead of duplicating it. */
export async function requireAdmin(): Promise<{ error: string; churchId?: never; churchSlug?: never } | { error?: never; churchId: string; churchSlug: string }> {
  const { isAdmin, churchId } = await getAdminStatus();
  if (!isAdmin || !churchId) return { error: "Apenas administradores podem fazer essa alteração." };

  const supabase = await createClient();
  const { data: church } = await supabase.from("churches").select("slug").eq("id", churchId).single();
  if (!church) return { error: "Igreja não encontrada." };

  return { churchId, churchSlug: church.slug };
}

// Scales ---------------------------------------------------------------

export type ScaleAssignmentInput = {
  roleId: string;
  personId: string | null;
  customPersonName: string | null;
  position: number;
  notes: string;
};

/**
 * Fase 11.8.4 (Parte 3, 10): the five text inputs are gone from the form - `assignments` (JSON in
 * the `assignments` field) now drives who's on the schedule, resolved against whatever roles
 * exist for this church. The five legacy `scales` columns are still written on every save (best
 * effort, only for roles that carry a `legacy_field_key`) purely so nothing that reads them today
 * - Android, the public site, exports - has to change to keep working.
 */
export async function saveScaleAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  let assignments: ScaleAssignmentInput[];
  try {
    assignments = JSON.parse(String(formData.get("assignments") ?? "[]"));
  } catch {
    return { error: "Funções e pessoas inválidas." };
  }

  const roleIds = [...new Set(assignments.map((a) => a.roleId))];
  const personIds = [...new Set(assignments.map((a) => a.personId).filter((v): v is string => !!v))];

  const [{ data: roles }, { data: people }] = await Promise.all([
    roleIds.length
      ? supabase.from("organization_roles").select("id, name, legacy_field_key, allows_multiple_people").eq("church_id", admin.churchId).in("id", roleIds)
      : Promise.resolve({ data: [] as { id: string; name: string; legacy_field_key: string | null; allows_multiple_people: boolean }[] }),
    personIds.length
      ? supabase.from("organization_people").select("id, full_name, display_name").eq("church_id", admin.churchId).in("id", personIds)
      : Promise.resolve({ data: [] as { id: string; full_name: string; display_name: string | null }[] }),
  ]);
  const roleById = new Map((roles ?? []).map((r) => [r.id, r]));
  const personById = new Map((people ?? []).map((p) => [p.id, p]));

  const legacyByField: Record<string, string[]> = {};
  const resolvedAssignments = assignments.map((a) => {
    const role = roleById.get(a.roleId);
    const personName = a.personId ? personById.get(a.personId)?.display_name || personById.get(a.personId)?.full_name || "" : a.customPersonName || "";
    if (role?.legacy_field_key && personName) {
      (legacyByField[role.legacy_field_key] ??= []).push(personName);
    }
    return { ...a, roleName: role?.name ?? "Função", personName };
  });

  const legacyPayload = {
    reception_person: (legacyByField["reception_person"] ?? []).join(" e "),
    sound_person: (legacyByField["sound_person"] ?? []).join(" e "),
    preaching_person: (legacyByField["preaching_person"] ?? []).join(" e "),
    conducting_person: (legacyByField["conducting_person"] ?? []).join(" e "),
    musical_message_person: (legacyByField["musical_message_person"] ?? []).join(" e "),
  };

  const payload = {
    date: String(formData.get("date")),
    start_time: String(formData.get("start_time")),
    end_time: formData.get("end_time") ? String(formData.get("end_time")) : null,
    title: String(formData.get("title")),
    ...legacyPayload,
    notes: String(formData.get("notes") ?? ""),
    is_special_event: formData.get("is_special_event") === "on",
    source_type: "OFFICIAL",
    updated_at: Date.now(),
  };

  let scaleId = id;
  if (id) {
    const { error } = await supabase.from("scales").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: error.message };
    await supabase.from("scale_assignments").delete().eq("scale_id", id);
  } else {
    const { data: inserted, error } = await supabase
      .from("scales")
      .insert({ ...payload, church_id: admin.churchId, created_at: Date.now() })
      .select("id")
      .single();
    if (error) return { error: error.message };
    scaleId = inserted.id;
  }

  if (scaleId && resolvedAssignments.length > 0) {
    const now = Date.now();
    const { error: assignError } = await supabase.from("scale_assignments").insert(
      resolvedAssignments.map((a) => ({
        scale_id: scaleId,
        role_id: a.roleId,
        person_id: a.personId,
        custom_person_name: a.personId ? null : a.customPersonName,
        role_name_snapshot: a.roleName,
        person_name_snapshot: a.personName,
        position: a.position,
        notes: a.notes,
        created_at: now,
        updated_at: now,
      }))
    );
    if (assignError) return { error: assignError.message };
  }

  revalidatePath("/admin/escalas");
  revalidatePath(`/c/${admin.churchSlug}`);
  return {};
}

export async function deleteScaleAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase.from("scales").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/escalas");
  revalidatePath(`/c/${admin.churchSlug}`);
}

/**
 * Fase 11.8.4 (Parte 11): "Reutilizar estrutura" - the main mode requested. Copies title, type,
 * role list and order, and structural notes into a brand-new scale at a new date; people are
 * never copied (every assignment lands with `person_id`/`custom_person_name` both null, ready to
 * fill in). The source scale and its own assignments are never touched.
 */
export async function cloneScaleStructureAction(sourceId: string, formData: FormData): Promise<ActionResult & { newId?: string }> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const { data: source, error: sourceError } = await supabase
    .from("scales")
    .select("*")
    .eq("id", sourceId)
    .eq("church_id", admin.churchId)
    .single();
  if (sourceError || !source) return { error: "Escala original não encontrada." };

  const { data: sourceAssignments } = await supabase
    .from("scale_assignments")
    .select("role_id, role_name_snapshot, position")
    .eq("scale_id", sourceId)
    .order("position", { ascending: true });

  const newDate = String(formData.get("date") ?? "");
  const newTitle = String(formData.get("title") ?? "").trim() || source.title;
  const newStartTime = String(formData.get("start_time") ?? source.start_time?.slice(0, 5) ?? "");
  if (!newDate) return { error: "Escolha uma nova data para a escala reutilizada." };
  if (!newStartTime) return { error: "Informe o horário de início." };

  const now = Date.now();
  const { data: inserted, error: insertError } = await supabase
    .from("scales")
    .insert({
      church_id: admin.churchId,
      date: newDate,
      start_time: newStartTime,
      end_time: formData.get("end_time") ? String(formData.get("end_time")) : null,
      type: source.type,
      title: newTitle,
      reception_person: "",
      sound_person: "",
      preaching_person: "",
      conducting_person: "",
      musical_message_person: "",
      notes: source.notes,
      is_special_event: source.is_special_event,
      source_type: "OFFICIAL",
      created_at: now,
      updated_at: now,
    })
    .select("id")
    .single();
  if (insertError) return { error: insertError.message };

  if (sourceAssignments && sourceAssignments.length > 0) {
    const { error: assignError } = await supabase.from("scale_assignments").insert(
      sourceAssignments.map((a) => ({
        scale_id: inserted.id,
        role_id: a.role_id,
        person_id: null,
        custom_person_name: null,
        role_name_snapshot: a.role_name_snapshot,
        person_name_snapshot: "",
        position: a.position,
        notes: "",
        created_at: now,
        updated_at: now,
      }))
    );
    if (assignError) return { error: assignError.message };
  }

  revalidatePath("/admin/escalas");
  revalidatePath(`/c/${admin.churchSlug}`);
  return { newId: inserted.id };
}

// Doxologies -------------------------------------------------------------

export async function saveDoxologyAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const stepsRaw = String(formData.get("program_order") ?? "[]");
  let programOrder;
  try {
    programOrder = JSON.parse(stepsRaw);
  } catch {
    return { error: "Ordem do culto inválida." };
  }

  const payload = {
    date: String(formData.get("date")),
    start_time: String(formData.get("start_time")),
    end_time: formData.get("end_time") ? String(formData.get("end_time")) : null,
    title: String(formData.get("title")),
    notes: String(formData.get("notes") ?? ""),
    program_order: programOrder,
    source_type: "OFFICIAL",
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase
      .from("doxologies")
      .update(payload)
      .eq("id", id)
      .eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("doxologies")
      .insert({ ...payload, church_id: admin.churchId, created_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/doxologia");
  revalidatePath(`/c/${admin.churchSlug}/doxologia`);
  return {};
}

export async function deleteDoxologyAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase.from("doxologies").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/doxologia");
  revalidatePath(`/c/${admin.churchSlug}/doxologia`);
}

export async function toggleDoxologyFavoriteAction(id: string, isFavorite: boolean): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  const { error } = await supabase
    .from("doxologies")
    .update({ is_favorite: isFavorite })
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/doxologia");
  revalidatePath("/admin/doxologia/reutilizar");
  return {};
}

export type DoxologyCopyMode = "ALL" | "STRUCTURE" | "SELECTED";

/**
 * Fase 11.8.3 (Bloco N-R): clones an existing Doxologia into a brand-new row - the source is
 * never modified except for its own `times_reused` counter. `selectedStepIndexes` only matters
 * for copyMode "SELECTED"; for the other two modes every step is copied (structure-only mode
 * still keeps every step, it just drops responsible_person/notes per Bloco Q).
 */
export async function cloneDoxologyAction(
  sourceId: string,
  formData: FormData
): Promise<ActionResult & { newId?: string }> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const { data: source, error: sourceError } = await supabase
    .from("doxologies")
    .select("*")
    .eq("id", sourceId)
    .eq("church_id", admin.churchId)
    .single();
  if (sourceError || !source) return { error: "Programação original não encontrada." };

  const copyMode = String(formData.get("copy_mode") ?? "ALL") as DoxologyCopyMode;
  const newDate = String(formData.get("date") ?? "");
  const newTitle = String(formData.get("title") ?? "").trim();
  const newStartTime = String(formData.get("start_time") ?? "");
  if (!newDate) return { error: "Escolha uma nova data para a programação reutilizada." };
  if (!newTitle) return { error: "Informe um título." };
  if (!newStartTime) return { error: "Informe o horário de início." };

  const selectedIndexes: number[] | null =
    copyMode === "SELECTED"
      ? String(formData.get("selected_steps") ?? "")
          .split(",")
          .filter((v) => v.length > 0)
          .map(Number)
      : null;

  const sourceSteps: ProgramStep[] = source.program_order ?? [];
  const keptSteps = selectedIndexes ? sourceSteps.filter((_, i) => selectedIndexes.includes(i)) : sourceSteps;
  if (keptSteps.length === 0) return { error: "Selecione pelo menos uma etapa para reutilizar." };

  const newSteps: ProgramStep[] =
    copyMode === "STRUCTURE"
      ? keptSteps.map((step, i) => ({
          order: i + 1,
          title: step.title,
          description: undefined,
          responsible_person: undefined,
          estimated_duration_minutes: step.estimated_duration_minutes ?? null,
        }))
      : keptSteps.map((step, i) => ({ ...step, order: i + 1 }));

  const now = Date.now();
  const { data: inserted, error: insertError } = await supabase
    .from("doxologies")
    .insert({
      church_id: admin.churchId,
      date: newDate,
      start_time: newStartTime,
      end_time: formData.get("end_time") ? String(formData.get("end_time")) : null,
      title: newTitle,
      notes: copyMode === "ALL" ? source.notes : "",
      program_order: newSteps,
      source_type: "OFFICIAL",
      reused_from_doxology_id: sourceId,
      created_at: now,
      updated_at: now,
    })
    .select("id")
    .single();
  if (insertError) return { error: insertError.message };

  // Best-effort counter on the source - never blocks the clone from succeeding if it fails.
  await supabase
    .from("doxologies")
    .update({ times_reused: (source.times_reused ?? 0) + 1 })
    .eq("id", sourceId);

  revalidatePath("/admin/doxologia");
  revalidatePath(`/c/${admin.churchSlug}/doxologia`);
  return { newId: inserted?.id };
}

// Announcements ------------------------------------------------------------

// Media is uploaded directly from the browser to Supabase Storage (see AnnouncementForm) -
// Vercel Server Actions cap request bodies at a few MB, far below a typical photo/video, so
// this action only ever receives the resulting URL, never the file itself.
export async function saveAnnouncementAction(
  id: string | null,
  formData: FormData
): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const mediaType = String(formData.get("media_type") ?? "NONE");
  const mediaUrl = formData.get("media_url") ? String(formData.get("media_url")) : null;
  const mediaFileName = formData.get("media_file_name") ? String(formData.get("media_file_name")) : null;

  const payload = {
    title: String(formData.get("title")),
    description: String(formData.get("description") ?? ""),
    media_type: mediaType === "NONE" ? "NONE" : mediaUrl ? mediaType : "NONE",
    media_url: mediaUrl,
    media_file_name: mediaFileName,
    related_event_date: formData.get("related_event_date")
      ? String(formData.get("related_event_date"))
      : null,
    source_type: "OFFICIAL",
    is_pinned: formData.get("is_pinned") === "on",
    is_active: true,
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase
      .from("announcements")
      .update(payload)
      .eq("id", id)
      .eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("announcements")
      .insert({ ...payload, church_id: admin.churchId, published_at: Date.now(), updated_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/anuncios");
  revalidatePath(`/c/${admin.churchSlug}/anuncios`);
  return {};
}

export async function deleteAnnouncementAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase.from("announcements").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/anuncios");
  revalidatePath(`/c/${admin.churchSlug}/anuncios`);
}

// Retrospective (photo/video feed) -----------------------------------------

// Large photo/video files are uploaded directly from the browser to Supabase Storage
// (see RetrospectivaForm) - Vercel Serverless/Server Actions cap request bodies at a few MB,
// far below a typical video file, so this action only ever receives URLs, never the file itself.
export async function saveRetrospectiveItemAction(
  id: string | null,
  formData: FormData
): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const mediaUrl = String(formData.get("media_url") ?? "");
  if (!mediaUrl) return { error: "Selecione uma foto ou vídeo." };

  const payload = {
    title: String(formData.get("title") ?? ""),
    description: String(formData.get("description") ?? ""),
    media_type: String(formData.get("media_type") ?? "IMAGE"),
    media_url: mediaUrl,
    media_file_name: formData.get("media_file_name") ? String(formData.get("media_file_name")) : null,
    media_aspect_ratio: String(formData.get("media_aspect_ratio") ?? "4:3"),
    poster_url: formData.get("poster_url") ? String(formData.get("poster_url")) : null,
    event_date: formData.get("event_date") ? String(formData.get("event_date")) : null,
    is_active: true,
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase
      .from("retrospective_items")
      .update(payload)
      .eq("id", id)
      .eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("retrospective_items")
      .insert({ ...payload, church_id: admin.churchId, published_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/retrospectiva");
  revalidatePath(`/c/${admin.churchSlug}/retrospectiva`);
  return {};
}

export async function deleteRetrospectiveItemAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase
    .from("retrospective_items")
    .delete()
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/retrospectiva");
  revalidatePath(`/c/${admin.churchSlug}/retrospectiva`);
}

// Bulletins (Boletins) ------------------------------------------------------

// PDF + cover are uploaded directly from the browser to Supabase Storage (see BulletinForm) -
// Vercel Server Actions cap request bodies at a few MB, far below a typical PDF, so this action
// only ever receives URLs, never the file itself.
export async function saveBulletinAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const pdfUrl = String(formData.get("pdf_url") ?? "");
  if (!pdfUrl) return { error: "Selecione um PDF." };

  const relatedId = formData.get("related_announcement_id")
    ? String(formData.get("related_announcement_id"))
    : null;

  const payload = {
    title: String(formData.get("title") ?? ""),
    pdf_url: pdfUrl,
    pdf_file_name: formData.get("pdf_file_name") ? String(formData.get("pdf_file_name")) : null,
    cover_url: formData.get("cover_url") ? String(formData.get("cover_url")) : null,
    related_announcement_id: relatedId || null,
    is_active: true,
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("bulletins").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("bulletins")
      .insert({ ...payload, church_id: admin.churchId, published_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/boletins");
  revalidatePath(`/c/${admin.churchSlug}/boletins`);
  revalidatePath(`/c/${admin.churchSlug}/anuncios`);
  return {};
}

export async function deleteBulletinAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase.from("bulletins").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/boletins");
  revalidatePath(`/c/${admin.churchSlug}/boletins`);
  revalidatePath(`/c/${admin.churchSlug}/anuncios`);
}

// Sonoplastia shared files -------------------------------------------------

// The file is uploaded directly from the browser to Supabase Storage (see UploadForm) -
// Vercel Server Actions cap request bodies at a few MB, far below a typical shared file, so
// this action only ever receives the resulting metadata, never the file itself.
export async function saveSharedFileMetadataAction(metadata: {
  file_name: string;
  url: string;
  media_type: "IMAGE" | "VIDEO" | "DOCUMENT" | "LINK" | "YOUTUBE";
  size_bytes: number;
}): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const { error } = await supabase.from("shared_files").insert({
    ...metadata,
    church_id: admin.churchId,
    is_pinned: false,
    uploaded_at: Date.now(),
  });
  if (error) return { error: error.message };

  revalidatePath("/admin/sonoplastia");
  return {};
}

export async function deleteSharedFileAction(id: string) {
  const admin = await requireAdmin();
  if (admin.error) throw new Error(admin.error);
  const supabase = await createClient();
  const { error } = await supabase.from("shared_files").delete().eq("id", id).eq("church_id", admin.churchId);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/sonoplastia");
}

export async function toggleSharedFilePinAction(id: string, pinned: boolean): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();
  const { error } = await supabase
    .from("shared_files")
    .update({ is_pinned: pinned })
    .eq("id", id)
    .eq("church_id", admin.churchId);
  if (error) return { error: error.message };
  revalidatePath("/admin/sonoplastia");
  return {};
}
