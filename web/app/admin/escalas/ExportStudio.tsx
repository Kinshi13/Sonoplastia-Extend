"use client";

import { useEffect, useState } from "react";
import {
  Download,
  FileText,
  Printer,
  Image as ImageIcon,
  X,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Share2,
  Copy,
  Link as LinkIcon,
} from "lucide-react";
import { exportGeneralScaleCsvAction, getNextScaleForExportAction } from "./export-actions";
import { renderScaleCardPng } from "./export-png";
import { constellationForDay } from "@/components/celestial/DayConstellation";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { buildPublicChurchUrl } from "@/lib/publicUrl";
import { copyText, shareContent } from "@/lib/share";

type Format = "csv" | "print" | "png";
type PrintTheme = "celestial" | "economic";
type Status = "idle" | "loading" | "success" | "error";

/** Correção/expansão de compartilhamento (Parte 20): single source of truth for the generated
 *  share card - the preview, download buttons and share button all read from this one object so
 *  the image, text, link and code can never drift apart from each other. */
type ScheduleShareState = {
  churchName: string;
  churchSlug: string;
  publicUrl: string;
  scheduleTitle: string;
  dateLabel: string;
  timeLabel: string;
  shareText: string;
  blob: Blob;
  previewUrl: string;
  filename: string;
};

function buildScheduleShareMessage({
  churchName,
  scheduleTitle,
  dateLabel,
  timeLabel,
  publicUrl,
  churchCode,
}: {
  churchName: string;
  scheduleTitle: string;
  dateLabel: string;
  timeLabel: string;
  publicUrl: string;
  churchCode: string;
}): string {
  const programLine = scheduleTitle.trim() || "Próxima programação";
  const lines = [
    `📅 Escala da ${churchName}`,
    "",
    programLine,
    timeLabel ? `${dateLabel} — ${timeLabel}` : dateLabel,
    "",
    "Confira a escala completa, anúncios e demais informações:",
    publicUrl,
    "",
    "Código da igreja:",
    churchCode,
    "",
    "Compartilhado pelo Escala Church.",
  ];
  return lines.join("\n");
}

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
  const [share, setShare] = useState<ScheduleShareState | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  // The preview <img> uses an object URL - revoke it whenever we replace or discard the state so
  // repeated "Gerar" clicks in one session don't leak blob URLs.
  useEffect(() => {
    return () => {
      if (share) URL.revokeObjectURL(share.previewUrl);
    };
  }, [share]);

  useEffect(() => {
    if (!feedback) return;
    const timer = setTimeout(() => setFeedback(null), 2500);
    return () => clearTimeout(timer);
  }, [feedback]);

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
    const scale = result.scale;
    const churchName = result.churchName || "";
    const churchSlug = result.churchSlug || "";
    const publicUrl = buildPublicChurchUrl(churchSlug);
    const dateLabel = formatDatePt(scale.date);
    const timeLabel = `${formatTimePt(scale.start_time)}${scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}`;

    const kind = constellationForDay(new Date(`${scale.date}T00:00:00`).getDay(), scale.is_special_event);
    let blob: Blob;
    try {
      blob = await renderScaleCardPng(scale, kind, churchName, formatDatePt, formatTimePt, {
        publicUrl,
        churchCode: churchSlug,
      });
    } catch {
      setStatus("error");
      setError("Não foi possível preparar a imagem.");
      return;
    }

    if (share) URL.revokeObjectURL(share.previewUrl);
    setShare({
      churchName,
      churchSlug,
      publicUrl,
      scheduleTitle: scale.title,
      dateLabel,
      timeLabel,
      shareText: buildScheduleShareMessage({
        churchName,
        scheduleTitle: scale.title,
        dateLabel,
        timeLabel,
        publicUrl,
        churchCode: churchSlug,
      }),
      blob,
      previewUrl: URL.createObjectURL(blob),
      filename: `escala-${churchSlug}-${scale.date}.png`,
    });
    setStatus("success");
  }

  function handleDownloadPng() {
    if (!share) return;
    const link = document.createElement("a");
    link.href = share.previewUrl;
    link.download = share.filename;
    link.click();
  }

  async function handleSharePng() {
    if (!share) return;
    const file = new File([share.blob], share.filename, { type: share.blob.type });
    const outcome = await shareContent({
      title: `Escala da ${share.churchName}`,
      text: share.shareText,
      url: share.publicUrl,
      files: [file],
    });
    if (outcome === "unsupported") setFeedback("Este navegador não permite compartilhar a imagem diretamente. Use Baixar PNG.");
    else if (outcome === "error") setFeedback("Não foi possível compartilhar. Tente baixar a imagem.");
    else if (outcome === "shared" || outcome === "shared-without-file") setFeedback("Imagem compartilhada.");
    // "cancelled" needs no message - the user just changed their mind.
  }

  async function handleCopy(kind: "text" | "link" | "code") {
    if (!share) return;
    const value = kind === "text" ? share.shareText : kind === "link" ? share.publicUrl : share.churchSlug;
    const ok = await copyText(value);
    setFeedback(
      ok
        ? kind === "text"
          ? "Texto copiado."
          : kind === "link"
            ? "Link copiado."
            : "Código copiado."
        : "Não foi possível copiar."
    );
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
              <FormatButton
                icon={FileText}
                label="CSV"
                active={format === "csv"}
                onClick={() => {
                  setFormat("csv");
                  setStatus("idle");
                }}
              />
              <FormatButton
                icon={Printer}
                label="PDF / Impressão"
                active={format === "print"}
                onClick={() => {
                  setFormat("print");
                  setStatus("idle");
                }}
              />
              <FormatButton
                icon={ImageIcon}
                label="PNG (próxima)"
                active={format === "png"}
                onClick={() => {
                  setFormat("png");
                  setStatus("idle");
                }}
              />
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

            {format === "png" && share && status === "success" && (
              <div className="mt-4 flex flex-col gap-3 border-t border-border-soft pt-4">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={share.previewUrl}
                  alt={`Prévia do card da escala: ${share.scheduleTitle}`}
                  className="w-full max-w-[220px] self-center rounded-[var(--radius-md)] border border-border-soft"
                />
                <p className="whitespace-pre-line rounded-[var(--radius-md)] bg-background p-3 text-xs text-text-secondary">
                  {share.shareText}
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <ActionButton icon={Share2} label="Compartilhar" onClick={handleSharePng} primary />
                  <ActionButton icon={Download} label="Baixar PNG" onClick={handleDownloadPng} />
                  <ActionButton icon={Copy} label="Copiar texto" onClick={() => handleCopy("text")} />
                  <ActionButton icon={LinkIcon} label="Copiar link" onClick={() => handleCopy("link")} />
                  <ActionButton icon={Copy} label="Copiar código" onClick={() => handleCopy("code")} />
                </div>
                {feedback && (
                  <p role="status" className="text-center text-xs text-text-secondary">
                    {feedback}
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function ActionButton({
  icon: Icon,
  label,
  onClick,
  primary,
}: {
  icon: typeof Download;
  label: string;
  onClick: () => void;
  primary?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-center gap-1.5 rounded-full px-3 py-2 text-xs font-medium transition-colors ${
        primary
          ? "bg-primary text-white hover:opacity-90"
          : "border border-border-soft text-foreground/80 hover:border-primary hover:text-primary"
      }`}
    >
      <Icon size={14} /> {label}
    </button>
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
