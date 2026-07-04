import Stripe from "stripe";
import { NextResponse, type NextRequest } from "next/server";
import { createServiceRoleClient } from "@/lib/supabase/service";

// Instantiated inside the handler, not at module scope - a missing STRIPE_SECRET_KEY would
// otherwise throw during Next's build-time page-data collection, which imports this module
// without ever calling it.
function getStripeClient() {
  return new Stripe(process.env.STRIPE_SECRET_KEY!);
}

// Activates a church + promotes its buyer to admin after a successful one-time payment.
// Uses the service-role client because there's no authenticated user in this request - Stripe
// is calling us directly, verified only by the webhook signature below.
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

  return NextResponse.json({ received: true });
}
