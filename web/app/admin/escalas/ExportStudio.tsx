"use client";

import { useEffect, useRef, useState } from "react";
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

  const openButtonRef = useRef<HTMLButtonElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const resultRef = useRef<HTMLDivElement>(null);

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

  // Hotfix (modal mobile): background can't scroll while the modal is open - on a short phone
  // screen a scrollable body behind a partially-covering overlay was one more way for the real
  // action bar to end up somewhere the user couldn't reach. Focus also moves into the dialog on
  // open and back to the "Exportar" button on close, per the accessibility requirements below.
  useEffect(() => {
    if (!open) return;
    const previouslyFocused = (document.activeElement as HTMLElement | null) ?? openButtonRef.current;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    closeButtonRef.current?.focus();

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        return;
      }
      if (event.key !== "Tab" || !dialogRef.current) return;
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKeyDown);
      previouslyFocused?.focus();
    };
  }, [open]);

  // Once the PNG + share text are ready, bring the result (and the sticky "Compartilhar" bar
  // sitting right below it) into view instead of leaving the user scrolled up at the format
  // picker - respects prefers-reduced-motion by skipping the smooth animation, not the scroll.
  useEffect(() => {
    if (!share || status !== "success") return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    resultRef.current?.scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth", block: "start" });
  }, [share, status]);

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

  const pngReady = format === "png" && share && status === "success";

  return (
    <>
      <button
        ref={openButtonRef}
        onClick={() => setOpen(true)}
        className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
      >
        <Download size={16} /> Exportar
      </button>

      {open && (
        // Hotfix (modal mobile): this overlay sits at --z-modal (50), above both --z-dock (30) and
        // --z-core-trigger (38) - previously it matched the dock's z-30 exactly, so on mobile
        // whichever of the two mounted later in the DOM (the portaled dock) painted on top,
        // burying the action bar underneath the bottom navigation and the Stella Core button.
        <div
          className="fixed inset-0 flex items-end justify-center bg-black/40 sm:items-center sm:p-4"
          style={{ zIndex: "var(--z-modal)" }}
          onClick={() => setOpen(false)}
          role="presentation"
        >
          <div
            ref={dialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="export-studio-title"
            onClick={(e) => e.stopPropagation()}
            className="flex w-full flex-col overflow-hidden rounded-t-[var(--radius-lg)] border border-border-soft bg-surface [box-shadow:var(--elevation-overlay)] sm:max-w-md sm:rounded-[var(--radius-lg)]"
            style={{ height: "min(88dvh, 640px)", maxHeight: "calc(100dvh - env(safe-area-inset-top) - 16px)" }}
          >
            {/* Header - compact, never scrolls away */}
            <div className="flex shrink-0 items-center justify-between border-b border-border-soft px-5 py-3.5">
              <h2 id="export-studio-title" className="font-display text-base">
                Exportar escala
              </h2>
              <button
                ref={closeButtonRef}
                onClick={() => setOpen(false)}
                aria-label="Fechar"
                className="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary hover:text-foreground hover:bg-background"
              >
                <X size={18} />
              </button>
            </div>

            {/* Scrollable content - the only thing that scrolls; the page behind the modal is
                locked, and the action bar below stays fixed to the modal's own bottom. */}
            <div className="flex-1 overflow-y-auto overscroll-contain px-5 py-4" style={{ WebkitOverflowScrolling: "touch" }}>
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

              {pngReady && share && (
                <div ref={resultRef} className="mt-4 flex flex-col gap-3 border-t border-border-soft pt-4">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={share.previewUrl}
                    alt={`Prévia do card da escala: ${share.scheduleTitle}`}
                    className="max-h-[38vh] w-auto max-w-full self-center rounded-[var(--radius-md)] border border-border-soft object-contain"
                  />
                  <p className="whitespace-pre-line break-words rounded-[var(--radius-md)] bg-background p-3 text-xs text-text-secondary">
                    {share.shareText}
                  </p>
                </div>
              )}
            </div>

            {/* Sticky action bar - always the last thing pinned to the bottom of the modal itself
                (not the viewport), padded for the iPhone home-indicator safe area, so it's the one
                thing guaranteed reachable no matter how tall the scrollable content above gets. */}
            <div
              className="shrink-0 border-t border-border-soft bg-surface px-5 pt-3"
              style={{ paddingBottom: "calc(env(safe-area-inset-bottom) + 12px)" }}
            >
              {status === "loading" && (
                <p className="mb-2 flex items-center gap-2 text-sm text-text-secondary">
                  <Loader2 size={14} className="animate-spin" /> Gerando arquivo...
                </p>
              )}
              {feedback && (
                <p role="status" className="mb-2 text-center text-xs text-text-secondary">
                  {feedback}
                </p>
              )}
              {status === "success" && !pngReady && (
                <p className="mb-2 flex items-center gap-2 text-sm text-success">
                  <CheckCircle2 size={14} /> Pronto.
                </p>
              )}

              {pngReady ? (
                <div className="flex flex-col gap-2">
                  <ActionButton icon={Share2} label="Compartilhar" onClick={handleSharePng} primary />
                  <div className="grid grid-cols-3 gap-2">
                    <ActionButton icon={Download} label="Baixar PNG" onClick={handleDownloadPng} />
                    <ActionButton icon={Copy} label="Copiar texto" onClick={() => handleCopy("text")} />
                    <ActionButton icon={LinkIcon} label="Copiar link" onClick={() => handleCopy("link")} />
                  </div>
                  <ActionButton icon={Copy} label="Copiar código" onClick={() => handleCopy("code")} />
                </div>
              ) : (
                <button
                  onClick={handleGenerate}
                  disabled={status === "loading"}
                  className="w-full rounded-full bg-primary px-4 py-2.5 text-sm font-medium text-white hover:opacity-90 transition-opacity disabled:opacity-60"
                  style={{ minHeight: 44 }}
                >
                  {status === "loading" ? "Gerando..." : "Gerar"}
                </button>
              )}
            </div>
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
      style={{ minHeight: 44 }}
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
