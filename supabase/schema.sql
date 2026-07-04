-- Escala Church - Supabase schema
-- Run this once in Supabase Dashboard -> SQL Editor -> New query -> Run

-- Profiles: one row per auth user, admin flag drives write access everywhere else.
-- A brand new signup is never an admin by default - only a project owner can promote one
-- (see the "grant admin" query at the bottom), mirroring how Firebase Auth accounts were
-- created manually in the console.
create table profiles (
  id uuid primary key references auth.users(id) on delete cascade,
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

create policy "scales: public read" on scales for select using (true);

create policy "scales: admin write" on scales for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "scales: admin update" on scales for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "scales: admin delete" on scales for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));

-- Doxologies (Ordem do culto) ---------------------------------------------
create table doxologies (
  id uuid primary key default gen_random_uuid(),
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
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "doxologies: admin update" on doxologies for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "doxologies: admin delete" on doxologies for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));

-- Announcements (Anúncios) -------------------------------------------------
create table announcements (
  id uuid primary key default gen_random_uuid(),
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
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "announcements: admin update" on announcements for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "announcements: admin delete" on announcements for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));

-- Retrospective (photo/video feed of recent services and events) --------
create table retrospective_items (
  id uuid primary key default gen_random_uuid(),
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
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "retrospective_items: admin update" on retrospective_items for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "retrospective_items: admin delete" on retrospective_items for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));

-- Shared files (Sonoplastia's remote file sharing: PPT/PDF/photos/videos moved phone <-> PC) ----
create table shared_files (
  id uuid primary key default gen_random_uuid(),
  file_name text not null,
  url text not null,
  media_type text not null default 'DOCUMENT',
  size_bytes bigint not null default 0,
  uploaded_at bigint not null
);

alter table shared_files enable row level security;

create policy "shared_files: public read" on shared_files for select using (true);
create policy "shared_files: admin write" on shared_files for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin));
create policy "shared_files: admin delete" on shared_files for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin));

-- Grants: RLS policies above only control *which rows* a role can see/touch - Postgres also
-- requires the role to be granted the privilege to attempt the operation on the table at all.
-- Without these, PostgREST returns "permission denied for table X" even though the RLS policies
-- are otherwise satisfied.
grant usage on schema public to anon, authenticated;
grant select on public.scales, public.doxologies, public.announcements, public.shared_files, public.retrospective_items to anon, authenticated;
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
-- After running the block above, sign up (or ask the admin to sign up) once
-- through the app, then run this to promote that account to admin - replace
-- the email with the real admin's email:
--
-- update profiles set is_admin = true
-- where id = (select id from auth.users where email = 'admin@example.com');
