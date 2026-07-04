"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { RetrospectiveItem } from "@/lib/types/database";
import { saveRetrospectiveItemAction } from "../actions";

export function RetrospectivaForm({ existing }: { existing: RetrospectiveItem | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);
  const [aspectRatio, setAspectRatio] = useState(existing?.media_aspect_ratio ?? "4:3");
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [posterFile, setPosterFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<{ url: string; isVideo: boolean } | null>(
    existing?.media_url ? { url: existing.media_url, isVideo: existing.media_type === "VIDEO" } : null
  );
  const canvasRef = useRef<HTMLCanvasElement>(null);

  async function handleFileChange(file: File | null) {
    setPickedFile(file);
    setPosterFile(null);
    if (!file) {
      setPreview(existing?.media_url ? { url: existing.media_url, isVideo: existing.media_type === "VIDEO" } : null);
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    const isVideo = file.type.startsWith("video/");
    setPreview({ url: objectUrl, isVideo });

    if (isVideo) {
      const video = document.createElement("video");
      video.src = objectUrl;
      video.muted = true;
      video.playsInline = true;
      await new Promise<void>((resolve) => {
        video.onloadedmetadata = () => {
          setAspectRatio(`${video.videoWidth}:${video.videoHeight}`);
          video.currentTime = Math.min(0.1, video.duration / 2);
        };
        video.onseeked = () => resolve();
      });

      const canvas = canvasRef.current;
      if (canvas) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext("2d");
        ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);
        const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.85));
        if (blob) setPosterFile(new File([blob], "poster.jpg", { type: "image/jpeg" }));
      }
    } else {
      const img = new Image();
      img.src = objectUrl;
      await new Promise<void>((resolve) => {
        img.onload = () => {
          setAspectRatio(`${img.naturalWidth}:${img.naturalHeight}`);
          resolve();
        };
      });
    }
  }

  async function uploadToStorage(file: File, prefix: string): Promise<string> {
    const supabase = createClient();
    const extension = file.name.split(".").pop() || "bin";
    const path = `retrospectiva/${prefix}${crypto.randomUUID()}.${extension}`;
    const { error: uploadError } = await supabase.storage
      .from("church-files")
      .upload(path, file, { upsert: false });
    if (uploadError) throw new Error(uploadError.message);
    const { data } = supabase.storage.from("church-files").getPublicUrl(path);
    return data.publicUrl;
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    // The <input type="file"> is still part of this FormData - strip it before it's ever sent
    // to the server action, since the file itself was already (or is about to be) uploaded
    // directly to Supabase Storage above. Forwarding it here would re-send the full binary to
    // the server action, hitting the same body-size ceiling we just worked around.
    formData.delete("media_file");

    try {
      let mediaUrl = existing?.media_url ?? null;
      let mediaFileName = existing?.media_file_name ?? null;
      let mediaType = existing?.media_type ?? "IMAGE";
      let posterUrl = existing?.poster_url ?? null;

      if (pickedFile) {
        setProgressLabel(
          `Enviando ${pickedFile.type.startsWith("video/") ? "vídeo" : "foto"} (${(pickedFile.size / (1024 * 1024)).toFixed(1)} MB)...`
        );
        mediaUrl = await uploadToStorage(pickedFile, "");
        mediaFileName = pickedFile.name;
        mediaType = pickedFile.type.startsWith("video/") ? "VIDEO" : "IMAGE";

        if (posterFile) {
          setProgressLabel("Enviando miniatura do vídeo...");
          posterUrl = await uploadToStorage(posterFile, "posters/");
        }
      }

      if (!mediaUrl) {
        setError("Selecione uma foto ou vídeo.");
        setPending(false);
        setProgressLabel(null);
        return;
      }

      formData.set("media_url", mediaUrl);
      formData.set("media_file_name", mediaFileName ?? "");
      formData.set("media_type", mediaType);
      formData.set("media_aspect_ratio", aspectRatio);
      formData.set("poster_url", mediaType === "VIDEO" ? (posterUrl ?? "") : "");

      setProgressLabel("Salvando...");
      const result = await saveRetrospectiveItemAction(existing?.id ?? null, formData);
      if (result.error) {
        setError(result.error);
        setPending(false);
        setProgressLabel(null);
        return;
      }
      router.push("/admin/retrospectiva");
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no envio do arquivo.");
      setPending(false);
      setProgressLabel(null);
    }
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <canvas ref={canvasRef} className="hidden" />

      <Field label="Título (opcional)">
        <input name="title" defaultValue={existing?.title} className={inputClass} />
      </Field>
      <Field label="Descrição (opcional)">
        <textarea name="description" defaultValue={existing?.description} rows={3} className={inputClass} />
      </Field>
      <Field label="Data do evento (opcional)">
        <input type="date" name="event_date" defaultValue={existing?.event_date ?? ""} className={inputClass} />
      </Field>

      <Field label={existing ? "Substituir foto/vídeo (opcional)" : "Foto ou vídeo"}>
        <input
          type="file"
          name="media_file"
          accept="image/*,video/*"
          required={!existing}
          onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
          className={inputClass}
        />
      </Field>

      {preview && (
        <div className="w-40 overflow-hidden rounded-xl border border-divider">
          {preview.isVideo ? (
            <video src={preview.url} className="aspect-4/3 w-full object-cover" muted />
          ) : (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={preview.url} alt="Pré-visualização" className="aspect-4/3 w-full object-cover" />
          )}
        </div>
      )}

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
