-- ===========================================================================
-- RUN_PENDING_MIGRATIONS_006_010.sql
-- ==========================================================================
-- Diagnostic finding (Hotfix, this session): migrations 001-005 are applied in
-- production, but 006-010 were NEVER run. plans/subscriptions/export_audit_log/
-- organization_roles (and friends) don't exist at all, and doxologies is missing
-- every column from 009 onward - confirmed live against the production database
-- with direct SELECT queries (PostgREST error 'column does not exist' /
-- 'Could not find the table ... in the schema cache'), not guessed from files.
--
-- This is the root cause of BOTH bugs reported in this hotfix:
--   - Doxologia reuse fails: reused_from_doxology_id is from migration 009.
--   - Can't grant a premium plan: subscriptions/plans themselves are from 006.
--
-- Run this ONCE, in order, in the Supabase SQL Editor (or psql) against your
-- production database. It is exactly migrations 006 through 010 concatenated,
-- byte-for-byte from the files already in this repo - nothing rewritten, nothing
-- skipped. Safe to run once on a database that has 001-005 applied and nothing
-- from 006 onward (i.e. exactly this database's current state).
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 006_plans_entitlements.sql
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 007_stripe_billing.sql
-- ---------------------------------------------------------------------------
-- Fase 10 - real recurring billing for the 3 paid recurring plans (Essencial/Pro/Organização).
-- FREE stays unpaid and FOUNDER stays the existing one-time Checkout flow (cadastrar-igreja) -
-- neither needs a Stripe price here. Safe to run once on the existing database.

-- Each plan carries its own Stripe Price IDs so the checkout/webhook code never hardcodes one -
-- set these from the Stripe Dashboard (Products -> a plan's monthly/yearly price -> "API ID").
-- Null on FREE/FOUNDER on purpose (FOUNDER already has its own price via STRIPE_PRICE_ID env var,
-- unrelated to this table).
alter table plans add column if not exists stripe_price_id_monthly text;
alter table plans add column if not exists stripe_price_id_yearly text;

-- Needed to resolve subscription.updated/deleted webhook events back to a church_id (those events
-- only carry Stripe's own IDs, not ours) and to open the Stripe Billing Portal for a church.
alter table subscriptions add column if not exists stripe_customer_id text;
alter table subscriptions add column if not exists stripe_subscription_id text;

create unique index if not exists subscriptions_stripe_subscription_id_key
  on subscriptions (stripe_subscription_id)
  where stripe_subscription_id is not null;

-- Stripe IDs are never read by an anon/authenticated client directly (checkout/portal are server
-- actions using the service-role client for writes), but the row itself is still public-select
-- per the existing "subscriptions: public read" policy from 006 - a member seeing their own
-- church's stripe_customer_id isn't a secret (it identifies a Customer object, not a payment
-- method), same trust level as knowing which plan the church is on.

-- Fill in your real Price IDs after creating the Products/Prices in the Stripe Dashboard, e.g.:
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ESSENTIAL';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'PRO';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ORGANIZATION';

-- ---------------------------------------------------------------------------
-- 008_export_and_atlas_prep.sql
-- ---------------------------------------------------------------------------
-- Fase 11.7 - export center permissions/audit trail (Parte 10-11), and dormant Stella Atlas
-- integration scaffolding (Parte 16 - "não implementar infraestrutura excessivamente complexa se
-- o Atlas ainda não existe": this table exists so the shape is settled, nothing writes to it yet).

-- Grant the new export FeatureKeys to the plans that already include EXPORT, so nothing regresses
-- for an existing paying church - Essencial+ keeps exporting, just through named capabilities now.
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV"]'::jsonb
  where code in ('ESSENTIAL');
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV","EXPORT_SCALE_PDF","EXPORT_SCALE_IMAGE","PUBLIC_READONLY_LINK"]'::jsonb
  where code in ('PRO', 'ORGANIZATION', 'FOUNDER');

-- Parte 10 (Auditoria): who exported what, from where.
create table if not exists export_audit_log (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  user_id uuid not null references auth.users(id),
  format text not null, -- 'csv' | 'pdf' | 'image' | 'print'
  period text,          -- e.g. the month requested, free-form
  filters jsonb not null default '{}',
  created_at bigint not null
);

alter table export_audit_log enable row level security;
drop policy if exists "export_audit_log: admin read own church" on export_audit_log;
drop policy if exists "export_audit_log: admin insert own church" on export_audit_log;
create policy "export_audit_log: admin read own church" on export_audit_log for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = export_audit_log.church_id));
create policy "export_audit_log: admin insert own church" on export_audit_log for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = export_audit_log.church_id));

grant select, insert on public.export_audit_log to authenticated;

-- Parte 16 (Outbox pattern) - domain events queued here whenever something Atlas-relevant
-- changes, processed asynchronously by a future worker rather than a synchronous call inside the
-- request that made the change (so a slow/down Atlas never blocks or fails a save). Nothing in
-- this codebase writes to this table yet - see lib/atlas/types.ts for the StellaDomainEvent shape
-- this row's `payload` is meant to hold, and AtlasEventOutbox for the (currently no-op) writer.
create table if not exists integration_outbox (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references churches(id),
  app_code text not null default 'STELLA_SCALE',
  event_type text not null,   -- e.g. 'scale.created', see lib/atlas/types.ts
  entity_type text not null,  -- e.g. 'scale'
  entity_id uuid not null,
  payload jsonb not null default '{}',
  status text not null default 'PENDING', -- PENDING | SENT | FAILED
  attempts integer not null default 0,
  created_at bigint not null,
  processed_at bigint,
  last_error text
);

alter table integration_outbox enable row level security;
-- No client-facing policy on purpose - this table is only ever touched by a future service-role
-- worker, mirroring how the Stripe webhook is the only writer of `subscriptions.status`. No
-- grant to anon/authenticated either, unlike export_audit_log above.

-- ---------------------------------------------------------------------------
-- 009_doxology_reuse.sql
-- ---------------------------------------------------------------------------
-- Fase 11.8.3 (Bloco N-S): "Reutilizar programação recente" for Doxologia. All additive/nullable
-- with safe defaults - no existing row is touched, nothing destructive. Safe to run once on the
-- existing production database.

-- Bloco S: favoriting a Doxologia so it sorts to the top of the reuse picker.
alter table doxologies add column if not exists is_favorite boolean not null default false;

-- Bloco R: an optional, non-authoritative pointer back to whatever Doxologia this one was cloned
-- from - `on delete set null` so deleting the original never blocks/cascades into deleting a copy
-- that has since taken on a life of its own. Never required (see Bloco R: "não transformar esse
-- campo em dependência obrigatória").
alter table doxologies add column if not exists reused_from_doxology_id uuid references doxologies(id) on delete set null;

-- Bloco P: "mais reutilizadas" sort - incremented on the *source* row every time it's used as the
-- base for a new Doxologia (never on the clone itself).
alter table doxologies add column if not exists times_reused integer not null default 0;

create index if not exists doxologies_church_favorite_idx on doxologies (church_id, is_favorite, date desc);

-- ---------------------------------------------------------------------------
-- 010_organization_roles_people.sql
-- ---------------------------------------------------------------------------
-- Fase 11.8.4 (Parte 2-13): configurable roles, a people/teams bank, and dynamic scale
-- assignments - additive only, no existing column/table is touched or dropped. `scales`' five
-- legacy person columns (reception_person, sound_person, preaching_person, conducting_person,
-- musical_message_person) stay exactly as they are - the Android app and every existing public
-- page/export keep reading them unchanged. The web admin now writes to *both*: scale_assignments
-- (authoritative, supports arbitrary custom roles) and, best-effort, the matching legacy column
-- when a role maps to one of the five original functions (see `legacy_field_key` below).

create table organization_roles (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  name text not null,
  short_name text,
  description text not null default '',
  icon_key text not null default 'users',
  color_token text,
  sort_order integer not null default 0,
  is_active boolean not null default true,
  is_required boolean not null default false,
  allows_multiple_people boolean not null default false,
  team_id uuid,
  -- One of 'reception_person' | 'sound_person' | 'preaching_person' | 'conducting_person' |
  -- 'musical_message_person' | null - lets a role stay in sync with the legacy `scales` columns
  -- Android/exports/public pages already read, without those surfaces knowing anything changed.
  legacy_field_key text,
  created_at bigint not null,
  updated_at bigint not null,
  created_by uuid
);

create table organization_teams (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  name text not null,
  description text not null default '',
  icon_key text not null default 'users',
  color_token text,
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at bigint not null,
  updated_at bigint not null
);

alter table organization_roles add constraint organization_roles_team_fk
  foreign key (team_id) references organization_teams(id) on delete set null;

create table organization_people (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  full_name text not null,
  display_name text,
  email text,
  phone text,
  photo_url text,
  notes text not null default '',
  is_favorite boolean not null default false,
  is_active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  created_by uuid,
  -- Bloco 4: a person can optionally be linked to a login - never required.
  linked_user_id uuid
);

create table person_team_memberships (
  person_id uuid not null references organization_people(id) on delete cascade,
  team_id uuid not null references organization_teams(id) on delete cascade,
  is_primary boolean not null default false,
  created_at bigint not null,
  primary key (person_id, team_id)
);

create table scale_assignments (
  id uuid primary key default gen_random_uuid(),
  scale_id uuid not null references scales(id) on delete cascade,
  role_id uuid not null references organization_roles(id),
  person_id uuid references organization_people(id) on delete set null,
  custom_person_name text,
  -- Snapshots (Parte 3): preserve what the schedule actually said at the time, even if the role
  -- or person is renamed later - history should never silently rewrite itself.
  role_name_snapshot text not null,
  person_name_snapshot text not null default '',
  position integer not null default 0,
  notes text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);

create table scale_templates (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  name text not null,
  description text not null default '',
  -- [{roleId, position}] - templates only carry role structure, never people (Parte 13).
  roles jsonb not null default '[]',
  default_start_time time,
  default_end_time time,
  default_notes text not null default '',
  is_favorite boolean not null default false,
  is_active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  created_by uuid
);

create index organization_roles_church_idx on organization_roles (church_id, sort_order);
create index organization_people_church_idx on organization_people (church_id, is_active);
create index organization_teams_church_idx on organization_teams (church_id, sort_order);
create index scale_assignments_scale_idx on scale_assignments (scale_id, position);
create index scale_assignments_role_idx on scale_assignments (role_id);
create index scale_assignments_person_idx on scale_assignments (person_id);
create index scale_templates_church_idx on scale_templates (church_id, is_active);

alter table organization_roles enable row level security;
alter table organization_teams enable row level security;
alter table organization_people enable row level security;
alter table person_team_memberships enable row level security;
alter table scale_assignments enable row level security;
alter table scale_templates enable row level security;

-- Public read (Parte 35): matches the existing precedent for scales/doxologies - a schedule and
-- who's assigned to it is already public information on the church's own site. Only admins of the
-- owning church may write.
create policy "organization_roles: public read" on organization_roles for select using (true);
create policy "organization_roles: admin write" on organization_roles for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));
create policy "organization_roles: admin update" on organization_roles for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));
create policy "organization_roles: admin delete" on organization_roles for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));

create policy "organization_teams: public read" on organization_teams for select using (true);
create policy "organization_teams: admin write" on organization_teams for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));
create policy "organization_teams: admin update" on organization_teams for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));
create policy "organization_teams: admin delete" on organization_teams for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));

create policy "organization_people: public read" on organization_people for select using (true);
create policy "organization_people: admin write" on organization_people for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));
create policy "organization_people: admin update" on organization_people for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));
create policy "organization_people: admin delete" on organization_people for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));

create policy "person_team_memberships: public read" on person_team_memberships for select using (true);
create policy "person_team_memberships: admin write" on person_team_memberships for insert
  with check (exists (
    select 1 from organization_people p join profiles pr on pr.id = auth.uid()
    where p.id = person_team_memberships.person_id and pr.is_admin and pr.church_id = p.church_id
  ));
create policy "person_team_memberships: admin delete" on person_team_memberships for delete
  using (exists (
    select 1 from organization_people p join profiles pr on pr.id = auth.uid()
    where p.id = person_team_memberships.person_id and pr.is_admin and pr.church_id = p.church_id
  ));

create policy "scale_assignments: public read" on scale_assignments for select using (true);
create policy "scale_assignments: admin write" on scale_assignments for insert
  with check (exists (
    select 1 from scales s join profiles pr on pr.id = auth.uid()
    where s.id = scale_assignments.scale_id and pr.is_admin and pr.church_id = s.church_id
  ));
create policy "scale_assignments: admin update" on scale_assignments for update
  using (exists (
    select 1 from scales s join profiles pr on pr.id = auth.uid()
    where s.id = scale_assignments.scale_id and pr.is_admin and pr.church_id = s.church_id
  ));
create policy "scale_assignments: admin delete" on scale_assignments for delete
  using (exists (
    select 1 from scales s join profiles pr on pr.id = auth.uid()
    where s.id = scale_assignments.scale_id and pr.is_admin and pr.church_id = s.church_id
  ));

create policy "scale_templates: admin read" on scale_templates for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin write" on scale_templates for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin update" on scale_templates for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin delete" on scale_templates for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));

grant select on public.organization_roles, public.organization_teams, public.organization_people, public.person_team_memberships, public.scale_assignments to anon, authenticated;
grant insert, update, delete on public.organization_roles, public.organization_teams, public.organization_people, public.person_team_memberships, public.scale_assignments to authenticated;
grant select, insert, update, delete on public.scale_templates to authenticated;

-- Parte 3: seed the five existing hardcoded functions as real, renameable OrganizationRole rows
-- for every church that doesn't have them yet - this is what makes "migrar as funções existentes"
-- happen automatically, with no manual per-church setup, while never touching the legacy columns
-- themselves.
insert into organization_roles (church_id, name, icon_key, sort_order, is_required, legacy_field_key, created_at, updated_at)
select c.id, v.name, v.icon_key, v.sort_order, true, v.legacy_field_key, extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
from churches c
cross join (values
  ('Recepção', 'users', 0, 'reception_person'),
  ('Sonoplastia', 'headphones', 1, 'sound_person'),
  ('Pregação', 'book-open', 2, 'preaching_person'),
  ('Regência', 'music-2', 3, 'conducting_person'),
  ('Mensagem musical', 'music', 4, 'musical_message_person')
) as v(name, icon_key, sort_order, legacy_field_key)
where not exists (
  select 1 from organization_roles r where r.church_id = c.id and r.legacy_field_key = v.legacy_field_key
);

