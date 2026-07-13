"use client";

import { useState } from "react";
import { startPlanCheckoutAction, openBillingPortalAction } from "../billing-actions";

/** "Assinar" (mensal/anual) for a plan the church isn't on, or "Gerenciar assinatura" (opens the
 *  Stripe Billing Portal) when it's already the church's real Stripe-backed plan - mirrors the
 *  client-calls-server-action-then-redirects pattern already used in cadastrar-igreja/page.tsx. */
export function PlanActionButtons({
  planId,
  isCurrentStripePlan,
  hasMonthly,
  hasYearly,
}: {
  planId: string;
  isCurrentStripePlan: boolean;
  hasMonthly: boolean;
  hasYearly: boolean;
}) {
  const [pending, setPending] = useState<"monthly" | "yearly" | "portal" | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function subscribe(period: "monthly" | "yearly") {
    setPending(period);
    setError(null);
    const result = await startPlanCheckoutAction(planId, period);
    if (result.error) {
      setError(result.error);
      setPending(null);
      return;
    }
    if (result.url) window.location.href = result.url;
  }

  async function manage() {
    setPending("portal");
    setError(null);
    const result = await openBillingPortalAction();
    if (result.error) {
      setError(result.error);
      setPending(null);
      return;
    }
    if (result.url) window.location.href = result.url;
  }

  if (isCurrentStripePlan) {
    return (
      <div className="flex flex-col gap-1.5">
        <button
          onClick={manage}
          disabled={pending !== null}
          className="rounded-full border border-divider px-3 py-2 text-sm font-medium hover:border-primary hover:text-primary transition-colors disabled:opacity-60"
        >
          {pending === "portal" ? "Abrindo..." : "Gerenciar assinatura"}
        </button>
        {error && <p className="text-xs text-error">{error}</p>}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex gap-2">
        {hasMonthly && (
          <button
            onClick={() => subscribe("monthly")}
            disabled={pending !== null}
            className="flex-1 rounded-full bg-primary px-3 py-2 text-sm font-medium text-white hover:opacity-90 transition-opacity disabled:opacity-60"
          >
            {pending === "monthly" ? "Abrindo..." : "Assinar mensal"}
          </button>
        )}
        {hasYearly && (
          <button
            onClick={() => subscribe("yearly")}
            disabled={pending !== null}
            className="flex-1 rounded-full border border-primary px-3 py-2 text-sm font-medium text-primary hover:bg-primary-container/40 transition-colors disabled:opacity-60"
          >
            {pending === "yearly" ? "Abrindo..." : "Assinar anual"}
          </button>
        )}
      </div>
      {!hasMonthly && !hasYearly && (
        <p className="text-xs text-text-secondary">Pagamento em breve.</p>
      )}
      {error && <p className="text-xs text-error">{error}</p>}
    </div>
  );
}
