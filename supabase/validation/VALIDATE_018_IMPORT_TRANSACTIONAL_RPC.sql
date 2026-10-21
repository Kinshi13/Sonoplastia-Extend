-- ===========================================================================
-- VALIDATE_018_IMPORT_TRANSACTIONAL_RPC.sql
--
-- Somente leitura. Confirma se a migration 018 foi de fato aplicada - não
-- presuma isso só porque o arquivo de migration existe no Git (ver
-- MIGRATION_POLICY.md §2: CREATED != APPLIED != VERIFIED).
-- ===========================================================================

select 'TABLE' as category, 'public.import_operations' as object_name,
  case when exists (select 1 from information_schema.tables where table_schema='public' and table_name='import_operations')
    then 'EXISTS' else 'MISSING' end as status;

select 'FUNCTION' as category, 'public.import_schedule_with_assignments' as object_name,
  case when exists (
    select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'import_schedule_with_assignments'
  ) then 'EXISTS' else 'MISSING' end as status;

select 'FUNCTION_SECURITY' as category, 'import_schedule_with_assignments.prosecdef' as object_name,
  coalesce((
    select case when p.prosecdef then 'SECURITY DEFINER (correto)' else 'SECURITY INVOKER (ERRADO - revisar)' end
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'import_schedule_with_assignments'
    limit 1
  ), 'N/A - função ainda não existe') as status;

select 'POLICY' as category, 'import_operations: admin read own church' as object_name,
  case when exists (
    select 1 from pg_policies where schemaname='public' and tablename='import_operations'
      and policyname='import_operations: admin read own church'
  ) then 'EXISTS' else 'MISSING' end as status;

-- Nenhuma policy de insert/update/delete deve existir para anon/authenticated - só a função
-- SECURITY DEFINER escreve. Esta query deve retornar ZERO linhas.
select 'UNEXPECTED_WRITE_GRANT' as category, grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public' and table_name = 'import_operations'
  and privilege_type in ('INSERT','UPDATE','DELETE')
  and grantee in ('anon','authenticated');

select 'ROW_COUNT' as category, 'import_operations' as object_name,
  coalesce((select count(*)::text from import_operations), 'N/A - tabela ainda não existe') as status;
