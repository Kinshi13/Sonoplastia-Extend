-- ===========================================================================
-- VALIDATE_PENDING_014_016.sql
--
-- SOMENTE LEITURA. Atualizado para validar o modelo V2 (ver
-- supabase/manual/APPLY_PENDING_014_016_SAFE_V2.sql) - depois de uma
-- revisão de segurança, web_push_subscriptions passou a usar duas funções
-- `security definer` (upsert_web_push_subscription/delete_web_push_subscription)
-- em vez de policies públicas de insert/update/delete. worship_songs não
-- mudou. Confirma que worship_songs e web_push_subscriptions (com toda a
-- estrutura das migrations 014/015/016) ficaram exatamente como esperado.
--
-- Mesmo formato de REMOTE_SCHEMA_REPORT.sql: uma única consulta,
--   category | object_name | detail | status
-- PRESENT/MISSING (ou SECURE_*/REGRESSION_* nas checagens de segurança) por
-- item. Escopo restrito só a worship_songs e web_push_subscriptions - para o
-- resto do banco, use REMOTE_SCHEMA_REPORT.sql.
--
-- Não contém CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, TRUNCATE, GRANT,
-- REVOKE ou NOTIFY como comando real. Seguro rodar quantas vezes quiser.
-- ===========================================================================

select category, object_name, detail, status from (

  -- ===== TABLE ==============================================================
  select 'TABLE' as category, 'public.' || x.name as object_name, 'exists' as detail,
    case when exists (select 1 from information_schema.tables where table_schema='public' and table_name=x.name)
      then 'PRESENT' else 'MISSING' end as status
  from (values ('worship_songs'), ('web_push_subscriptions')) as x(name)

  union all

  -- ===== COLUMN (todas as colunas esperadas após 014+015+016) ================
  select 'COLUMN', 'public.' || x.table_name || '.' || x.column_name,
    coalesce((select data_type from information_schema.columns where table_schema='public' and table_name=x.table_name and column_name=x.column_name), 'missing'),
    case when exists (select 1 from information_schema.columns where table_schema='public' and table_name=x.table_name and column_name=x.column_name)
      then 'PRESENT' else 'MISSING' end
  from (values
    -- worship_songs: 014 + 015 + 016
    ('worship_songs','id'), ('worship_songs','church_id'), ('worship_songs','schedule_id'),
    ('worship_songs','program_date'), ('worship_songs','title'), ('worship_songs','artist'),
    ('worship_songs','youtube_url'), ('worship_songs','youtube_video_id'), ('worship_songs','thumbnail_url'),
    ('worship_songs','moment_label'), ('worship_songs','notes'), ('worship_songs','order_index'),
    ('worship_songs','is_published'), ('worship_songs','created_at'), ('worship_songs','updated_at'),
    ('worship_songs','program_type'), ('worship_songs','is_daily_recommendation'),
    ('worship_songs','recommendation_date'), ('worship_songs','notification_enabled'),
    ('worship_songs','notification_time'), ('worship_songs','notification_title'),
    ('worship_songs','notification_body'), ('worship_songs','recommendation_message'),
    -- web_push_subscriptions: 015
    ('web_push_subscriptions','id'), ('web_push_subscriptions','church_id'),
    ('web_push_subscriptions','endpoint'), ('web_push_subscriptions','p256dh'),
    ('web_push_subscriptions','auth'), ('web_push_subscriptions','user_agent'),
    ('web_push_subscriptions','platform'), ('web_push_subscriptions','topics'),
    ('web_push_subscriptions','enabled'), ('web_push_subscriptions','created_at'),
    ('web_push_subscriptions','updated_at'), ('web_push_subscriptions','last_seen_at')
  ) as x(table_name, column_name)

  union all

  -- ===== RLS (ligado nas duas tabelas) ========================================
  select 'RLS', 'public.' || x.name, 'enabled',
    case when coalesce((select relrowsecurity from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relname=x.name), false)
      then 'PRESENT' else 'MISSING' end
  from (values ('worship_songs'), ('web_push_subscriptions')) as x(name)

  union all

  -- ===== POLICY ================================================================
  select 'POLICY', 'public.' || x.table_name || ': ' || x.policy_name, 'policy',
    case when exists (select 1 from pg_policies where schemaname='public' and tablename=x.table_name and policyname=x.policy_name)
      then 'PRESENT' else 'MISSING' end
  from (values
    ('worship_songs','worship_songs: public read published'),
    ('worship_songs','worship_songs: admin write'),
    ('worship_songs','worship_songs: admin update'),
    ('worship_songs','worship_songs: admin delete')
  ) as x(table_name, policy_name)

  union all

  -- ===== web_push_subscriptions: modelo V2 (RPCs, não policies) ==============
  -- Desde a revisão de segurança, web_push_subscriptions NÃO tem nenhuma policy
  -- de insert/update/delete para anon/authenticated - toda escrita passa pelas
  -- duas funções abaixo. Ver supabase/manual/APPLY_PENDING_014_016_SAFE_V2.sql.

  select 'RPC', 'public.' || x.name, 'security definer, deve existir',
    case when exists (
      select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
      where n.nspname='public' and p.proname=x.name and p.prosecdef
    ) then 'PRESENT' else 'MISSING' end
  from (values ('upsert_web_push_subscription'), ('delete_web_push_subscription')) as x(name)

  union all

  -- Confirma que NENHUMA policy de escrita pública foi criada para
  -- web_push_subscriptions (por design - se aparecer PRESENT aqui, é uma
  -- regressão de segurança, não algo esperado).
  select 'SECURITY_CHECK', 'public.web_push_subscriptions: nenhuma policy pública de insert/update/delete',
    'deve estar ausente por design',
    case when exists (
      select 1 from pg_policies
      where schemaname='public' and tablename='web_push_subscriptions'
        and cmd in ('INSERT','UPDATE','DELETE')
    ) then 'REGRESSION_POLICY_FOUND' else 'SECURE_NO_POLICY' end

  union all

  -- Confirma que anon/authenticated NÃO têm grant direto de
  -- insert/update/delete na tabela (só EXECUTE nas duas funções acima).
  select 'SECURITY_CHECK', 'public.web_push_subscriptions: sem GRANT direto de insert/update/delete para anon/authenticated',
    'deve estar ausente por design',
    case when exists (
      select 1 from information_schema.role_table_grants
      where table_schema='public' and table_name='web_push_subscriptions'
        and grantee in ('anon','authenticated')
        and privilege_type in ('INSERT','UPDATE','DELETE')
    ) then 'REGRESSION_GRANT_FOUND' else 'SECURE_NO_GRANT' end

  union all

  -- ===== INDEX ==================================================================
  select 'INDEX', 'public.' || x.table_name || '.' || x.index_name,
    case when x.is_unique then 'unique index' else 'index' end,
    case when exists (select 1 from pg_indexes where schemaname='public' and tablename=x.table_name and indexname=x.index_name)
      then 'PRESENT' else 'MISSING' end
  from (values
    ('worship_songs','worship_songs_church_idx', false),
    ('worship_songs','worship_songs_schedule_idx', false),
    ('worship_songs','worship_songs_program_date_idx', false),
    ('worship_songs','worship_songs_published_idx', false),
    ('worship_songs','worship_songs_order_idx', false),
    ('worship_songs','worship_songs_recommendation_date_idx', false),
    ('worship_songs','worship_songs_is_daily_recommendation_idx', false),
    ('worship_songs','worship_songs_one_recommendation_per_day', true),
    ('web_push_subscriptions','web_push_subscriptions_church_idx', false),
    ('web_push_subscriptions','web_push_subscriptions_enabled_idx', false)
  ) as x(table_name, index_name, is_unique)

  union all

  -- ===== CONSTRAINT (primary keys + foreign keys) =============================
  select 'CONSTRAINT', 'public.' || x.name, 'primary key',
    case when exists (select 1 from information_schema.table_constraints where table_schema='public' and table_name=x.name and constraint_type='PRIMARY KEY')
      then 'PRESENT' else 'MISSING' end
  from (values ('worship_songs'), ('web_push_subscriptions')) as x(name)

  union all

  select 'CONSTRAINT', 'public.' || x.table_name || '.' || x.column_name || ' -> ' || x.ref_table, 'foreign key',
    case when exists (
      select 1 from information_schema.table_constraints tc
      join information_schema.key_column_usage kcu on kcu.constraint_name=tc.constraint_name and kcu.table_schema=tc.table_schema
      join information_schema.constraint_column_usage ccu on ccu.constraint_name=tc.constraint_name and ccu.constraint_schema=tc.constraint_schema
      where tc.table_schema='public' and tc.constraint_type='FOREIGN KEY'
        and tc.table_name=x.table_name and kcu.column_name=x.column_name and ccu.table_name=x.ref_table
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('worship_songs','church_id','churches'),
    ('worship_songs','schedule_id','scales'),
    ('web_push_subscriptions','church_id','churches')
  ) as x(table_name, column_name, ref_table)

) as report
order by category, status asc, object_name;
