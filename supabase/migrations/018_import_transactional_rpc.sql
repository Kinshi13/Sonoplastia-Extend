-- Fase 11.15-B (Importação Inteligente V2, Etapa E) - a single, atomic, idempotent way to create
-- one schedule + its individual scale_assignments, for the Android importer (and, later, the web
-- import UI). Additive only: no existing table/column/policy is touched or dropped.
--
-- PROPOSTA - NÃO APLICADA EM PRODUÇÃO. CREATED apenas (ver supabase/MIGRATION_POLICY.md §2).
-- Classificação: REVIEW (ver supabase/MIGRATION_INVENTORY.md) - introduz uma função SECURITY
-- DEFINER nova que escreve dados, o que o próprio MIGRATION_POLICY.md exclui explicitamente da
-- faixa SAFE para RPCs ("sem side-effects"). Precisa de autorização explícita e específica antes
-- de ser executada contra o banco real.
--
-- Por que uma função em vez de duas chamadas client-side (insert em `scales`, depois insert em
-- `scale_assignments`): uma função PL/pgSQL chamada via RPC é atômica em relação ao erro - se
-- qualquer INSERT dentro dela levantar uma exceção, TUDO que a função já tinha feito é desfeito
-- automaticamente (não precisa de um BEGIN/COMMIT manual), então nunca existe um estado "escala
-- criada, mas sem nenhuma atribuição" por uma falha de rede no meio do caminho - só existia esse
-- risco porque o Android fazia 1 chamada por INSERT.

-- Idempotência: a tabela abaixo é a fonte de verdade de "esta operação de importação já
-- aconteceu" - a RPC consulta por p_operation_id ANTES de inserir qualquer coisa, e retorna o
-- `scale_id` já criado em vez de criar de novo. O identificador vem do cliente (Android gera um
-- UUID estável por rascunho de importação - ver CanonicalSchedule.localId - e reenvia o mesmo
-- valor em qualquer retry/reenvio, nunca um novo).
create table if not exists import_operations (
  id uuid primary key,
  church_id uuid not null references churches(id),
  scale_id uuid not null references scales(id) on delete cascade,
  created_at bigint not null
);

create index if not exists import_operations_church_idx on import_operations (church_id);

alter table import_operations enable row level security;

-- Só leitura para admin da própria igreja - nenhuma policy de insert/update/delete para
-- anon/authenticated, porque só a função SECURITY DEFINER abaixo escreve aqui (mesmo padrão já
-- usado para web_push_subscriptions nesta mesma conversa: nenhuma permissão direta de escrita,
-- só através de uma função estreita).
drop policy if exists "import_operations: admin read own church" on import_operations;
create policy "import_operations: admin read own church" on import_operations for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = import_operations.church_id));

revoke insert, update, delete on import_operations from anon, authenticated;
grant select on import_operations to authenticated;

-- p_schedule: objeto JSON com as mesmas chaves de `scales` (date, start_time, end_time, type,
-- title, reception_person, sound_person, preaching_person, conducting_person,
-- musical_message_person, notes, is_special_event, is_temporary, source_type).
-- p_assignments: array JSON de objetos {role_id, person_id, custom_person_name,
-- role_name_snapshot, person_name_snapshot, position, notes} - um por CanonicalParticipant já
-- pronto para publicação (role_id sempre uma `organization_roles.id` real; uma função ainda não
-- conciliada nunca entra aqui - ver CanonicalSchedule.isReadyToPublish no Android).
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

  -- Esta função roda como o owner da tabela (SECURITY DEFINER), então as policies normais de
  -- `scales`/`scale_assignments` não se aplicam aqui - esta checagem substitui exatamente o que
  -- "scales: admin write"/"scale_assignments: admin write" já exigem.
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

-- Ninguém chama isto anonimamente - só uma sessão autenticada (a checagem de admin acima decide
-- o resto). Mesma régua já usada para upsert_web_push_subscription/delete_web_push_subscription.
revoke all on function import_schedule_with_assignments(uuid, uuid, jsonb, jsonb) from public;
grant execute on function import_schedule_with_assignments(uuid, uuid, jsonb, jsonb) to authenticated;
