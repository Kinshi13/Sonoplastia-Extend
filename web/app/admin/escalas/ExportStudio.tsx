"use client";

import { useState } from "react";
import { Download, FileText, Printer, Image as ImageIcon, X, Loader2, CheckCircle2, AlertCircle } from "lucide-react";
import { exportGeneralScaleCsvAction, getNextScaleForExportAction } from "./export-actions";
import { renderScaleCardPng } from "./export-png";
import { constellationForDay } from "@/components/celestial/DayConstellation";
import { formatDatePt, formatTimePt } from "@/lib/format";

type Format = "csv" | "print" | "png";
type PrintTheme = "celestial" | "economic";
type Status = "idle" | "loading" | "success" | "error";

/**
 * Fase 11.8 (Parte 17-18, "Export Studio"): Escala Geral → Exportar → escolher formato → (tema,
 * quando aplicável) → gerar. Four formats shipped this phase: CSV, impressão em dois temas
 * (Celestial/Econômico), e PNG 4:5 da próxima escala - see the Fase 11.8 report for what Parte
 * 18-21 asks that isn't here yet (Story 9:16, Landscape 16:9, mês inteiro em PNG, exportação de
 * Doxologia, histórico de exportações).
 */
export function ExportStudio() {
  const [open, setOpen] = useState(false);
  const [format, setFormat] = useState<Format>("csv");
  const [theme, setTheme] = useState<PrintTheme>("celestial");
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string | null>(null);

  async function handleGenerate() {
    setStatus("loading");
    setError(null);

    if (format === "csv") {
      const result = await exportGeneralScaleCsvAction();
      if (result.error || !result.csv) {
        setStatus("error");
        setError(result.error ?? "Não foi possível gerar o arquivo.");
        return;
      }
      downloadText(result.csv, result.filename ?? "escala-geral.csv", "text/csv;charset=utf-8");
      setStatus("success");
      return;
    }

    if (format === "print") {
      window.open(`/admin/escalas/imprimir?theme=${theme}`, "_blank", "noopener,noreferrer");
      setStatus("success");
      return;
    }

    // format === "png"
    const result = await getNextScaleForExportAction();
    if (result.error || !result.scale) {
      setStatus("error");
      setError(result.error ?? "Não foi possível gerar a imagem.");
      return;
    }
    const kind = constellationForDay(new Date(`${result.scale.date}T00:00:00`).getDay(), result.scale.is_special_event);
    const blob = await renderScaleCardPng(result.scale, kind, "Escala Church", formatDatePt, formatTimePt);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `proxima-escala-${result.scale.date}.png`;
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
          <div className="w-full max-w-md rounded-[var(--radius-lg)] border border-border-soft bg-surface p-6 [box-shadow:var(--elevation-overlay)]">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-display text-lg">Export Studio</h2>
              <button onClick={() => setOpen(false)} aria-label="Fechar" className="text-text-secondary hover:text-foreground">
                <X size={18} />
              </button>
            </div>

            <p className="text-xs font-medium uppercase tracking-wide text-text-secondary mb-2">Formato</p>
            <div className="grid grid-cols-3 gap-2 mb-4">
              <FormatButton icon={FileText} label="CSV" active={format === "csv"} onClick={() => setFormat("csv")} />
              <FormatButton icon={Printer} label="PDF / Impressão" active={format === "print"} onClick={() => setFormat("print")} />
              <FormatButton icon={ImageIcon} label="PNG (próxima)" active={format === "png"} onClick={() => setFormat("png")} />
            </div>

            {format === "print" && (
              <>
                <p className="text-xs font-medium uppercase tracking-wide text-text-secondary mb-2">Tema</p>
                <div className="grid grid-cols-2 gap-2 mb-4">
                  <button
                    onClick={() => setTheme("celestial")}
                    className={`rounded-[var(--radius-md)] border px-3 py-2 text-sm text-left transition-colors ${
                      theme === "celestial" ? "border-primary bg-primary-container/30" : "border-border-soft"
                    }`}
                  >
                    <span className="block font-medium">Celestial</span>
                    <span className="block text-xs text-text-secondary">Identidade completa</span>
                  </button>
                  <button
                    onClick={() => setTheme("economic")}
                    className={`rounded-[var(--radius-md)] border px-3 py-2 text-sm text-left transition-colors ${
                      theme === "economic" ? "border-primary bg-primary-container/30" : "border-border-soft"
                    }`}
                  >
                    <span className="block font-medium">Econômico</span>
                    <span className="block text-xs text-text-secondary">Baixo consumo de tinta</span>
                  </button>
                </div>
              </>
            )}

            <ExportPreview format={format} theme={theme} />

            <button
              onClick={handleGenerate}
              disabled={status === "loading"}
              className="mt-4 w-full rounded-full bg-primary px-4 py-2.5 text-sm font-medium text-white hover:opacity-90 transition-opacity disabled:opacity-60"
            >
              {status === "loading" ? "Gerando..." : "Gerar"}
            </button>

            {status === "loading" && (
              <p className="mt-3 flex items-center gap-2 text-sm text-text-secondary">
                <Loader2 size={14} className="animate-spin" /> Gerando arquivo...
              </p>
            )}
            {status === "success" && (
              <p className="mt-3 flex items-center gap-2 text-sm text-success">
                <CheckCircle2 size={14} /> Pronto.
              </p>
            )}
            {status === "error" && (
              <div className="mt-3 flex items-start gap-2 text-sm text-error">
                <AlertCircle size={14} className="mt-0.5 shrink-0" />
                <span>
                  {error}{" "}
                  <button onClick={handleGenerate} className="underline">
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

function FormatButton({
  icon: Icon,
  label,
  active,
  onClick,
}: {
  icon: typeof FileText;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex flex-col items-center gap-1.5 rounded-[var(--radius-md)] border px-2 py-3 text-xs font-medium transition-colors ${
        active ? "border-primary bg-primary-container/30 text-primary" : "border-border-soft text-foreground/70"
      }`}
    >
      <Icon size={18} />
      {label}
    </button>
  );
}

/** Parte 28 ("preview fiel"): a compact, real preview of the layout that will be generated -
 *  same corner-cut frame + accent language as CelestialCard, not a decorative mockup. */
function ExportPreview({ format, theme }: { format: Format; theme: PrintTheme }) {
  const isEconomic = format === "print" && theme === "economic";
  return (
    <div
      className="rounded-[var(--radius-md)] border border-border-soft p-4"
      style={{ background: isEconomic ? "#fff" : "linear-gradient(155deg, var(--surface), var(--surface-elevated))" }}
    >
      <p className="text-[10px] uppercase tracking-wider mb-1" style={{ color: isEconomic ? "#00000080" : "var(--accent-constellation)" }}>
        {format === "csv" ? "planilha.csv" : format === "png" ? "Constelação do Farol · 4:5" : "Constelação do Farol"}
      </p>
      <p className={`font-semibold ${isEconomic ? "text-black" : "text-foreground"}`}>Próxima escala</p>
      <p className={`text-xs mt-1 ${isEconomic ? "text-black/60" : "text-text-secondary"}`}>
        {format === "csv"
          ? "Data, dia, horário, responsáveis por coluna."
          : format === "png"
            ? "Card 1080×1350 com moldura, constelação e responsáveis."
            : "Lista completa do período, com legenda das constelações."}
      </p>
    </div>
  );
}

function downloadText(content: string, filename: string, mime: string) {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
