"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { extractYouTubeId } from "@/lib/youtube";
import { saveSharedFileMetadataAction } from "../actions";

type Mode = "file" | "link";

export function UploadForm() {
  const router = useRouter();
  const formRef = useRef<HTMLFormElement>(null);
  const [mode, setMode] = useState<Mode>("file");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);

  async function handleFileSubmit(formData: FormData) {
    const file = formData.get("file") as File | null;
    if (!file || file.size === 0) {
      setError("Selecione um arquivo.");
      return;
    }

    setProgressLabel(`Enviando (${(file.size / (1024 * 1024)).toFixed(1)} MB)...`);
    const supabase = createClient();
    const extension = file.name.split(".").pop() || "bin";
    const path = `sonoplastia/${crypto.randomUUID()}.${extension}`;
    const { error: uploadError } = await supabase.storage
      .from("church-files")
      .upload(path, file, { upsert: false });
    if (uploadError) throw new Error(uploadError.message);
    const { data } = supabase.storage.from("church-files").getPublicUrl(path);

    const mediaType = file.type.startsWith("image/")
      ? "IMAGE"
      : file.type.startsWith("video/")
        ? "VIDEO"
        : "DOCUMENT";

    const result = await saveSharedFileMetadataAction({
      file_name: file.name,
      url: data.publicUrl,
      media_type: mediaType,
      size_bytes: file.size,
    });
    if (result.error) throw new Error(result.error);
  }

  async function handleLinkSubmit(formData: FormData) {
    const url = String(formData.get("link_url") ?? "").trim();
    const label = String(formData.get("link_label") ?? "").trim();
    if (!url) {
      setError("Cole o link.");
      return;
    }

    const youtubeId = extractYouTubeId(url);
    const result = await saveSharedFileMetadataAction({
      file_name: label || url,
      url: youtubeId ? `https://www.youtube.com/watch?v=${youtubeId}` : url,
      media_type: youtubeId ? "YOUTUBE" : "LINK",
      size_bytes: 0,
    });
    if (result.error) throw new Error(result.error);
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);

    try {
      if (mode === "file") {
        await handleFileSubmit(formData);
      } else {
        await handleLinkSubmit(formData);
      }
      formRef.current?.reset();
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao salvar.");
    } finally {
      setPending(false);
      setProgressLabel(null);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex w-fit rounded-lg border border-divider p-1 text-sm">
        <button
          type="button"
          onClick={() => setMode("file")}
          className={`rounded-md px-4 py-1.5 font-medium transition-colors ${mode === "file" ? "bg-primary text-white" : "text-text-secondary"}`}
        >
          Enviar arquivo
        </button>
        <button
          type="button"
          onClick={() => setMode("link")}
          className={`rounded-md px-4 py-1.5 font-medium transition-colors ${mode === "link" ? "bg-primary text-white" : "text-text-secondary"}`}
        >
          Adicionar link
        </button>
      </div>

      <form ref={formRef} action={handleSubmit} className="flex flex-wrap items-center gap-3">
        {mode === "file" ? (
          <input
            type="file"
            name="file"
            required
            className="text-sm file:mr-3 file:rounded-full file:border-0 file:bg-primary-container file:px-4 file:py-2 file:text-sm file:font-medium file:text-on-primary-container"
          />
        ) : (
          <>
            <input
              name="link_url"
              required
              placeholder="https://... (link do YouTube ou qualquer outro)"
              className="min-w-64 flex-1 rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary"
            />
            <input
              name="link_label"
              placeholder="Nome (opcional)"
              className="w-48 rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary"
            />
          </>
        )}
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60 shrink-0"
        >
          {pending ? "Enviando..." : mode === "file" ? "Enviar" : "Adicionar"}
        </button>
      </form>
      {progressLabel && <p className="text-sm text-text-secondary">{progressLabel}</p>}
      {error && <p className="text-sm text-error">{error}</p>}
    </div>
  );
}
