"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, Download, X, Share2, Video as VideoIcon } from "lucide-react";
import { RetrospectiveItem } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
import { extractYouTubeId, youTubeEmbedUrl } from "@/lib/youtube";

/** Correção pré-lançamento: safe, readable download filenames - strips anything that isn't
 *  alphanumeric/dash, collapses runs, and keeps the original extension when we have one. */
function safeDownloadFilename(item: RetrospectiveItem): string {
  const base = (item.title || "retrospectiva")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  const dateSuffix = item.event_date || new Date(item.published_at).toISOString().slice(0, 10);
  const sourceExt = item.media_file_name?.split(".").pop();
  const urlExt = item.media_url.split(/[?#]/)[0].split(".").pop();
  const ext = (sourceExt || urlExt || "jpg").toLowerCase().slice(0, 5);
  return `${base || "retrospectiva"}-${dateSuffix}.${ext}`;
}

const IS_IOS =
  typeof navigator !== "undefined" && /iPad|iPhone|iPod/.test(navigator.userAgent) && !("MSStream" in window);

export function RetrospectivaGrid({ items }: { items: RetrospectiveItem[] }) {
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  return (
    <>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
        {items.map((item, index) => (
          <button
            key={item.id}
            onClick={() => setSelectedIndex(index)}
            className="group relative aspect-4/3 w-full overflow-hidden rounded-2xl border border-divider bg-surface shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-0.5"
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={item.media_type === "IMAGE" ? item.media_url : (item.poster_url ?? item.media_url)}
              alt={item.title ? `Foto da retrospectiva: ${item.title}` : "Foto da retrospectiva"}
              loading="lazy"
              className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
            />
            {(item.media_type === "VIDEO" || item.media_type === "YOUTUBE") && (
              <span className="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-black/50 text-white">
                <VideoIcon size={14} />
              </span>
            )}
            {item.title && (
              <span className="absolute inset-x-0 bottom-0 bg-linear-to-t from-black/60 to-transparent px-2 pb-1.5 pt-4 text-left text-xs font-medium text-white truncate">
                {item.title}
              </span>
            )}
          </button>
        ))}
      </div>

      {selectedIndex !== null && (
        <Lightbox
          items={items}
          index={selectedIndex}
          onClose={() => setSelectedIndex(null)}
          onNavigate={(nextIndex) => setSelectedIndex(nextIndex)}
        />
      )}
    </>
  );
}

function Lightbox({
  items,
  index,
  onClose,
  onNavigate,
}: {
  items: RetrospectiveItem[];
  index: number;
  onClose: () => void;
  onNavigate: (index: number) => void;
}) {
  const item = items[index];
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const aspectRatio = item.media_aspect_ratio.replace(":", " / ");
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const touchStartX = useRef<number | null>(null);
  const hasPrev = index > 0;
  const hasNext = index < items.length - 1;

  // Focus goes to the close button on open so keyboard/screen-reader users land somewhere
  // useful immediately, and the browser Back button / Escape both still close it - no focus trap.
  useEffect(() => {
    closeButtonRef.current?.focus();
  }, []);

  // Background scroll is locked only while the viewer is open, and always restored on close -
  // including on unmount from a route change, so a stuck lock can never survive this component.
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
      else if (event.key === "ArrowLeft" && hasPrev) onNavigate(index - 1);
      else if (event.key === "ArrowRight" && hasNext) onNavigate(index + 1);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [index, hasPrev, hasNext, onClose, onNavigate]);

  async function handleShare() {
    const shareData = {
      title: item.title || "Retrospectiva",
      text: item.title || "Confira essa foto/vídeo da igreja",
      url: item.media_url,
    };
    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch {
        // user cancelled the native share sheet - nothing to do
      }
      return;
    }
    await navigator.clipboard.writeText(item.media_url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  function handleWhatsApp() {
    window.open(`https://wa.me/?text=${encodeURIComponent(item.media_url)}`, "_blank", "noopener,noreferrer");
  }

  // Fetches the original (never the thumbnail/poster) as a blob so the browser saves it under a
  // clean filename instead of navigating to the image URL. iOS Safari still won't trigger a real
  // "save" this way (it has no download-blob API for images) - the caption near the button is the
  // honest fallback rather than promising a save that can't happen there.
  async function handleDownload() {
    if (downloading) return;
    // Safari on iOS has no reliable way to trigger a real file save from JS - it either ignores
    // the download attribute or just navigates. Opening the original in a new tab plus the
    // "segure e salve" caption below is the honest option there, not a fake "download" that
    // silently does nothing.
    if (IS_IOS) {
      window.open(item.media_url, "_blank", "noopener,noreferrer");
      return;
    }
    setDownloading(true);
    try {
      const response = await fetch(item.media_url);
      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = safeDownloadFilename(item);
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch {
      // Fall back to opening the original in a new tab - still lets the visitor save it manually.
      window.open(item.media_url, "_blank", "noopener,noreferrer");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-black/90 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={item.title ? `Visualizador de foto: ${item.title}` : "Visualizador de foto"}
    >
      <button
        ref={closeButtonRef}
        onClick={onClose}
        aria-label="Fechar visualizador"
        className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20"
      >
        <X size={20} />
      </button>

      {items.length > 1 && (
        <p className="absolute left-4 top-4 rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-white/80">
          {index + 1} de {items.length}
        </p>
      )}

      {hasPrev && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onNavigate(index - 1);
          }}
          aria-label="Foto anterior"
          className="absolute left-2 top-1/2 hidden -translate-y-1/2 h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 sm:flex"
        >
          <ChevronLeft size={22} />
        </button>
      )}
      {hasNext && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onNavigate(index + 1);
          }}
          aria-label="Próxima foto"
          className="absolute right-2 top-1/2 hidden -translate-y-1/2 h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 sm:flex"
        >
          <ChevronRight size={22} />
        </button>
      )}

      <div
        onClick={(e) => e.stopPropagation()}
        onTouchStart={(e) => {
          touchStartX.current = e.changedTouches[0].clientX;
        }}
        onTouchEnd={(e) => {
          if (touchStartX.current === null) return;
          const deltaX = e.changedTouches[0].clientX - touchStartX.current;
          touchStartX.current = null;
          const SWIPE_THRESHOLD = 50;
          if (deltaX > SWIPE_THRESHOLD && hasPrev) onNavigate(index - 1);
          else if (deltaX < -SWIPE_THRESHOLD && hasNext) onNavigate(index + 1);
        }}
        className="flex max-h-[85vh] max-w-full flex-col gap-3"
      >
        {item.media_type === "YOUTUBE" ? (
          (() => {
            const videoId = extractYouTubeId(item.media_url);
            return videoId ? (
              <iframe
                src={`${youTubeEmbedUrl(videoId)}?autoplay=1`}
                title={item.title || "Retrospectiva"}
                allow="autoplay; encrypted-media; picture-in-picture"
                allowFullScreen
                style={{ aspectRatio }}
                className="h-[75vh] max-h-[75vh] w-auto max-w-full rounded-lg"
              />
            ) : null;
          })()
        ) : item.media_type === "VIDEO" ? (
          <video
            src={item.media_url}
            controls
            autoPlay
            playsInline
            style={{ aspectRatio }}
            className="max-h-[75vh] w-auto max-w-full rounded-lg object-contain"
          />
        ) : (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={item.media_url}
            alt={item.title || "Retrospectiva"}
            style={{ aspectRatio }}
            className="max-h-[75vh] w-auto max-w-full rounded-lg object-contain"
          />
        )}

        <div onClick={(e) => e.stopPropagation()} className="flex flex-col gap-2 text-white">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              {item.title && <p className="truncate font-medium">{item.title}</p>}
              <p className="text-xs text-white/60">{formatPublishedAt(item.published_at)}</p>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              {item.media_type === "IMAGE" && (
                <button
                  onClick={handleDownload}
                  disabled={downloading}
                  aria-label={IS_IOS ? "Abrir foto original" : "Baixar foto"}
                  className="flex items-center gap-1.5 rounded-full bg-white/10 px-4 py-2 text-sm font-medium hover:bg-white/20 disabled:opacity-60"
                >
                  <Download size={15} /> {downloading ? "Baixando..." : IS_IOS ? "Abrir foto" : "Baixar"}
                </button>
              )}
              <button
                onClick={handleWhatsApp}
                className="rounded-full bg-white/10 px-4 py-2 text-sm font-medium hover:bg-white/20"
              >
                WhatsApp
              </button>
              <button
                onClick={handleShare}
                className="flex items-center gap-1.5 rounded-full bg-white/10 px-4 py-2 text-sm font-medium hover:bg-white/20"
              >
                <Share2 size={15} /> {copied ? "Link copiado!" : "Compartilhar"}
              </button>
            </div>
          </div>
          {item.media_type === "IMAGE" && IS_IOS && (
            <p className="text-xs text-white/50">
              No iPhone, o botão &quot;Baixar&quot; abre a foto - toque e segure a imagem e escolha &quot;Salvar em
              Fotos&quot; para guardá-la.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
