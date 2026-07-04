-- Escala Church - Supabase schema
-- Run this once in Supabase Dashboard -> SQL Editor -> New query -> Run
--
-- This file describes the schema for a brand-new project. If you already have a live project
-- from before multi-tenancy, do NOT re-run this file - use supabase/migrations/002_multi_tenant.sql
-- instead, which upgrades an existing single-church database in place without losing data.

-- Churches: one row per paying customer. A church only becomes browsable/writable once
-- is_active is true, which the Stripe webhook flips after a successful one-time payment.
create table churches (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  is_active boolean not null default false,
  created_at bigint not null
);

alter table churches enable row level security;

-- Public read is open (not scoped by is_active) so the "check se o link existe" flow on
-- /c/[slug] can tell a real-but-inactive church (payment pending) apart from a typo/404.
create policy "churches: public read" on churches for select using (true);

-- Profiles: one row per auth user. church_id + is_admin together say "this person
-- administers this church" - both are set at once by the Stripe webhook, never by the app
-- directly, so there's no self-serve way to become an admin without paying.
create table profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  church_id uuid references churches(id),
  is_admin boolean not null default false,
  created_at timestamptz not null default now()
);

alter table profiles enable row level security;

create policy "profiles: read own" on profiles
  for select using (auth.uid() = id);

create policy "profiles: insert own" on profiles
  for insert with check (auth.uid() = id);

-- Auto-create a profiles row whenever a new auth user signs up.
create function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id) values (new.id);
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Scales (Escala) --------------------------------------------------------
create table scales (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  date date not null,
  start_time time not null,
  end_time time,
  type text not null default 'COMMON_SCALE',
  title text not null,
  reception_person text not null default '',
  sound_person text not null default '',
  preaching_person text not null default '',
  conducting_person text not null default '',
  musical_message_person text not null default '',
  notes text not null default '',
  is_special_event boolean not null default false,
  source_type text not null default 'OFFICIAL',
  -- Epoch millis (bigint), not timestamptz, to match the app's Kotlin Long timestamps 1:1.
  created_at bigint not null,
  updated_at bigint not null
);

alter table scales enable row level security;

-- Public read stays unrestricted by RLS - a page for one church's slug is scoped by an
-- explicit `.eq("church_id", ...)` in the query itself, since anonymous visitors have no
-- session to scope by. RLS's job here is only to stop cross-church writes.
create policy "scales: public read" on scales for select using (true);

create policy "scales: admin write" on scales for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));
create policy "scales: admin update" on scales for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));
create policy "scales: admin delete" on scales for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = scales.church_id));

-- Doxologies (Ordem do culto) ---------------------------------------------
create table doxologies (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  date date not null,
  start_time time not null,
  title text not null,
  notes text not null default '',
  program_order jsonb not null default '[]',
  source_type text not null default 'OFFICIAL',
  created_at bigint not null,
  updated_at bigint not null
);

alter table doxologies enable row level security;

create policy "doxologies: public read" on doxologies for select using (true);
create policy "doxologies: admin write" on doxologies for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));
create policy "doxologies: admin update" on doxologies for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));
create policy "doxologies: admin delete" on doxologies for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = doxologies.church_id));

-- Announcements (Anúncios) -------------------------------------------------
create table announcements (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  title text not null,
  description text not null default '',
  media_type text not null default 'NONE',
  media_url text,
  media_file_name text,
  image_aspect_ratio text not null default '4:3',
  affected_classes text[] not null default '{}',
  related_event_date date,
  source_type text not null default 'OFFICIAL',
  published_at bigint not null,
  updated_at bigint not null,
  is_pinned boolean not null default false,
  is_active boolean not null default true
);

alter table announcements enable row level security;

create policy "announcements: public read active" on announcements for select using (is_active);
create policy "announcements: admin write" on announcements for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));
create policy "announcements: admin update" on announcements for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));
create policy "announcements: admin delete" on announcements for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = announcements.church_id));

-- Retrospective (photo/video feed of recent services and events) --------
create table retrospective_items (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  title text not null default '',
  description text not null default '',
  media_type text not null default 'IMAGE', -- 'IMAGE' | 'VIDEO'
  media_url text not null,
  media_file_name text,
  -- Original width:height (e.g. "16:9", "9:16") so the feed can crop to 4:3 while the
  -- detail/lightbox view can still render the media at its real proportions.
  media_aspect_ratio text not null default '4:3',
  -- First-frame thumbnail for videos, generated client-side at upload time - the <video>
  -- tag alone doesn't reliably paint a frame before playback across browsers.
  poster_url text,
  event_date date,
  published_at bigint not null,
  updated_at bigint not null,
  is_active boolean not null default true
);

alter table retrospective_items enable row level security;

create policy "retrospective_items: public read active" on retrospective_items for select using (is_active);
create policy "retrospective_items: admin write" on retrospective_items for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));
create policy "retrospective_items: admin update" on retrospective_items for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));
create policy "retrospective_items: admin delete" on retrospective_items for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = retrospective_items.church_id));

-- Shared files (Sonoplastia's remote file sharing: PPT/PDF/photos/videos moved phone <-> PC) ----
create table shared_files (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  file_name text not null,
  url text not null,
  -- 'IMAGE' | 'VIDEO' | 'DOCUMENT' | 'LINK' | 'YOUTUBE' - LINK/YOUTUBE rows have no Storage
  -- object behind them, just an external url.
  media_type text not null default 'DOCUMENT',
  size_bytes bigint not null default 0,
  is_pinned boolean not null default false,
  uploaded_at bigint not null
);

alter table shared_files enable row level security;

create policy "shared_files: public read" on shared_files for select using (true);
create policy "shared_files: admin write" on shared_files for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));
create policy "shared_files: admin update" on shared_files for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));
create policy "shared_files: admin delete" on shared_files for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));

-- Grants: RLS policies above only control *which rows* a role can see/touch - Postgres also
-- requires the role to be granted the privilege to attempt the operation on the table at all.
-- Without these, PostgREST returns "permission denied for table X" even though the RLS policies
-- are otherwise satisfied.
grant usage on schema public to anon, authenticated;
grant select on public.churches, public.scales, public.doxologies, public.announcements, public.shared_files, public.retrospective_items to anon, authenticated;
grant insert, update, delete on public.scales, public.doxologies, public.announcements, public.shared_files, public.retrospective_items to authenticated;
grant select, insert on public.profiles to authenticated;

-- Realtime: let clients subscribe to live changes on these tables.
alter publication supabase_realtime add table scales, doxologies, announcements, shared_files, retrospective_items;

-- Storage: one public bucket for everything shared from the app (announcement media and
-- files shared from the Sonoplastia screen). Create the bucket "church-files" first in
-- Dashboard -> Storage -> New bucket -> mark it Public, then run the policies below.
--
-- storage.objects already has RLS enabled by Supabase itself and anon/authenticated already
-- have base table grants out of the box - only the policies below are needed. "drop policy if
-- exists" makes this block safe to re-run if you already ran an earlier version of it.
--
-- Note: write access here only checks is_admin, not which church - any admin can upload to
-- any path. That's fine since actual reads/writes are always scoped by the church_id column
-- on the owning row (scales/announcements/etc), and storage paths are namespaced by feature
-- (announcements/, sonoplastia/, retrospectiva/), not by tenant.
drop policy if exists "church-files: public read" on storage.objects;
drop policy if exists "church-files: admin write" on storage.objects;
drop policy if exists "church-files: admin update" on storage.objects;
drop policy if exists "church-files: admin delete" on storage.objects;

create policy "church-files: public read" on storage.objects for select
  using (bucket_id = 'church-files');
create policy "church-files: admin write" on storage.objects for insert
  with check (bucket_id = 'church-files' and exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "church-files: admin update" on storage.objects for update
  using (bucket_id = 'church-files' and exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "church-files: admin delete" on storage.objects for delete
  using (bucket_id = 'church-files' and exists (select 1 from profiles where id = auth.uid() and is_admin));

-- -------------------------------------------------------------------------
-- Churches are normally activated by the Stripe webhook (see app/api/stripe/webhook), which
-- also sets the paying user's profile church_id + is_admin. To do it manually while testing:
--
-- insert into churches (slug, name, is_active, created_at) values ('minha-igreja', 'Minha Igreja', true, extract(epoch from now()) * 1000)
-- returning id;
--
-- update profiles set is_admin = true, church_id = '<id returned above>'
-- where id = (select id from auth.users where email = 'admin@example.com');
