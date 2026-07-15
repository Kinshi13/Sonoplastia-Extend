// Mirrors supabase/schema.sql - kept in snake_case (matching DB columns) to avoid a mapping layer
// for a project this size, same spirit as the Android app's *Dto classes.

export type Church = {
  id: string;
  slug: string;
  name: string;
  is_active: boolean;
  created_at: number;
};

export type ProgramStep = {
  order: number;
  title: string;
  description?: string;
  responsible_person?: string;
  estimated_duration_minutes?: number | null;
};

export type Scale = {
  id: string;
  church_id: string;
  date: string; // ISO date, e.g. "2026-07-05"
  start_time: string; // "HH:MM:SS"
  end_time: string | null;
  type: string;
  title: string;
  reception_person: string;
  sound_person: string;
  preaching_person: string;
  conducting_person: string;
  musical_message_person: string;
  notes: string;
  is_special_event: boolean;
  source_type: string;
  created_at: number;
  updated_at: number;
};

export type Doxology = {
  id: string;
  church_id: string;
  date: string;
  start_time: string;
  end_time: string | null;
  title: string;
  notes: string;
  program_order: ProgramStep[];
  source_type: string;
  created_at: number;
  updated_at: number;
};

export type MediaType = "NONE" | "IMAGE" | "VIDEO" | "DOCUMENT";

export type Announcement = {
  id: string;
  church_id: string;
  title: string;
  description: string;
  media_type: MediaType;
  media_url: string | null;
  media_file_name: string | null;
  image_aspect_ratio: string;
  affected_classes: string[];
  related_event_date: string | null;
  source_type: string;
  published_at: number;
  updated_at: number;
  is_pinned: boolean;
  is_active: boolean;
};

export type RetrospectiveItem = {
  id: string;
  church_id: string;
  title: string;
  description: string;
  media_type: "IMAGE" | "VIDEO" | "YOUTUBE";
  media_url: string;
  media_file_name: string | null;
  media_aspect_ratio: string; // "W:H", e.g. "16:9", "9:16"
  poster_url: string | null;
  event_date: string | null;
  published_at: number;
  updated_at: number;
  is_active: boolean;
};

export type Bulletin = {
  id: string;
  church_id: string;
  title: string;
  pdf_url: string;
  pdf_file_name: string | null;
  cover_url: string | null;
  related_announcement_id: string | null;
  published_at: number;
  updated_at: number;
  is_active: boolean;
};

export type SharedFileMediaType = "IMAGE" | "VIDEO" | "DOCUMENT" | "LINK" | "YOUTUBE";

export type SharedFile = {
  id: string;
  church_id: string;
  file_name: string;
  url: string;
  media_type: SharedFileMediaType;
  size_bytes: number;
  is_pinned: boolean;
  uploaded_at: number;
};

export type Profile = {
  id: string;
  church_id: string | null;
  is_admin: boolean;
  created_at: string;
};

// Fase 3 - plan catalog + per-church subscription. Mirrors 006_plans_entitlements.sql and the
// Android entitlements/ package - see lib/entitlements.ts for the resolved-entitlement logic.
export type PlanCode = "FREE" | "ESSENTIAL" | "PRO" | "ORGANIZATION" | "FOUNDER";

export type SubscriptionStatus =
  | "FREE"
  | "TRIAL"
  | "ACTIVE"
  | "PAST_DUE"
  | "GRACE_PERIOD"
  | "CANCELED"
  | "EXPIRED";

export type FeatureKey =
  | "VIEW_OFFICIAL_SCALE"
  | "VIEW_DOXOLOGY"
  | "VIEW_ANNOUNCEMENTS"
  | "VIEW_CALENDAR"
  | "CLASS_HIGHLIGHTS"
  | "PERSONAL_EVENTS"
  | "PERSONAL_CARDS"
  | "EXTENDED_HISTORY"
  | "ADVANCED_ADMIN"
  | "MULTI_ADMIN"
  | "ADVANCED_NOTIFICATIONS"
  | "CUSTOM_FONTS"
  | "PREMIUM_FONTS"
  | "CUSTOM_THEMES"
  | "PREMIUM_THEMES"
  | "EXPORT"
  | "REPORTS"
  | "ORGANIZATION_BRANDING"
  | "ADVANCED_MEDIA"
  | "PRIORITY_SYNC"
  | "AUTOMATIONS"
  // Fase 11.7 (Parte 10): granular export permissions for the Escala Geral export center.
  // EXPORT_SCALE_PDF/EXPORT_SCALE_IMAGE/PUBLIC_READONLY_LINK are reserved (in the plan catalog,
  // gated by FeatureGate wherever they're checked) but have no shipped feature behind them yet -
  // only EXPORT_GENERAL_SCALE (the export flow itself) and EXPORT_SCALE_CSV are wired to real UI.
  | "EXPORT_GENERAL_SCALE"
  | "EXPORT_SCALE_PDF"
  | "EXPORT_SCALE_IMAGE"
  | "EXPORT_SCALE_CSV"
  | "PUBLIC_READONLY_LINK"
  // Fase 11.8 (Parte 25) - reserved, not wired to any UI yet (see the phase report).
  | "EXPORT_DOXOLOGY"
  | "EXPORT_PRINT"
  | "EXPORT_CUSTOM_BRANDING"
  | "EXPORT_STORY_FORMAT";

export type PlanLimits = {
  maxAdmins: number | null;
  maxPersonalEvents: number | null;
  maxPersonalCards: number | null;
  historyMonths: number | null;
  maxAnnouncements: number | null;
  maxMediaStorageMb: number | null;
  maxOrganizations: number | null;
};

export type Plan = {
  id: string;
  code: PlanCode;
  name: string;
  description: string;
  monthly_price_cents: number | null;
  yearly_price_cents: number | null;
  currency: string;
  billing_period: "recurring" | "one_time";
  is_active: boolean;
  is_public: boolean;
  sort_order: number;
  features: FeatureKey[];
  limits: PlanLimits;
  stripe_price_id_monthly: string | null;
  stripe_price_id_yearly: string | null;
};

export type Subscription = {
  id: string;
  church_id: string;
  plan_id: string;
  status: SubscriptionStatus;
  started_at: number;
  expires_at: number | null;
  trial_ends_at: number | null;
  grace_period_ends_at: number | null;
  source: string;
  stripe_customer_id: string | null;
  stripe_subscription_id: string | null;
  updated_at: number;
};

export const USER_CLASS_LABELS: Record<string, string> = {
  SONOPLASTA: "Sonoplasta",
  REGENTE: "Regente",
  CANTOR: "Cantor(a)",
  PREGADOR: "Pregador(a)",
  RECEPCIONISTA: "Recepcionista",
};
