import { Doxology } from "@/lib/types/database";

/** True when `doxology` is today's session currently in progress (needs both an end_time and
 *  for "now" to fall inside [start_time, end_time]). */
export function isDoxologyLiveNow(doxology: Doxology, now: Date = new Date()): boolean {
  if (!doxology.end_time) return false;

  const todayStr = now.toISOString().slice(0, 10);
  if (doxology.date !== todayStr) return false;

  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const toMinutes = (time: string) => {
    const [h, m] = time.split(":").map(Number);
    return h * 60 + m;
  };

  return nowMinutes >= toMinutes(doxology.start_time) && nowMinutes <= toMinutes(doxology.end_time);
}
