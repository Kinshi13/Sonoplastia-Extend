"use client";

import { useState } from "react";
import { Sparkles, PlayCircle, Share2, Copy } from "lucide-react";
import { WorshipSong } from "@/lib/types/database";
import { CelestialCard } from "@/components/celestial/CelestialCard";
import { formatDatePt } from "@/lib/format";
import { buildPublicChurchUrl } from "@/lib/publicUrl";
import { copyText, shareContent } from "@/lib/share";

/**
 * Música e Louvor Parte 2 - "Recomendação do dia", visually distinct from the plain grid card
 * (Bloco 9: "mais bonito que o card comum"). Reuses CelestialCard (same glow/frame language as
 * Anúncios/Escala) instead of a one-off design, with `glow="active"` for a constant (not
 * hover-only) highlight and a horizontal layout on desktop.
 */
export function WorshipRecommendationCard({
  song,
  churchName,
  churchSlug,
}: {
  song: WorshipSong;
  churchName: string;
  churchSlug: string;
}) {
  const [feedback, setFeedback] = useState<string | null>(null);
  const [thumbFailed, setThumbFailed] = useState(false);
  const publicUrl = buildPublicChurchUrl(churchSlug, "/musica");

  async function handleShare() {
    const text = buildRecommendationShareMessage({
      title: song.title,
      message: song.recommendation_message,
      youtubeUrl: song.youtube_url,
      churchName,
      churchCode: churchSlug,
      publicUrl,
    });
    const outcome = await shareContent({ title: song.title, text, url: song.youtube_url });
    if (outcome === "unsupported" || outcome === "error") {
      const ok = await copyText(text);
      setFeedback(ok ? "Texto copiado." : "Não foi possível copiar.");
      setTimeout(() => setFeedback(null), 2500);
    }
  }

  async function handleCopyLink() {
    const ok = await copyText(song.youtube_url);
    setFeedback(ok ? "Link copiado." : "Não foi possível copiar.");
    setTimeout(() => setFeedback(null), 2500);
  }

  return (
    <CelestialCard glow="active" id={song.id} className="p-5 sm:p-6">
      <div className="grid grid-cols-1 sm:grid-cols-[minmax(0,260px)_1fr] gap-5 items-center">
        <a
          href={song.youtube_url}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`Abrir "${song.title}" no YouTube`}
          className="group relative block aspect-video w-full overflow-hidden rounded-[var(--radius-md)] bg-background"
        >
          {song.thumbnail_url && !thumbFailed ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={song.thumbnail_url}
              alt={`Thumbnail da recomendação do dia: ${song.title}`}
              loading="lazy"
              width={480}
              height={270}
              onError={() => setThumbFailed(true)}
              className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-text-secondary">
              <PlayCircle size={28} />
            </div>
          )}
          <span className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-opacity duration-200 group-hover:bg-black/20 group-hover:opacity-100">
            <PlayCircle size={36} className="text-white drop-shadow" />
          </span>
        </a>

        <div className="flex flex-col gap-2">
          <span className="flex w-fit items-center gap-1.5 rounded-full bg-primary-container px-3 py-1 text-xs font-medium text-on-primary-container">
            <Sparkles size={12} /> Recomendação do dia
          </span>
          <h3 className="font-display text-xl sm:text-2xl leading-tight">{song.title}</h3>
          {song.artist && <p className="text-sm text-text-secondary">{song.artist}</p>}
          <p className="text-sm text-foreground/90 italic">
            {song.recommendation_message?.trim() || "Ouça esta música para se preparar para a programação."}
          </p>
          {song.recommendation_date && (
            <p className="text-xs text-text-secondary">{formatDatePt(song.recommendation_date)}</p>
          )}

          <div className="mt-1 flex flex-wrap items-center gap-2">
            <a
              href={song.youtube_url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90 transition-opacity"
            >
              <PlayCircle size={15} /> Ouvir no YouTube
            </a>
            <button
              onClick={handleCopyLink}
              aria-label="Copiar link da recomendação"
              className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-2 text-sm font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
            >
              <Copy size={14} /> Copiar link
            </button>
            <button
              onClick={handleShare}
              aria-label="Compartilhar recomendação do dia"
              className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-2 text-sm font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
            >
              <Share2 size={14} /> Compartilhar
            </button>
          </div>
          {feedback && <p className="text-xs text-text-secondary">{feedback}</p>}
        </div>
      </div>
    </CelestialCard>
  );
}

function buildRecommendationShareMessage({
  title,
  message,
  youtubeUrl,
  churchName,
  churchCode,
  publicUrl,
}: {
  title: string;
  message: string | null;
  youtubeUrl: string;
  churchName: string;
  churchCode: string;
  publicUrl: string;
}): string {
  const lines = ["🎵 Recomendação do dia", "", title];
  if (message?.trim()) lines.push("", message.trim());
  lines.push(
    "",
    "Ouça aqui:",
    youtubeUrl,
    "",
    `Veja a programação da ${churchName}:`,
    publicUrl,
    "",
    "Código da igreja:",
    churchCode,
    "",
    "Compartilhado pelo Escala Church."
  );
  return lines.join("\n");
}
