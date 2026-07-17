-- Hotfix: converts each old scale's five legacy person columns into real scale_assignments rows,
-- so "Funções e pessoas" shows real, editable data for scales created before this system existed
-- instead of relying only on the client-side fallback (lib/legacyRoles.ts). Additive and
-- idempotent - never touches the legacy columns themselves, never overwrites/duplicates an
-- assignment that already exists, safe to run more than once.
--
-- REQUIRES: 006_plans_entitlements.sql through 010_organization_roles_people.sql already applied
-- (this needs organization_roles/scale_assignments to exist). Confirmed live against production
-- that as of this hotfix, 006-010 have NOT been applied yet - run
-- RUN_PENDING_MIGRATIONS_006_010.sql first, then this one.

-- Step 0: same idempotent seed as 010's own backfill, in case a church was created between 010
-- running and this migration running (010's own seed only covers churches that existed at 010's
-- own run time).
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

-- Step 1: backfill scale_assignments from each of the five legacy columns. One statement per
-- column (rather than one generic unpivoted query) so each is trivial to read, audit, and re-run
-- individually if needed. `not exists` against scale_assignments is the idempotency/no-overwrite
-- guard (Parte "não sobrescreva assignments existentes").
with inserted_reception as (
  insert into scale_assignments (scale_id, role_id, person_id, custom_person_name, role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at)
  select s.id, r.id, null, s.reception_person, r.name, s.reception_person, 0, '', extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
  from scales s
  join organization_roles r on r.church_id = s.church_id and r.legacy_field_key = 'reception_person'
  where coalesce(trim(s.reception_person), '') <> ''
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id and sa.role_id = r.id)
  returning 1
)
select 'reception_person backfilled' as step, count(*) as rows_converted from inserted_reception;

with inserted_sound as (
  insert into scale_assignments (scale_id, role_id, person_id, custom_person_name, role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at)
  select s.id, r.id, null, s.sound_person, r.name, s.sound_person, 1, '', extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
  from scales s
  join organization_roles r on r.church_id = s.church_id and r.legacy_field_key = 'sound_person'
  where coalesce(trim(s.sound_person), '') <> ''
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id and sa.role_id = r.id)
  returning 1
)
select 'sound_person backfilled' as step, count(*) as rows_converted from inserted_sound;

with inserted_preaching as (
  insert into scale_assignments (scale_id, role_id, person_id, custom_person_name, role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at)
  select s.id, r.id, null, s.preaching_person, r.name, s.preaching_person, 2, '', extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
  from scales s
  join organization_roles r on r.church_id = s.church_id and r.legacy_field_key = 'preaching_person'
  where coalesce(trim(s.preaching_person), '') <> ''
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id and sa.role_id = r.id)
  returning 1
)
select 'preaching_person backfilled' as step, count(*) as rows_converted from inserted_preaching;

with inserted_conducting as (
  insert into scale_assignments (scale_id, role_id, person_id, custom_person_name, role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at)
  select s.id, r.id, null, s.conducting_person, r.name, s.conducting_person, 3, '', extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
  from scales s
  join organization_roles r on r.church_id = s.church_id and r.legacy_field_key = 'conducting_person'
  where coalesce(trim(s.conducting_person), '') <> ''
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id and sa.role_id = r.id)
  returning 1
)
select 'conducting_person backfilled' as step, count(*) as rows_converted from inserted_conducting;

with inserted_musical as (
  insert into scale_assignments (scale_id, role_id, person_id, custom_person_name, role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at)
  select s.id, r.id, null, s.musical_message_person, r.name, s.musical_message_person, 4, '', extract(epoch from now()) * 1000, extract(epoch from now()) * 1000
  from scales s
  join organization_roles r on r.church_id = s.church_id and r.legacy_field_key = 'musical_message_person'
  where coalesce(trim(s.musical_message_person), '') <> ''
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id and sa.role_id = r.id)
  returning 1
)
select 'musical_message_person backfilled' as step, count(*) as rows_converted from inserted_musical;

-- Step 2 (informational, run separately to check the result): total assignments now on file per
-- church, and how many scales still have zero assignments (should be none, for a church with
-- roles seeded, after the five statements above).
-- select s.church_id, count(distinct s.id) as scales, count(sa.id) as assignments
-- from scales s left join scale_assignments sa on sa.scale_id = s.id
-- group by s.church_id;
