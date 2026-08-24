-- ===========================================================================
-- REMOTE_SCHEMA_DIAGNOSTIC.sql
--
-- SOMENTE LEITURA. Não contém INSERT, UPDATE, DELETE, ALTER, CREATE, DROP,
-- TRUNCATE, GRANT, REVOKE, NOTIFY, nem qualquer comando de migration repair.
-- Toda consulta abaixo lê apenas de information_schema / pg_catalog (e, na
-- última seção, da tabela de controle do Supabase CLI, se ela existir) — o
-- Postgres nunca deixa uma consulta a essas views/tabelas de sistema alterar
-- dados ou schema, então isto é seguro para rodar em produção a qualquer
-- momento, quantas vezes quiser.
--
-- Objetivo: comparar o schema remoto real contra o que as migrations locais
-- 002-017 (+ schema.sql, o baseline) esperam que exista. Cole cada seção (ou
-- o arquivo inteiro, se o SQL Editor aceitar múltiplos result sets) no
-- Supabase SQL Editor e me envie os resultados — eu classifico cada migration
-- como APPLIED / PARTIALLY_APPLIED / NOT_APPLIED / SUPERSEDED / UNKNOWN depois
-- de ver os dados reais, não antes.
--
-- Nada aqui aplica, corrige ou reverte nada. PRODUCTION UNCHANGED ao rodar isto.
-- ===========================================================================


-- ===========================================================================
-- TABLES
-- Lista fixa = toda tabela esperada por schema.sql (baseline) + migrations
-- 002-017. LEFT JOIN contra information_schema.tables: uma linha com
-- table_schema/table_type NULL significa "não existe no banco remoto".
-- ===========================================================================
select
  expected.table_name,
  expected.expected_from,
  t.table_schema,
  t.table_type,
  (t.table_name is not null) as exists_remotely
from (values
  ('churches',                 'schema.sql (baseline)'),
  ('profiles',                 'schema.sql (baseline)'),
  ('scales',                   'schema.sql (baseline)'),
  ('doxologies',               'schema.sql (baseline)'),
  ('announcements',            'schema.sql (baseline)'),
  ('retrospective_items',      'schema.sql (baseline)'),
  ('shared_files',             'schema.sql (baseline)'),
  ('bulletins',                '004_bulletins.sql'),
  ('plans',                    '006_plans_entitlements.sql'),
  ('subscriptions',            '006_plans_entitlements.sql'),
  ('export_audit_log',         '008_export_and_atlas_prep.sql'),
  ('integration_outbox',       '008_export_and_atlas_prep.sql'),
  ('organization_roles',       '010_organization_roles_people.sql'),
  ('organization_teams',       '010_organization_roles_people.sql'),
  ('organization_people',      '010_organization_roles_people.sql'),
  ('person_team_memberships',  '010_organization_roles_people.sql'),
  ('scale_assignments',        '010_organization_roles_people.sql'),
  ('scale_templates',          '010_organization_roles_people.sql'),
  ('worship_songs',            '014_worship_songs.sql'),
  ('web_push_subscriptions',   '015_worship_recommendation_and_push.sql')
) as expected(table_name, expected_from)
left join information_schema.tables t
  on t.table_schema = 'public' and t.table_name = expected.table_name
order by exists_remotely asc, expected.table_name;


-- ===========================================================================
-- COLUMNS
-- Colunas críticas (as que já causaram incidente, ou que uma migration
-- recente adicionou) + colunas estruturais (church_id, timestamps, status,
-- is_published) nas tabelas multi-tenant. Mesma lógica: LEFT JOIN, linha com
-- data_type NULL = coluna ausente.
-- ===========================================================================
select
  expected.table_name,
  expected.column_name,
  expected.expected_from,
  c.data_type,
  c.is_nullable,
  c.column_default,
  (c.column_name is not null) as exists_remotely
from (values
  -- scales: incidente relatado (is_temporary) + o que ScaleForm/export usam
  ('scales', 'is_temporary',              '017_scale_templates_and_temporary.sql'),
  ('scales', 'is_special_event',          'schema.sql (baseline)'),
  ('scales', 'source_type',               'schema.sql (baseline)'),
  ('scales', 'church_id',                 'schema.sql (baseline)'),
  ('scales', 'created_at',                'schema.sql (baseline)'),
  ('scales', 'updated_at',                'schema.sql (baseline)'),
  -- reused_from_scale_id / equivalente: NÃO existe no código hoje (auditado
  -- numa sessão anterior - "Reutilizar"/"Aplicar modelo" criam um id novo
  -- sem nenhuma coluna de rastreamento). Incluído aqui só para o resultado
  -- confirmar isso também do lado do banco, não porque é esperado existir.
  ('scales', 'reused_from_scale_id',      'NÃO esperado - nenhuma migration cria isto, ver nota acima'),
  -- scale_templates
  ('scale_templates', 'is_protected',     '017_scale_templates_and_temporary.sql'),
  ('scale_templates', 'roles',            '010_organization_roles_people.sql'),
  ('scale_templates', 'church_id',        '010_organization_roles_people.sql'),
  -- worship_songs: tabela inteira + colunas de recomendação/notificação
  ('worship_songs', 'church_id',                  '014_worship_songs.sql'),
  ('worship_songs', 'schedule_id',                '014_worship_songs.sql'),
  ('worship_songs', 'is_published',               '014_worship_songs.sql'),
  ('worship_songs', 'order_index',                '014_worship_songs.sql'),
  ('worship_songs', 'program_type',               '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'is_daily_recommendation',    '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'recommendation_date',        '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'notification_enabled',       '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'notification_time',          '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'notification_title',         '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'notification_body',          '015_worship_recommendation_and_push.sql'),
  ('worship_songs', 'recommendation_message',     '016_worship_recommendation_message.sql'),
  -- web_push_subscriptions
  ('web_push_subscriptions', 'church_id',   '015_worship_recommendation_and_push.sql'),
  ('web_push_subscriptions', 'endpoint',    '015_worship_recommendation_and_push.sql'),
  ('web_push_subscriptions', 'enabled',     '015_worship_recommendation_and_push.sql'),
  -- organization_* / scale_assignments (migration 010)
  ('organization_roles', 'church_id',            '010_organization_roles_people.sql'),
  ('organization_roles', 'legacy_field_key',     '010_organization_roles_people.sql'),
  ('organization_roles', 'team_id',              '010_organization_roles_people.sql'),
  ('organization_people', 'church_id',           '010_organization_roles_people.sql'),
  ('organization_people', 'is_active',           '010_organization_roles_people.sql'),
  ('organization_people', 'linked_user_id',      '010_organization_roles_people.sql'),
  ('person_team_memberships', 'person_id',       '010_organization_roles_people.sql'),
  ('person_team_memberships', 'team_id',         '010_organization_roles_people.sql'),
  ('scale_assignments', 'scale_id',              '010_organization_roles_people.sql'),
  ('scale_assignments', 'role_id',               '010_organization_roles_people.sql'),
  ('scale_assignments', 'person_id',             '010_organization_roles_people.sql'),
  ('scale_assignments', 'role_name_snapshot',    '010_organization_roles_people.sql'),
  -- plans/subscriptions (006/007)
  ('plans', 'stripe_price_id_monthly',        '007_stripe_billing.sql'),
  ('plans', 'stripe_price_id_yearly',         '007_stripe_billing.sql'),
  ('subscriptions', 'status',                 '006_plans_entitlements.sql'),
  ('subscriptions', 'stripe_customer_id',     '007_stripe_billing.sql'),
  ('subscriptions', 'stripe_subscription_id', '007_stripe_billing.sql'),
  -- doxologies (009)
  ('doxologies', 'is_favorite',               '009_doxology_reuse.sql'),
  ('doxologies', 'reused_from_doxology_id',   '009_doxology_reuse.sql'),
  ('doxologies', 'times_reused',              '009_doxology_reuse.sql'),
  ('doxologies', 'end_time',                  '005_doxology_sessions.sql'),
  -- shared_files (003)
  ('shared_files', 'is_pinned',               '003_sonoplastia_pins.sql'),
  -- multi-tenant church_id (002) nas tabelas mais antigas
  ('doxologies', 'church_id',                 '002_multi_tenant.sql'),
  ('announcements', 'church_id',              '002_multi_tenant.sql'),
  ('retrospective_items', 'church_id',        '002_multi_tenant.sql'),
  ('shared_files', 'church_id',               '002_multi_tenant.sql')
) as expected(table_name, column_name, expected_from)
left join information_schema.columns c
  on c.table_schema = 'public'
  and c.table_name = expected.table_name
  and c.column_name = expected.column_name
order by exists_remotely asc, expected.table_name, expected.column_name;


-- ===========================================================================
-- RPCS
-- Toda função pública que o código (site + Android) chama, mais uma listagem
-- geral de tudo que existe em pg_proc no schema public (para pegar qualquer
-- função extra não documentada).
-- ===========================================================================

-- 1) As RPCs esperadas, com assinatura/retorno/security a partir do catálogo
select
  expected.function_name,
  expected.expected_from,
  p.oid is not null as exists_remotely,
  pg_get_function_identity_arguments(p.oid) as arguments,
  pg_get_function_result(p.oid) as returns,
  case when p.prosecdef then 'security definer' else 'security invoker' end as security,
  n.nspname as schema
from (values
  ('get_church_subscription', '012_public_subscription_lookup.sql'),
  ('get_church_by_code',      '013_public_church_lookup.sql')
) as expected(function_name, expected_from)
left join pg_proc p on p.proname = expected.function_name
left join pg_namespace n on n.oid = p.pronamespace and n.nspname = 'public'
order by exists_remotely asc, expected.function_name;

-- 2) Toda função que existe hoje no schema public (para achar qualquer coisa
--    fora da lista acima - RPCs manuais aplicadas sem registro, por exemplo)
select
  n.nspname as schema,
  p.proname as function_name,
  pg_get_function_identity_arguments(p.oid) as arguments,
  pg_get_function_result(p.oid) as returns,
  case when p.prosecdef then 'security definer' else 'security invoker' end as security
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
order by p.proname;


-- ===========================================================================
-- VIEWS
-- Nenhuma migration 002-017 cria view. Consulta incluída só para confirmar
-- que não existe nenhuma view não documentada no schema public.
-- ===========================================================================
select table_name as view_name
from information_schema.views
where table_schema = 'public'
order by table_name;


-- ===========================================================================
-- TRIGGERS
-- A única função de trigger conhecida é handle_new_user (schema.sql,
-- baseline - dispara em auth.users, não em nenhuma tabela criada por
-- 002-017). Consulta genérica para achar qualquer trigger no schema public
-- ou em auth.users.
-- ===========================================================================
select
  event_object_schema as table_schema,
  event_object_table as table_name,
  trigger_name,
  action_timing,
  event_manipulation,
  action_statement
from information_schema.triggers
where event_object_schema in ('public', 'auth')
order by event_object_schema, event_object_table, trigger_name;


-- ===========================================================================
-- ENUMS
-- Nenhuma migration 002-017 cria enum (type). Consulta genérica para
-- confirmar - se retornar linhas, é algo aplicado fora do que os arquivos
-- locais descrevem.
-- ===========================================================================
select
  t.typname as enum_name,
  e.enumlabel as value,
  e.enumsortorder as sort_order
from pg_type t
join pg_enum e on e.enumtypid = t.oid
join pg_namespace n on n.oid = t.typnamespace
where n.nspname = 'public'
order by t.typname, e.enumsortorder;


-- ===========================================================================
-- RLS
-- Para cada tabela crítica: RLS está ligado? Quais policies existem, para
-- qual comando, qual role, e as expressões using/with_check.
-- ===========================================================================

-- 1) RLS ligado/desligado por tabela
select
  c.relname as table_name,
  c.relrowsecurity as rls_enabled,
  c.relforcerowsecurity as rls_forced
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relkind = 'r'
  and c.relname in (
    'churches','profiles','scales','doxologies','announcements','retrospective_items',
    'shared_files','bulletins','plans','subscriptions','export_audit_log','integration_outbox',
    'organization_roles','organization_teams','organization_people','person_team_memberships',
    'scale_assignments','scale_templates','worship_songs','web_push_subscriptions'
  )
order by c.relname;

-- 2) Policies (roles, command, using, with_check) por tabela
select
  schemaname,
  tablename,
  policyname,
  roles,
  cmd as command,
  qual as using_expression,
  with_check as with_check_expression
from pg_policies
where schemaname = 'public'
order by tablename, policyname;


-- ===========================================================================
-- INDEXES
-- Todo índice esperado pelas migrations 002-017, listado a partir de
-- pg_indexes (que já traz a definição completa, incluindo se é único).
-- ===========================================================================
select
  tablename,
  indexname,
  indexdef
from pg_indexes
where schemaname = 'public'
  and tablename in (
    'scales','doxologies','shared_files','subscriptions',
    'organization_roles','organization_people','organization_teams',
    'scale_assignments','scale_templates',
    'worship_songs','web_push_subscriptions'
  )
order by tablename, indexname;


-- ===========================================================================
-- CONSTRAINTS
-- Primary keys, unique constraints e foreign keys, com a tabela/coluna de
-- origem e (para FK) a tabela/coluna referenciada.
-- ===========================================================================

-- 1) Primary keys e unique constraints
select
  tc.table_name,
  tc.constraint_name,
  tc.constraint_type,
  string_agg(kcu.column_name, ', ' order by kcu.ordinal_position) as columns
from information_schema.table_constraints tc
join information_schema.key_column_usage kcu
  on kcu.constraint_name = tc.constraint_name
  and kcu.table_schema = tc.table_schema
where tc.table_schema = 'public'
  and tc.constraint_type in ('PRIMARY KEY', 'UNIQUE')
  and tc.table_name in (
    'churches','profiles','scales','doxologies','announcements','retrospective_items',
    'shared_files','bulletins','plans','subscriptions','export_audit_log','integration_outbox',
    'organization_roles','organization_teams','organization_people','person_team_memberships',
    'scale_assignments','scale_templates','worship_songs','web_push_subscriptions'
  )
group by tc.table_name, tc.constraint_name, tc.constraint_type
order by tc.table_name, tc.constraint_type;

-- 2) Foreign keys (origem -> referência), incluindo on delete
select
  tc.table_name as from_table,
  kcu.column_name as from_column,
  ccu.table_name as to_table,
  ccu.column_name as to_column,
  rc.delete_rule as on_delete
from information_schema.table_constraints tc
join information_schema.key_column_usage kcu
  on kcu.constraint_name = tc.constraint_name and kcu.table_schema = tc.table_schema
join information_schema.constraint_column_usage ccu
  on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema
join information_schema.referential_constraints rc
  on rc.constraint_name = tc.constraint_name and rc.constraint_schema = tc.table_schema
where tc.table_schema = 'public'
  and tc.constraint_type = 'FOREIGN KEY'
  and tc.table_name in (
    'scales','doxologies','organization_roles','organization_teams','organization_people',
    'person_team_memberships','scale_assignments','scale_templates','worship_songs',
    'web_push_subscriptions','subscriptions'
  )
order by tc.table_name, kcu.column_name;


-- ===========================================================================
-- MIGRATION HISTORY
-- Tabela de controle que o Supabase CLI usa (`supabase_migrations.schema_migrations`)
-- - só existe se o projeto já foi vinculado e usado com o CLI alguma vez.
-- Como este projeto nunca usou o CLI para aplicar nada (tudo foi colado
-- manualmente no SQL Editor até hoje - ver supabase/MIGRATION_POLICY.md), o
-- resultado mais provável aqui é "a tabela não existe" - isso é uma resposta
-- válida e esperada, não um erro. Se ela existir, lista as versões que a CLI
-- já considera aplicadas (o que só aconteceria se algum `supabase db push`
-- tivesse rodado no passado, fora desta conversa).
-- ===========================================================================
select exists (
  select 1 from information_schema.tables
  where table_schema = 'supabase_migrations' and table_name = 'schema_migrations'
) as migration_history_table_exists;

-- Só retorna linhas se a consulta acima for `true`; se a tabela não existir,
-- o Supabase SQL Editor mostra um erro "relation does not exist" para o
-- select abaixo - isso é esperado e não significa que algo deu errado, só
-- que o CLI nunca foi usado neste projeto. Comente a linha abaixo antes de
-- rodar se preferir evitar esse erro.
-- select version, name, statements from supabase_migrations.schema_migrations order by version;
