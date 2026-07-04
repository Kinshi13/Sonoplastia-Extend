"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { RetrospectiveItem } from "@/lib/types/database";
import { saveRetrospectiveItemAction } from "../actions";

export function RetrospectivaForm({ existing }: { existing: RetrospectiveItem | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [aspectRatio, setAspectRatio] = useState(existing?.media_aspect_ratio ?? "4:3");
  const [posterFile, setPosterFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<{ url: string; isVideo: boolean } | null>(
    existing?.media_url ? { url: existing.media_url, isVideo: existing.media_type === "VIDEO" } : null
  );
  const canvasRef = useRef<HTMLCanvasElement>(null);

  async function handleFileChange(file: File | null) {
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

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    if (posterFile) formData.set("poster_file", posterFile);
    const result = await saveRetrospectiveItemAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/retrospectiva");
    router.refresh();
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

      <input type="hidden" name="media_aspect_ratio" value={aspectRatio} />
      <input type="hidden" name="existing_media_url" value={existing?.media_url ?? ""} />
      <input type="hidden" name="existing_media_file_name" value={existing?.media_file_name ?? ""} />
      <input type="hidden" name="existing_media_type" value={existing?.media_type ?? "IMAGE"} />
      <input type="hidden" name="existing_poster_url" value={existing?.poster_url ?? ""} />

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
