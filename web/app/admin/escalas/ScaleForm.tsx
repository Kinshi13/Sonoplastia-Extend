"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Scale } from "@/lib/types/database";
import { saveScaleAction } from "../actions";

export function ScaleForm({ existing }: { existing: Scale | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await saveScaleAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/escalas");
    router.refresh();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <Field label="Título">
        <input name="title" required defaultValue={existing?.title} className={inputClass} />
      </Field>

      <div className="grid grid-cols-3 gap-3">
        <Field label="Data">
          <input type="date" name="date" required defaultValue={existing?.date} className={inputClass} />
        </Field>
        <Field label="Início">
          <input
            type="time"
            name="start_time"
            required
            defaultValue={existing?.start_time?.slice(0, 5)}
            className={inputClass}
          />
        </Field>
        <Field label="Fim (opcional)">
          <input
            type="time"
            name="end_time"
            defaultValue={existing?.end_time?.slice(0, 5) ?? ""}
            className={inputClass}
          />
        </Field>
      </div>

      <Field label="Sonoplastia">
        <input name="sound_person" defaultValue={existing?.sound_person} className={inputClass} />
      </Field>
      <Field label="Regência">
        <input name="conducting_person" defaultValue={existing?.conducting_person} className={inputClass} />
      </Field>
      <Field label="Mensagem musical">
        <input
          name="musical_message_person"
          defaultValue={existing?.musical_message_person}
          className={inputClass}
        />
      </Field>
      <Field label="Pregação">
        <input name="preaching_person" defaultValue={existing?.preaching_person} className={inputClass} />
      </Field>
      <Field label="Recepção">
        <input name="reception_person" defaultValue={existing?.reception_person} className={inputClass} />
      </Field>
      <Field label="Observações">
        <textarea name="notes" defaultValue={existing?.notes} rows={3} className={inputClass} />
      </Field>

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="is_special_event" defaultChecked={existing?.is_special_event} />
        Evento especial
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
