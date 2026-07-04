-- Adds pinning + link/YouTube rows to the Sonoplastia shared-files screen. Safe to run once on
-- the existing production database - existing rows default to unpinned.
alter table shared_files add column if not exists is_pinned boolean not null default false;

-- Pinning needs an UPDATE policy - shared_files only had insert/delete before.
drop policy if exists "shared_files: admin update" on shared_files;
create policy "shared_files: admin update" on shared_files for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = shared_files.church_id));
