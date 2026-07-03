"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Doxology, ProgramStep } from "@/lib/types/database";
import { saveDoxologyAction } from "../actions";

export function DoxologyForm({ existing }: { existing: Doxology | null }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [steps, setSteps] = useState<ProgramStep[]>(existing?.program_order ?? []);

  function addStep() {
    setSteps((prev) => [
      ...prev,
      { order: prev.length + 1, title: "", description: "", responsible_person: "", estimated_duration_minutes: null },
    ]);
  }

  function updateStep(index: number, patch: Partial<ProgramStep>) {
    setSteps((prev) => prev.map((step, i) => (i === index ? { ...step, ...patch } : step)));
  }

  function removeStep(index: number) {
    setSteps((prev) => prev.filter((_, i) => i !== index).map((step, i) => ({ ...step, order: i + 1 })));
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    formData.set("program_order", JSON.stringify(steps));
    const result = await saveDoxologyAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/doxologia");
    router.refresh();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <Field label="Título">
        <input name="title" required defaultValue={existing?.title} className={inputClass} />
      </Field>

      <div className="grid grid-cols-2 gap-3">
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
      </div>

      <Field label="Observações">
        <textarea name="notes" defaultValue={existing?.notes} rows={2} className={inputClass} />
      </Field>

      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium">Ordem do culto</span>
          <button type="button" onClick={addStep} className="text-sm text-primary">
            + Adicionar item
          </button>
        </div>
        <div className="flex flex-col gap-3">
          {steps.map((step, index) => (
            <div key={index} className="rounded-lg border border-divider p-3 flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <span className="text-xs text-text-secondary shrink-0 w-5">{step.order}.</span>
                <input
                  placeholder="Título"
                  value={step.title}
                  onChange={(e) => updateStep(index, { title: e.target.value })}
                  className={inputClass}
                />
                <button
                  type="button"
                  onClick={() => removeStep(index)}
                  className="text-xs text-error shrink-0"
                >
                  Remover
                </button>
              </div>
              <input
                placeholder="Responsável"
                value={step.responsible_person ?? ""}
                onChange={(e) => updateStep(index, { responsible_person: e.target.value })}
                className={inputClass}
              />
              <input
                type="number"
                placeholder="Duração estimada (min)"
                value={step.estimated_duration_minutes ?? ""}
                onChange={(e) =>
                  updateStep(index, {
                    estimated_duration_minutes: e.target.value ? Number(e.target.value) : null,
                  })
                }
                className={inputClass}
              />
            </div>
          ))}
          {steps.length === 0 && (
            <p className="text-sm text-text-secondary">Nenhum item na ordem do culto ainda.</p>
          )}
        </div>
      </div>

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
