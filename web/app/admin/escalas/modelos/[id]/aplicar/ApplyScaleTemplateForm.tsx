"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ScaleTemplate } from "@/lib/types/database";
import { applyScaleTemplateAction } from "../../../../actions";

const inputClass = "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

export function ApplyScaleTemplateForm({ template, roleNames }: { template: ScaleTemplate; roleNames: string[] }) {
  const router = useRouter();
  const [title, setTitle] = useState(template.name);
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState(template.default_start_time?.slice(0, 5) ?? "");
  const [endTime, setEndTime] = useState(template.default_end_time?.slice(0, 5) ?? "");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!date) {
      setError("Escolha uma data para a nova escala.");
      return;
    }
    setPending(true);
    setError(null);
    const formData = new FormData();
    formData.set("title", title);
    formData.set("date", date);
    formData.set("start_time", startTime);
    formData.set("end_time", endTime);
    const result = await applyScaleTemplateAction(template.id, formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    router.push(result.newId ? `/admin/escalas/${result.newId}` : "/admin/escalas");
    router.refresh();
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6 max-w-xl">
      <div className="rounded-lg border border-divider p-4">
        <p className="text-sm font-medium mb-2">
          Estrutura do modelo ({roleNames.length} {roleNames.length === 1 ? "função" : "funções"})
        </p>
        {roleNames.length === 0 ? (
          <p className="text-sm text-text-secondary">Este modelo não tem funções cadastradas.</p>
        ) : (
          <ul className="flex flex-col gap-1 text-sm text-text-secondary">
            {roleNames.map((name, i) => (
              <li key={i}>{i + 1}. {name}</li>
            ))}
          </ul>
        )}
        <p className="mt-2 text-xs text-text-muted">As pessoas ficam em branco - você preenche depois de salvar.</p>
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Título</span>
        <input required value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
      </label>

      <div className="grid grid-cols-3 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Data</span>
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

      {error && <p className="text-sm text-error">{error}</p>}

      <button type="submit" disabled={pending} className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60">
        {pending ? "Salvando..." : "Criar escala a partir do modelo"}
      </button>
    </form>
  );
}
