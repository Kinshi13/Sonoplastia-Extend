import { cache } from "react";
import { createClient } from "@/lib/supabase/server";
import { FeatureKey, Plan, PlanLimits, Subscription, SubscriptionStatus } from "@/lib/types/database";

/** The floor every church gets even with no subscription row at all - mirrors
 *  EntitlementService's FREE_FLOOR on Android. Should be unreachable in practice since
 *  006_plans_entitlements.sql backfills every church, but a server render must never throw
 *  just because a subscription row is momentarily missing. */
const FREE_FEATURES: FeatureKey[] = [
  "VIEW_OFFICIAL_SCALE",
  "VIEW_DOXOLOGY",
  "VIEW_ANNOUNCEMENTS",
  "VIEW_CALENDAR",
  "CLASS_HIGHLIGHTS",
  "PERSONAL_EVENTS",
  "PERSONAL_CARDS",
];

const FREE_LIMITS: PlanLimits = {
  maxAdmins: 1,
  maxPersonalEvents: 10,
  maxPersonalCards: 5,
  historyMonths: 1,
  maxAnnouncements: null,
  maxMediaStorageMb: 100,
  maxOrganizations: 1,
};

const STATUSES_THAT_GRANT_ACCESS: SubscriptionStatus[] = ["FREE", "TRIAL", "ACTIVE", "PAST_DUE", "GRACE_PERIOD"];

export type Entitlements = {
  planCode: Plan["code"];
  planName: string;
  status: SubscriptionStatus;
  features: Set<FeatureKey>;
  limits: PlanLimits;
};

const FREE_ENTITLEMENTS: Entitlements = {
  planCode: "FREE",
  planName: "Free",
  status: "FREE",
  features: new Set(FREE_FEATURES),
  limits: FREE_LIMITS,
};

/** Full public plan catalog, cheapest first - same table every church reads. */
export const getPlanCatalog = cache(async (): Promise<Plan[]> => {
  const supabase = await createClient();
  const { data } = await supabase.from("plans").select("*").order("sort_order", { ascending: true });
  return (data as Plan[]) ?? [];
});

/** Resolves what a specific church can actually use right now - the web mirror of Android's
 *  EntitlementService. No offline cache here (a server render is always "online" by definition),
 *  so this always reflects the live DB row; a missing/expired subscription just falls back to
 *  the hardcoded FREE floor rather than throwing. */
export async function getEntitlements(churchId: string): Promise<Entitlements> {
  const supabase = await createClient();
  const [{ data: subscription }, plans] = await Promise.all([
    supabase.from("subscriptions").select("*").eq("church_id", churchId).maybeSingle(),
    getPlanCatalog(),
  ]);

  const sub = subscription as Subscription | null;
  if (!sub || !STATUSES_THAT_GRANT_ACCESS.includes(sub.status)) return FREE_ENTITLEMENTS;

  const plan = plans.find((p) => p.id === sub.plan_id);
  if (!plan) return FREE_ENTITLEMENTS;

  return {
    planCode: plan.code,
    planName: plan.name,
    status: sub.status,
    features: new Set(plan.features),
    limits: plan.limits,
  };
}

export function hasFeature(entitlements: Entitlements, feature: FeatureKey): boolean {
  return entitlements.features.has(feature);
}
