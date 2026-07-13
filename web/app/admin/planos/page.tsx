import { CheckCircle2 } from "lucide-react";
import { getAdminStatus } from "@/lib/supabase/auth";
import { getEntitlements, getPlanCatalog } from "@/lib/entitlements";
import { createClient } from "@/lib/supabase/server";
import { Subscription } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { PlanActionButtons } from "./PlanActionButtons";

export const revalidate = 0;

const STATUS_LABELS: Record<string, string> = {
  FREE: "Free",
  TRIAL: "Em teste",
  ACTIVE: "Ativa",
  PAST_DUE: "Pagamento pendente",
  GRACE_PERIOD: "Período de carência",
  CANCELED: "Cancelada",
  EXPIRED: "Expirada",
};

function formatPrice(cents: number | null): string {
  if (cents === null) return "-";
  if (cents === 0) return "Grátis";
  return (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

const FEATURE_LABELS: Record<string, string> = {
  VIEW_OFFICIAL_SCALE: "Escala oficial",
  VIEW_DOXOLOGY: "Doxologia",
  VIEW_ANNOUNCEMENTS: "Anúncios",
  VIEW_CALENDAR: "Calendário",
  CLASS_HIGHLIGHTS: "Destaques por função",
  PERSONAL_EVENTS: "Eventos pessoais",
  PERSONAL_CARDS: "Cartões pessoais",
  EXTENDED_HISTORY: "Histórico estendido",
  ADVANCED_ADMIN: "Administração avançada",
  MULTI_ADMIN: "Múltiplos administradores",
  ADVANCED_NOTIFICATIONS: "Notificações avançadas",
  CUSTOM_FONTS: "Fontes personalizadas",
  PREMIUM_FONTS: "Fontes premium",
  CUSTOM_THEMES: "Temas personalizados",
  PREMIUM_THEMES: "Temas premium",
  EXPORT: "Exportar escalas",
  REPORTS: "Relatórios",
  ORGANIZATION_BRANDING: "Identidade da organização",
  ADVANCED_MEDIA: "Mídia avançada",
  PRIORITY_SYNC: "Sincronização prioritária",
  AUTOMATIONS: "Automações",
};

export default async function PlanosPage({
  searchParams,
}: {
  searchParams: Promise<{ checkout?: string }>;
}) {
  const { checkout } = await searchParams;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();

  const [plans, current, subscriptionRow] = await Promise.all([
    getPlanCatalog(),
    churchId ? getEntitlements(churchId) : Promise.resolve(null),
    churchId
      ? supabase.from("subscriptions").select("*").eq("church_id", churchId).maybeSingle()
      : Promise.resolve({ data: null }),
  ]);

  const subscription = subscriptionRow.data as Subscription | null;
  const hasStripeSubscription = !!subscription?.stripe_customer_id;
  const publicPlans = plans.filter((plan) => plan.is_public);

  return (
    <div className="flex flex-col gap-6">
      {checkout === "success" && (
        <div className="rounded-xl border border-primary/30 bg-primary-container/40 px-4 py-3 text-sm text-foreground">
          Pagamento confirmado! Pode levar alguns instantes para a assinatura aparecer atualizada aqui.
        </div>
      )}
      {checkout === "canceled" && (
        <div className="rounded-xl border border-divider bg-surface px-4 py-3 text-sm text-text-secondary">
          Assinatura não concluída - nenhuma cobrança foi feita.
        </div>
      )}

      <div>
        <h1 className="text-2xl font-semibold">Planos e recursos</h1>
        {current && (
          <p className="mt-1 text-sm text-text-secondary">
            Plano atual da igreja: <span className="font-medium text-foreground">{current.planName}</span>
            {subscription && (
              <>
                {" · "}
                <span className="font-medium text-foreground">{STATUS_LABELS[subscription.status] ?? subscription.status}</span>
              </>
            )}
          </p>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {publicPlans.map((plan) => {
          const isCurrent = current?.planCode === plan.code;
          const isFree = plan.code === "FREE";
          return (
            <Card
              key={plan.id}
              className={`p-5 flex flex-col gap-3 ${isCurrent ? "border-primary/50 shadow-md" : ""}`}
            >
              <div className="flex items-center justify-between gap-2">
                <h2 className="text-lg font-semibold">{plan.name}</h2>
                {isCurrent && (
                  <span className="rounded-full bg-primary-container px-2.5 py-0.5 text-xs font-medium text-on-primary-container">
                    Atual
                  </span>
                )}
              </div>
              <p className="text-sm text-text-secondary">{plan.description}</p>
              <p className="text-xl font-bold text-primary">
                {formatPrice(plan.monthly_price_cents)}
                {plan.billing_period === "recurring" && plan.monthly_price_cents ? (
                  <span className="text-sm font-normal text-text-secondary">/mês</span>
                ) : plan.billing_period === "one_time" ? (
                  <span className="text-sm font-normal text-text-secondary"> único</span>
                ) : null}
              </p>
              <ul className="flex flex-col gap-1.5 mt-1">
                {plan.features.slice(0, 8).map((feature) => (
                  <li key={feature} className="flex items-center gap-2 text-sm">
                    <CheckCircle2 size={14} className="text-primary shrink-0" />
                    {FEATURE_LABELS[feature] ?? feature}
                  </li>
                ))}
              </ul>
              {!isFree && plan.billing_period === "recurring" && (
                <PlanActionButtons
                  planId={plan.id}
                  isCurrentStripePlan={isCurrent && hasStripeSubscription}
                  hasMonthly={!!plan.stripe_price_id_monthly}
                  hasYearly={!!plan.stripe_price_id_yearly}
                />
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
}
