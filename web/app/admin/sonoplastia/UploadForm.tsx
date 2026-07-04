"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { saveSharedFileMetadataAction } from "../actions";

export function UploadForm() {
  const router = useRouter();
  const formRef = useRef<HTMLFormElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);

    const file = formData.get("file") as File | null;
    if (!file || file.size === 0) {
      setError("Selecione um arquivo.");
      setPending(false);
      return;
    }

    try {
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
      if (result.error) {
        setError(result.error);
        return;
      }
      formRef.current?.reset();
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no envio do arquivo.");
    } finally {
      setPending(false);
      setProgressLabel(null);
    }
  }

  return (
    <form ref={formRef} action={handleSubmit} className="flex items-center gap-3">
      <input
        type="file"
        name="file"
        required
        className="text-sm file:mr-3 file:rounded-full file:border-0 file:bg-primary-container file:px-4 file:py-2 file:text-sm file:font-medium file:text-on-primary-container"
      />
      <button
        type="submit"
        disabled={pending}
        className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60 shrink-0"
      >
        {pending ? "Enviando..." : "Enviar"}
      </button>
      {progressLabel && <p className="text-sm text-text-secondary">{progressLabel}</p>}
      {error && <p className="text-sm text-error">{error}</p>}
    </form>
  );
}
