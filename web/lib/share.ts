/**
 * Correção/expansão de compartilhamento (Anúncios + Escala): shared Web Share API helpers -
 * pulled out now that two features (announcement share, schedule share) both need the same
 * "try navigator.share, fall back to clipboard/WhatsApp/download" logic that used to live only,
 * inline, inside RetrospectivaGrid's lightbox.
 */

export const IS_IOS =
  typeof navigator !== "undefined" && /iPad|iPhone|iPod/.test(navigator.userAgent) && !("MSStream" in window);

export type ShareOutcome = "shared" | "shared-without-file" | "cancelled" | "unsupported" | "error";

/**
 * Tries navigator.share with a file attached first (when the browser claims support via
 * navigator.canShare), then retries text-only if the file attempt itself fails (some browsers
 * accept the file in canShare() but still throw on share() for certain MIME types/sizes) - only
 * after both attempts fail does this report "unsupported", so a caller's fallback menu only shows
 * up when there's genuinely nothing else to try.
 */
export async function shareContent({
  title,
  text,
  url,
  files,
}: {
  title: string;
  text: string;
  url?: string;
  files?: File[];
}): Promise<ShareOutcome> {
  if (typeof navigator === "undefined" || !navigator.share) return "unsupported";

  const canShareFiles = !!files?.length && navigator.canShare?.({ files });

  if (canShareFiles) {
    try {
      await navigator.share({ title, text, url, files });
      return "shared";
    } catch (err) {
      if (isAbort(err)) return "cancelled";
      // fall through to a text-only retry below
    }
  }

  try {
    await navigator.share({ title, text, url });
    return canShareFiles ? "shared-without-file" : "shared";
  } catch (err) {
    if (isAbort(err)) return "cancelled";
    return "error";
  }
}

function isAbort(err: unknown): boolean {
  return err instanceof DOMException && err.name === "AbortError";
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

export function buildWhatsAppLink(text: string): string {
  return `https://wa.me/?text=${encodeURIComponent(text)}`;
}

/** Fetches a remote image as a File for navigator.share - returns null (never throws) so callers
 *  can always fall back to a text-only share when the image is unreachable (CORS, offline, 404). */
export async function fetchAsShareableFile(url: string, filename: string): Promise<File | null> {
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const blob = await response.blob();
    return new File([blob], filename, { type: blob.type || "image/jpeg" });
  } catch {
    return null;
  }
}

/** Safe, readable filename fragment: strips accents/symbols, collapses to single dashes. */
export function sanitizeFilenameBase(base: string): string {
  return (
    base
      .normalize("NFD")
      .replace(/[̀-ͯ]/g, "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "") || "compartilhado"
  );
}
