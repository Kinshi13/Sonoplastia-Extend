"use server";

import Stripe from "stripe";
import { headers } from "next/headers";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Plan, Subscription } from "@/lib/types/database";

// Instantiated per-call, not at module scope - same reasoning as the webhook route: a missing
// STRIPE_SECRET_KEY shouldn't throw during Next's build-time page-data collection.
function getStripeClient() {
  return new Stripe(process.env.STRIPE_SECRET_KEY!);
}

export type BillingActionResult = { url?: string; error?: string };

/**
 * Starts a Stripe Checkout session (mode: subscription) for one of the recurring paid plans
 * (Essencial/Pro/Organização - never FREE, and never FOUNDER, which keeps its own one-time flow
 * in cadastrar-igreja/actions.ts). The plan's Stripe Price ID lives on the `plans` row itself
 * (see migration 007) rather than being hardcoded here, so changing a price in the Stripe
 * Dashboard + updating that one column is enough - no redeploy needed.
 */
export async function startPlanCheckoutAction(planId: string, billingPeriod: "monthly" | "yearly"): Promise<BillingActionResult> {
  const { user, isAdmin, churchId } = await getAdminStatus();
  if (!user || !isAdmin || !churchId) {
    return { error: "Apenas administradores podem assinar um plano." };
  }

  const supabase = await createClient();
  const { data: plan } = await supabase.from("plans").select("*").eq("id", planId).single();
  if (!plan) return { error: "Plano não encontrado." };
  const typedPlan = plan as Plan;

  if (typedPlan.billing_period !== "recurring") {
    return { error: "Esse plano não é vendido por assinatura recorrente." };
  }
  const priceId = billingPeriod === "monthly" ? typedPlan.stripe_price_id_monthly : typedPlan.stripe_price_id_yearly;
  if (!priceId) {
    return { error: "Esse plano ainda não está configurado para pagamento (fale com quem administra o projeto)." };
  }

  const { data: existingSub } = await supabase
    .from("subscriptions")
    .select("stripe_customer_id")
    .eq("church_id", churchId)
    .maybeSingle();
  const existingCustomerId = (existingSub as Pick<Subscription, "stripe_customer_id"> | null)?.stripe_customer_id ?? undefined;

  const origin = (await headers()).get("origin") ?? "";
  const stripe = getStripeClient();

  const session = await stripe.checkout.sessions.create({
    mode: "subscription",
    payment_method_types: ["card"],
    line_items: [{ price: priceId, quantity: 1 }],
    client_reference_id: churchId,
    metadata: { church_id: churchId, plan_id: typedPlan.id },
    subscription_data: { metadata: { church_id: churchId, plan_id: typedPlan.id } },
    ...(existingCustomerId ? { customer: existingCustomerId } : { customer_email: user.email }),
    success_url: `${origin}/admin/planos?checkout=success`,
    cancel_url: `${origin}/admin/planos?checkout=canceled`,
  });

  if (!session.url) return { error: "Não foi possível iniciar o pagamento. Tente novamente." };
  return { url: session.url };
}

/**
 * Opens Stripe's hosted Billing Portal (cancel, change plan, update payment method, see
 * invoices) instead of hand-rolling that UI - it already handles proration and PCI scope
 * correctly. Requires the church to already have a stripe_customer_id, i.e. at least one
 * successful checkout via [startPlanCheckoutAction] or the Founder flow.
 */
export async function openBillingPortalAction(): Promise<BillingActionResult> {
  const { isAdmin, churchId } = await getAdminStatus();
  if (!isAdmin || !churchId) return { error: "Apenas administradores podem gerenciar a assinatura." };

  const supabase = await createClient();
  const { data: sub } = await supabase
    .from("subscriptions")
    .select("stripe_customer_id")
    .eq("church_id", churchId)
    .maybeSingle();
  const customerId = (sub as Pick<Subscription, "stripe_customer_id"> | null)?.stripe_customer_id;
  if (!customerId) {
    return { error: "Essa igreja ainda não tem uma assinatura paga via Stripe para gerenciar." };
  }

  const origin = (await headers()).get("origin") ?? "";
  const stripe = getStripeClient();
  const portalSession = await stripe.billingPortal.sessions.create({
    customer: customerId,
    return_url: `${origin}/admin/planos`,
  });

  return { url: portalSession.url };
}
