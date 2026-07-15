/**
 * Fase 11.7 (Parte 12-19): the "Stella Integration Layer" - contracts and shapes only. Nothing in
 * this file is called by the app yet; it exists so a future Stella Atlas integration has a
 * settled shape to build against instead of starting from zero, per the Master Plan's own
 * instruction not to build "infraestrutura excessivamente complexa" before the Atlas exists.
 *
 * Design principle (Parte 14): the Atlas must never read or scrape this site's UI/HTML - it only
 * ever consumes events/rows this layer explicitly publishes. See migration 008's
 * `integration_outbox` table for where a future AtlasEventOutbox would write, and
 * supabase/migrations/008_export_and_atlas_prep.sql's comment for why that table is dormant.
 */

// Parte 12: which product surface a feature/event belongs to - lets a FeatureKey (or, later, an
// Atlas event) be scoped to "this only applies on the web", "this is Android-only", or "shared"
// between every Stella app, without inventing a second entitlement system.
export type ProductSurface = "ANDROID" | "WEB" | "ATLAS" | "SHARED";

/** Static reference table (Parte 13) - not enforced anywhere yet (see FEATURE_PRODUCT_SURFACE
 *  usage note below), just the documented mapping a future entitlements check would read. Kept
 *  intentionally incomplete: only listing the keys the spec gave as examples plus the export
 *  keys added this same phase, not force-fitting every existing FeatureKey into a guess. */
export const FEATURE_PRODUCT_SURFACE: Partial<Record<string, ProductSurface>> = {
  VIEW_OFFICIAL_SCALE: "SHARED",
  VIEW_DOXOLOGY: "SHARED",
  VIEW_ANNOUNCEMENTS: "SHARED",
  VIEW_CALENDAR: "SHARED",
  CUSTOM_THEMES: "SHARED",
  ORGANIZATION_BRANDING: "SHARED",
  EXPORT_GENERAL_SCALE: "WEB",
  EXPORT_SCALE_CSV: "WEB",
  EXPORT_SCALE_PDF: "WEB",
  EXPORT_SCALE_IMAGE: "WEB",
  PUBLIC_READONLY_LINK: "WEB",
  ADVANCED_NOTIFICATIONS: "ANDROID",
  PRIORITY_SYNC: "ANDROID",
};

// Parte 15: the app code that produced an event - one value today, more once other Stella
// products exist (this is deliberately not just "ESCALA_CHURCH" - the product is meant to
// outlive this one app's name).
export type AppCode = "STELLA_SCALE";

/** One domain event this app could publish for the Atlas to consume later (Parte 15). Never
 *  includes secrets/tokens/full personal data (Parte 17) - `payload` should carry only what a
 *  cross-app summary view would need (ids, titles, dates), not raw table rows. */
export type StellaEventType =
  | "scale.created"
  | "scale.updated"
  | "scale.deleted"
  | "doxology.created"
  | "doxology.updated"
  | "announcement.published"
  | "event.created"
  | "event.updated"
  | "subscription.changed"
  | "member.assigned"
  | "class.assignment.changed";

export interface StellaDomainEvent<TPayload = Record<string, unknown>> {
  id: string;
  appCode: AppCode;
  organizationId: string;
  eventType: StellaEventType;
  entityType: string;
  entityId: string;
  occurredAt: string; // ISO 8601
  version: number;
  payload: TPayload;
  visibility: "ORGANIZATION" | "PUBLIC_SUMMARY";
  createdBy: string | null;
  processedAt: string | null;
}

/**
 * Parte 14 components - named per the spec, implemented as the minimal stub each needs to exist
 * without pretending to talk to a real Atlas:
 * - AtlasEventPublisher: would translate an app-side change into a StellaDomainEvent and hand it
 *   to the outbox. Not implemented - no call site produces domain events yet.
 * - AtlasEventOutbox: would write into `integration_outbox` (see migration 008). Not implemented.
 * - AtlasIntegrationRepository / StellaAppRegistry / IntegrationPermissionService / AtlasSyncStatus:
 *   left as type-level contracts below for the same reason - building working versions of these
 *   before the Atlas exists to receive anything would be exactly the "infraestrutura excessiva"
 *   the Master Plan says to avoid this phase.
 */
export interface AtlasSyncStatus {
  connected: boolean;
  lastSyncedAt: string | null;
  pendingEvents: number;
  failedEvents: number;
}

export interface IntegrationPermission {
  organizationId: string;
  appCode: AppCode;
  grantedScopes: string[]; // e.g. ["scale:read", "announcement:read"]
  grantedAt: string | null;
}

// Parte 18 - future read-only endpoints for the Atlas, namespaced and versioned from day one.
// Documented, not implemented: no route exists at any of these paths yet.
export const ATLAS_API_CONTRACTS = [
  "GET /api/v1/atlas/organizations/{id}/upcoming",
  "GET /api/v1/atlas/organizations/{id}/calendar",
  "GET /api/v1/atlas/organizations/{id}/announcements",
  "GET /api/v1/atlas/organizations/{id}/changes",
  "GET /api/v1/atlas/organizations/{id}/summary",
] as const;

// Fase 11.8 (Parte 29) - the Celestial Card System's own vocabulary, documented so a future Atlas
// (or any other future Stella surface) can request "a card" and get back something built from the
// same closed set of variants/identities/themes this app already renders, instead of guessing at
// a layout. Mirrors the names already used in code: components/celestial/CelestialCard.tsx
// (CardVariant), components/celestial/DayConstellation.tsx (DayIdentity/ConstellationType), and
// export-actions.ts/export-png.ts (ExportPreset).
export type CardVariant = "hero" | "scale" | "compact" | "doxology" | "announcement" | "export" | "share" | "print";
export type DayIdentity = "WEDNESDAY" | "SATURDAY" | "SUNDAY" | "SPECIAL";
export type ConstellationType = "BEACON" | "CROWN" | "DAWN" | "PILGRIM";
export type ThemeVariant = "celestial" | "economic";
export type ExportPreset = "pdf_monthly" | "print" | "png_card_4x5" | "png_story_9x16" | "png_landscape_16x9" | "csv";
