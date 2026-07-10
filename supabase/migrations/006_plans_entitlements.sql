-- Fase 3 - plan catalog + per-organization (church) subscription state. Safe to run once on the
-- existing production database.
--
-- Design note: features/limits live as jsonb on the plan row itself, not as separate join/limit
-- tables. A plan's feature set and limits always change together as one unit when a plan is
-- edited - splitting them into normalized tables would add joins without adding real flexibility
-- at 5 plans / ~20 feature keys. FeatureKey values are still a single closed list (enforced in
-- app code, see FeatureKey.kt on Android and lib/entitlements.ts on web), so this isn't "booleans
-- scattered around" - it's one governed catalog, just stored as structured JSON instead of rows.

create table if not exists plans (
  id uuid primary key default gen_random_uuid(),
  code text not null unique, -- FREE | ESSENTIAL | PRO | ORGANIZATION | FOUNDER
  name text not null,
  description text not null default '',
  monthly_price_cents integer, -- null = not sold monthly (e.g. FOUNDER)
  yearly_price_cents integer,  -- null = not sold yearly / one-time (e.g. FOUNDER's single price)
  currency text not null default 'BRL',
  -- Founder-style one-time plans use monthly_price_cents as the single price and
  -- yearly_price_cents null; billing_period documents which column means what.
  billing_period text not null default 'recurring', -- 'recurring' | 'one_time'
  is_active boolean not null default true,   -- sellable at all right now
  is_public boolean not null default true,   -- shown in the catalog UI (FOUNDER stays false-by-default: campaign/eligibility gated)
  sort_order integer not null default 0,
  features jsonb not null default '[]',  -- array of FeatureKey strings
  limits jsonb not null default '{}',    -- object of PlanLimits fields, missing/null key = unlimited
  created_at bigint not null,
  updated_at bigint not null
);

alter table plans enable row level security;
drop policy if exists "plans: public read" on plans;
create policy "plans: public read" on plans for select using (true);

create table if not exists subscriptions (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null unique references churches(id), -- one subscription per organization
  plan_id uuid not null references plans(id),
  status text not null default 'FREE', -- FREE|TRIAL|ACTIVE|PAST_DUE|GRACE_PERIOD|CANCELED|EXPIRED
  started_at bigint not null,
  expires_at bigint,          -- null = no fixed expiry (lifetime/Founder, or FREE)
  trial_ends_at bigint,
  grace_period_ends_at bigint,
  source text not null default 'manual', -- 'stripe_checkout' | 'manual' | 'founder_grant' | ...
  updated_at bigint not null
);

alter table subscriptions enable row level security;
-- Public read: a regular member (no login in this app) can still see their church's plan: "usuário
-- comum pode ver o plano da organização" - no billing secrets live in this row, just plan/status/dates.
drop policy if exists "subscriptions: public read" on subscriptions;
drop policy if exists "subscriptions: admin update" on subscriptions;
create policy "subscriptions: public read" on subscriptions for select using (true);
create policy "subscriptions: admin update" on subscriptions for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = subscriptions.church_id));

grant select on public.plans, public.subscriptions to anon, authenticated;
grant update on public.subscriptions to authenticated;
-- Inserts into subscriptions/plans happen via the service-role key only (webhook / manual seeding
-- via the SQL editor) - no anon/authenticated insert policy is defined on purpose, mirroring how
-- churches.is_active is only ever flipped by the Stripe webhook, never by a client.

alter publication supabase_realtime add table plans, subscriptions;

-- Seed catalog -------------------------------------------------------------
insert into plans (code, name, description, monthly_price_cents, yearly_price_cents, currency, billing_period, is_active, is_public, sort_order, features, updated_at, created_at)
values
  ('FREE', 'Free', 'O essencial para acompanhar sua igreja, sem custo.',
    0, 0, 'BRL', 'recurring', true, true, 0,
    '["VIEW_OFFICIAL_SCALE","VIEW_DOXOLOGY","VIEW_ANNOUNCEMENTS","VIEW_CALENDAR","CLASS_HIGHLIGHTS","PERSONAL_EVENTS","PERSONAL_CARDS"]',
    extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),

  ('ESSENTIAL', 'Essencial', 'Mais histórico, personalização e exportação.',
    1490, 14900, 'BRL', 'recurring', true, true, 1,
    '["VIEW_OFFICIAL_SCALE","VIEW_DOXOLOGY","VIEW_ANNOUNCEMENTS","VIEW_CALENDAR","CLASS_HIGHLIGHTS","PERSONAL_EVENTS","PERSONAL_CARDS","EXTENDED_HISTORY","ADVANCED_NOTIFICATIONS","CUSTOM_FONTS","CUSTOM_THEMES","EXPORT"]',
    extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),

  ('PRO', 'Pro', 'Administração avançada, relatórios e mídia sem limites apertados.',
    2990, 29900, 'BRL', 'recurring', true, true, 2,
    '["VIEW_OFFICIAL_SCALE","VIEW_DOXOLOGY","VIEW_ANNOUNCEMENTS","VIEW_CALENDAR","CLASS_HIGHLIGHTS","PERSONAL_EVENTS","PERSONAL_CARDS","EXTENDED_HISTORY","ADVANCED_NOTIFICATIONS","CUSTOM_FONTS","CUSTOM_THEMES","EXPORT","ADVANCED_ADMIN","PREMIUM_FONTS","PREMIUM_THEMES","REPORTS","ADVANCED_MEDIA","PRIORITY_SYNC"]',
    extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),

  ('ORGANIZATION', 'Organização', 'Para redes com múltiplos administradores e identidade própria.',
    7990, 79900, 'BRL', 'recurring', true, true, 3,
    '["VIEW_OFFICIAL_SCALE","VIEW_DOXOLOGY","VIEW_ANNOUNCEMENTS","VIEW_CALENDAR","CLASS_HIGHLIGHTS","PERSONAL_EVENTS","PERSONAL_CARDS","EXTENDED_HISTORY","ADVANCED_NOTIFICATIONS","CUSTOM_FONTS","CUSTOM_THEMES","EXPORT","ADVANCED_ADMIN","PREMIUM_FONTS","PREMIUM_THEMES","REPORTS","ADVANCED_MEDIA","PRIORITY_SYNC","MULTI_ADMIN","ORGANIZATION_BRANDING","AUTOMATIONS"]',
    extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),

  ('FOUNDER', 'Fundador Vitalício', 'Acesso vitalício completo - campanha de lançamento, por elegibilidade.',
    24900, null, 'BRL', 'one_time', true, false, 4,
    '["VIEW_OFFICIAL_SCALE","VIEW_DOXOLOGY","VIEW_ANNOUNCEMENTS","VIEW_CALENDAR","CLASS_HIGHLIGHTS","PERSONAL_EVENTS","PERSONAL_CARDS","EXTENDED_HISTORY","ADVANCED_NOTIFICATIONS","CUSTOM_FONTS","CUSTOM_THEMES","EXPORT","ADVANCED_ADMIN","PREMIUM_FONTS","PREMIUM_THEMES","REPORTS","ADVANCED_MEDIA","PRIORITY_SYNC","MULTI_ADMIN","ORGANIZATION_BRANDING","AUTOMATIONS"]',
    extract(epoch from now()) * 1000, extract(epoch from now()) * 1000)
on conflict (code) do nothing;

-- Limits per plan (null in a key = unlimited for that field)
update plans set limits = '{"maxAdmins":1,"maxPersonalEvents":10,"maxPersonalCards":5,"historyMonths":1,"maxAnnouncements":null,"maxMediaStorageMb":100,"maxOrganizations":1}' where code = 'FREE';
update plans set limits = '{"maxAdmins":2,"maxPersonalEvents":50,"maxPersonalCards":30,"historyMonths":6,"maxAnnouncements":null,"maxMediaStorageMb":1000,"maxOrganizations":1}' where code = 'ESSENTIAL';
update plans set limits = '{"maxAdmins":5,"maxPersonalEvents":200,"maxPersonalCards":100,"historyMonths":24,"maxAnnouncements":null,"maxMediaStorageMb":5000,"maxOrganizations":1}' where code = 'PRO';
update plans set limits = '{"maxAdmins":null,"maxPersonalEvents":null,"maxPersonalCards":null,"historyMonths":null,"maxAnnouncements":null,"maxMediaStorageMb":20000,"maxOrganizations":3}' where code = 'ORGANIZATION';
update plans set limits = '{"maxAdmins":null,"maxPersonalEvents":null,"maxPersonalCards":null,"historyMonths":null,"maxAnnouncements":null,"maxMediaStorageMb":null,"maxOrganizations":null}' where code = 'FOUNDER';

-- Backfill: every existing church gets a subscription row so entitlement lookups never hit a
-- missing row. Existing churches (created before this system existed, via direct SQL/manual
-- activation rather than a real Stripe charge) are granted FOUNDER manually so nothing already
-- built regresses - swap to FREE (or run a real checkout) once real billing exists.
insert into subscriptions (church_id, plan_id, status, started_at, source, updated_at)
select c.id, (select id from plans where code = 'FOUNDER'), 'ACTIVE', extract(epoch from now()) * 1000, 'manual_grant', extract(epoch from now()) * 1000
from churches c
where not exists (select 1 from subscriptions s where s.church_id = c.id);
