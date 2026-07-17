import { LegacyScaleFieldKey, OrganizationRole, Scale } from "@/lib/types/database";

/**
 * Hotfix: the five original hardcoded functions, usable even when `organization_roles` hasn't
 * been seeded yet (migrations 006-010 not applied) or, for an existing scale, when the migrated
 * role for a legacy field doesn't exist for some other reason. This is what lets "Funções e
 * pessoas" show *something* editable instead of a silently empty section - see ScaleForm.tsx.
 */
export const LEGACY_ROLE_DEFS: { field: LegacyScaleFieldKey; name: string; iconKey: string; sortOrder: number }[] = [
  { field: "reception_person", name: "Recepção", iconKey: "users", sortOrder: 0 },
  { field: "sound_person", name: "Sonoplastia", iconKey: "headphones", sortOrder: 1 },
  { field: "preaching_person", name: "Pregação", iconKey: "book-open", sortOrder: 2 },
  { field: "conducting_person", name: "Regência", iconKey: "music-2", sortOrder: 3 },
  { field: "musical_message_person", name: "Mensagem musical", iconKey: "music", sortOrder: 4 },
];

/** A client-only role id for a legacy field that has no real `organization_roles` row (yet). Never
 *  sent to the database as-is - saveScaleAction recognizes this prefix and resolves the legacy
 *  field key straight from the string instead of looking the id up in the DB. */
export function syntheticRoleId(field: LegacyScaleFieldKey): string {
  return `legacy:${field}`;
}

export function isSyntheticRoleId(roleId: string): LegacyScaleFieldKey | null {
  if (!roleId.startsWith("legacy:")) return null;
  const field = roleId.slice("legacy:".length) as LegacyScaleFieldKey;
  return LEGACY_ROLE_DEFS.some((d) => d.field === field) ? field : null;
}

/** A fully-formed OrganizationRole-shaped object for a legacy field, so the rest of the form
 *  (which only knows how to render real OrganizationRole rows) doesn't need a second code path. */
export function syntheticLegacyRole(churchId: string, field: LegacyScaleFieldKey): OrganizationRole {
  const def = LEGACY_ROLE_DEFS.find((d) => d.field === field)!;
  const now = Date.now();
  return {
    id: syntheticRoleId(field),
    church_id: churchId,
    name: def.name,
    short_name: null,
    description: "",
    icon_key: def.iconKey,
    color_token: null,
    sort_order: def.sortOrder,
    is_active: true,
    is_required: true,
    allows_multiple_people: false,
    team_id: null,
    legacy_field_key: field,
    created_at: now,
    updated_at: now,
    created_by: null,
  };
}

/** Merges whatever real roles came from the DB with synthetic legacy roles for any of the five
 *  original fields that aren't already covered by a real role (matched by legacy_field_key) - the
 *  DB version always wins once it exists, so this becomes a no-op the moment migrations run. */
export function withLegacyRoleFallback(churchId: string, roles: OrganizationRole[]): OrganizationRole[] {
  const coveredFieldKeys = new Set(roles.map((r) => r.legacy_field_key).filter(Boolean));
  const missing = LEGACY_ROLE_DEFS.filter((d) => !coveredFieldKeys.has(d.field)).map((d) => syntheticLegacyRole(churchId, d.field));
  return [...roles, ...missing].sort((a, b) => a.sort_order - b.sort_order);
}

/** Builds rows straight from a scale's own legacy columns (used when no scale_assignments exist
 *  at all for it yet - either an old scale, or any scale saved before migration 010). */
export function legacyRowsFromScale(scale: Scale): { field: LegacyScaleFieldKey; name: string }[] {
  return LEGACY_ROLE_DEFS.filter((d) => (scale[d.field] ?? "").trim().length > 0).map((d) => ({ field: d.field, name: d.name }));
}
