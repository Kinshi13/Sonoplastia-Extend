"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Announcement } from "@/lib/types/database";
import { saveAnnouncementAction } from "../actions";

export function AnnouncementForm({ existing }: { existing: Announcement | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [mediaType, setMediaType] = useState(existing?.media_type ?? "NONE");

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await saveAnnouncementAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/anuncios");
    router.refresh();
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
          <input type="hidden" name="existing_media_url" value={existing?.media_url ?? ""} />
          <input type="hidden" name="existing_media_file_name" value={existing?.media_file_name ?? ""} />
        </>
      )}

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="is_pinned" defaultChecked={existing?.is_pinned} />
        Fixar no topo do feed
      </label>

      {error && <p className="text-sm text-error">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60"
      >
        {pending ? "Salvando..." : "Salvar"}
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
