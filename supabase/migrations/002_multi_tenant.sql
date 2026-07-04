-- Multi-tenant migration - run this ONCE on the existing production database (do not re-run
-- schema.sql, it would try to re-create tables that already exist).
--
-- BEFORE RUNNING: edit the two placeholder values right below to your real church's name/slug.
-- The slug becomes part of the public URL (seusite.com/c/<slug>), so keep it short, lowercase,
-- hyphen-separated, no spaces/accents.
do $$
declare
  v_church_name text := 'Minha Igreja';   -- <-- EDIT ME
  v_church_slug text := 'minha-igreja';   -- <-- EDIT ME
  v_church_id uuid;
begin

  -- 1. Churches table (safe if this is the first time this migration runs).
  create table if not exists churches (
    id uuid primary key default gen_random_uuid(),
    slug text not null unique,
    name text not null,
    is_active boolean not null default false,
    created_at bigint not null
  );
  alter table churches enable row level security;
  drop policy if exists "churches: public read" on churches;
  create policy "churches: public read" on churches for select using (true);

  -- 2. Create (or reuse) the church row for your existing data.
  select id into v_church_id from churches where slug = v_church_slug;
  if v_church_id is null then
    insert into churches (slug, name, is_active, created_at)
    values (v_church_slug, v_church_name, true, extract(epoch from now()) * 1000)
    returning id into v_church_id;
  end if;

  -- 3. Add church_id everywhere (nullable for now so the backfill below can populate it).
  alter table profiles add column if not exists church_id uuid references churches(id);
  alter table scales add column if not exists church_id uuid references churches(id);
  alter table doxologies add column if not exists church_id uuid references churches(id);
  alter table announcements add column if not exists church_id uuid references churches(id);
  alter table retrospective_items add column if not exists church_id uuid references churches(id);
  alter table shared_files add column if not exists church_id uuid references churches(id);

  -- 4. Backfill every existing row (and every existing profile - today there's only your own
  -- admin account(s), all belonging to this one church) to point at the church created above.
  update profiles set church_id = v_church_id where church_id is null;
  update scales set church_id = v_church_id where church_id is null;
  update doxologies set church_id = v_church_id where church_id is null;
  update announcements set church_id = v_church_id where church_id is null;
  update retrospective_items set church_id = v_church_id where church_id is null;
  update shared_files set church_id = v_church_id where church_id is null;

  raise notice 'Migrated to church_id = %', v_church_id;
end $$;

-- 5. Now that every row has a church_id, make it mandatory (skip profiles - future signups
-- won't have a church until they pay).
alter table scales alter column church_id set not null;
alter table doxologies alter column church_id set not null;
alter table announcements alter column church_id set not null;
alter table retrospective_items alter column church_id set not null;
alter table shared_files alter column church_id set not null;

-- 6. Replace the admin write/update/delete policies to also require the row's church_id to
-- match the acting admin's own church_id - this is what stops one church's admin from
-- touching another church's data.
drop policy if exists "scales: admin write" on scales;
drop policy if exists "scales: admin update" on scales;
drop policy if exists "scales: admin delete" on scales;
create policy "scales: admin write" on scales for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));
create policy "scales: admin update" on scales for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));
create policy "scales: admin delete" on scales for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));

drop policy if exists "doxologies: admin write" on doxologies;
drop policy if exists "doxologies: admin update" on doxologies;
drop policy if exists "doxologies: admin delete" on doxologies;
create policy "doxologies: admin write" on doxologies for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));
create policy "doxologies: admin update" on doxologies for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));
create policy "doxologies: admin delete" on doxologies for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));

drop policy if exists "announcements: admin write" on announcements;
drop policy if exists "announcements: admin update" on announcements;
drop policy if exists "announcements: admin delete" on announcements;
create policy "announcements: admin write" on announcements for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));
create policy "announcements: admin update" on announcements for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));
create policy "announcements: admin delete" on announcements for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));

drop policy if exists "retrospective_items: admin write" on retrospective_items;
drop policy if exists "retrospective_items: admin update" on retrospective_items;
drop policy if exists "retrospective_items: admin delete" on retrospective_items;
create policy "retrospective_items: admin write" on retrospective_items for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));
create policy "retrospective_items: admin update" on retrospective_items for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));
create policy "retrospective_items: admin delete" on retrospective_items for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));

drop policy if exists "shared_files: admin write" on shared_files;
drop policy if exists "shared_files: admin delete" on shared_files;
create policy "shared_files: admin write" on shared_files for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));
create policy "shared_files: admin delete" on shared_files for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));

-- 7. Grants: churches is a new table, needs the same base read grant as everything else.
grant select on public.churches to anon, authenticated;
