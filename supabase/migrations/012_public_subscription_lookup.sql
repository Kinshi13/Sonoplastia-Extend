-- ===========================================================================
-- 012_public_subscription_lookup.sql
-- Consulta publica minima de plano e entitlements para o Android.
--
-- Motivo:
-- A visualizacao publica do Android opera como `anon` e nao pode consultar
-- diretamente `subscriptions` apos o endurecimento das policies (SAFE
-- migration 006-010: "subscriptions: read own church" exige auth.uid() com
-- profiles.church_id correspondente). Apenas o login Admin autentica; o uso
-- normal do app (ver escalas/doxologia/avisos) nunca tem sessao - toda
-- resolucao de assinatura estava falhando com 42501 e caindo em Free.
--
-- Esta RPC retorna somente os dados necessarios para o Android resolver
-- plano/status/expiracao/features/limits. Nao retorna plan_id, id, source,
-- nem qualquer coluna Stripe (stripe_customer_id/stripe_subscription_id) -
-- mesmo que essas colunas existam hoje ou sejam adicionadas no futuro, esta
-- funcao nunca as projeta, e nao ha SELECT direto liberado a anon sobre
-- `subscriptions` (isso permanece revogado, como a migration SAFE deixou).
--
-- Nota: este arquivo substitui a primeira versao de 012 (que retornava
-- `setof subscriptions`, expondo a linha inteira da tabela) diretamente, sem
-- criar um 012_..._safe.sql separado - confirmado ao vivo contra producao
-- (erro PGRST202 "could not find function") que a versao anterior nunca
-- chegou a ser executada, entao nao ha historico em producao para preservar.
-- ===========================================================================

begin;

create or replace function public.get_church_subscription(
  p_church_id uuid
)
returns table (
  church_id uuid,
  plan_code text,
  plan_name text,
  subscription_status text,
  started_at bigint,
  expires_at bigint,
  trial_ends_at bigint,
  grace_period_ends_at bigint,
  features jsonb,
  limits jsonb,
  updated_at bigint
)
language sql
security definer
set search_path = ''
stable
as $$
  select
    s.church_id,
    p.code as plan_code,
    p.name as plan_name,
    s.status as subscription_status,
    s.started_at,
    s.expires_at,
    s.trial_ends_at,
    s.grace_period_ends_at,
    p.features,
    p.limits,
    s.updated_at
  from public.subscriptions s
  inner join public.plans p
    on p.id = s.plan_id
  inner join public.churches c
    on c.id = s.church_id
  where s.church_id = p_church_id
    and c.is_active = true
  limit 1;
$$;

revoke all
on function public.get_church_subscription(uuid)
from public;

grant execute
on function public.get_church_subscription(uuid)
to anon, authenticated;

commit;
