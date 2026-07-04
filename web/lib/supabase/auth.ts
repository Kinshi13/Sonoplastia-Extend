import { createClient } from "@/lib/supabase/server";

/**
 * Resolves the signed-in user (if any), whether their `profiles.is_admin` flag is set, and
 * which church they administer. is_admin and church_id are always set together by the Stripe
 * webhook after a successful one-time payment - there's no self-serve way to become an admin.
 */
export async function getAdminStatus() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) return { user: null, isAdmin: false, churchId: null as string | null };

  const { data: profile } = await supabase
    .from("profiles")
    .select("is_admin, church_id")
    .eq("id", user.id)
    .single();

  return {
    user,
    isAdmin: profile?.is_admin === true && !!profile.church_id,
    churchId: profile?.church_id ?? null,
  };
}
