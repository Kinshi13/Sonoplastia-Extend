-- ===========================================================================
-- VALIDATE_CHURCH_ENTRY_SCHEMA.sql
-- Fase 11.9 Parte 3 - diagnostico somente leitura para preparar a entrada por
-- codigo da igreja no Android. Nao contem INSERT/UPDATE/DELETE/DROP/ALTER.
-- Rode cada bloco no SQL Editor e compare com os achados documentados no
-- commit desta etapa (a versao ja executada contra producao com a anon key
-- esta reproduzida, resumida, no cabecalho de cada secao abaixo).
-- ===========================================================================

-- A. Estrutura real da tabela churches
-- Achado ao vivo: id uuid, slug text, name text, is_active boolean,
-- created_at bigint. Nao existe code/church_code/access_code/display_name/
-- logo_url - "slug" e o unico identificador publico hoje (o mesmo usado nas
-- URLs /c/<slug> do site).
select
  column_name,
  data_type,
  is_nullable,
  column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'churches'
order by ordinal_position;

-- B. Policies e grants de churches
-- Achado ao vivo: "churches: public read" using (true) - toda a tabela
-- (todas as igrejas, ativas ou nao) e legivel por anon sem filtro nem
-- paginacao. Isso e uma decisao intencional documentada em schema.sql (o
-- fluxo /c/[slug] do site precisa diferenciar "igreja inativa" de "slug
-- nao existe"). Uma RPC de busca por codigo NAO adiciona exposicao nova -
-- a tabela inteira ja e publica; a RPC so evita que o Android acople
-- dependencia direta ao nome/formato das colunas e permite normalizar a
-- entrada (trim/lower) num unico lugar.
select schemaname, tablename, policyname, permissive, roles, cmd, qual
from pg_policies
where schemaname = 'public' and tablename = 'churches';

select grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public' and table_name = 'churches';

-- C. profiles -> church_id
-- Achado ao vivo: SELECT de profiles retorna 42501 para anon (sem grant) -
-- so "profiles: read own" (auth.uid() = id) para authenticated. church_id e
-- nullable (fica null ate o Stripe webhook vincular a uma igreja).
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public' and table_name = 'profiles'
order by ordinal_position;

select schemaname, tablename, policyname, roles, cmd, qual
from pg_policies
where schemaname = 'public' and tablename = 'profiles';

-- D. subscriptions -> church_id
-- Achado ao vivo: SELECT direto por anon retorna 42501 (migration SAFE
-- 006-010). church_id e unique (uma subscription por igreja). Resolvido no
-- Android via RPC get_church_subscription(p_church_id) - inalterada aqui.
select schemaname, tablename, policyname, roles, cmd, qual
from pg_policies
where schemaname = 'public' and tablename = 'subscriptions';

-- E. scales / doxologies / announcements -> church_id
-- Achado ao vivo: todas com "public read using (true)" + church_id not null
-- - o Android ja filtra cada uma por church_id (ScaleRepository,
-- DoxologyRepository, AnnouncementRepository), so falta trocar de onde vem
-- esse church_id (BuildConfig fixo -> igreja ativa selecionada em runtime).
select table_name, column_name, is_nullable
from information_schema.columns
where table_schema = 'public'
  and table_name in ('scales', 'doxologies', 'announcements')
  and column_name = 'church_id';

select schemaname, tablename, policyname, roles, cmd, qual
from pg_policies
where schemaname = 'public' and tablename in ('scales', 'doxologies', 'announcements');

-- F. Quantas igrejas existem hoje (para planejar teste multi-igreja)
select id, slug, name, is_active, created_at from churches order by created_at asc;

-- G. Funcoes RPC ja instaladas (confirma get_church_subscription e, apos
-- rodar 013_public_church_lookup.sql, get_church_by_code)
select routine_name, data_type, security_type
from information_schema.routines
where routine_schema = 'public'
  and routine_name in ('get_church_subscription', 'get_church_by_code');
