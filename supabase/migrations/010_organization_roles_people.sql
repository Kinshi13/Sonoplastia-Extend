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
