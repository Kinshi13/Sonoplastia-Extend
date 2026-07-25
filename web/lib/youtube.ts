// Música e Louvor - widened to also accept music.youtube.com/watch?v=, the one shape the
// Retrospectiva-era pattern didn't cover yet. Kept as the single shared parser (not duplicated
// into a separate "YouTubeUrlParser") so every feature that accepts a YouTube link agrees on what
// counts as valid.
const YOUTUBE_ID_PATTERN =
  /(?:(?:music\.)?youtube\.com\/(?:watch\?v=|shorts\/|embed\/)|youtu\.be\/)([a-zA-Z0-9_-]{11})/;

export function extractYouTubeId(url: string): string | null {
  const match = url.match(YOUTUBE_ID_PATTERN);
  return match ? match[1] : null;
}

export function buildYouTubeWatchUrl(videoId: string): string {
  return `https://www.youtube.com/watch?v=${videoId}`;
}

export function youTubeThumbnailUrl(videoId: string): string {
  return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
}

export function youTubeEmbedUrl(videoId: string): string {
  return `https://www.youtube.com/embed/${videoId}`;
}

export type ParsedYouTubeUrl = {
  videoId: string | null;
  normalizedUrl: string | null;
  thumbnailUrl: string | null;
  isValid: boolean;
};

/** "YouTubeUrlParser" (Música e Louvor, Bloco 9) - a single-call wrapper over the functions above,
 *  for callers (MusicaForm) that want the full videoId/normalizedUrl/thumbnailUrl/isValid shape at
 *  once instead of composing the three calls themselves. */
export function parseYouTubeUrl(url: string): ParsedYouTubeUrl {
  const videoId = extractYouTubeId(url.trim());
  if (!videoId) return { videoId: null, normalizedUrl: null, thumbnailUrl: null, isValid: false };
  return {
    videoId,
    normalizedUrl: buildYouTubeWatchUrl(videoId),
    thumbnailUrl: youTubeThumbnailUrl(videoId),
    isValid: true,
  };
}
