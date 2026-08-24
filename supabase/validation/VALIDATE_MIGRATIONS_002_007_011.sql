-- ===========================================================================
-- VALIDATE_MIGRATIONS_002_007_011.sql
--
-- SOMENTE LEITURA. Fecha os UNKNOWN de MIGRATION_RECONCILIATION_REPORT.md:
-- migrations 002, 003, 004, 005, 007 (schema) e 011 (dados).
--
-- Uma única consulta (mesmo formato de REMOTE_SCHEMA_REPORT.sql):
--   category | object_name | detail | status
--
-- Para 002-007: TABLE/COLUMN/NOT_NULL/POLICY/INDEX, PRESENT/MISSING.
-- Para 011 (migração de dados, não de schema - REMOTE_SCHEMA_REPORT.sql não
-- alcança isso): uma contagem read-only que distingue "o backfill rodou" de
-- "nunca rodou, só há dados de saves recentes pela UI nova" - ver a seção
-- DATA_BACKFILL no final, com status BACKFILL_COMPLETE/BACKFILL_INCOMPLETE.
--
-- Não contém CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, TRUNCATE, GRANT,
-- REVOKE ou NOTIFY como comando real em nenhum lugar (auditado por script
-- antes de commitar, mesmo processo usado em REMOTE_SCHEMA_REPORT.sql).
-- Seguro rodar em produção quantas vezes quiser.
-- ===========================================================================

select category, object_name, detail, status from (

  -- ===== 002_multi_tenant.sql ================================================

  -- Tabela churches (criada por 002, não pelo baseline)
  select 'TABLE' as category, 'public.churches' as object_name, 'exists (002)' as detail,
    case when exists (select 1 from information_schema.tables where table_schema='public' and table_name='churches')
      then 'PRESENT' else 'MISSING' end as status

  union all

  -- church_id NOT NULL nas 5 tabelas que 002 endurece (passo 5 do arquivo)
  select 'NOT_NULL', 'public.' || x.table_name || '.church_id', 'is_nullable',
    case when coalesce((
      select c.is_nullable = 'NO' from information_schema.columns c
      where c.table_schema='public' and c.table_name=x.table_name and c.column_name='church_id'
    ), false) then 'PRESENT' else 'MISSING' end
  from (values ('scales'),('doxologies'),('announcements'),('retrospective_items'),('shared_files')) as x(table_name)

  union all

  -- As 14 policies que 002 recria com o filtro de church_id (passo 6)
  select 'POLICY', 'public.' || x.table_name || ': ' || x.policy_name, 'policy (002 - com filtro church_id)',
    case when exists (
      select 1 from pg_policies pol where pol.schemaname='public' and pol.tablename=x.table_name and pol.policyname=x.policy_name
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('scales','scales: admin write'), ('scales','scales: admin update'), ('scales','scales: admin delete'),
    ('doxologies','doxologies: admin write'), ('doxologies','doxologies: admin update'), ('doxologies','doxologies: admin delete'),
    ('announcements','announcements: admin write'), ('announcements','announcements: admin update'), ('announcements','announcements: admin delete'),
    ('retrospective_items','retrospective_items: admin write'), ('retrospective_items','retrospective_items: admin update'), ('retrospective_items','retrospective_items: admin delete'),
    ('shared_files','shared_files: admin write'), ('shared_files','shared_files: admin delete')
  ) as x(table_name, policy_name)

  union all

  -- ===== 003_sonoplastia_pins.sql =============================================

  select 'COLUMN', 'public.shared_files.is_pinned', coalesce((
      select data_type from information_schema.columns where table_schema='public' and table_name='shared_files' and column_name='is_pinned'
    ), 'missing'),
    case when exists (select 1 from information_schema.columns where table_schema='public' and table_name='shared_files' and column_name='is_pinned')
      then 'PRESENT' else 'MISSING' end

  union all

  select 'POLICY', 'public.shared_files: shared_files: admin update', 'policy (003)',
    case when exists (select 1 from pg_policies where schemaname='public' and tablename='shared_files' and policyname='shared_files: admin update')
      then 'PRESENT' else 'MISSING' end

  union all

  -- ===== 004_bulletins.sql =====================================================

  select 'TABLE', 'public.bulletins', 'exists (004)',
    case when exists (select 1 from information_schema.tables where table_schema='public' and table_name='bulletins')
      then 'PRESENT' else 'MISSING' end

  union all

  select 'COLUMN', 'public.bulletins.' || x.column_name, coalesce((
      select data_type from information_schema.columns where table_schema='public' and table_name='bulletins' and column_name=x.column_name
    ), 'missing'),
    case when exists (select 1 from information_schema.columns where table_schema='public' and table_name='bulletins' and column_name=x.column_name)
      then 'PRESENT' else 'MISSING' end
  from (values ('id'),('church_id'),('title'),('pdf_url'),('pdf_file_name'),('cover_url'),
               ('related_announcement_id'),('published_at'),('updated_at'),('is_active')) as x(column_name)

  union all

  select 'POLICY', 'public.bulletins: ' || x.policy_name, 'policy (004)',
    case when exists (select 1 from pg_policies where schemaname='public' and tablename='bulletins' and policyname=x.policy_name)
      then 'PRESENT' else 'MISSING' end
  from (values ('bulletins: public read active'),('bulletins: admin write'),('bulletins: admin update'),('bulletins: admin delete')) as x(policy_name)

  union all

  -- ===== 005_doxology_sessions.sql =============================================

  select 'COLUMN', 'public.doxologies.end_time', coalesce((
      select data_type from information_schema.columns where table_schema='public' and table_name='doxologies' and column_name='end_time'
    ), 'missing'),
    case when exists (select 1 from information_schema.columns where table_schema='public' and table_name='doxologies' and column_name='end_time')
      then 'PRESENT' else 'MISSING' end

  union all

  -- ===== 007_stripe_billing.sql ================================================

  select 'COLUMN', 'public.' || x.table_name || '.' || x.column_name, coalesce((
      select data_type from information_schema.columns where table_schema='public' and table_name=x.table_name and column_name=x.column_name
    ), 'missing'),
    case when exists (select 1 from information_schema.columns where table_schema='public' and table_name=x.table_name and column_name=x.column_name)
      then 'PRESENT' else 'MISSING' end
  from (values
    ('plans','stripe_price_id_monthly'), ('plans','stripe_price_id_yearly'),
    ('subscriptions','stripe_customer_id'), ('subscriptions','stripe_subscription_id')
  ) as x(table_name, column_name)

  union all

  select 'INDEX', 'public.subscriptions.subscriptions_stripe_subscription_id_key', 'unique index (007)',
    case when exists (select 1 from pg_indexes where schemaname='public' and tablename='subscriptions' and indexname='subscriptions_stripe_subscription_id_key')
      then 'PRESENT' else 'MISSING' end

  union all

  -- ===== 011_backfill_legacy_scale_assignments.sql (DADOS, não schema) ========
  --
  -- O que 011 deveria ter feito: para cada scale com pelo menos um dos 5 campos
  -- de pessoa legados preenchidos (reception_person, sound_person,
  -- preaching_person, conducting_person, musical_message_person), criar a(s)
  -- linha(s) correspondente(s) em scale_assignments (via join com
  -- organization_roles.legacy_field_key), sem sobrescrever nada que já
  -- existisse (guarda `not exists`).
  --
  -- Como verificar sem side-effect: se o backfill rodou, TODA scale com algum
  -- campo legado preenchido deveria ter pelo menos 1 linha em scale_assignments.
  -- Se nunca rodou, só as escalas que foram reabertas e salvas pela UI nova
  -- (saveScaleAction também grava scale_assignments a cada save, independente
  -- de 011) teriam assignments - as mais antigas, nunca reabertas, ficariam
  -- com dado legado mas zero assignments.
  --
  -- Requer que organization_roles/scale_assignments existam (010 aplicada -
  -- já confirmado PARTIALLY_APPLIED/schema completo em
  -- MIGRATION_RECONCILIATION_REPORT.md) - se não existirem, esta contagem
  -- retorna erro em vez de um número; nesse caso 011 é necessariamente
  -- NOT_APPLIED (não tem como ter rodado sem a tabela-alvo existir).

  select 'DATA_BACKFILL', 'scales com dado legado e ZERO scale_assignments', count(*)::text,
    case when count(*) = 0 then 'BACKFILL_COMPLETE' else 'BACKFILL_INCOMPLETE_OR_NEVER_RAN' end
  from scales s
  where (
      coalesce(trim(s.reception_person), '') <> '' or
      coalesce(trim(s.sound_person), '') <> '' or
      coalesce(trim(s.preaching_person), '') <> '' or
      coalesce(trim(s.conducting_person), '') <> '' or
      coalesce(trim(s.musical_message_person), '') <> ''
    )
    and not exists (select 1 from scale_assignments sa where sa.scale_id = s.id)

  union all

  -- Contexto informativo (não é PRESENT/MISSING, é um número de referência)
  select 'DATA_BACKFILL', 'total de scales com algum dado legado preenchido', count(*)::text, 'INFO'
  from scales s
  where (
      coalesce(trim(s.reception_person), '') <> '' or
      coalesce(trim(s.sound_person), '') <> '' or
      coalesce(trim(s.preaching_person), '') <> '' or
      coalesce(trim(s.conducting_person), '') <> '' or
      coalesce(trim(s.musical_message_person), '') <> ''
    )

  union all

  select 'DATA_BACKFILL', 'total de linhas em scale_assignments', count(*)::text, 'INFO'
  from scale_assignments

) as report
order by category, status asc, object_name;
