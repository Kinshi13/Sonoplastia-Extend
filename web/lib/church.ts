import { cache } from "react";
import { createClient } from "@/lib/supabase/server";
import { Church } from "@/lib/types/database";

/** Cached per-request so the layout and page for /c/[slug] don't each re-query it. */
export const getChurchBySlug = cache(async (slug: string): Promise<Church | null> => {
  const supabase = await createClient();
  const { data } = await supabase.from("churches").select("*").eq("slug", slug).single();
  return (data as Church) ?? null;
});
