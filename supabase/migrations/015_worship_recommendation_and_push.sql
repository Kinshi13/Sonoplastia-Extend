-- ===========================================================================
-- 015_worship_recommendation_and_push.sql
-- Música e Louvor - "Recomendação do dia" + base para notificações web.
--
-- Auditoria antes de alterar: worship_songs já existe (014_worship_songs.sql) -
-- estas colunas são aditivas (add column if not exists), nenhuma recriada.
-- web_push_subscriptions é nova - não existe nada equivalente hoje.
--
-- Envio real de push NÃO é feito por esta migration nem por este ciclo de
-- trabalho - só a estrutura (tabela + RLS) para capturar a inscrição do
-- navegador. Ver o server action de assinatura para o que já funciona hoje.
-- ===========================================================================

alter table worship_songs add column if not exists program_type text;
alter table worship_songs add column if not exists is_daily_recommendation boolean not null default false;
alter table worship_songs add column if not exists recommendation_date date;
alter table worship_songs add column if not exists notification_enabled boolean not null default false;
alter table worship_songs add column if not exists notification_time time;
alter table worship_songs add column if not exists notification_title text;
alter table worship_songs add column if not exists notification_body text;

create index if not exists worship_songs_recommendation_date_idx on worship_songs (recommendation_date);
create index if not exists worship_songs_is_daily_recommendation_idx on worship_songs (is_daily_recommendation);

-- Web Push subscriptions --------------------------------------------------
-- One row per browser/device subscription (the Push API's own `endpoint` is already globally
-- unique per browser install, so it doubles as the natural dedup key). `topics` starts with just
-- "worship_daily" (Bloco 18: "nesta etapa, usar inicialmente: worship_daily") - modeled as an
-- array from day one so announcements/schedule_updates/doxology can subscribe to the same table
-- later without a schema change.
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

-- There is no visitor login on the public site (only Admin has real Supabase Auth) - "usuário
-- comum" subscribes to reminders without any account, so the insert/update/delete policies below
-- are necessarily open to anon, the same trust boundary the rest of the public site already
-- operates under (anyone who can view a church's public page by slug already sees everything
-- non-admin there). The one guard available is that `church_id` must reference a real, active
-- church - this can't be a login check (there is none), but it stops a subscription being
-- attached to a made-up/inactive church id.
--
-- No SELECT policy at all, for anon or authenticated: a subscription's endpoint/keys are the
-- closest thing to a credential this table has (Bloco 20: "não vender como mensagem individual"),
-- and the browser never needs to read this row back - it already holds its own PushSubscription
-- object locally, from the Push API itself.
create policy "web_push_subscriptions: public insert for active church" on web_push_subscriptions for insert
  with check (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));
create policy "web_push_subscriptions: public update own endpoint" on web_push_subscriptions for update
  using (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));
create policy "web_push_subscriptions: public delete own endpoint" on web_push_subscriptions for delete
  using (exists (select 1 from churches where id = web_push_subscriptions.church_id and is_active));

grant insert, update, delete on public.web_push_subscriptions to anon, authenticated;
