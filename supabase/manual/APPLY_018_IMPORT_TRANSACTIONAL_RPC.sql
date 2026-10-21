-- ===========================================================================
-- APPLY_018_IMPORT_TRANSACTIONAL_RPC.sql
--
-- Cole no SQL Editor do Supabase para aplicar a migration 018
-- (018_import_transactional_rpc.sql) - idêntico ao arquivo de migration,
-- reproduzido aqui só para o fluxo "colar no SQL Editor quando o CLI não
-- pode ser usado" (ver supabase/MIGRATION_POLICY.md §6).
--
-- NÃO FOI EXECUTADO CONTRA PRODUÇÃO. Este arquivo existe no estado CREATED
-- (ver MIGRATION_POLICY.md §2) - alguém com acesso ao projeto Supabase real
-- precisa revisar e colar isto manualmente; esta sessão nunca roda SQL
-- contra o banco de produção por conta própria.
--
-- Classificação: REVIEW (não SAFE) - cria uma função SECURITY DEFINER nova
-- que ESCREVE dados (insere em `scales` e `scale_assignments`), o que o
-- próprio MIGRATION_POLICY.md exclui da faixa SAFE para RPCs. Aditiva (não
-- toca nenhuma tabela/coluna/policy/função existente), mas ainda merece
-- revisão humana antes de ir ao ar - é exatamente o tipo de objeto novo que
-- pode, em teoria, ser chamado por qualquer usuário autenticado (a checagem
-- de admin é feita DENTRO da função, não pelo RLS normal).
--
-- Idempotente: seguro rodar mais de uma vez.
--   - CREATE TABLE ... IF NOT EXISTS
--   - CREATE INDEX ... IF NOT EXISTS
--   - DROP POLICY IF EXISTS antes do CREATE POLICY
--   - CREATE OR REPLACE FUNCTION
--
-- Pré-requisitos: migrations 002-011 já aplicadas (precisa de `scales`,
-- `organization_roles`, `organization_people`, `scale_assignments`,
-- `profiles` - todas já CONFIRMADAS APPLIED em produção, ver
-- MIGRATION_INVENTORY.md).
-- ===========================================================================

create table if not exists import_operations (
  id uuid primary key,
  church_id uuid not null references churches(id),
  scale_id uuid not null references scales(id) on delete cascade,
  created_at bigint not null
);

create index if not exists import_operations_church_idx on import_operations (church_id);

alter table import_operations enable row level security;

drop policy if exists "import_operations: admin read own church" on import_operations;
create policy "import_operations: admin read own church" on import_operations for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = import_operations.church_id));

revoke insert, update, delete on import_operations from anon, authenticated;
grant select on import_operations to authenticated;

create or replace function import_schedule_with_assignments(
  p_operation_id uuid,
  p_church_id uuid,
  p_schedule jsonb,
  p_assignments jsonb
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_existing_scale_id uuid;
  v_scale_id uuid;
  v_is_admin boolean;
  v_assignment jsonb;
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  select scale_id into v_existing_scale_id from import_operations where id = p_operation_id;
  if v_existing_scale_id is not null then
    return v_existing_scale_id;
  end if;

  select exists (
    select 1 from profiles where id = auth.uid() and is_admin and church_id = p_church_id
  ) into v_is_admin;
  if not v_is_admin then
    raise exception 'not authorized to import a schedule for this church';
  end if;

  insert into scales (
    church_id, date, start_time, end_time, type, title,
    reception_person, sound_person, preaching_person, conducting_person, musical_message_person,
    notes, is_special_event, is_temporary, source_type, created_at, updated_at
  ) values (
    p_church_id,
    (p_schedule->>'date')::date,
    (p_schedule->>'start_time')::time,
    case when p_schedule->>'end_time' is null then null else (p_schedule->>'end_time')::time end,
    coalesce(p_schedule->>'type', ''),
    coalesce(p_schedule->>'title', ''),
    coalesce(p_schedule->>'reception_person', ''),
    coalesce(p_schedule->>'sound_person', ''),
    coalesce(p_schedule->>'preaching_person', ''),
    coalesce(p_schedule->>'conducting_person', ''),
    coalesce(p_schedule->>'musical_message_person', ''),
    coalesce(p_schedule->>'notes', ''),
    coalesce((p_schedule->>'is_special_event')::boolean, false),
    coalesce((p_schedule->>'is_temporary')::boolean, false),
    coalesce(p_schedule->>'source_type', 'OFFICIAL'),
    v_now, v_now
  )
  returning id into v_scale_id;

  for v_assignment in select * from jsonb_array_elements(p_assignments)
  loop
    insert into scale_assignments (
      scale_id, role_id, person_id, custom_person_name,
      role_name_snapshot, person_name_snapshot, position, notes, created_at, updated_at
    ) values (
      v_scale_id,
      (v_assignment->>'role_id')::uuid,
      case when v_assignment->>'person_id' is null then null else (v_assignment->>'person_id')::uuid end,
      v_assignment->>'custom_person_name',
      coalesce(v_assignment->>'role_name_snapshot', ''),
      coalesce(v_assignment->>'person_name_snapshot', ''),
      coalesce((v_assignment->>'position')::int, 0),
      coalesce(v_assignment->>'notes', ''),
      v_now, v_now
    );
  end loop;

  insert into import_operations (id, church_id, scale_id, created_at) values (p_operation_id, p_church_id, v_scale_id, v_now);

  return v_scale_id;
end;
$$;

revoke all on function import_schedule_with_assignments(uuid, uuid, jsonb, jsonb) from public;
grant execute on function import_schedule_with_assignments(uuid, uuid, jsonb, jsonb) to authenticated;
