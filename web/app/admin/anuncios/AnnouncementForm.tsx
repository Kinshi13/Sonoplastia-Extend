"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { Announcement } from "@/lib/types/database";
import { saveAnnouncementAction } from "../actions";

type MediaType = Announcement["media_type"];

type FormState = {
  title: string;
  description: string;
  relatedEventDate: string;
  mediaType: MediaType;
  mediaUrl: string;
  mediaFileName: string;
  isPinned: boolean;
  isActive: boolean;
};

function stateFrom(existing: Announcement | null): FormState {
  return {
    title: existing?.title ?? "",
    description: existing?.description ?? "",
    relatedEventDate: existing?.related_event_date ?? "",
    mediaType: existing?.media_type ?? "NONE",
    mediaUrl: existing?.media_url ?? "",
    mediaFileName: existing?.media_file_name ?? "",
    isPinned: existing?.is_pinned ?? false,
    isActive: existing?.is_active ?? true,
  };
}

function isDirty(a: FormState, b: FormState): boolean {
  return (
    a.title !== b.title ||
    a.description !== b.description ||
    a.relatedEventDate !== b.relatedEventDate ||
    a.mediaType !== b.mediaType ||
    a.mediaUrl !== b.mediaUrl ||
    a.mediaFileName !== b.mediaFileName ||
    a.isPinned !== b.isPinned ||
    a.isActive !== b.isActive
  );
}

/**
 * Usabilidade (edição de anúncios) - every field is now controlled state (title/description/data
 * used to be uncontrolled `defaultValue` inputs) so "alterações não salvas" can be answered by one
 * comparison against the baseline captured at mount ([initial]), instead of not being answerable
 * at all. A picked file is still uploaded straight to Storage from the browser (unchanged - Server
 * Actions cap request bodies far below a typical image/video).
 */
export function AnnouncementForm({ existing }: { existing: Announcement | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);
  const [showRemoveImageConfirm, setShowRemoveImageConfirm] = useState(false);
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [initial] = useState(() => stateFrom(existing));
  const [form, setForm] = useState<FormState>(initial);
  const dirty = isDirty(initial, form) || pickedFile !== null;

  // Catches a hard reload/tab close with unsaved changes - the in-app "Cancelar" flow below
  // handles the Next.js navigation case (which beforeunload does NOT fire for).
  useEffect(() => {
    function onBeforeUnload(e: BeforeUnloadEvent) {
      if (!dirty) return;
      e.preventDefault();
    }
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [dirty]);

  function handleCancel() {
    if (dirty && !confirm("Descartar alterações não salvas neste anúncio?")) return;
    router.push("/admin/anuncios");
  }

  function handleFileChange(file: File | null) {
    setPickedFile(file);
    if (preview) URL.revokeObjectURL(preview);
    setPreview(file ? URL.createObjectURL(file) : null);
  }

  function handleRemoveImage() {
    setForm((f) => ({ ...f, mediaType: "NONE", mediaUrl: "", mediaFileName: "" }));
    setPickedFile(null);
    if (preview) URL.revokeObjectURL(preview);
    setPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    setShowRemoveImageConfirm(false);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);

    try {
      let mediaUrl = form.mediaUrl;
      let mediaFileName = form.mediaFileName;
      let mediaType: MediaType = form.mediaType;

      if (pickedFile) {
        setProgressLabel(`Enviando arquivo (${(pickedFile.size / (1024 * 1024)).toFixed(1)} MB)...`);
        const supabase = createClient();
        const extension = pickedFile.name.split(".").pop() || "bin";
        const path = `announcements/${crypto.randomUUID()}.${extension}`;
        const { error: uploadError } = await supabase.storage
          .from("church-files")
          .upload(path, pickedFile, { upsert: false });
        // A failed upload never touches the announcement's existing image - the old mediaUrl is
        // still sitting in `form`, untouched, so the save below (if retried) keeps it intact.
        if (uploadError) throw new Error(uploadError.message);
        const { data } = supabase.storage.from("church-files").getPublicUrl(path);
        mediaUrl = data.publicUrl;
        mediaFileName = pickedFile.name;
        mediaType = pickedFile.type.startsWith("video/") ? "VIDEO" : pickedFile.type.startsWith("image/") ? "IMAGE" : "DOCUMENT";
      }

      const formData = new FormData();
      formData.set("title", form.title);
      formData.set("description", form.description);
      formData.set("related_event_date", form.relatedEventDate);
      formData.set("media_type", mediaType);
      formData.set("media_url", mediaUrl);
      formData.set("media_file_name", mediaFileName);
      formData.set("is_pinned", form.isPinned ? "on" : "off");
      formData.set("is_active", form.isActive ? "true" : "false");

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

  const hasMedia = !!form.mediaUrl && !pickedFile;

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <Field label="Título">
        <input
          required
          value={form.title}
          onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          className={inputClass}
        />
      </Field>
      <Field label="Texto">
        <textarea
          value={form.description}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          rows={3}
          className={inputClass}
        />
      </Field>
      <Field label="Data do evento (opcional)">
        <input
          type="date"
          value={form.relatedEventDate}
          onChange={(e) => setForm((f) => ({ ...f, relatedEventDate: e.target.value }))}
          className={inputClass}
        />
      </Field>

      <div>
        <span className="text-sm font-medium">Mídia</span>
        {hasMedia && (
          <p className="mt-1 text-xs text-text-secondary">
            Arquivo atual: {form.mediaFileName || form.mediaUrl}
          </p>
        )}
        {preview && <p className="mt-1 text-xs text-text-secondary">Nova mídia selecionada: {pickedFile?.name}</p>}
        {(form.mediaType === "IMAGE" || pickedFile?.type.startsWith("image/")) && (preview || form.mediaUrl) && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={preview ?? form.mediaUrl}
            alt="Pré-visualização"
            className="mt-2 w-40 rounded-xl border border-divider aspect-4/3 object-cover"
          />
        )}
        <div className="mt-2 flex items-center gap-3">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*,video/*,application/pdf,.ppt,.pptx"
            onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
            className={inputClass}
          />
        </div>
        <p className="mt-1 text-xs text-text-secondary">
          {hasMedia || preview ? "Escolher um arquivo substitui a mídia atual." : "Imagem, vídeo ou documento (opcional)."}
        </p>
        {(hasMedia || preview) && (
          <button
            type="button"
            onClick={() => setShowRemoveImageConfirm(true)}
            className="mt-2 text-sm font-medium text-error"
          >
            Remover mídia
          </button>
        )}
      </div>

      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={form.isPinned}
          onChange={(e) => setForm((f) => ({ ...f, isPinned: e.target.checked }))}
        />
        Fixar no topo do feed
      </label>

      <label className="flex items-start gap-2 text-sm">
        <input
          type="checkbox"
          className="mt-0.5"
          checked={form.isActive}
          onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))}
        />
        <span>
          Publicado
          <span className="block text-xs text-text-secondary">
            {form.isActive ? "Visível para os membros da igreja." : "Rascunho - só administradores conseguem ver."}
          </span>
        </span>
      </label>

      {error && <p className="text-sm text-error">{error}</p>}
      {progressLabel && <p className="text-sm text-text-secondary">{progressLabel}</p>}

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60"
        >
          {pending ? "Salvando..." : form.isActive ? "Publicar" : "Salvar rascunho"}
        </button>
        <button type="button" onClick={handleCancel} disabled={pending} className="text-sm font-medium text-text-secondary disabled:opacity-60">
          Cancelar
        </button>
      </div>

      {showRemoveImageConfirm && (
        <div className="rounded-lg border border-divider bg-surface p-4 flex flex-col gap-3 max-w-sm">
          <p className="text-sm">Remover mídia deste anúncio?</p>
          <div className="flex items-center gap-3">
            <button type="button" onClick={handleRemoveImage} className="text-sm font-medium text-error">
              Remover
            </button>
            <button type="button" onClick={() => setShowRemoveImageConfirm(false)} className="text-sm font-medium text-text-secondary">
              Cancelar
            </button>
          </div>
        </div>
      )}
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
