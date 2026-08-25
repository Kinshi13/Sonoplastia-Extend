-- ===========================================================================
-- APPLY_PENDING_014_016_SAFE_V2.sql
--
-- Substitui APPLY_PENDING_014_016_SAFE.sql (mantido no repositório, NÃO
-- sobrescrito - ver histórico do arquivo original). Diferença única: o
-- modelo de acesso de `web_push_subscriptions` foi redesenhado depois de
-- uma revisão de segurança que encontrou uma inconsistência real nas
-- policies "public update own endpoint"/"public delete own endpoint" da
-- migration 015 original - o nome dizia "own endpoint", mas o USING só
-- verificava `church_id` de uma igreja ativa, sem nenhuma prova de que o
-- chamador é o dono daquele endpoint. Como a tabela ainda NÃO existe em
-- produção (confirmado NOT_APPLIED), dá para corrigir isso na origem, sem
-- nenhuma migration corretiva depois - é uma instalação nova, não um
-- reparo.
--
-- 014 (worship_songs) e 016 (recommendation_message) permanecem
-- IDÊNTICOS ao V1 - a auditoria não encontrou problema neles (ver
-- relatório completo entregue junto com este arquivo). Só a seção 015
-- (web_push_subscriptions) mudou.
--
-- NÃO incluído de propósito: 017 (já APPLIED) e 006/010 (RLS real em
-- produção é a versão SAFE, diferente do arquivo cru - ver
-- MIGRATION_RECONCILIATION_REPORT.md, Categoria B).
--
-- Idempotente: seguro rodar mais de uma vez.
--   - CREATE TABLE ... IF NOT EXISTS
--   - ALTER TABLE ... ADD COLUMN IF NOT EXISTS
--   - CREATE INDEX ... IF NOT EXISTS
--   - DROP POLICY IF EXISTS antes de todo CREATE POLICY
--   - CREATE OR REPLACE FUNCTION (idempotente por natureza)
--
-- Não apaga dados, não trunca, não dropa tabela nem coluna, não altera
-- nenhum objeto que já existe além de adicionar o que falta a ele.
-- ===========================================================================

begin;

-- 014_worship_songs.sql (idêntico ao V1) -------------------------------------

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

-- "using (is_published)" - sem checar churches.is_active - é o mesmo padrão
-- usado por TODA outra tabela pública deste projeto (scales: public read
-- usa `using (true)`; doxologies idem; announcements/retrospective_items/
-- bulletins usam só a própria flag `is_active`, nunca a da igreja). Não é
-- uma lacuna do worship_songs - é a arquitetura inteira. Não alterado aqui,
-- para não introduzir uma exceção isolada e inconsistente com o resto do
-- schema (ver auditoria completa).
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

-- 015_worship_recommendation_and_push.sql -------------------------------------

alter table worship_songs add column if not exists program_type text;
alter table worship_songs add column if not exists is_daily_recommendation boolean not null default false;
alter table worship_songs add column if not exists recommendation_date date;
alter table worship_songs add column if not exists notification_enabled boolean not null default false;
alter table worship_songs add column if not exists notification_time time;
alter table worship_songs add column if not exists notification_title text;
alter table worship_songs add column if not exists notification_body text;

create index if not exists worship_songs_recommendation_date_idx on worship_songs (recommendation_date);
create index if not exists worship_songs_is_daily_recommendation_idx on worship_songs (is_daily_recommendation);

-- web_push_subscriptions -----------------------------------------------------
--
-- MUDANÇA DE SEGURANÇA vs a migration 015 original / V1 deste script:
--
-- A versão original tinha `grant insert, update, delete on
-- web_push_subscriptions to anon, authenticated` + policies de update/delete
-- que só checavam `church_id` de uma igreja ativa - qualquer chamador
-- (autenticado ou não) que soubesse (ou adivinhasse) o `id` de uma linha, ou
-- simplesmente soubesse o `church_id` de uma igreja ativa, podia UPDATE ou
-- DELETE em massa TODAS as inscrições daquela igreja, mesmo sem nunca ter
-- criado nenhuma delas - um ataque de negação de serviço de notificação
-- (desativa/apaga os lembretes de todo mundo), não é vazamento de dado (não
-- há SELECT nunca liberado), mas é real.
--
-- Correção: a tabela em si NÃO recebe nenhum grant de insert/update/delete
-- para anon/authenticated. Toda escrita passa por duas funções
-- `security definer`, estreitas e auditáveis, que só aceitam exatamente o
-- que uma inscrição/remoção legítima precisa - nunca uma condição genérica
-- por church_id sozinho:
--
--   upsert_web_push_subscription(...)  - criar/atualizar a PRÓPRIA
--     inscrição (identificada pelo endpoint que o navegador está enviando -
--     o mesmo padrão ON CONFLICT (endpoint) de antes, só que a validação e
--     a escrita agora rodam dentro da função, não mais direto pela policy).
--
--   delete_web_push_subscription(p_endpoint text) - remove exatamente UMA
--     linha, a que tem esse endpoint exato. Não aceita church_id sozinho,
--     não aceita id, não tem como apagar em massa.
--
-- Nenhuma das duas permite listar/ler nada (retornam void). O `security
-- definer` roda com o dono da função (bypassa RLS internamente), mas isso é
-- seguro justamente porque a lógica interna é fixa e estreita - o chamador
-- não controla a query, só os parâmetros.

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

-- De propósito: NENHUMA policy de insert/update/delete/select é criada aqui
-- para anon/authenticated - RLS ligado + zero policy = acesso negado por
-- padrão para todo mundo além do dono da tabela. Toda escrita legítima
-- passa pelas duas funções abaixo, que já embutem sua própria validação.

drop function if exists public.upsert_web_push_subscription(uuid, text, text, text, text, text, text[]);
create function public.upsert_web_push_subscription(
  p_church_id uuid,
  p_endpoint text,
  p_p256dh text,
  p_auth text,
  p_user_agent text default null,
  p_platform text default null,
  p_topics text[] default array['worship_daily']
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_endpoint is null or length(trim(p_endpoint)) = 0
     or p_p256dh is null or length(trim(p_p256dh)) = 0
     or p_auth is null or length(trim(p_auth)) = 0 then
    raise exception 'Dados de inscrição incompletos.';
  end if;

  if not exists (select 1 from public.churches where id = p_church_id and is_active) then
    raise exception 'Igreja inválida ou inativa.';
  end if;

  -- ON CONFLICT (endpoint) é o mecanismo natural de "própria inscrição": o
  -- `endpoint` é UNIQUE, então só existe conflito com a linha que já tem
  -- exatamente esse mesmo endpoint - nunca com a linha de outra pessoa.
  insert into public.web_push_subscriptions (
    church_id, endpoint, p256dh, auth, user_agent, platform, topics,
    enabled, created_at, updated_at, last_seen_at
  ) values (
    p_church_id, p_endpoint, p_p256dh, p_auth, p_user_agent, p_platform,
    coalesce(p_topics, array['worship_daily']),
    true,
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint
  )
  on conflict (endpoint) do update set
    church_id = excluded.church_id,
    p256dh = excluded.p256dh,
    auth = excluded.auth,
    user_agent = excluded.user_agent,
    platform = excluded.platform,
    topics = excluded.topics,
    enabled = true,
    updated_at = excluded.updated_at,
    last_seen_at = excluded.last_seen_at;
end;
$$;

revoke all on function public.upsert_web_push_subscription(uuid, text, text, text, text, text, text[]) from public;
grant execute on function public.upsert_web_push_subscription(uuid, text, text, text, text, text, text[]) to anon, authenticated;

drop function if exists public.delete_web_push_subscription(text);
create function public.delete_web_push_subscription(p_endpoint text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_endpoint is null or length(trim(p_endpoint)) = 0 then
    return; -- unsubscribe de um endpoint vazio/ausente é um no-op silencioso, não um erro
  end if;
  -- Só apaga a UNICA linha com esse endpoint exato - não há como este
  -- comando atingir mais de uma linha (endpoint é UNIQUE), nem como
  -- direcioná-lo por church_id/id.
  delete from public.web_push_subscriptions where endpoint = p_endpoint;
end;
$$;

revoke all on function public.delete_web_push_subscription(text) from public;
grant execute on function public.delete_web_push_subscription(text) to anon, authenticated;

-- 016_worship_recommendation_message.sql (idêntico ao V1) --------------------

alter table worship_songs add column if not exists recommendation_message text;

create unique index if not exists worship_songs_one_recommendation_per_day
  on worship_songs (church_id, recommendation_date)
  where is_daily_recommendation and recommendation_date is not null;

commit;

-- Recarrega o cache de esquema do PostgREST imediatamente (fora da transação
-- acima de propósito - é um sinal assíncrono, não uma alteração de dados).
notify pgrst, 'reload schema';
