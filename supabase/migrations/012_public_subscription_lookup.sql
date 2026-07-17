-- Hotfix (Fase 11.9, escopo reduzido - resolucao de plano/entitlements no Android).
--
-- The SAFE migration (006-010) correctly locked down `subscriptions` so `anon` can no longer
-- read the table directly - only an authenticated user whose own `profiles.church_id` matches can
-- (see "subscriptions: read own church" in RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql).
--
-- That is the right policy for the web app (every visitor either has a profile or doesn't need
-- subscription data). It broke Android, though: the app's "public view" (the normal way anyone
-- opens the app - browsing scales/doxologies/announcements) never authenticates at all, since only
-- Admin has a login screen. Every one of those sessions is `anon`, so PlanRepository's
-- subscription read started failing with 42501 for every church, always - not a per-church bug.
--
-- Fix: a SECURITY DEFINER RPC that returns only the single subscription row for a church_id the
-- caller already supplies (never a blanket table read, never a way to enumerate other churches).
-- Knowing "this church_id is on plan X" is not sensitive on its own - it's the same shape of fact
-- the public site already shows on `/precos` - so this does not reopen anything the SAFE migration
-- was protecting.
begin;

create or replace function public.get_church_subscription(p_church_id uuid)
returns setof subscriptions
language sql
security definer
set search_path = public
stable
as $$
  select * from subscriptions where church_id = p_church_id;
$$;

grant execute on function public.get_church_subscription(uuid) to anon, authenticated;

commit;
