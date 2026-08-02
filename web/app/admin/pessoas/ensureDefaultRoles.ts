"use server";

import { createClient } from "@/lib/supabase/server";
import { LEGACY_ROLE_DEFS } from "@/lib/legacyRoles";

/**
 * Hotfix: self-heals a church that has zero `organization_roles` rows - either because migrations
 * 006-010 haven't run yet (this silently no-ops, since `organization_roles` doesn't exist to
 * insert into - the client-side fallback in lib/legacyRoles.ts covers the form in the meantime),
 * or because it's a church created *after* those migrations ran (010's own seed only covers
 * churches that existed at the time it ran, so anything newer needs this). Never duplicates - the
 * `not exists` guard mirrors the one in 010_organization_roles_people.sql exactly.
 *
 * Lives in its own module (not `pessoas/actions.ts`) so `admin/actions.ts` can import it without
 * a circular dependency, since `pessoas/actions.ts` itself imports from `admin/actions.ts`.
 */
export async function ensureDefaultRolesAction(churchId: string): Promise<void> {
  const supabase = await createClient();
  const { data: existing, error } = await supabase.from("organization_roles").select("legacy_field_key").eq("church_id", churchId);
  if (error) return; // table doesn't exist yet - nothing to seed into.

  const covered = new Set((existing ?? []).map((r) => r.legacy_field_key));
  const missing = LEGACY_ROLE_DEFS.filter((d) => !covered.has(d.field));
  if (missing.length === 0) return;

  const now = Date.now();
  await supabase.from("organization_roles").insert(
    missing.map((d) => ({
      church_id: churchId,
      name: d.name,
      icon_key: d.iconKey,
      sort_order: d.sortOrder,
      is_required: true,
      legacy_field_key: d.field,
      created_at: now,
      updated_at: now,
    }))
  );
}
