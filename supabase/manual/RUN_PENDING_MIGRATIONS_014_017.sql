-- ===========================================================================
-- RUN_PENDING_MIGRATIONS_014_017.sql
--
-- Consolidado idempotente das migrations 014-017, para colar direto no
-- Supabase Dashboard -> SQL Editor e rodar de uma vez só.
--
-- Por que este arquivo existe: este ambiente de execução não tem credenciais
-- de banco (sem service role key, sem senha do Postgres, sem projeto Supabase
-- CLI vinculado) - não é possível aplicar migrations diretamente aqui. As
-- migrations 014-017 já existem como arquivos versionados em
-- supabase/migrations/, mas aparentemente nunca foram executadas no banco
-- real (confirmado: uma consulta direta via REST a worship_songs retornou
-- HTTP 404 / PGRST205 "Could not find the table 'public.worship_songs'").
--
-- Cobre:
--   014 - tabela worship_songs (Música e Louvor)
--   015 - colunas de recomendação do dia / notificação + tabela
--         web_push_subscriptions
--   016 - coluna recommendation_message + índice de uma recomendação
--         principal por dia
--   017 - scales.is_temporary (Escala Temporária) + scale_templates.is_protected
--         (Escalas padrão protegidas)
--
-- Seguro rodar mais de uma vez: todo `create table`/`create index` usa
-- `if not exists`, todo `alter table ... add column` usa `if not exists`, e
-- toda `create policy` é precedida por `drop policy if exists` (Postgres não
-- tem `CREATE POLICY IF NOT EXISTS`).
-- ===========================================================================

begin;

-- 014_worship_songs.sql -----------------------------------------------------

create table if not exists worship_songs (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
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

drop policy if exists "worship_songs: public read published" on worship_songs;
create policy "worship_songs: public read published" on worship_songs for select using (is_published);

drop policy if exists "worship_songs: admin write" on worship_songs;
create policy "worship_songs: admin write" on worship_songs for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));

drop policy if exists "worship_songs: admin update" on worship_songs;
create policy "worship_songs: admin update" on worship_songs for update
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));

drop policy if exists "worship_songs: admin delete" on worship_songs;
create policy "worship_songs: admin delete" on worship_songs for delete
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = worship_songs.church_id));

grant select on public.worship_songs to anon, authenticated;
grant insert, update, delete on public.worship_songs to authenticated;

-- 015_worship_recommendation_and_push.sql ------------------------------------

alter table worship_songs add column if not exists program_type text;
alter table worship_songs add column if not exists is_daily_recommendation boolean not null default false;
alter table worship_songs add column if not exists recommendation_date date;
alter table worship_songs add column if not exists notification_enabled boolean not null default false;
alter table worship_songs add column if not exists notification_time time;
alter table worship_songs add column if not exists notification_title text;
alter table worship_songs add column if not exists notification_body text;

create index if not exists worship_songs_recommendation_date_idx on worship_songs (recommendation_date);
create index if not exists worship_songs_is_daily_recommendation_idx on worship_songs (is_daily_recommendation);

create table if not exists web_push_subscriptions (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  endpoint text not null unique,
  p256dh text not null,
  auth text not null,
  user_agent text,
  platform text,
  topics text[] not null default array['worship_daily'],
  enabled boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  last_seen_at bigint not null
);

create index if not exists web_push_subscriptions_church_idx on web_push_subscriptions (church_id);
create index if not exists web_push_subscriptions_enabled_idx on web_push_subscriptions (enabled);

alter table web_push_subscriptions enable row level security;

drop policy if exists "web_push_subscriptions: public insert for active church" on web_push_subscriptions;
create policy "web_push_subscriptions: public insert for active church" on web_push_subscriptions for insert
  with check (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));

drop policy if exists "web_push_subscriptions: public update own endpoint" on web_push_subscriptions;
create policy "web_push_subscriptions: public update own endpoint" on web_push_subscriptions for update
  using (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));

drop policy if exists "web_push_subscriptions: public delete own endpoint" on web_push_subscriptions;
create policy "web_push_subscriptions: public delete own endpoint" on web_push_subscriptions for delete
  using (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));

grant insert, update, delete on public.web_push_subscriptions to anon, authenticated;

-- 016_worship_recommendation_message.sql -------------------------------------

alter table worship_songs add column if not exists recommendation_message text;

create unique index if not exists worship_songs_one_recommendation_per_day
  on worship_songs (church_id, recommendation_date)
  where is_daily_recommendation and recommendation_date is not null;

-- 017_scale_templates_and_temporary.sql ---------------------------------------

alter table scales add column if not exists is_temporary boolean not null default false;

alter table scale_templates add column if not exists is_protected boolean not null default false;

commit;
