"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";
import { generatePdfCoverBlob } from "@/lib/pdfCover";
import { Bulletin, Announcement } from "@/lib/types/database";
import { saveBulletinAction } from "../actions";

export function BulletinForm({
  existing,
  announcements,
}: {
  existing: Bulletin | null;
  announcements: Announcement[];
}) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [progressLabel, setProgressLabel] = useState<string | null>(null);
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState<string | null>(existing?.cover_url ?? null);
  const [coverBlob, setCoverBlob] = useState<Blob | null>(null);

  async function handleFileChange(file: File | null) {
    setPickedFile(file);
    setCoverBlob(null);
    if (!file) {
      setCoverPreview(existing?.cover_url ?? null);
      return;
    }
    setProgressLabel("Gerando capa...");
    try {
      const blob = await generatePdfCoverBlob(file);
      if (blob) {
        setCoverBlob(blob);
        setCoverPreview(URL.createObjectURL(blob));
      }
    } catch {
      // Cover generation is a nice-to-have - fall back to no cover if the PDF can't be parsed.
      setCoverPreview(null);
    } finally {
      setProgressLabel(null);
    }
  }

  async function uploadToStorage(file: File | Blob, path: string): Promise<string> {
    const supabase = createClient();
    const { error: uploadError } = await supabase.storage.from("church-files").upload(path, file, {
      upsert: false,
    });
    if (uploadError) throw new Error(uploadError.message);
    const { data } = supabase.storage.from("church-files").getPublicUrl(path);
    return data.publicUrl;
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    formData.delete("pdf_file");

    try {
      let pdfUrl = existing?.pdf_url ?? null;
      let pdfFileName = existing?.pdf_file_name ?? null;
      let coverUrl = existing?.cover_url ?? null;

      if (pickedFile) {
        const id = crypto.randomUUID();
        setProgressLabel(`Enviando PDF (${(pickedFile.size / (1024 * 1024)).toFixed(1)} MB)...`);
        pdfUrl = await uploadToStorage(pickedFile, `boletins/${id}.pdf`);
        pdfFileName = pickedFile.name;

        if (coverBlob) {
          setProgressLabel("Enviando capa...");
          coverUrl = await uploadToStorage(coverBlob, `boletins/covers/${id}.jpg`);
        }
      }

      if (!pdfUrl) {
        setError("Selecione um PDF.");
        setPending(false);
        setProgressLabel(null);
        return;
      }

      formData.set("pdf_url", pdfUrl);
      formData.set("pdf_file_name", pdfFileName ?? "");
      formData.set("cover_url", coverUrl ?? "");

      setProgressLabel("Salvando...");
      const result = await saveBulletinAction(existing?.id ?? null, formData);
      if (result.error) {
        setError(result.error);
        setPending(false);
        setProgressLabel(null);
        return;
      }
      router.push("/admin/boletins");
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

      <Field label="Anúncio relacionado (opcional)">
        <select
          name="related_announcement_id"
          defaultValue={existing?.related_announcement_id ?? ""}
          className={inputClass}
        >
          <option value="">Nenhum</option>
          {announcements.map((a) => (
            <option key={a.id} value={a.id}>
              {a.title}
            </option>
          ))}
        </select>
      </Field>

      <Field label={existing ? "Substituir PDF (opcional)" : "Arquivo PDF"}>
        <input
          type="file"
          name="pdf_file"
          accept="application/pdf"
          required={!existing}
          onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
          className={inputClass}
        />
      </Field>

      {coverPreview && (
        <div className="w-40 overflow-hidden rounded-xl border border-divider">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={coverPreview} alt="Capa do boletim" className="aspect-4/3 w-full object-cover" />
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
