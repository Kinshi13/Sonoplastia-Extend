"use client";

import { useState } from "react";
import { X, Share2, Video as VideoIcon } from "lucide-react";
import { RetrospectiveItem } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
import { extractYouTubeId, youTubeEmbedUrl } from "@/lib/youtube";

export function RetrospectivaGrid({ items }: { items: RetrospectiveItem[] }) {
  const [selected, setSelected] = useState<RetrospectiveItem | null>(null);

  return (
    <>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {items.map((item) => (
          <button
            key={item.id}
            onClick={() => setSelected(item)}
            className="group relative aspect-4/3 w-full overflow-hidden rounded-2xl border border-divider bg-surface shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-0.5"
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={item.media_type === "IMAGE" ? item.media_url : (item.poster_url ?? item.media_url)}
              alt={item.title || "Retrospectiva"}
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

      {selected && <Lightbox item={selected} onClose={() => setSelected(null)} />}
    </>
  );
}

function Lightbox({ item, onClose }: { item: RetrospectiveItem; onClose: () => void }) {
  const [copied, setCopied] = useState(false);
  const aspectRatio = item.media_aspect_ratio.replace(":", " / ");

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

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-black/90 p-4"
      onClick={onClose}
    >
      <button
        onClick={onClose}
        className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20"
      >
        <X size={20} />
      </button>

      <div onClick={(e) => e.stopPropagation()} className="flex max-h-[85vh] max-w-full flex-col gap-3">
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

        <div onClick={(e) => e.stopPropagation()} className="flex items-center justify-between gap-3 text-white">
          <div className="min-w-0">
            {item.title && <p className="truncate font-medium">{item.title}</p>}
            <p className="text-xs text-white/60">{formatPublishedAt(item.published_at)}</p>
          </div>
          <div className="flex shrink-0 items-center gap-2">
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
      </div>
    </div>
  );
}
