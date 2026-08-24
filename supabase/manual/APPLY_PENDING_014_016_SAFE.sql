-- ===========================================================================
-- APPLY_PENDING_014_016_SAFE.sql
--
-- Contém SOMENTE o que está confirmado ausente em produção, segundo
-- REMOTE_SCHEMA_REPORT.sql rodado ao vivo e MIGRATION_RECONCILIATION_REPORT.md:
--
--   014 - tabela worship_songs (Música e Louvor) - MISSING confirmado
--   015 - colunas de recomendação/notificação em worship_songs + tabela
--         web_push_subscriptions - MISSING confirmado
--   016 - worship_songs.recommendation_message + índice de 1
--         recomendação/dia - MISSING confirmado (depende de 014)
--
-- NÃO incluído de propósito:
--   017 - já APPLIED (scales.is_temporary e scale_templates.is_protected
--         confirmados PRESENT em produção) - incluir de novo seria só um
--         no-op inofensivo, mas o pedido foi "somente o que falta", então
--         fica de fora.
--   006/010 - não fazem parte do que falta aqui. Além disso, a versão que
--         está de fato em produção para essas duas (RLS restrito de
--         "subscriptions"/"organization_people", ver
--         supabase/manual/RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql)
--         é diferente do arquivo cru em supabase/migrations/ - misturar
--         isso aqui arriscaria reintroduzir a policy aberta antiga por
--         engano. Ver MIGRATION_RECONCILIATION_REPORT.md, Categoria B.
--
-- Idempotente: seguro rodar mais de uma vez.
--   - CREATE TABLE ... IF NOT EXISTS
--   - ALTER TABLE ... ADD COLUMN IF NOT EXISTS
--   - CREATE INDEX ... IF NOT EXISTS
--   - DROP POLICY IF EXISTS antes de todo CREATE POLICY (Postgres não tem
--     CREATE POLICY IF NOT EXISTS)
--
-- Não apaga dados, não trunca, não dropa tabela nem coluna, não altera
-- nenhum objeto que já existe além de adicionar o que falta a ele
-- (worship_songs em si é uma tabela nova - nada pré-existente é tocado).
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

commit;

-- Recarrega o cache de esquema do PostgREST imediatamente (fora da transação
-- acima de propósito - é um sinal assíncrono, não uma alteração de dados).
notify pgrst, 'reload schema';
