export function formatDatePt(isoDate: string): string {
  const date = new Date(`${isoDate}T00:00:00`);
  return date.toLocaleDateString("pt-BR", {
    weekday: "long",
    day: "2-digit",
    month: "long",
  });
}

export function formatTimePt(time: string): string {
  return time.slice(0, 5);
}

export function formatPublishedAt(epochMillis: number): string {
  return new Date(epochMillis).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  });
}
