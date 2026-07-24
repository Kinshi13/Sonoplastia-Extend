"use client";

import { useEffect, useRef, useState } from "react";
import { Share2, Copy, Link as LinkIcon, Download, ExternalLink } from "lucide-react";
import { Announcement } from "@/lib/types/database";
import { buildPublicChurchUrl } from "@/lib/publicUrl";
import { IS_IOS, copyText, fetchAsShareableFile, sanitizeFilenameBase, shareContent } from "@/lib/share";

/**
 * Compartilhar anúncio (Anúncios + Escala, Parte 1): builds the message from real announcement
 * data (never a fixed placeholder string), reuses the single public-church-URL builder, and tries
 * navigator.share with the image attached before falling back to a small menu. Any announcement
 * this button receives has already passed through the public page's own query (is_active=true,
 * not expired, correct church) - there is no separate "is this shareable" check here on purpose,
 * since duplicating that filter here could drift out of sync with the real one.
 */
export function AnnouncementShareButton({
  announcement,
  churchName,
  churchSlug,
}: {
  announcement: Announcement;
  churchName: string;
  churchSlug: string;
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) setMenuOpen(false);
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, [menuOpen]);

  useEffect(() => {
    if (!feedback) return;
    const timer = setTimeout(() => setFeedback(null), 2500);
    return () => clearTimeout(timer);
  }, [feedback]);

  const publicUrl = buildPublicChurchUrl(churchSlug, "/anuncios");
  const shareText = buildAnnouncementShareMessage({
    title: announcement.title,
    description: announcement.description,
    churchName,
    churchCode: churchSlug,
    publicUrl,
  });
  const hasImage = announcement.media_type === "IMAGE" && !!announcement.media_url;

  async function handleShareClick() {
    if (busy) return;
    if (typeof navigator === "undefined" || !navigator.share) {
      setMenuOpen(true);
      return;
    }

    setBusy(true);
    try {
      const file = hasImage
        ? await fetchAsShareableFile(
            announcement.media_url!,
            `${sanitizeFilenameBase(announcement.title)}.jpg`
          )
        : null;

      const outcome = await shareContent({
        title: announcement.title,
        text: shareText,
        url: publicUrl,
        files: file ? [file] : undefined,
      });

      if (outcome === "unsupported" || outcome === "error") setMenuOpen(true);
      // "shared" / "shared-without-file" / "cancelled" all need no further action - a cancel is
      // not an error, it's just the user changing their mind.
    } finally {
      setBusy(false);
    }
  }

  async function handleCopyText() {
    const ok = await copyText(shareText);
    setFeedback(ok ? "Texto do anúncio copiado" : "Não foi possível copiar o texto.");
    setMenuOpen(false);
  }

  async function handleCopyLink() {
    const ok = await copyText(publicUrl);
    setFeedback(ok ? "Link do anúncio copiado" : "Não foi possível copiar o link.");
    setMenuOpen(false);
  }

  async function handleDownloadImage() {
    if (!hasImage) return;
    if (IS_IOS) {
      window.open(announcement.media_url!, "_blank", "noopener,noreferrer");
      setMenuOpen(false);
      return;
    }
    try {
      const file = await fetchAsShareableFile(announcement.media_url!, `${sanitizeFilenameBase(announcement.title)}.jpg`);
      if (!file) {
        setFeedback("Não foi possível preparar a imagem.");
        return;
      }
      const objectUrl = URL.createObjectURL(file);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = file.name;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } finally {
      setMenuOpen(false);
    }
  }

  function handleOpenImage() {
    if (!hasImage) return;
    window.open(announcement.media_url!, "_blank", "noopener,noreferrer");
    setMenuOpen(false);
  }

  return (
    <div ref={containerRef} className="relative inline-block">
      <button
        onClick={handleShareClick}
        disabled={busy}
        aria-label="Compartilhar anúncio"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-xs font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors disabled:opacity-60"
      >
        <Share2 size={14} /> Compartilhar
      </button>

      {menuOpen && (
        <div
          role="menu"
          aria-label="Opções de compartilhamento"
          className="absolute right-0 z-20 mt-2 w-56 rounded-[var(--radius-md)] border border-border-soft bg-surface p-1.5 [box-shadow:var(--elevation-overlay)]"
        >
          <MenuItem icon={Copy} label="Copiar texto" onClick={handleCopyText} />
          <MenuItem icon={LinkIcon} label="Copiar link" onClick={handleCopyLink} />
          {hasImage && (
            <>
              <MenuItem icon={Download} label={IS_IOS ? "Abrir imagem" : "Baixar imagem"} onClick={handleDownloadImage} />
              {!IS_IOS && <MenuItem icon={ExternalLink} label="Abrir imagem" onClick={handleOpenImage} />}
            </>
          )}
        </div>
      )}

      {feedback && (
        <p role="status" className="absolute right-0 top-full mt-1 w-max max-w-[220px] text-xs text-text-secondary">
          {feedback}
        </p>
      )}
    </div>
  );
}

function MenuItem({
  icon: Icon,
  label,
  onClick,
}: {
  icon: typeof Copy;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      role="menuitem"
      onClick={onClick}
      className="flex w-full items-center gap-2.5 rounded-[var(--radius-sm)] px-2.5 py-2 text-left text-sm text-foreground/90 hover:bg-primary-container/30"
    >
      <Icon size={15} className="text-text-secondary" />
      {label}
    </button>
  );
}

function buildAnnouncementShareMessage({
  title,
  description,
  churchName,
  churchCode,
  publicUrl,
}: {
  title: string;
  description: string;
  churchName: string;
  churchCode: string;
  publicUrl: string;
}): string {
  const lines = [`📢 ${title}`];
  if (description.trim()) {
    lines.push("", description.trim());
  }
  lines.push(
    "",
    `Veja todos os anúncios e a programação da ${churchName}:`,
    publicUrl,
    "",
    "Código da igreja:",
    churchCode,
    "",
    "Compartilhado pelo Escala Church."
  );
  return lines.join("\n");
}
