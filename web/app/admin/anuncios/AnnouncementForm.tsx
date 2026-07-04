"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { Announcement } from "@/lib/types/database";
import { saveAnnouncementAction } from "../actions";

export function AnnouncementForm({ existing }: { existing: Announcement | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);
  const [mediaType, setMediaType] = useState(existing?.media_type ?? "NONE");

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);

    const mediaFile = formData.get("media_file") as File | null;
    // The <input type="file"> is still part of this FormData - strip it before sending to the
    // server action. Vercel Server Actions cap request bodies at a few MB, so the file itself
    // is uploaded straight to Supabase Storage from here instead.
    formData.delete("media_file");

    try {
      let mediaUrl = existing?.media_url ?? "";
      let mediaFileName = existing?.media_file_name ?? "";

      if (mediaFile && mediaFile.size > 0) {
        setProgressLabel(`Enviando arquivo (${(mediaFile.size / (1024 * 1024)).toFixed(1)} MB)...`);
        const supabase = createClient();
        const extension = mediaFile.name.split(".").pop() || "bin";
        const path = `announcements/${crypto.randomUUID()}.${extension}`;
        const { error: uploadError } = await supabase.storage
          .from("church-files")
          .upload(path, mediaFile, { upsert: false });
        if (uploadError) throw new Error(uploadError.message);
        const { data } = supabase.storage.from("church-files").getPublicUrl(path);
        mediaUrl = data.publicUrl;
        mediaFileName = mediaFile.name;
      }

      formData.set("media_url", mediaUrl);
      formData.set("media_file_name", mediaFileName);

      setProgressLabel("Salvando...");
      const result = await saveAnnouncementAction(existing?.id ?? null, formData);
      if (result.error) {
        setError(result.error);
        setPending(false);
        setProgressLabel(null);
        return;
      }
      router.push("/admin/anuncios");
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no envio do arquivo.");
      setPending(false);
      setProgressLabel(null);
    }
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <Field label="Título">
        <input name="title" required defaultValue={existing?.title} className={inputClass} />
      </Field>
      <Field label="Texto">
        <textarea name="description" defaultValue={existing?.description} rows={3} className={inputClass} />
      </Field>
      <Field label="Data do evento (opcional)">
        <input
          type="date"
          name="related_event_date"
          defaultValue={existing?.related_event_date ?? ""}
          className={inputClass}
        />
      </Field>

      <Field label="Mídia">
        <select
          name="media_type"
          value={mediaType}
          onChange={(e) => setMediaType(e.target.value as Announcement["media_type"])}
          className={inputClass}
        >
          <option value="NONE">Nenhuma</option>
          <option value="IMAGE">Imagem</option>
          <option value="VIDEO">Vídeo</option>
          <option value="DOCUMENT">Documento</option>
        </select>
      </Field>

      {mediaType !== "NONE" && (
        <>
          {existing?.media_url && (
            <p className="text-xs text-text-secondary">
              Arquivo atual: {existing.media_file_name ?? existing.media_url}
            </p>
          )}
          <Field label="Substituir arquivo (opcional se já existe um)">
            <input type="file" name="media_file" className={inputClass} />
          </Field>
        </>
      )}

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="is_pinned" defaultChecked={existing?.is_pinned} />
        Fixar no topo do feed
      </label>

      {error && <p className="text-sm text-error">{error}</p>}
      {progressLabel && <p className="text-sm text-text-secondary">{progressLabel}</p>}

      <button
        type="submit"
        disabled={pending}
        className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60"
      >
        {pending ? "Enviando..." : "Salvar"}
      </button>
    </form>
  );
}

const inputClass =
  "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{label}</span>
      {children}
    </label>
  );
}
