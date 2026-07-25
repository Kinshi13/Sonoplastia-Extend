"use server";

import { createClient } from "@/lib/supabase/server";

export type SubscribeResult = { error?: string; ok?: boolean };

/**
 * Notificações (base) - saves a browser's Push subscription so a future sending job has
 * something to read. No visitor login exists on the public site, so this is intentionally
 * callable by anyone (see migrations/015_worship_recommendation_and_push.sql's RLS note) - the
 * only guard is that `churchId` must reference a real, active church. `endpoint` is globally
 * unique per browser install, so re-subscribing (e.g. after a permission reset) just upserts the
 * same row instead of accumulating duplicates.
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
  const now = Date.now();
  const { error } = await supabase.from("web_push_subscriptions").upsert(
    {
      church_id: input.churchId,
      endpoint: input.endpoint,
      p256dh: input.p256dh,
      auth: input.auth,
      user_agent: input.userAgent ?? null,
      platform: input.platform ?? null,
      topics: input.topics ?? ["worship_daily"],
      enabled: true,
      updated_at: now,
      last_seen_at: now,
      created_at: now,
    },
    { onConflict: "endpoint" }
  );
  if (error) return { error: "Não foi possível ativar os lembretes agora." };
  return { ok: true };
}

/** "Remover inscrição" - deletes by endpoint (unique per browser install), not by id, since the
 *  client only ever has the PushSubscription object from the browser, never a database id. */
export async function removeWebPushSubscriptionAction(endpoint: string): Promise<SubscribeResult> {
  if (!endpoint) return { error: "Inscrição inválida." };
  const supabase = await createClient();
  const { error } = await supabase.from("web_push_subscriptions").delete().eq("endpoint", endpoint);
  if (error) return { error: "Não foi possível remover os lembretes agora." };
  return { ok: true };
}
