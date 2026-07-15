"use client";

import { useState } from "react";
import { Download, FileText, Printer, X, Loader2, CheckCircle2, AlertCircle } from "lucide-react";
import { exportGeneralScaleCsvAction } from "./export-actions";

type Status = "idle" | "loading" | "success" | "error";

/**
 * Fase 11.7 (Parte 11): "Export Center" flow - Escala Geral → Exportar → escolher formato →
 * gerar. Only CSV and print/PDF are wired to real output this phase (see export-actions.ts for
 * why PNG/a dedicated PDF library were left out); the dialog still names every state Parte 11
 * asks for (loading/success/error/retry) for the two formats that exist.
 */
export function ExportScaleDialog() {
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string | null>(null);

  async function handleCsv() {
    setStatus("loading");
    setError(null);
    const result = await exportGeneralScaleCsvAction();
    if (result.error || !result.csv) {
      setStatus("error");
      setError(result.error ?? "Não foi possível gerar o arquivo.");
      return;
    }
    const blob = new Blob([result.csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = result.filename ?? "escala-geral.csv";
    link.click();
    URL.revokeObjectURL(url);
    setStatus("success");
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
      >
        <Download size={16} /> Exportar
      </button>

      {open && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-sm rounded-[var(--radius-lg)] border border-border-soft bg-surface p-6 [box-shadow:var(--elevation-overlay)]">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-display text-lg">Exportar Escala Geral</h2>
              <button onClick={() => setOpen(false)} aria-label="Fechar" className="text-text-secondary hover:text-foreground">
                <X size={18} />
              </button>
            </div>

            <div className="flex flex-col gap-2">
              <button
                onClick={handleCsv}
                disabled={status === "loading"}
                className="flex items-center gap-3 rounded-[var(--radius-md)] border border-border-soft px-4 py-3 text-sm text-left hover:border-primary transition-colors disabled:opacity-60"
              >
                <FileText size={18} className="text-primary shrink-0" />
                <span>
                  <span className="block font-medium">CSV</span>
                  <span className="block text-xs text-text-secondary">Dados estruturados para planilha</span>
                </span>
              </button>

              <a
                href="/admin/escalas/imprimir"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 rounded-[var(--radius-md)] border border-border-soft px-4 py-3 text-sm text-left hover:border-primary transition-colors"
              >
                <Printer size={18} className="text-primary shrink-0" />
                <span>
                  <span className="block font-medium">Imprimir / PDF</span>
                  <span className="block text-xs text-text-secondary">Abre uma versão pronta para impressão</span>
                </span>
              </a>
            </div>

            {status === "loading" && (
              <p className="mt-4 flex items-center gap-2 text-sm text-text-secondary">
                <Loader2 size={14} className="animate-spin" /> Gerando arquivo...
              </p>
            )}
            {status === "success" && (
              <p className="mt-4 flex items-center gap-2 text-sm text-success">
                <CheckCircle2 size={14} /> CSV baixado.
              </p>
            )}
            {status === "error" && (
              <div className="mt-4 flex items-start gap-2 text-sm text-error">
                <AlertCircle size={14} className="mt-0.5 shrink-0" />
                <span>
                  {error}{" "}
                  <button onClick={handleCsv} className="underline">
                    Tentar novamente
                  </button>
                </span>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
