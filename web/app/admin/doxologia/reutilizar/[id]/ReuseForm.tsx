"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Doxology, ProgramStep } from "@/lib/types/database";
import { cloneDoxologyAction, DoxologyCopyMode } from "../../../actions";

const inputClass =
  "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

const COPY_MODES: { value: DoxologyCopyMode; label: string; description: string }[] = [
  { value: "ALL", label: "Reutilizar tudo", description: "Título, etapas, responsáveis, duração e observações." },
  { value: "STRUCTURE", label: "Apenas a estrutura", description: "Só as etapas, ordem e duração - sem responsáveis nem observações." },
  { value: "SELECTED", label: "Selecionar etapas", description: "Escolha manualmente quais etapas entram na nova programação." },
];

/**
 * Fase 11.8.3 (Bloco O-T): the "escolher modo de cópia → revisar → definir nova data → salvar"
 * steps of the reuse flow, all on one screen. A new date/title/start time are always required
 * (Bloco T) - the source Doxologia is only ever read here, never written to (the clone action
 * only touches `times_reused` on it).
 */
export function ReuseForm({ source }: { source: Doxology }) {
  const router = useRouter();
  const [copyMode, setCopyMode] = useState<DoxologyCopyMode>("ALL");
  const [selectedSteps, setSelectedSteps] = useState<Set<number>>(new Set(source.program_order.map((_, i) => i)));
  const [title, setTitle] = useState(source.title);
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState(source.start_time?.slice(0, 5) ?? "");
  const [endTime, setEndTime] = useState(source.end_time?.slice(0, 5) ?? "");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const previewSteps: ProgramStep[] = useMemo(() => {
    const steps = copyMode === "SELECTED" ? source.program_order.filter((_, i) => selectedSteps.has(i)) : source.program_order;
    if (copyMode === "STRUCTURE") {
      return steps.map((step, i) => ({ order: i + 1, title: step.title, estimated_duration_minutes: step.estimated_duration_minutes ?? null }));
    }
    return steps.map((step, i) => ({ ...step, order: i + 1 }));
  }, [copyMode, selectedSteps, source.program_order]);

  function toggleStep(index: number) {
    setSelectedSteps((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);

    if (!date) {
      setError("Escolha uma nova data para a programação reutilizada.");
      return;
    }
    if (previewSteps.length === 0) {
      setError("Selecione pelo menos uma etapa para reutilizar.");
      return;
    }

    setPending(true);
    const formData = new FormData();
    formData.set("copy_mode", copyMode);
    formData.set("title", title);
    formData.set("date", date);
    formData.set("start_time", startTime);
    formData.set("end_time", endTime);
    if (copyMode === "SELECTED") formData.set("selected_steps", Array.from(selectedSteps).join(","));

    const result = await cloneDoxologyAction(source.id, formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    router.push(result.newId ? `/admin/doxologia/${result.newId}` : "/admin/doxologia");
    router.refresh();
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6 max-w-2xl">
      <fieldset className="flex flex-col gap-2">
        <legend className="text-sm font-medium mb-1">Modo de cópia</legend>
        {COPY_MODES.map((mode) => (
          <label
            key={mode.value}
            className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors ${
              copyMode === mode.value ? "border-primary bg-primary-container/20" : "border-divider"
            }`}
          >
            <input
              type="radio"
              name="copy_mode"
              value={mode.value}
              checked={copyMode === mode.value}
              onChange={() => setCopyMode(mode.value)}
              className="mt-1"
            />
            <span>
              <span className="block text-sm font-medium">{mode.label}</span>
              <span className="block text-xs text-text-secondary">{mode.description}</span>
            </span>
          </label>
        ))}
      </fieldset>

      {copyMode === "SELECTED" && (
        <div>
          <p className="text-sm font-medium mb-2">Etapas a reutilizar</p>
          <div className="flex flex-col gap-1.5">
            {source.program_order.map((step, i) => (
              <label key={i} className="flex items-center gap-2 rounded-lg border border-divider p-2 text-sm">
                <input type="checkbox" checked={selectedSteps.has(i)} onChange={() => toggleStep(i)} />
                {step.title || "(sem título)"}
              </label>
            ))}
            {source.program_order.length === 0 && <p className="text-sm text-text-secondary">Esta programação não tem etapas.</p>}
          </div>
        </div>
      )}

      <div>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Título</span>
          <input required value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
        </label>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Nova data</span>
          <input type="date" required value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Início</span>
          <input type="time" required value={startTime} onChange={(e) => setStartTime(e.target.value)} className={inputClass} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Fim (opcional)</span>
          <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} className={inputClass} />
        </label>
      </div>

      <div className="rounded-lg border border-divider p-4">
        <p className="text-sm font-medium mb-2">Pré-visualização ({previewSteps.length} {previewSteps.length === 1 ? "etapa" : "etapas"})</p>
        {previewSteps.length === 0 ? (
          <p className="text-sm text-text-secondary">Nenhuma etapa selecionada.</p>
        ) : (
          <ol className="flex flex-col gap-1 text-sm text-text-secondary">
            {previewSteps.map((step, i) => (
              <li key={i}>
                {step.order}. {step.title || "(sem título)"}
                {copyMode === "ALL" && step.responsible_person ? ` - ${step.responsible_person}` : ""}
              </li>
            ))}
          </ol>
        )}
      </div>

      {error && <p className="text-sm text-error">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60"
      >
        {pending ? "Salvando..." : "Salvar como nova"}
      </button>
    </form>
  );
}
