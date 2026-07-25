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
