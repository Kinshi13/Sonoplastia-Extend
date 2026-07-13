import Stripe from "stripe";
import { NextResponse, type NextRequest } from "next/server";
import { createServiceRoleClient } from "@/lib/supabase/service";

// Instantiated inside the handler, not at module scope - a missing STRIPE_SECRET_KEY would
// otherwise throw during Next's build-time page-data collection, which imports this module
// without ever calling it.
function getStripeClient() {
  return new Stripe(process.env.STRIPE_SECRET_KEY!);
}

// Fase 10: maps a Stripe Subscription status to our own SubscriptionStatus vocabulary (see
// lib/entitlements.ts / Android's SubscriptionStatus enum) - kept as a single source of truth
// here since only this webhook ever writes subscriptions.status for a Stripe-backed row.
function mapStripeStatus(stripeStatus: Stripe.Subscription.Status): string {
  switch (stripeStatus) {
    case "trialing":
      return "TRIAL";
    case "active":
      return "ACTIVE";
    case "past_due":
    case "unpaid":
      return "PAST_DUE";
    case "paused":
      return "GRACE_PERIOD";
    case "canceled":
      return "CANCELED";
    case "incomplete":
    case "incomplete_expired":
      // The very first payment on a brand-new subscription never went through - there is no
      // established plan to fall back to, so there's nothing useful to write. A real duplicate
      // event won't retry into money owed since Stripe never created an active subscription.
      return "";
    default:
      return "";
  }
}

/**
 * Resolves which `plans` row a Stripe Subscription is actually billing right now, by its Price
 * ID rather than by trusting the (possibly stale) metadata copied onto the subscription at
 * creation time - a plan change made through the Billing Portal updates the subscription's price
 * but doesn't invoke our code, so metadata.plan_id could otherwise go stale after a self-serve
 * upgrade/downgrade.
 */
async function resolvePlanIdForSubscription(
  supabase: ReturnType<typeof createServiceRoleClient>,
  subscription: Stripe.Subscription
): Promise<string | null> {
  const priceId = subscription.items.data[0]?.price.id;
  if (!priceId) return null;
  const { data } = await supabase
    .from("plans")
    .select("id")
    .or(`stripe_price_id_monthly.eq.${priceId},stripe_price_id_yearly.eq.${priceId}`)
    .maybeSingle();
  return data?.id ?? null;
}

async function upsertSubscriptionFromStripe(subscription: Stripe.Subscription) {
  const status = mapStripeStatus(subscription.status);
  if (!status) return; // incomplete/expired - nothing established to record yet

  const churchId = subscription.metadata?.church_id;
  const supabase = createServiceRoleClient();

  // Fall back to looking the row up by stripe_subscription_id when metadata is missing (should
  // not happen for subscriptions this app created, but keeps a manually-created Stripe test
  // subscription from crashing the handler instead of just being ignored).
  const churchIdResolved =
    churchId ??
    (
      await supabase
        .from("subscriptions")
        .select("church_id")
        .eq("stripe_subscription_id", subscription.id)
        .maybeSingle()
    ).data?.church_id;
  if (!churchIdResolved) return;

  const planId = await resolvePlanIdForSubscription(supabase, subscription);
  if (!planId) return;

  const customerId = typeof subscription.customer === "string" ? subscription.customer : subscription.customer.id;

  await supabase.from("subscriptions").upsert(
    {
      church_id: churchIdResolved,
      plan_id: planId,
      status,
      started_at: subscription.start_date * 1000,
      expires_at: subscription.cancel_at ? subscription.cancel_at * 1000 : null,
      trial_ends_at: subscription.trial_end ? subscription.trial_end * 1000 : null,
      grace_period_ends_at: null,
      source: "stripe_checkout",
      stripe_customer_id: customerId,
      stripe_subscription_id: subscription.id,
      updated_at: Date.now(),
    },
    { onConflict: "church_id" }
  );
}

// Verified only by the webhook signature below - there is no authenticated user in this request,
// Stripe is calling us directly, so every handler here uses the service-role client.
export async function POST(request: NextRequest) {
  const stripe = getStripeClient();
  const body = await request.text();
  const signature = request.headers.get("stripe-signature");

  let event: Stripe.Event;
  try {
    event = stripe.webhooks.constructEvent(body, signature!, process.env.STRIPE_WEBHOOK_SECRET!);
  } catch (err) {
    const message = err instanceof Error ? err.message : "invalid signature";
    return NextResponse.json({ error: `Webhook signature verification failed: ${message}` }, { status: 400 });
  }

  if (event.type === "checkout.session.completed") {
    const session = event.data.object as Stripe.Checkout.Session;

    // Two unrelated checkout flows share this event: the one-time "Fundador Vitalício" purchase
    // (mode: payment, cadastrar-igreja) creates the church itself; a recurring plan checkout
    // (mode: subscription, admin/billing-actions.ts) is for a church that already exists - its
    // subscriptions row is written from customer.subscription.created below instead, once Stripe
    // has actually established the subscription (not just started checkout).
    if (session.mode === "payment") {
      const userId = session.client_reference_id;
      const churchName = session.metadata?.church_name;
      const slug = session.metadata?.slug;

      if (!userId || !churchName || !slug) {
        return NextResponse.json({ error: "Missing client_reference_id or metadata on session" }, { status: 400 });
      }

      const supabase = createServiceRoleClient();

      const { data: church, error: churchError } = await supabase
        .from("churches")
        .insert({ slug, name: churchName, is_active: true, created_at: Date.now() })
        .select("id")
        .single();

      if (churchError || !church) {
        return NextResponse.json({ error: churchError?.message ?? "Failed to create church" }, { status: 500 });
      }

      const { error: profileError } = await supabase
        .from("profiles")
        .update({ church_id: church.id, is_admin: true })
        .eq("id", userId);

      if (profileError) {
        return NextResponse.json({ error: profileError.message }, { status: 500 });
      }
    }
  }

  if (
    event.type === "customer.subscription.created" ||
    event.type === "customer.subscription.updated" ||
    event.type === "customer.subscription.deleted"
  ) {
    await upsertSubscriptionFromStripe(event.data.object as Stripe.Subscription);
  }

  return NextResponse.json({ received: true });
}
