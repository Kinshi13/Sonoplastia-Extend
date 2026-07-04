-- Adds the Bulletins (Boletins) feature - PDF newsletters, optionally linked to one announcement.
-- Safe to run once on the existing production database.
create table if not exists bulletins (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  title text not null,
  pdf_url text not null,
  pdf_file_name text,
  cover_url text,
  related_announcement_id uuid references announcements(id) on delete set null,
  published_at bigint not null,
  updated_at bigint not null,
  is_active boolean not null default true
);

alter table bulletins enable row level security;

drop policy if exists "bulletins: public read active" on bulletins;
drop policy if exists "bulletins: admin write" on bulletins;
drop policy if exists "bulletins: admin update" on bulletins;
drop policy if exists "bulletins: admin delete" on bulletins;

create policy "bulletins: public read active" on bulletins for select using (is_active);
create policy "bulletins: admin write" on bulletins for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = bulletins.church_id));
create policy "bulletins: admin update" on bulletins for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = bulletins.church_id));
create policy "bulletins: admin delete" on bulletins for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = bulletins.church_id));

grant select on public.bulletins to anon, authenticated;
grant insert, update, delete on public.bulletins to authenticated;

alter publication supabase_realtime add table bulletins;
