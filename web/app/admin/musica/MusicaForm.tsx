"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { WorshipSong } from "@/lib/types/database";
import { extractYouTubeId, buildYouTubeWatchUrl, youTubeThumbnailUrl } from "@/lib/youtube";
import { saveWorshipSongAction } from "../actions";

type FormState = {
  title: string;
  artist: string;
  youtubeUrl: string;
  momentLabel: string;
  notes: string;
  programDate: string;
  orderIndex: number;
  isPublished: boolean;
};

function stateFrom(existing: WorshipSong | null): FormState {
  return {
    title: existing?.title ?? "",
    artist: existing?.artist ?? "",
    youtubeUrl: existing?.youtube_url ?? "",
    momentLabel: existing?.moment_label ?? "",
    notes: existing?.notes ?? "",
    programDate: existing?.program_date ?? "",
    orderIndex: existing?.order_index ?? 0,
    isPublished: existing?.is_published ?? true,
  };
}

function isDirty(a: FormState, b: FormState): boolean {
  return JSON.stringify(a) !== JSON.stringify(b);
}

/**
 * Música e Louvor - "YouTubeUrlParser" lives in lib/youtube.ts (shared with Retrospectiva's
 * YouTube-mode uploads, not duplicated here). Pasting a link recomputes videoId + thumbnail live,
 * before the form is even submitted, so the preview the Admin sees is exactly what gets saved.
 */
export function MusicaForm({ existing }: { existing: WorshipSong | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const [initial] = useState(() => stateFrom(existing));
  const [form, setForm] = useState<FormState>(initial);
  const dirty = isDirty(initial, form);

  const videoId = extractYouTubeId(form.youtubeUrl.trim());
  const linkTouched = form.youtubeUrl.trim().length > 0;
  const linkIsValid = !linkTouched || !!videoId;

  function handleCancel() {
    if (dirty && !confirm("Descartar alterações não salvas nesta música?")) return;
    router.push("/admin/musica");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!form.title.trim()) {
      setError("Informe o título da música.");
      return;
    }
    if (!videoId) {
      setError("Link do YouTube inválido.");
      return;
    }

    setPending(true);
    const formData = new FormData();
    formData.set("title", form.title.trim());
    formData.set("artist", form.artist);
    formData.set("youtube_url", buildYouTubeWatchUrl(videoId));
    formData.set("youtube_video_id", videoId);
    formData.set("thumbnail_url", youTubeThumbnailUrl(videoId));
    formData.set("moment_label", form.momentLabel);
    formData.set("notes", form.notes);
    formData.set("program_date", form.programDate);
    formData.set("order_index", String(form.orderIndex));
    formData.set("is_published", form.isPublished ? "true" : "false");

    const result = await saveWorshipSongAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/musica");
    router.refresh();
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <Field label="Link do YouTube">
        <input
          value={form.youtubeUrl}
          onChange={(e) => setForm((f) => ({ ...f, youtubeUrl: e.target.value }))}
          placeholder="https://www.youtube.com/watch?v=..."
          className={`${inputClass} ${linkTouched && !linkIsValid ? "border-error" : ""}`}
        />
        {linkTouched && !linkIsValid && <p className="mt-1 text-xs text-error">Link do YouTube inválido.</p>}
      </Field>

      {videoId && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={youTubeThumbnailUrl(videoId)}
          alt="Prévia da thumbnail"
          className="w-48 rounded-xl border border-divider aspect-video object-cover"
        />
      )}

      <Field label="Título da música">
        <input
          required
          value={form.title}
          onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          className={inputClass}
        />
      </Field>
      <Field label="Artista/Canal (opcional)">
        <input
          value={form.artist}
          onChange={(e) => setForm((f) => ({ ...f, artist: e.target.value }))}
          className={inputClass}
        />
      </Field>
      <Field label="Momento da programação (opcional)">
        <input
          value={form.momentLabel}
          onChange={(e) => setForm((f) => ({ ...f, momentLabel: e.target.value }))}
          placeholder="Ex.: Louvor inicial, Mensagem musical, Apelo"
          className={inputClass}
        />
      </Field>
      <Field label="Data da programação (opcional)">
        <input
          type="date"
          value={form.programDate}
          onChange={(e) => setForm((f) => ({ ...f, programDate: e.target.value }))}
          className={inputClass}
        />
      </Field>
      <Field label="Observações (opcional)">
        <textarea
          value={form.notes}
          onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
          rows={2}
          className={inputClass}
        />
      </Field>
      <Field label="Ordem">
        <input
          type="number"
          value={form.orderIndex}
          onChange={(e) => setForm((f) => ({ ...f, orderIndex: Number(e.target.value) || 0 }))}
          className={inputClass}
        />
      </Field>

      <label className="flex items-start gap-2 text-sm">
        <input
          type="checkbox"
          className="mt-0.5"
          checked={form.isPublished}
          onChange={(e) => setForm((f) => ({ ...f, isPublished: e.target.checked }))}
        />
        <span>
          Publicado
          <span className="block text-xs text-text-secondary">
            {form.isPublished ? "Visível para os membros da igreja." : "Rascunho - só administradores conseguem ver."}
          </span>
        </span>
      </label>

      {error && <p className="text-sm text-error">{error}</p>}

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60"
        >
          {pending ? "Salvando..." : form.isPublished ? "Publicar" : "Salvar rascunho"}
        </button>
        <button type="button" onClick={handleCancel} disabled={pending} className="text-sm font-medium text-text-secondary disabled:opacity-60">
          Cancelar
        </button>
      </div>
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
