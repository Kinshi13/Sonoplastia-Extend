-- ===========================================================================
-- 014_worship_songs.sql
-- "Música e Louvor" - lista de músicas vinculadas à programação/escala da
-- igreja, com link do YouTube, thumbnail automática e ordem de execução.
--
-- Auditoria antes de criar: nao existe nenhuma tabela equivalente hoje
-- (grep por "worship"/"musica"/"louvor" em schema.sql/migrations nao
-- retornou nada) - esta e uma tabela nova, nao uma duplicata.
--
-- Mesmo padrao ja usado por retrospective_items/announcements: leitura
-- publica so de linhas publicadas, mutacao so por admin da propria igreja.
-- "codigo da igreja" continua sendo churches.slug - nenhuma coluna nova de
-- codigo e criada aqui.
-- ===========================================================================

create table if not exists worship_songs (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  -- Optional link back to a specific scale (Escala do Dia) - nullable because a song list can
  -- also stand on its own for a program_date that doesn't have (or doesn't need) a linked scale.
  schedule_id uuid references scales(id) on delete set null,
  program_date date,
  title text not null default '',
  artist text not null default '',
  youtube_url text not null default '',
  youtube_video_id text not null default '',
  thumbnail_url text not null default '',
  moment_label text not null default '',
  notes text not null default '',
  order_index integer not null default 0,
  is_published boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null
);

create index if not exists worship_songs_church_idx on worship_songs (church_id);
create index if not exists worship_songs_schedule_idx on worship_songs (schedule_id);
create index if not exists worship_songs_program_date_idx on worship_songs (program_date);
create index if not exists worship_songs_published_idx on worship_songs (is_published);
create index if not exists worship_songs_order_idx on worship_songs (church_id, program_date, order_index);

alter table worship_songs enable row level security;

create policy "worship_songs: public read published" on worship_songs for select using (is_published);
create policy "worship_songs: admin write" on worship_songs for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));
create policy "worship_songs: admin update" on worship_songs for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));
create policy "worship_songs: admin delete" on worship_songs for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));

grant select on public.worship_songs to anon, authenticated;
grant insert, update, delete on public.worship_songs to authenticated;
