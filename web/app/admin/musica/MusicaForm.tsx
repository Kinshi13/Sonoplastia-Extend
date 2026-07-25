"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { WorshipSong } from "@/lib/types/database";
import { parseYouTubeUrl } from "@/lib/youtube";
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
  isDailyRecommendation: boolean;
  recommendationDate: string;
  notificationEnabled: boolean;
  notificationTime: string;
  notificationTitle: string;
  notificationBody: string;
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
    isDailyRecommendation: existing?.is_daily_recommendation ?? false,
    recommendationDate: existing?.recommendation_date ?? "",
    notificationEnabled: existing?.notification_enabled ?? false,
    notificationTime: existing?.notification_time?.slice(0, 5) ?? "",
    notificationTitle: existing?.notification_title ?? "",
    notificationBody: existing?.notification_body ?? "",
  };
}

function isDirty(a: FormState, b: FormState): boolean {
  return JSON.stringify(a) !== JSON.stringify(b);
}

/**
 * Música e Louvor - "YouTubeUrlParser" (parseYouTubeUrl, lib/youtube.ts) is shared with
 * Retrospectiva's YouTube-mode uploads, not duplicated here. Pasting a link recomputes
 * videoId/thumbnail live, before the form is even submitted, so the preview the Admin sees is
 * exactly what gets saved.
 *
 * Notification fields (title/body/enabled/time) are captured here but no push is actually sent
 * yet (Bloco 15/19: "preparar arquitetura, não precisa enviar push real ainda") - they're stored
 * so a future sending job has real data to read instead of nothing.
 */
export function MusicaForm({ existing }: { existing: WorshipSong | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const [initial] = useState(() => stateFrom(existing));
  const [form, setForm] = useState<FormState>(initial);
  const dirty = isDirty(initial, form);

  const parsed = parseYouTubeUrl(form.youtubeUrl);
  const linkTouched = form.youtubeUrl.trim().length > 0;
  const linkIsValid = !linkTouched || parsed.isValid;

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
    if (!parsed.isValid || !parsed.videoId) {
      setError("Link do YouTube inválido.");
      return;
    }

    setPending(true);
    const formData = new FormData();
    formData.set("title", form.title.trim());
    formData.set("artist", form.artist);
    formData.set("youtube_url", parsed.normalizedUrl!);
    formData.set("youtube_video_id", parsed.videoId);
    formData.set("thumbnail_url", parsed.thumbnailUrl!);
    formData.set("moment_label", form.momentLabel);
    formData.set("notes", form.notes);
    formData.set("program_date", form.programDate);
    formData.set("order_index", String(form.orderIndex));
    formData.set("is_published", form.isPublished ? "true" : "false");
    formData.set("is_daily_recommendation", form.isDailyRecommendation ? "true" : "false");
    formData.set("recommendation_date", form.recommendationDate);
    formData.set("notification_enabled", form.notificationEnabled ? "true" : "false");
    formData.set("notification_time", form.notificationTime);
    formData.set("notification_title", form.notificationTitle);
    formData.set("notification_body", form.notificationBody);

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

      {parsed.thumbnailUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={parsed.thumbnailUrl}
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

      <div className="rounded-lg border border-divider p-3 flex flex-col gap-3">
        <label className="flex items-start gap-2 text-sm">
          <input
            type="checkbox"
            className="mt-0.5"
            checked={form.isDailyRecommendation}
            onChange={(e) => setForm((f) => ({ ...f, isDailyRecommendation: e.target.checked }))}
          />
          <span>
            Marcar como recomendação do dia
            <span className="block text-xs text-text-secondary">
              Aparece em destaque na tela pública, na data escolhida abaixo.
            </span>
          </span>
        </label>
        {form.isDailyRecommendation && (
          <Field label="Data da recomendação">
            <input
              type="date"
              value={form.recommendationDate}
              onChange={(e) => setForm((f) => ({ ...f, recommendationDate: e.target.value }))}
              className={inputClass}
            />
          </Field>
        )}
      </div>

      <div className="rounded-lg border border-divider p-3 flex flex-col gap-3">
        <label className="flex items-start gap-2 text-sm">
          <input
            type="checkbox"
            className="mt-0.5"
            checked={form.notificationEnabled}
            onChange={(e) => setForm((f) => ({ ...f, notificationEnabled: e.target.checked }))}
          />
          <span>
            Preparar notificação para esta música
            <span className="block text-xs text-text-secondary">
              Só guarda o conteúdo por enquanto - o envio automático ainda não está ativo (ver seção de notificações).
            </span>
          </span>
        </label>
        {form.notificationEnabled && (
          <>
            <Field label="Horário do lembrete (opcional)">
              <input
                type="time"
                value={form.notificationTime}
                onChange={(e) => setForm((f) => ({ ...f, notificationTime: e.target.value }))}
                className={inputClass}
              />
            </Field>
            <Field label="Título da notificação (opcional)">
              <input
                value={form.notificationTitle}
                onChange={(e) => setForm((f) => ({ ...f, notificationTitle: e.target.value }))}
                placeholder={form.title || "Música e Louvor"}
                className={inputClass}
              />
            </Field>
            <Field label="Texto da notificação (opcional)">
              <textarea
                value={form.notificationBody}
                onChange={(e) => setForm((f) => ({ ...f, notificationBody: e.target.value }))}
                rows={2}
                placeholder="Confira a recomendação de hoje."
                className={inputClass}
              />
            </Field>
          </>
        )}
      </div>

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
