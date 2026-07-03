"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { uploadSharedFileAction } from "../actions";

export function UploadForm() {
  const router = useRouter();
  const formRef = useRef<HTMLFormElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await uploadSharedFileAction(formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    formRef.current?.reset();
    router.refresh();
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
      {error && <p className="text-sm text-error">{error}</p>}
    </form>
  );
}
