/**
 * Fase 11.8.2 (Bloco C): a small "igrejas recentes" list so a returning visitor doesn't have to
 * retype their church's code every time. Stored client-side only (localStorage) - the code itself
 * is a public access identifier, not a secret, so persisting it locally is fine; nothing here is
 * synced anywhere or shared across devices.
 */

export type RecentChurch = {
  churchId: string;
  slug: string;
  churchName: string;
  logoUrl?: string | null;
  lastAccessedAt: number;
  isFavorite?: boolean;
};

const STORAGE_KEY = "escala-church:recent-churches";
const MAX_RECENT = 5;

export function getRecentChurches(): RecentChurch[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return sortRecentChurches(parsed);
  } catch {
    return [];
  }
}

function sortRecentChurches(list: RecentChurch[]): RecentChurch[] {
  return [...list].sort((a, b) => {
    if (!!a.isFavorite !== !!b.isFavorite) return a.isFavorite ? -1 : 1;
    return b.lastAccessedAt - a.lastAccessedAt;
  });
}

/** Called on every successful visit to a church page - adds it to the front of the list, or just
 *  refreshes `lastAccessedAt` when it's already there. */
export function recordChurchAccess(entry: Omit<RecentChurch, "lastAccessedAt" | "isFavorite">): void {
  if (typeof window === "undefined") return;
  const existing = getRecentChurches();
  const previous = existing.find((c) => c.churchId === entry.churchId);
  const next: RecentChurch = {
    ...entry,
    lastAccessedAt: Date.now(),
    isFavorite: previous?.isFavorite ?? false,
  };
  const withoutEntry = existing.filter((c) => c.churchId !== entry.churchId);
  const merged = sortRecentChurches([next, ...withoutEntry]).slice(0, MAX_RECENT);
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(merged));
}

export function forgetChurch(churchId: string): void {
  if (typeof window === "undefined") return;
  const next = getRecentChurches().filter((c) => c.churchId !== churchId);
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
}

export function setChurchFavorite(churchId: string, isFavorite: boolean): void {
  if (typeof window === "undefined") return;
  const next = getRecentChurches().map((c) => (c.churchId === churchId ? { ...c, isFavorite } : c));
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(sortRecentChurches(next)));
}
