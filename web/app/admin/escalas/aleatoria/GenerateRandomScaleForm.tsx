"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { generateRandomScaleAction } from "../../actions";

const inputClass = "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

/**
 * "Gerar escala aleatória" - one specific date, sorteia as pessoas entre as funções ativas (ou as
 * do modelo padrão daquele dia da semana, quando existir um). O resultado sempre nasce como Escala
 * Temporária (Bloco: "essa deve ser temporária") e abre direto na edição, pra conferir/trocar
 * qualquer nome antes de publicar de verdade.
 */
export function GenerateRandomScaleForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!date) {
      setError("Escolha o dia para gerar a escala.");
      return;
    }
    if (!startTime) {
      setError("Informe o horário de início.");
      return;
    }
    setPending(true);
    setError(null);
    const formData = new FormData();
    formData.set("title", title);
    formData.set("date", date);
    formData.set("start_time", startTime);
    formData.set("end_time", endTime);
    const result = await generateRandomScaleAction(formData);
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
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Título (opcional)</span>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Ex.: Culto de quarta" className={inputClass} />
      </label>

      <div className="grid grid-cols-3 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Dia</span>
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

      <p className="text-xs text-text-secondary">
        As pessoas são sorteadas entre as já cadastradas (priorizando a equipe de cada função,
        quando houver). Você pode revisar e trocar qualquer nome depois de gerar.
      </p>

      {error && <p className="text-sm text-error">{error}</p>}

      <button type="submit" disabled={pending} className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60">
        {pending ? "Sorteando..." : "Gerar escala aleatória"}
      </button>
    </form>
  );
}
