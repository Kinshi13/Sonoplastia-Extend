"use server";

import { createClient } from "@/lib/supabase/server";

export type SubscribeResult = { error?: string; ok?: boolean };

/**
 * Notificações (base) - saves a browser's Push subscription so a future sending job has
 * something to read. No visitor login exists on the public site, so this is intentionally
 * callable by anyone - but goes through the `upsert_web_push_subscription` RPC (security
 * definer, see supabase/manual/APPLY_PENDING_014_016_SAFE_V2.sql) instead of a raw table
 * write: a security review found that a raw `insert/update` grant plus an RLS policy scoped
 * only to "church is active" (not to the caller's own endpoint) would let anyone update/disable
 * any subscription in any active church, not just their own. The RPC only ever touches the row
 * matching the `endpoint` parameter (unique per browser install), so re-subscribing (e.g. after
 * a permission reset) upserts the same row instead of accumulating duplicates or touching
 * anyone else's.
 */
export async function saveWebPushSubscriptionAction(input: {
  churchId: string;
  endpoint: string;
  p256dh: string;
  auth: string;
  userAgent?: string;
  platform?: string;
  topics?: string[];
}): Promise<SubscribeResult> {
  if (!input.churchId || !input.endpoint || !input.p256dh || !input.auth) {
    return { error: "Dados de inscrição incompletos." };
  }
  const supabase = await createClient();
  const { error } = await supabase.rpc("upsert_web_push_subscription", {
    p_church_id: input.churchId,
    p_endpoint: input.endpoint,
    p_p256dh: input.p256dh,
    p_auth: input.auth,
    p_user_agent: input.userAgent ?? null,
    p_platform: input.platform ?? null,
    p_topics: input.topics ?? ["worship_daily"],
  });
  if (error) return { error: "Não foi possível ativar os lembretes agora." };
  return { ok: true };
}

/** "Remover inscrição" - removes by endpoint (unique per browser install), not by id, since the
 *  client only ever has the PushSubscription object from the browser, never a database id. Goes
 *  through the `delete_web_push_subscription` RPC (security definer) - it only ever deletes the
 *  single row matching the exact endpoint passed in, never a broader condition. */
export async function removeWebPushSubscriptionAction(endpoint: string): Promise<SubscribeResult> {
  if (!endpoint) return { error: "Inscrição inválida." };
  const supabase = await createClient();
  const { error } = await supabase.rpc("delete_web_push_subscription", { p_endpoint: endpoint });
  if (error) return { error: "Não foi possível remover os lembretes agora." };
  return { ok: true };
}
