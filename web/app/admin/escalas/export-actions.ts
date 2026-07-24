"use server";

import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { getEntitlements } from "@/lib/entitlements";
import { Scale } from "@/lib/types/database";
import { formatTimePt } from "@/lib/format";

export type ExportResult = { csv?: string; filename?: string; error?: string };
export type ScaleForExportResult = { scale?: Scale; churchName?: string; churchSlug?: string; error?: string };

const ROLE_COLUMNS: { key: keyof Scale; label: string }[] = [
  { key: "reception_person", label: "Recepção" },
  { key: "sound_person", label: "Sonoplastia" },
  { key: "preaching_person", label: "Pregação" },
  { key: "conducting_person", label: "Regência" },
  { key: "musical_message_person", label: "Mensagem musical" },
];

function csvEscape(value: string): string {
  if (/[",\n]/.test(value)) return `"${value.replace(/"/g, '""')}"`;
  return value;
}

/**
 * Fase 11.7 (Parte 10-11): CSV export for the Escala Geral - admin-only, gated behind the
 * EXPORT_SCALE_CSV FeatureKey (not just an admin check - a Free-plan admin still can't export,
 * per "não depender apenas do botão escondido"). Every call is logged to export_audit_log
 * (Parte 10 - "registrar usuário, organização, formato, período, data, filtros").
 */
export async function exportGeneralScaleCsvAction(month?: string): Promise<ExportResult> {
  const { user, isAdmin, churchId } = await getAdminStatus();
  if (!user || !isAdmin || !churchId) return { error: "Apenas administradores podem exportar a escala." };

  const entitlements = await getEntitlements(churchId);
  if (!entitlements.features.has("EXPORT_SCALE_CSV")) {
    return { error: "Exportar em CSV faz parte de um plano superior. Veja Planos e recursos." };
  }

  const supabase = await createClient();
  let query = supabase.from("scales").select("*").eq("church_id", churchId).order("date", { ascending: true });
  if (month) {
    query = query.gte("date", `${month}-01`).lt("date", nextMonthIso(month));
  }
  const { data, error } = await query;
  if (error) return { error: error.message };
  const items = (data as Scale[]) ?? [];

  const header = ["Data", "Dia", "Horário", "Título", "Tipo", "Especial", ...ROLE_COLUMNS.map((r) => r.label), "Observações"];
  const rows = items.map((scale) => {
    const weekday = new Date(`${scale.date}T00:00:00`).toLocaleDateString("pt-BR", { weekday: "long" });
    return [
      scale.date,
      weekday,
      `${formatTimePt(scale.start_time)}${scale.end_time ? ` - ${formatTimePt(scale.end_time)}` : ""}`,
      scale.title,
      scale.source_type,
      scale.is_special_event ? "Sim" : "Não",
      ...ROLE_COLUMNS.map((r) => (scale[r.key] as string) ?? ""),
      scale.notes ?? "",
    ]
      .map((cell) => csvEscape(String(cell)))
      .join(",");
  });
  const csv = "﻿" + [header.join(","), ...rows].join("\n");

  await supabase.from("export_audit_log").insert({
    church_id: churchId,
    user_id: user.id,
    format: "csv",
    period: month ?? "all",
    filters: {},
    created_at: Date.now(),
  });

  return { csv, filename: `escala-geral${month ? `-${month}` : ""}.csv` };
}

function nextMonthIso(month: string): string {
  const [y, m] = month.split("-").map(Number);
  const next = m === 12 ? `${y + 1}-01` : `${y}-${String(m + 1).padStart(2, "0")}`;
  return `${next}-01`;
}

/**
 * Fase 11.8 (Parte 20): fetches the next upcoming scale for the PNG share-card export, gated by
 * EXPORT_SCALE_IMAGE specifically (separate from CSV/print's keys, per Parte 25's "granular
 * FeatureKeys"). The actual PNG drawing happens client-side (see export-png.ts) since it's a
 * canvas render, not something a server action can return as a file directly - this action's job
 * is just to authorize the request and hand back the data to draw, plus log the audit entry.
 */
export async function getNextScaleForExportAction(): Promise<ScaleForExportResult> {
  const { user, isAdmin, churchId } = await getAdminStatus();
  if (!user || !isAdmin || !churchId) return { error: "Apenas administradores podem exportar a escala." };

  const entitlements = await getEntitlements(churchId);
  if (!entitlements.features.has("EXPORT_SCALE_IMAGE")) {
    return { error: "Exportar como imagem faz parte de um plano superior. Veja Planos e recursos." };
  }

  const supabase = await createClient();
  const today = new Date().toISOString().slice(0, 10);
  const [{ data, error }, { data: church }] = await Promise.all([
    supabase
      .from("scales")
      .select("*")
      .eq("church_id", churchId)
      .gte("date", today)
      .order("date", { ascending: true })
      .order("start_time", { ascending: true })
      .limit(1)
      .maybeSingle(),
    supabase.from("churches").select("name, slug").eq("id", churchId).single(),
  ]);
  if (error) return { error: error.message };
  if (!data) return { error: "Nenhuma escala futura para exportar." };

  await supabase.from("export_audit_log").insert({
    church_id: churchId,
    user_id: user.id,
    format: "image",
    period: "next",
    filters: {},
    created_at: Date.now(),
  });

  return { scale: data as Scale, churchName: church?.name ?? "", churchSlug: church?.slug ?? "" };
}

/** Used by the print view to confirm access + log the export before rendering (Parte 11: the
 *  print flow counts as a "PDF" in practice via the browser's own print-to-PDF). */
export async function logScalePrintExportAction(month?: string): Promise<{ ok: boolean }> {
  const { user, isAdmin, churchId } = await getAdminStatus();
  if (!user || !isAdmin || !churchId) return { ok: false };
  const entitlements = await getEntitlements(churchId);
  if (!entitlements.features.has("EXPORT_GENERAL_SCALE")) return { ok: false };

  const supabase = await createClient();
  await supabase.from("export_audit_log").insert({
    church_id: churchId,
    user_id: user.id,
    format: "print",
    period: month ?? "all",
    filters: {},
    created_at: Date.now(),
  });
  return { ok: true };
}
