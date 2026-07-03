import { createClient } from "@/lib/supabase/server";

/**
 * Resolves the signed-in user (if any) and whether their `profiles.is_admin` flag is set.
 * Mirrors AdminSession on the Android app: only accounts created by whoever manages the Supabase
 * project (Dashboard -> Authentication -> Add user, promoted via the SQL in supabase/schema.sql)
 * can be admins - there's no self-serve sign-up here either.
 */
export async function getAdminStatus() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) return { user: null, isAdmin: false };

  const { data: profile } = await supabase
    .from("profiles")
    .select("is_admin")
    .eq("id", user.id)
    .single();

  return { user, isAdmin: profile?.is_admin === true };
}
