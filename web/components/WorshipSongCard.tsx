"use client";

import { useState } from "react";
import { PlayCircle, Share2, Copy } from "lucide-react";
import { WorshipSong } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { buildPublicChurchUrl } from "@/lib/publicUrl";
import { copyText, shareContent } from "@/lib/share";

/**
 * Música e Louvor - card no mesmo sistema visual do Card compartilhado (Retrospectiva/Anúncios já
 * usam a mesma base), sem player embutido (Bloco 7/19: "não incorporar player nesta etapa" /
 * "não usar player YouTube na lista") - o clique só abre o YouTube em nova aba.
 */
export function WorshipSongCard({
  song,
  position,
  churchName,
  churchSlug,
}: {
  song: WorshipSong;
  position: number;
  churchName: string;
  churchSlug: string;
}) {
  const [feedback, setFeedback] = useState<string | null>(null);
  const [thumbFailed, setThumbFailed] = useState(false);

  const publicUrl = buildPublicChurchUrl(churchSlug, "/musica");

  async function handleShare() {
    const text = buildWorshipShareMessage({
      title: song.title,
      moment: song.moment_label,
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
    <Card className="overflow-hidden flex flex-col hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      <a
        href={song.youtube_url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Abrir "${song.title}" no YouTube`}
        className="group relative block aspect-video w-full overflow-hidden bg-background"
      >
        {song.thumbnail_url && !thumbFailed ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={song.thumbnail_url}
            alt={`Thumbnail da música ${song.title}`}
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
        <span className="absolute left-2 top-2 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-xs font-semibold text-white">
          {position}
        </span>
        <span className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-opacity duration-200 group-hover:bg-black/20 group-hover:opacity-100">
          <PlayCircle size={36} className="text-white drop-shadow" />
        </span>
      </a>

      <div className="p-4 flex flex-col gap-2">
        <p className="font-semibold leading-tight">{song.title}</p>
        {(song.artist || song.moment_label) && (
          <p className="text-xs text-text-secondary">
            {song.moment_label}
            {song.moment_label && song.artist ? " · " : ""}
            {song.artist}
          </p>
        )}
        {song.notes && <p className="text-sm text-foreground/80 whitespace-pre-line">{song.notes}</p>}

        <div className="mt-1 flex flex-wrap items-center gap-2">
          <a
            href={song.youtube_url}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 rounded-full bg-primary-container px-3 py-1.5 text-xs font-medium text-on-primary-container"
          >
            <PlayCircle size={13} /> Ouvir no YouTube
          </a>
          <button
            onClick={handleCopyLink}
            aria-label="Copiar link da música"
            className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-xs font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
          >
            <Copy size={13} /> Copiar link
          </button>
          <button
            onClick={handleShare}
            aria-label="Compartilhar música"
            className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-xs font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors"
          >
            <Share2 size={13} /> Compartilhar
          </button>
        </div>
        {feedback && <p className="text-xs text-text-secondary">{feedback}</p>}
      </div>
    </Card>
  );
}

function buildWorshipShareMessage({
  title,
  moment,
  youtubeUrl,
  churchName,
  churchCode,
  publicUrl,
}: {
  title: string;
  moment: string;
  youtubeUrl: string;
  churchName: string;
  churchCode: string;
  publicUrl: string;
}): string {
  const lines = [`🎵 ${title}`];
  if (moment.trim()) lines.push("", `Momento: ${moment.trim()}`);
  lines.push(
    "",
    "Ouça aqui:",
    youtubeUrl,
    "",
    `Veja a programação completa da ${churchName}:`,
    publicUrl,
    "",
    "Código da igreja:",
    churchCode,
    "",
    "Compartilhado pelo Escala Church."
  );
  return lines.join("\n");
}
