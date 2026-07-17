-- ===========================================================================
-- RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql
-- ===========================================================================
-- Same base as RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT.sql (transactional,
-- idempotent, all of migrations 006-010, same tables, same seed data) plus three
-- security corrections requested after reviewing that file for production:
--
--   1. The subscriptions backfill no longer grants FOUNDER to every existing
--      church - it grants FREE/FREE/'migration_backfill'. PRO for the test
--      church is a separate, explicit step (see supabase/GRANT_TEST_PREMIUM.sql),
--      not something this migration decides on its own.
--   2. organization_people loses its public-read policy and its anon SELECT
--      grant. Only an authenticated admin of the same church can read it now.
--      Insert/update/delete were already admin-only and are unchanged.
--   3. subscriptions loses its public-read policy and its anon SELECT grant.
--      An authenticated user can now only read their own church's subscription
--      row (via profiles.church_id) - admin or not, since billing status
--      (plan/expiry) isn't itself a secret for a member of that church, but
--      it's no longer world-readable. Admin UPDATE is unchanged.
--
-- Every other table, policy, grant, index and the plan/role seed data are
-- byte-identical to the _IDEMPOTENT version - see the diff list at the end of
-- the chat response this file was delivered in for the exact line-by-line
-- differences. Original files (_IDEMPOTENT and the very first RUN_PENDING) are
-- untouched.
-- ===========================================================================

begin;

-- ---------------------------------------------------------------------------
-- 006_plans_entitlements.sql
-- ---------------------------------------------------------------------------
create table if not exists plans (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  name text not null,
  description text not null default '',
  monthly_price_cents integer,
  yearly_price_cents integer,
  currency text not null default 'BRL',
  billing_period text not null default 'recurring',
  is_active boolean not null default true,
  is_public boolean not null default true,
  sort_order integer not null default 0,
  features jsonb not null default '[]',
  limits jsonb not null default '{}',
  created_at bigint not null,
  updated_at bigint not null
);

alter table plans enable row level security;
drop policy if exists "plans: public read" on plans;
create policy "plans: public read" on plans for select using (true);

create table if not exists subscriptions (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null unique references churches(id),
  plan_id uuid not null references plans(id),
  status text not null default 'FREE',
  started_at bigint not null,
  expires_at bigint,
  trial_ends_at bigint,
  grace_period_ends_at bigint,
  source text not null default 'manual',
  updated_at bigint not null
);

alter table subscriptions enable row level security;

-- Correction 3: subscriptions is no longer world-readable. A plan/billing-status row is not
-- something an anonymous visitor of the public site needs, and it wasn't intentionally scoped
-- before - `using (true)` really did mean "anyone, including anon, can read every church's
-- subscription row". An authenticated user can now only read their own church's row.
drop policy if exists "subscriptions: public read" on subscriptions;
drop policy if exists "subscriptions: read own church" on subscriptions;
drop policy if exists "subscriptions: admin update" on subscriptions;
create policy "subscriptions: read own church" on subscriptions for select
  using (exists (select 1 from profiles where id = auth.uid() and church_id = subscriptions.church_id));
create policy "subscriptions: admin update" on subscriptions for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = subscriptions.church_id));

-- plans (the catalog) stays public - anon needs it to render pricing/plan comparison on the
-- public site before anyone logs in. subscriptions (who's on which plan, expiry, status) does not.
grant select on public.plans to anon, authenticated;
grant select on public.subscriptions to authenticated;
grant update on public.subscriptions to authenticated;

do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'plans') then
    alter publication supabase_realtime add table plans;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'subscriptions') then
    alter publication supabase_realtime add table subscriptions;
  end if;
end $$;

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

update plans set limits = '{"maxAdmins":1,"maxPersonalEvents":10,"maxPersonalCards":5,"historyMonths":1,"maxAnnouncements":null,"maxMediaStorageMb":100,"maxOrganizations":1}' where code = 'FREE';
update plans set limits = '{"maxAdmins":2,"maxPersonalEvents":50,"maxPersonalCards":30,"historyMonths":6,"maxAnnouncements":null,"maxMediaStorageMb":1000,"maxOrganizations":1}' where code = 'ESSENTIAL';
update plans set limits = '{"maxAdmins":5,"maxPersonalEvents":200,"maxPersonalCards":100,"historyMonths":24,"maxAnnouncements":null,"maxMediaStorageMb":5000,"maxOrganizations":1}' where code = 'PRO';
update plans set limits = '{"maxAdmins":null,"maxPersonalEvents":null,"maxPersonalCards":null,"historyMonths":null,"maxAnnouncements":null,"maxMediaStorageMb":20000,"maxOrganizations":3}' where code = 'ORGANIZATION';
update plans set limits = '{"maxAdmins":null,"maxPersonalEvents":null,"maxPersonalCards":null,"historyMonths":null,"maxAnnouncements":null,"maxMediaStorageMb":null,"maxOrganizations":null}' where code = 'FOUNDER';

-- Correction 1: every existing church gets FREE/FREE/'migration_backfill', not FOUNDER/ACTIVE.
-- Granting a full lifetime plan to every church that happens to predate this migration was never
-- something this script should decide unilaterally - the one church that needs PRO for testing
-- gets it explicitly and separately, via supabase/GRANT_TEST_PREMIUM.sql. This still guarantees
-- every church has *some* subscription row so entitlement lookups never hit a missing one.
insert into subscriptions (church_id, plan_id, status, started_at, source, updated_at)
select c.id, (select id from plans where code = 'FREE'), 'FREE', extract(epoch from now()) * 1000, 'migration_backfill', extract(epoch from now()) * 1000
from churches c
where not exists (select 1 from subscriptions s where s.church_id = c.id);

-- ---------------------------------------------------------------------------
-- 007_stripe_billing.sql
-- ---------------------------------------------------------------------------
alter table plans add column if not exists stripe_price_id_monthly text;
alter table plans add column if not exists stripe_price_id_yearly text;
alter table subscriptions add column if not exists stripe_customer_id text;
alter table subscriptions add column if not exists stripe_subscription_id text;

create unique index if not exists subscriptions_stripe_subscription_id_key
  on subscriptions (stripe_subscription_id)
  where stripe_subscription_id is not null;

-- Fill in your real Price IDs after creating the Products/Prices in the Stripe Dashboard, e.g.:
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ESSENTIAL';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'PRO';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ORGANIZATION';

-- ---------------------------------------------------------------------------
-- 008_export_and_atlas_prep.sql
-- ---------------------------------------------------------------------------
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV"]'::jsonb
  where code in ('ESSENTIAL') and not (features @> '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV"]'::jsonb);
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV","EXPORT_SCALE_PDF","EXPORT_SCALE_IMAGE","PUBLIC_READONLY_LINK"]'::jsonb
  where code in ('PRO', 'ORGANIZATION', 'FOUNDER')
    and not (features @> '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV","EXPORT_SCALE_PDF","EXPORT_SCALE_IMAGE","PUBLIC_READONLY_LINK"]'::jsonb);

create table if not exists export_audit_log (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  user_id uuid not null references auth.users(id),
  format text not null,
  period text,
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

create table if not exists integration_outbox (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references churches(id),
  app_code text not null default 'STELLA_SCALE',
  event_type text not null,
  entity_type text not null,
  entity_id uuid not null,
  payload jsonb not null default '{}',
  status text not null default 'PENDING',
  attempts integer not null default 0,
  created_at bigint not null,
  processed_at bigint,
  last_error text
);

alter table integration_outbox enable row level security;
-- No client-facing policy on purpose - service-role worker only, same as before.

-- ---------------------------------------------------------------------------
-- 009_doxology_reuse.sql
-- ---------------------------------------------------------------------------
alter table doxologies add column if not exists is_favorite boolean not null default false;
alter table doxologies add column if not exists reused_from_doxology_id uuid references doxologies(id) on delete set null;
alter table doxologies add column if not exists times_reused integer not null default 0;

create index if not exists doxologies_church_favorite_idx on doxologies (church_id, is_favorite, date desc);

-- ---------------------------------------------------------------------------
-- 010_organization_roles_people.sql
-- ---------------------------------------------------------------------------
create table if not exists organization_roles (
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
  legacy_field_key text,
  created_at bigint not null,
  updated_at bigint not null,
  created_by uuid
);

create table if not exists organization_teams (
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

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'organization_roles_team_fk'
  ) then
    alter table organization_roles add constraint organization_roles_team_fk
      foreign key (team_id) references organization_teams(id) on delete set null;
  end if;
end $$;

create table if not exists organization_people (
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
  linked_user_id uuid
);

create table if not exists person_team_memberships (
  person_id uuid not null references organization_people(id) on delete cascade,
  team_id uuid not null references organization_teams(id) on delete cascade,
  is_primary boolean not null default false,
  created_at bigint not null,
  primary key (person_id, team_id)
);

-- Note (unchanged from the original): scale_assignments.scale_id -> scales(id) on delete cascade
-- means deleting a scale from here on also deletes its scale_assignments rows. Intentional and
-- matches the app's own deleteScaleAction (it deletes the scale outright, not archives it) - just
-- flagging that this is the one place this migration changes future-delete behavior on a table
-- (`scales`) that already has real data, even though it deletes nothing right now.
create table if not exists scale_assignments (
  id uuid primary key default gen_random_uuid(),
  scale_id uuid not null references scales(id) on delete cascade,
  role_id uuid not null references organization_roles(id),
  person_id uuid references organization_people(id) on delete set null,
  custom_person_name text,
  role_name_snapshot text not null,
  person_name_snapshot text not null default '',
  position integer not null default 0,
  notes text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);

create table if not exists scale_templates (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  name text not null,
  description text not null default '',
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

create index if not exists organization_roles_church_idx on organization_roles (church_id, sort_order);
create index if not exists organization_people_church_idx on organization_people (church_id, is_active);
create index if not exists organization_teams_church_idx on organization_teams (church_id, sort_order);
create index if not exists scale_assignments_scale_idx on scale_assignments (scale_id, position);
create index if not exists scale_assignments_role_idx on scale_assignments (role_id);
create index if not exists scale_assignments_person_idx on scale_assignments (person_id);
create index if not exists scale_templates_church_idx on scale_templates (church_id, is_active);

alter table organization_roles enable row level security;
alter table organization_teams enable row level security;
alter table organization_people enable row level security;
alter table person_team_memberships enable row level security;
alter table scale_assignments enable row level security;
alter table scale_templates enable row level security;

drop policy if exists "organization_roles: public read" on organization_roles;
drop policy if exists "organization_roles: admin write" on organization_roles;
drop policy if exists "organization_roles: admin update" on organization_roles;
drop policy if exists "organization_roles: admin delete" on organization_roles;
create policy "organization_roles: public read" on organization_roles for select using (true);
create policy "organization_roles: admin write" on organization_roles for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));
create policy "organization_roles: admin update" on organization_roles for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));
create policy "organization_roles: admin delete" on organization_roles for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_roles.church_id));

drop policy if exists "organization_teams: public read" on organization_teams;
drop policy if exists "organization_teams: admin write" on organization_teams;
drop policy if exists "organization_teams: admin update" on organization_teams;
drop policy if exists "organization_teams: admin delete" on organization_teams;
create policy "organization_teams: public read" on organization_teams for select using (true);
create policy "organization_teams: admin write" on organization_teams for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));
create policy "organization_teams: admin update" on organization_teams for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));
create policy "organization_teams: admin delete" on organization_teams for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_teams.church_id));

-- Correction 2: organization_people is no longer world-readable (not even to a logged-in
-- non-admin member) - real names, and optionally emails/phone numbers, shouldn't be selectable by
-- anon just because a church has a public site. Only an authenticated admin of that church can
-- read this table now; insert/update/delete were already admin-only and stay exactly as they were.
drop policy if exists "organization_people: public read" on organization_people;
drop policy if exists "organization_people: admin read own church" on organization_people;
drop policy if exists "organization_people: admin write" on organization_people;
drop policy if exists "organization_people: admin update" on organization_people;
drop policy if exists "organization_people: admin delete" on organization_people;
create policy "organization_people: admin read own church" on organization_people for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));
create policy "organization_people: admin write" on organization_people for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));
create policy "organization_people: admin update" on organization_people for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));
create policy "organization_people: admin delete" on organization_people for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = organization_people.church_id));

drop policy if exists "person_team_memberships: public read" on person_team_memberships;
drop policy if exists "person_team_memberships: admin write" on person_team_memberships;
drop policy if exists "person_team_memberships: admin delete" on person_team_memberships;
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

drop policy if exists "scale_assignments: public read" on scale_assignments;
drop policy if exists "scale_assignments: admin write" on scale_assignments;
drop policy if exists "scale_assignments: admin update" on scale_assignments;
drop policy if exists "scale_assignments: admin delete" on scale_assignments;
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

drop policy if exists "scale_templates: admin read" on scale_templates;
drop policy if exists "scale_templates: admin write" on scale_templates;
drop policy if exists "scale_templates: admin update" on scale_templates;
drop policy if exists "scale_templates: admin delete" on scale_templates;
create policy "scale_templates: admin read" on scale_templates for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin write" on scale_templates for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin update" on scale_templates for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));
create policy "scale_templates: admin delete" on scale_templates for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scale_templates.church_id));

-- Correction 2 (grants half): organization_people drops out of the anon/authenticated combined
-- SELECT grant and gets its own authenticated-only one - table-level GRANT is a precondition for
-- the RLS policy above to ever run at all, so both have to change together.
grant select on public.organization_roles, public.organization_teams, public.person_team_memberships, public.scale_assignments to anon, authenticated;
grant select on public.organization_people to authenticated;
grant insert, update, delete on public.organization_roles, public.organization_teams, public.organization_people, public.person_team_memberships, public.scale_assignments to authenticated;
grant select, insert, update, delete on public.scale_templates to authenticated;

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

commit;
