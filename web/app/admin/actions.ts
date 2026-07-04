"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";

export type ActionResult = { error?: string };

export async function signOutAction() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}

/** Resolves the current admin's church (id + slug) or an error string - RLS is the real
 *  enforcement, this just avoids a raw Postgres error reaching the form and gives us the slug
 *  to revalidate the right public path after a write. */
async function requireAdmin(): Promise<{ error: string; churchId?: never; churchSlug?: never } | { error?: never; churchId: string; churchSlug: string }> {
  const { isAdmin, churchId } = await getAdminStatus();
  if (!isAdmin || !churchId) return { error: "Apenas administradores podem fazer essa alteração." };

  const supabase = await createClient();
  const { data: church } = await supabase.from("churches").select("slug").eq("id", churchId).single();
  if (!church) return { error: "Igreja não encontrada." };

  return { churchId, churchSlug: church.slug };
}

// Scales ---------------------------------------------------------------

export async function saveScaleAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const admin = await requireAdmin();
  if (admin.error) return { error: admin.error };
  const supabase = await createClient();

  const payload = {
    date: String(formData.get("date")),
    start_time: String(formData.get("start_time")),
    end_time: formData.get("end_time") ? String(formData.get("end_time")) : null,
    title: String(formData.get("title")),
    reception_person: String(formData.get("reception_person") ?? ""),
    sound_person: String(formData.get("sound_person") ?? ""),
    preaching_person: String(formData.get("preaching_person") ?? ""),
    conducting_person: String(formData.get("conducting_person") ?? ""),
    musical_message_person: String(formData.get("musical_message_person") ?? ""),
    notes: String(formData.get("notes") ?? ""),
    is_special_event: formData.get("is_special_event") === "on",
    source_type: "OFFICIAL",
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("scales").update(payload).eq("id", id).eq("church_id", admin.churchId);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("scales")
      .insert({ ...payload, church_id: admin.churchId, created_at: Date.now() });
    if (error) return { error: error.message };
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
