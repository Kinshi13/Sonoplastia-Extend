"use client";

import { useEffect } from "react";
import { recordChurchAccess } from "@/lib/recentChurches";

/** Invisible - just writes this visit into the local "igrejas recentes" list once on mount. */
export function RecentChurchRecorder({
  churchId,
  slug,
  churchName,
  logoUrl,
}: {
  churchId: string;
  slug: string;
  churchName: string;
  logoUrl?: string | null;
}) {
  useEffect(() => {
    recordChurchAccess({ churchId, slug, churchName, logoUrl });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [churchId]);

  return null;
}
