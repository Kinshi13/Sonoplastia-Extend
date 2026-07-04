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

export const USER_CLASS_LABELS: Record<string, string> = {
  SONOPLASTA: "Sonoplasta",
  REGENTE: "Regente",
  CANTOR: "Cantor(a)",
  PREGADOR: "Pregador(a)",
  RECEPCIONISTA: "Recepcionista",
};
