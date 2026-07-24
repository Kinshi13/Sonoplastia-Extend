/**
 * Correção/expansão de compartilhamento (Anúncios + Escala): a única fonte para montar a URL
 * pública de uma igreja no site - qualquer feature de compartilhamento (anúncio, escala, retro,
 * etc) deve chamar esta função em vez de montar `${origin}/c/${slug}` por conta própria, para que
 * o link nunca divirja entre features.
 *
 * There's no NEXT_PUBLIC_SITE_URL (or equivalent) configured for this project yet, so the origin
 * is taken from the browser at call time - every caller of this function is a client component
 * triggered by a user action (share/copy buttons), never server-rendered share metadata, so
 * `window` is always available when this runs.
 */
export function buildPublicChurchUrl(slug: string, path: string = ""): string {
  const origin = typeof window !== "undefined" ? window.location.origin : "";
  const cleanPath = path && !path.startsWith("/") ? `/${path}` : path;
  return `${origin}/c/${slug}${cleanPath}`;
}
