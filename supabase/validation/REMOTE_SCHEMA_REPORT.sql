-- ===========================================================================
-- REMOTE_SCHEMA_REPORT.sql
--
-- SOMENTE LEITURA. Uma única consulta (uma cadeia de SELECT ... UNION ALL,
-- terminando num único ORDER BY) que devolve UM result set consolidado no
-- formato:
--
--   category | object_name | detail | status
--
-- status é sempre PRESENT ou MISSING (ou N/A para as 3 categorias
-- informativas VIEW/TRIGGER/ENUM, que não têm uma "lista esperada" - nenhuma
-- migration 002-017 cria view, trigger ou enum novo, então essas seções só
-- reportam o que existe de fato, para flagrar qualquer coisa aplicada fora
-- do que os arquivos locais descrevem).
--
-- Não deduz nada sobre qual migration está aplicada - isso fica para depois,
-- com os resultados reais em mãos (ver instrução do usuário: classificação
-- APPLIED/PARTIALLY_APPLIED/NOT_APPLIED/SUPERSEDED/UNKNOWN vem só depois).
--
-- Cobre tudo que schema.sql (baseline) e as migrations 002-017 esperam que
-- exista: tabelas, colunas, RPCs, RLS ligado, policies, índices, primary
-- keys, foreign keys, mais uma checagem da tabela de histórico do Supabase
-- CLI. Todos os incidentes já conhecidos estão marcados explicitamente nos
-- comentários abaixo, incluindo scales.reused_from_scale_id, que é
-- verificado mas NÃO é esperado existir (nenhuma migration cria essa coluna
-- - ver supabase/MIGRATION_INVENTORY.md).
--
-- Não contém CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, TRUNCATE, GRANT,
-- REVOKE ou NOTIFY em nenhum lugar (nem dentro de string) - só leitura de
-- information_schema/pg_catalog. Auditado por grep antes de commitar (ver
-- mensagem de commit). Seguro rodar em produção quantas vezes quiser.
-- ===========================================================================

select category, object_name, detail, status from (

  -- ===== TABLE ==============================================================
  select
    'TABLE' as category,
    'public.' || x.name as object_name,
    'exists' as detail,
    case when exists (
      select 1 from information_schema.tables t
      where t.table_schema = 'public' and t.table_name = x.name
    ) then 'PRESENT' else 'MISSING' end as status
  from (values
    ('churches'), ('profiles'), ('scales'), ('doxologies'), ('announcements'),
    ('retrospective_items'), ('shared_files'), ('bulletins'), ('plans'), ('subscriptions'),
    ('export_audit_log'), ('integration_outbox'),
    ('organization_roles'), ('organization_teams'), ('organization_people'),
    ('person_team_memberships'), ('scale_assignments'), ('scale_templates'),
    ('worship_songs'),               -- incidente conhecido
    ('web_push_subscriptions')       -- incidente conhecido
  ) as x(name)

  union all

  -- ===== COLUMN ==============================================================
  select
    'COLUMN',
    'public.' || x.table_name || '.' || x.column_name,
    coalesce((
      select c.data_type from information_schema.columns c
      where c.table_schema = 'public' and c.table_name = x.table_name and c.column_name = x.column_name
    ), x.note),
    case when exists (
      select 1 from information_schema.columns c
      where c.table_schema = 'public' and c.table_name = x.table_name and c.column_name = x.column_name
    ) then 'PRESENT' else 'MISSING' end
  from (values
    -- scales: todas as colunas, incluindo o incidente relatado (is_temporary)
    -- e reused_from_scale_id (NÃO esperado - ver cabeçalho do arquivo)
    ('scales','id','—'), ('scales','church_id','—'), ('scales','date','—'),
    ('scales','start_time','—'), ('scales','end_time','—'), ('scales','type','—'),
    ('scales','title','—'), ('scales','reception_person','—'), ('scales','sound_person','—'),
    ('scales','preaching_person','—'), ('scales','conducting_person','—'),
    ('scales','musical_message_person','—'), ('scales','notes','—'),
    ('scales','is_special_event','—'),
    ('scales','is_temporary','missing (incidente relatado)'),                       -- incidente conhecido
    ('scales','reused_from_scale_id','NÃO esperado - nenhuma migration cria isto'), -- não é bug se MISSING
    ('scales','source_type','—'), ('scales','created_at','—'), ('scales','updated_at','—'),
    -- worship_songs: todas as colunas (incidente conhecido - tabela inteira)
    ('worship_songs','id','tabela ausente = incidente conhecido'),
    ('worship_songs','church_id','tabela ausente = incidente conhecido'),
    ('worship_songs','schedule_id','—'), ('worship_songs','program_date','—'),
    ('worship_songs','program_type','—'), ('worship_songs','title','—'),
    ('worship_songs','artist','—'), ('worship_songs','youtube_url','—'),
    ('worship_songs','youtube_video_id','—'), ('worship_songs','thumbnail_url','—'),
    ('worship_songs','moment_label','—'), ('worship_songs','notes','—'),
    ('worship_songs','order_index','—'), ('worship_songs','is_published','—'),
    ('worship_songs','is_daily_recommendation','—'),   -- campo de recommendation
    ('worship_songs','recommendation_date','—'),        -- campo de recommendation
    ('worship_songs','recommendation_message','—'),     -- campo de recommendation
    ('worship_songs','notification_enabled','—'),       -- campo de notification
    ('worship_songs','notification_time','—'),          -- campo de notification
    ('worship_songs','notification_title','—'),         -- campo de notification
    ('worship_songs','notification_body','—'),          -- campo de notification
    ('worship_songs','created_at','—'), ('worship_songs','updated_at','—'),
    -- web_push_subscriptions: todas as colunas (incidente conhecido)
    ('web_push_subscriptions','id','—'), ('web_push_subscriptions','church_id','—'),
    ('web_push_subscriptions','endpoint','—'), ('web_push_subscriptions','p256dh','—'),
    ('web_push_subscriptions','auth','—'), ('web_push_subscriptions','user_agent','—'),
    ('web_push_subscriptions','platform','—'), ('web_push_subscriptions','topics','—'),
    ('web_push_subscriptions','enabled','—'), ('web_push_subscriptions','created_at','—'),
    ('web_push_subscriptions','updated_at','—'), ('web_push_subscriptions','last_seen_at','—'),
    -- scale_templates: todas as colunas, incluindo is_protected (incidente conhecido)
    ('scale_templates','id','—'), ('scale_templates','church_id','—'),
    ('scale_templates','name','—'), ('scale_templates','description','—'),
    ('scale_templates','roles','—'), ('scale_templates','default_start_time','—'),
    ('scale_templates','default_end_time','—'), ('scale_templates','default_notes','—'),
    ('scale_templates','is_favorite','—'), ('scale_templates','is_active','—'),
    ('scale_templates','is_protected','missing (incidente relatado)'),  -- incidente conhecido
    ('scale_templates','created_at','—'), ('scale_templates','updated_at','—'),
    ('scale_templates','created_by','—'),
    -- organization_roles / organization_teams / organization_people /
    -- person_team_memberships / scale_assignments: todas as colunas (migration 010)
    ('organization_roles','id','—'), ('organization_roles','church_id','—'),
    ('organization_roles','name','—'), ('organization_roles','short_name','—'),
    ('organization_roles','description','—'), ('organization_roles','icon_key','—'),
    ('organization_roles','color_token','—'), ('organization_roles','sort_order','—'),
    ('organization_roles','is_active','—'), ('organization_roles','is_required','—'),
    ('organization_roles','allows_multiple_people','—'), ('organization_roles','team_id','—'),
    ('organization_roles','legacy_field_key','—'), ('organization_roles','created_at','—'),
    ('organization_roles','updated_at','—'), ('organization_roles','created_by','—'),
    ('organization_teams','id','—'), ('organization_teams','church_id','—'),
    ('organization_teams','name','—'), ('organization_teams','description','—'),
    ('organization_teams','icon_key','—'), ('organization_teams','color_token','—'),
    ('organization_teams','is_active','—'), ('organization_teams','sort_order','—'),
    ('organization_teams','created_at','—'), ('organization_teams','updated_at','—'),
    ('organization_people','id','—'), ('organization_people','church_id','—'),
    ('organization_people','full_name','—'), ('organization_people','display_name','—'),
    ('organization_people','email','—'), ('organization_people','phone','—'),
    ('organization_people','photo_url','—'), ('organization_people','notes','—'),
    ('organization_people','is_favorite','—'), ('organization_people','is_active','—'),
    ('organization_people','created_at','—'), ('organization_people','updated_at','—'),
    ('organization_people','created_by','—'), ('organization_people','linked_user_id','—'),
    ('person_team_memberships','person_id','—'), ('person_team_memberships','team_id','—'),
    ('person_team_memberships','is_primary','—'), ('person_team_memberships','created_at','—'),
    ('scale_assignments','id','—'), ('scale_assignments','scale_id','—'),
    ('scale_assignments','role_id','—'), ('scale_assignments','person_id','—'),
    ('scale_assignments','custom_person_name','—'), ('scale_assignments','role_name_snapshot','—'),
    ('scale_assignments','person_name_snapshot','—'), ('scale_assignments','position','—'),
    ('scale_assignments','notes','—'), ('scale_assignments','created_at','—'),
    ('scale_assignments','updated_at','—'),
    -- church_id nas demais entidades multi-tenant (002_multi_tenant.sql)
    ('doxologies','church_id','—'), ('announcements','church_id','—'),
    ('retrospective_items','church_id','—'), ('shared_files','church_id','—'),
    ('bulletins','church_id','—'), ('subscriptions','church_id','—'),
    ('export_audit_log','church_id','—'),
    ('integration_outbox','organization_id','multi-tenant, mas usa organization_id em vez de church_id'),
    ('scale_templates','church_id','—'), ('organization_roles','church_id','—'),
    -- doxologies (005/009), shared_files (003), plans/subscriptions (006/007)
    ('doxologies','end_time','—'), ('doxologies','is_favorite','—'),
    ('doxologies','reused_from_doxology_id','—'), ('doxologies','times_reused','—'),
    ('shared_files','is_pinned','—'),
    ('plans','stripe_price_id_monthly','—'), ('plans','stripe_price_id_yearly','—'),
    ('subscriptions','status','—'), ('subscriptions','stripe_customer_id','—'),
    ('subscriptions','stripe_subscription_id','—')
  ) as x(table_name, column_name, note)

  union all

  -- ===== RPC ==================================================================
  select
    'RPC',
    'public.' || x.name,
    coalesce((
      select case when p.prosecdef then 'security definer' else 'security invoker' end
      from pg_proc p join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public' and p.proname = x.name
      limit 1
    ), x.note),
    case when exists (
      select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public' and p.proname = x.name
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('get_church_subscription', 'missing (usada pelo Android)'),
    ('get_church_by_code', 'missing (usada pelo Android)')
  ) as x(name, note)

  union all

  -- ===== RLS (row level security ligado por tabela) ==========================
  select
    'RLS',
    'public.' || x.name,
    'enabled',
    case when coalesce((
      select c.relrowsecurity from pg_class c join pg_namespace n on n.oid = c.relnamespace
      where n.nspname = 'public' and c.relname = x.name
    ), false) then 'PRESENT' else 'MISSING' end
  from (values
    ('churches'), ('profiles'), ('scales'), ('doxologies'), ('announcements'),
    ('retrospective_items'), ('shared_files'), ('bulletins'), ('plans'), ('subscriptions'),
    ('export_audit_log'), ('integration_outbox'),
    ('organization_roles'), ('organization_teams'), ('organization_people'),
    ('person_team_memberships'), ('scale_assignments'), ('scale_templates'),
    ('worship_songs'), ('web_push_subscriptions')
  ) as x(name)

  union all

  -- ===== POLICY ================================================================
  -- Toda policy nomeada criada por schema.sql (baseline) ou migrations 002-017.
  select
    'POLICY',
    'public.' || x.table_name || ': ' || x.policy_name,
    'policy',
    case when exists (
      select 1 from pg_policies pol
      where pol.schemaname = 'public' and pol.tablename = x.table_name and pol.policyname = x.policy_name
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('churches','churches: public read'),
    ('profiles','profiles: read own'), ('profiles','profiles: insert own'),
    ('scales','scales: public read'), ('scales','scales: admin write'),
    ('scales','scales: admin update'), ('scales','scales: admin delete'),
    ('doxologies','doxologies: public read'), ('doxologies','doxologies: admin write'),
    ('doxologies','doxologies: admin update'), ('doxologies','doxologies: admin delete'),
    ('announcements','announcements: public read active'), ('announcements','announcements: admin write'),
    ('announcements','announcements: admin update'), ('announcements','announcements: admin delete'),
    ('retrospective_items','retrospective_items: public read active'), ('retrospective_items','retrospective_items: admin write'),
    ('retrospective_items','retrospective_items: admin update'), ('retrospective_items','retrospective_items: admin delete'),
    ('shared_files','shared_files: public read'), ('shared_files','shared_files: admin write'),
    ('shared_files','shared_files: admin update'), ('shared_files','shared_files: admin delete'),
    ('bulletins','bulletins: public read active'), ('bulletins','bulletins: admin write'),
    ('bulletins','bulletins: admin update'), ('bulletins','bulletins: admin delete'),
    ('plans','plans: public read'),
    ('subscriptions','subscriptions: public read'), ('subscriptions','subscriptions: admin update'),
    ('export_audit_log','export_audit_log: admin read own church'), ('export_audit_log','export_audit_log: admin insert own church'),
    ('organization_roles','organization_roles: public read'), ('organization_roles','organization_roles: admin write'),
    ('organization_roles','organization_roles: admin update'), ('organization_roles','organization_roles: admin delete'),
    ('organization_teams','organization_teams: public read'), ('organization_teams','organization_teams: admin write'),
    ('organization_teams','organization_teams: admin update'), ('organization_teams','organization_teams: admin delete'),
    ('organization_people','organization_people: public read'), ('organization_people','organization_people: admin write'),
    ('organization_people','organization_people: admin update'), ('organization_people','organization_people: admin delete'),
    ('person_team_memberships','person_team_memberships: public read'), ('person_team_memberships','person_team_memberships: admin write'),
    ('person_team_memberships','person_team_memberships: admin delete'),
    ('scale_assignments','scale_assignments: public read'), ('scale_assignments','scale_assignments: admin write'),
    ('scale_assignments','scale_assignments: admin update'), ('scale_assignments','scale_assignments: admin delete'),
    ('scale_templates','scale_templates: admin read'), ('scale_templates','scale_templates: admin write'),
    ('scale_templates','scale_templates: admin update'), ('scale_templates','scale_templates: admin delete'),
    ('worship_songs','worship_songs: public read published'), ('worship_songs','worship_songs: admin write'),
    ('worship_songs','worship_songs: admin update'), ('worship_songs','worship_songs: admin delete'),
    ('web_push_subscriptions','web_push_subscriptions: public insert for active church'),
    ('web_push_subscriptions','web_push_subscriptions: public update own endpoint'),
    ('web_push_subscriptions','web_push_subscriptions: public delete own endpoint')
  ) as x(table_name, policy_name)

  union all

  -- ===== INDEX ==================================================================
  select
    'INDEX',
    'public.' || x.table_name || '.' || x.index_name,
    case when x.is_unique then 'unique index' else 'index' end,
    case when exists (
      select 1 from pg_indexes pi
      where pi.schemaname = 'public' and pi.tablename = x.table_name and pi.indexname = x.index_name
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('doxologies','doxologies_church_favorite_idx', false),
    ('organization_people','organization_people_church_idx', false),
    ('organization_roles','organization_roles_church_idx', false),
    ('organization_teams','organization_teams_church_idx', false),
    ('scale_assignments','scale_assignments_person_idx', false),
    ('scale_assignments','scale_assignments_role_idx', false),
    ('scale_assignments','scale_assignments_scale_idx', false),
    ('scale_templates','scale_templates_church_idx', false),
    ('web_push_subscriptions','web_push_subscriptions_church_idx', false),
    ('web_push_subscriptions','web_push_subscriptions_enabled_idx', false),
    ('worship_songs','worship_songs_church_idx', false),
    ('worship_songs','worship_songs_is_daily_recommendation_idx', false),
    ('worship_songs','worship_songs_order_idx', false),
    ('worship_songs','worship_songs_program_date_idx', false),
    ('worship_songs','worship_songs_published_idx', false),
    ('worship_songs','worship_songs_recommendation_date_idx', false),
    ('worship_songs','worship_songs_schedule_idx', false),
    ('subscriptions','subscriptions_stripe_subscription_id_key', true),
    ('worship_songs','worship_songs_one_recommendation_per_day', true)  -- 1 recomendação/dia
  ) as x(table_name, index_name, is_unique)

  union all

  -- ===== CONSTRAINT (primary keys) ============================================
  select
    'CONSTRAINT',
    'public.' || x.name,
    'primary key',
    case when exists (
      select 1 from information_schema.table_constraints tc
      where tc.table_schema = 'public' and tc.table_name = x.name and tc.constraint_type = 'PRIMARY KEY'
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('churches'), ('profiles'), ('scales'), ('doxologies'), ('announcements'),
    ('retrospective_items'), ('shared_files'), ('bulletins'), ('plans'), ('subscriptions'),
    ('export_audit_log'), ('integration_outbox'),
    ('organization_roles'), ('organization_teams'), ('organization_people'),
    ('person_team_memberships'), ('scale_assignments'), ('scale_templates'),
    ('worship_songs'), ('web_push_subscriptions')
  ) as x(name)

  union all

  -- ===== CONSTRAINT (foreign keys) ============================================
  -- Verificado por (tabela, coluna, tabela referenciada), não pelo nome da
  -- constraint - o Postgres autogera nomes (<tabela>_<coluna>_fkey) que podem
  -- variar; o que importa é se o relacionamento existe de verdade.
  select
    'CONSTRAINT',
    'public.' || x.table_name || '.' || x.column_name || ' -> ' || x.ref_table,
    'foreign key',
    case when exists (
      select 1
      from information_schema.table_constraints tc
      join information_schema.key_column_usage kcu
        on kcu.constraint_name = tc.constraint_name and kcu.table_schema = tc.table_schema
      join information_schema.constraint_column_usage ccu
        on ccu.constraint_name = tc.constraint_name and ccu.constraint_schema = tc.constraint_schema
      where tc.table_schema = 'public'
        and tc.constraint_type = 'FOREIGN KEY'
        and tc.table_name = x.table_name
        and kcu.column_name = x.column_name
        and ccu.table_name = x.ref_table
    ) then 'PRESENT' else 'MISSING' end
  from (values
    ('profiles','id','users'), ('profiles','church_id','churches'),
    ('scales','church_id','churches'),
    ('doxologies','church_id','churches'), ('doxologies','reused_from_doxology_id','doxologies'),
    ('announcements','church_id','churches'),
    ('retrospective_items','church_id','churches'),
    ('shared_files','church_id','churches'),
    ('bulletins','church_id','churches'), ('bulletins','related_announcement_id','announcements'),
    ('subscriptions','church_id','churches'), ('subscriptions','plan_id','plans'),
    ('export_audit_log','church_id','churches'), ('export_audit_log','user_id','users'),
    ('integration_outbox','organization_id','churches'),
    ('worship_songs','church_id','churches'), ('worship_songs','schedule_id','scales'),
    ('web_push_subscriptions','church_id','churches'),
    ('organization_roles','church_id','churches'), ('organization_roles','team_id','organization_teams'),
    ('organization_teams','church_id','churches'),
    ('organization_people','church_id','churches'),
    ('person_team_memberships','person_id','organization_people'),
    ('person_team_memberships','team_id','organization_teams'),
    ('scale_assignments','scale_id','scales'), ('scale_assignments','role_id','organization_roles'),
    ('scale_assignments','person_id','organization_people'),
    ('scale_templates','church_id','churches')
  ) as x(table_name, column_name, ref_table)

  union all

  -- ===== MIGRATION_HISTORY ====================================================
  select
    'MIGRATION_HISTORY',
    'supabase_migrations.schema_migrations',
    'table (usada pelo Supabase CLI)',
    case when exists (
      select 1 from information_schema.tables
      where table_schema = 'supabase_migrations' and table_name = 'schema_migrations'
    ) then 'PRESENT' else 'MISSING' end

  union all

  -- ===== VIEW (informativo - nenhuma esperada por 002-017) ====================
  select 'VIEW', 'public.' || table_name, 'view encontrada (não esperada por nenhuma migration)', 'PRESENT'
  from information_schema.views where table_schema = 'public'
  union all
  select 'VIEW', '(nenhuma)', 'nenhuma view em public - esperado', 'N/A'
  where not exists (select 1 from information_schema.views where table_schema = 'public')

  union all

  -- ===== TRIGGER (informativo - só handle_new_user em auth.users é esperado) ===
  select 'TRIGGER',
    event_object_schema || '.' || event_object_table || '.' || trigger_name,
    action_timing || ' ' || event_manipulation || ' -> ' || action_statement,
    'PRESENT'
  from information_schema.triggers
  where event_object_schema in ('public','auth')
  union all
  select 'TRIGGER', '(nenhum)', 'nenhum trigger em public/auth - inesperado, esperava on_auth_user_created', 'N/A'
  where not exists (select 1 from information_schema.triggers where event_object_schema in ('public','auth'))

  union all

  -- ===== ENUM (informativo - nenhum esperado por 002-017) ======================
  select 'ENUM', t.typname, e.enumlabel, 'PRESENT'
  from pg_type t
  join pg_enum e on e.enumtypid = t.oid
  join pg_namespace n on n.oid = t.typnamespace
  where n.nspname = 'public'
  union all
  select 'ENUM', '(nenhum)', 'nenhum enum em public - esperado', 'N/A'
  where not exists (
    select 1 from pg_type t join pg_enum e on e.enumtypid = t.oid
    join pg_namespace n on n.oid = t.typnamespace where n.nspname = 'public'
  )

) as report
order by category, status asc, object_name;
