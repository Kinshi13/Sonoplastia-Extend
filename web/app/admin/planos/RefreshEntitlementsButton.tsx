"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { RefreshCw } from "lucide-react";

/**
 * Hotfix (Parte 4): "Atualizar plano e recursos" - a dev/admin convenience that only re-runs the
 * server components on this page (`router.refresh()`), nothing else. `getEntitlements` already
 * queries `subscriptions` fresh on every server render (no server-side cache to invalidate - see
 * lib/entitlements.ts), so this button never changes any plan; it just forces this tab to stop
 * showing whatever it rendered before a subscription row was granted/updated directly in the DB.
 */
export function RefreshEntitlementsButton() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [justRefreshed, setJustRefreshed] = useState(false);

  function handleClick() {
    startTransition(() => {
      router.refresh();
    });
    setJustRefreshed(true);
    setTimeout(() => setJustRefreshed(false), 2000);
  }

  return (
    <button
      onClick={handleClick}
      disabled={isPending}
      className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-xs font-medium text-text-secondary hover:text-foreground hover:bg-surface transition-colors disabled:opacity-60"
    >
      <RefreshCw size={12} className={isPending ? "animate-spin" : ""} />
      {justRefreshed ? "Atualizado" : "Atualizar plano e recursos"}
    </button>
  );
}
