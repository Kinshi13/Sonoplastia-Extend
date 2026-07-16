import { createClient } from "@/lib/supabase/server";

/**
 * Fase 11.8.4 (Parte 9): "recentes" for the PersonSelector, derived straight from the last
 * ScaleAssignments instead of a dedicated analytics table - deliberately the lightest possible
 * implementation of "últimos nomes usados", per the phase's own instruction not to over-build.
 */
export async function recentPersonIdsForChurch(churchId: string): Promise<string[]> {
  const supabase = await createClient();
  const { data } = await supabase
    .from("scale_assignments")
    .select("person_id, created_at, scales!inner(church_id)")
    .eq("scales.church_id", churchId)
    .not("person_id", "is", null)
    .order("created_at", { ascending: false })
    .limit(50);

  const seen = new Set<string>();
  const ordered: string[] = [];
  for (const row of (data as { person_id: string | null }[] | null) ?? []) {
    if (row.person_id && !seen.has(row.person_id)) {
      seen.add(row.person_id);
      ordered.push(row.person_id);
    }
  }
  return ordered.slice(0, 10);
}
