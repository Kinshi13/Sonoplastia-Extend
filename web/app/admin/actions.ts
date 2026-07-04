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

/** Returns an error string if the current session isn't an admin - RLS is the real enforcement,
 *  this just avoids a raw Postgres error reaching the form. */
async function requireAdmin(): Promise<string | null> {
  const { isAdmin } = await getAdminStatus();
  return isAdmin ? null : "Apenas administradores podem fazer essa alteração.";
}

// Scales ---------------------------------------------------------------

export async function saveScaleAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const adminError = await requireAdmin();
  if (adminError) return { error: adminError };
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
    const { error } = await supabase.from("scales").update(payload).eq("id", id);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase.from("scales").insert({ ...payload, created_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/escalas");
  revalidatePath("/");
  return {};
}

export async function deleteScaleAction(id: string) {
  const adminError = await requireAdmin();
  if (adminError) throw new Error(adminError);
  const supabase = await createClient();
  const { error } = await supabase.from("scales").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/escalas");
  revalidatePath("/");
}

// Doxologies -------------------------------------------------------------

export async function saveDoxologyAction(id: string | null, formData: FormData): Promise<ActionResult> {
  const adminError = await requireAdmin();
  if (adminError) return { error: adminError };
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
    const { error } = await supabase.from("doxologies").update(payload).eq("id", id);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase.from("doxologies").insert({ ...payload, created_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/doxologia");
  revalidatePath("/doxologia");
  return {};
}

export async function deleteDoxologyAction(id: string) {
  const adminError = await requireAdmin();
  if (adminError) throw new Error(adminError);
  const supabase = await createClient();
  const { error } = await supabase.from("doxologies").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/doxologia");
  revalidatePath("/doxologia");
}

// Announcements ------------------------------------------------------------

export async function saveAnnouncementAction(
  id: string | null,
  formData: FormData
): Promise<ActionResult> {
  const adminError = await requireAdmin();
  if (adminError) return { error: adminError };
  const supabase = await createClient();

  const mediaType = String(formData.get("media_type") ?? "NONE");
  const mediaFile = formData.get("media_file") as File | null;
  let mediaUrl = formData.get("existing_media_url") ? String(formData.get("existing_media_url")) : null;
  let mediaFileName = formData.get("existing_media_file_name")
    ? String(formData.get("existing_media_file_name"))
    : null;

  if (mediaFile && mediaFile.size > 0) {
    const extension = mediaFile.name.split(".").pop() || "bin";
    const path = `announcements/${crypto.randomUUID()}.${extension}`;
    const { error: uploadError } = await supabase.storage
      .from("church-files")
      .upload(path, mediaFile, { upsert: false });
    if (uploadError) return { error: uploadError.message };
    const { data: publicUrlData } = supabase.storage.from("church-files").getPublicUrl(path);
    mediaUrl = publicUrlData.publicUrl;
    mediaFileName = mediaFile.name;
  }

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
    const { error } = await supabase.from("announcements").update(payload).eq("id", id);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("announcements")
      .insert({ ...payload, published_at: Date.now(), updated_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/anuncios");
  revalidatePath("/anuncios");
  return {};
}

export async function deleteAnnouncementAction(id: string) {
  const adminError = await requireAdmin();
  if (adminError) throw new Error(adminError);
  const supabase = await createClient();
  const { error } = await supabase.from("announcements").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/anuncios");
  revalidatePath("/anuncios");
}

// Retrospective (photo/video feed) -----------------------------------------

export async function saveRetrospectiveItemAction(
  id: string | null,
  formData: FormData
): Promise<ActionResult> {
  const adminError = await requireAdmin();
  if (adminError) return { error: adminError };
  const supabase = await createClient();

  const mediaFile = formData.get("media_file") as File | null;
  const posterFile = formData.get("poster_file") as File | null;
  let mediaUrl = formData.get("existing_media_url") ? String(formData.get("existing_media_url")) : null;
  let mediaFileName = formData.get("existing_media_file_name")
    ? String(formData.get("existing_media_file_name"))
    : null;
  let posterUrl = formData.get("existing_poster_url") ? String(formData.get("existing_poster_url")) : null;
  let mediaType = formData.get("existing_media_type")
    ? String(formData.get("existing_media_type"))
    : "IMAGE";
  const aspectRatio = String(formData.get("media_aspect_ratio") ?? "4:3");

  if (mediaFile && mediaFile.size > 0) {
    const extension = mediaFile.name.split(".").pop() || "bin";
    const path = `retrospectiva/${crypto.randomUUID()}.${extension}`;
    const { error: uploadError } = await supabase.storage
      .from("church-files")
      .upload(path, mediaFile, { upsert: false });
    if (uploadError) return { error: uploadError.message };
    const { data: publicUrlData } = supabase.storage.from("church-files").getPublicUrl(path);
    mediaUrl = publicUrlData.publicUrl;
    mediaFileName = mediaFile.name;
    mediaType = mediaFile.type.startsWith("video/") ? "VIDEO" : "IMAGE";
  }

  if (!mediaUrl) return { error: "Selecione uma foto ou vídeo." };

  if (posterFile && posterFile.size > 0) {
    const path = `retrospectiva/posters/${crypto.randomUUID()}.jpg`;
    const { error: uploadError } = await supabase.storage
      .from("church-files")
      .upload(path, posterFile, { upsert: false });
    if (uploadError) return { error: uploadError.message };
    const { data: publicUrlData } = supabase.storage.from("church-files").getPublicUrl(path);
    posterUrl = publicUrlData.publicUrl;
  }

  const payload = {
    title: String(formData.get("title") ?? ""),
    description: String(formData.get("description") ?? ""),
    media_type: mediaType,
    media_url: mediaUrl,
    media_file_name: mediaFileName,
    media_aspect_ratio: aspectRatio,
    poster_url: mediaType === "VIDEO" ? posterUrl : null,
    event_date: formData.get("event_date") ? String(formData.get("event_date")) : null,
    is_active: true,
    updated_at: Date.now(),
  };

  if (id) {
    const { error } = await supabase.from("retrospective_items").update(payload).eq("id", id);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase
      .from("retrospective_items")
      .insert({ ...payload, published_at: Date.now() });
    if (error) return { error: error.message };
  }

  revalidatePath("/admin/retrospectiva");
  revalidatePath("/retrospectiva");
  return {};
}

export async function deleteRetrospectiveItemAction(id: string) {
  const adminError = await requireAdmin();
  if (adminError) throw new Error(adminError);
  const supabase = await createClient();
  const { error } = await supabase.from("retrospective_items").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/retrospectiva");
  revalidatePath("/retrospectiva");
}

// Sonoplastia shared files -------------------------------------------------

export async function uploadSharedFileAction(formData: FormData): Promise<ActionResult> {
  const adminError = await requireAdmin();
  if (adminError) return { error: adminError };
  const supabase = await createClient();

  const file = formData.get("file") as File | null;
  if (!file || file.size === 0) return { error: "Selecione um arquivo." };

  const extension = file.name.split(".").pop() || "bin";
  const path = `sonoplastia/${crypto.randomUUID()}.${extension}`;
  const { error: uploadError } = await supabase.storage
    .from("church-files")
    .upload(path, file, { upsert: false });
  if (uploadError) return { error: uploadError.message };

  const { data: publicUrlData } = supabase.storage.from("church-files").getPublicUrl(path);

  const mediaType = file.type.startsWith("image/")
    ? "IMAGE"
    : file.type.startsWith("video/")
      ? "VIDEO"
      : "DOCUMENT";

  const { error } = await supabase.from("shared_files").insert({
    file_name: file.name,
    url: publicUrlData.publicUrl,
    media_type: mediaType,
    size_bytes: file.size,
    uploaded_at: Date.now(),
  });
  if (error) return { error: error.message };

  revalidatePath("/admin/sonoplastia");
  return {};
}

export async function deleteSharedFileAction(id: string) {
  const adminError = await requireAdmin();
  if (adminError) throw new Error(adminError);
  const supabase = await createClient();
  const { error } = await supabase.from("shared_files").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/admin/sonoplastia");
}
