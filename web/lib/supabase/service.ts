import { createClient as createSupabaseClient } from "@supabase/supabase-js";

/**
 * Service-role client that bypasses RLS entirely - only ever used server-side, and only by the
 * Stripe webhook, to create a church row and promote its paying user to admin. Never import this
 * from anything reachable by a request that isn't a verified Stripe webhook call.
 */
export function createServiceRoleClient() {
  return createSupabaseClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!,
    { auth: { autoRefreshToken: false, persistSession: false } }
  );
}
