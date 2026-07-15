-- Fase 11.7 - export center permissions/audit trail (Parte 10-11), and dormant Stella Atlas
-- integration scaffolding (Parte 16 - "não implementar infraestrutura excessivamente complexa se
-- o Atlas ainda não existe": this table exists so the shape is settled, nothing writes to it yet).

-- Grant the new export FeatureKeys to the plans that already include EXPORT, so nothing regresses
-- for an existing paying church - Essencial+ keeps exporting, just through named capabilities now.
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV"]'::jsonb
  where code in ('ESSENTIAL');
update plans set features = features || '["EXPORT_GENERAL_SCALE","EXPORT_SCALE_CSV","EXPORT_SCALE_PDF","EXPORT_SCALE_IMAGE","PUBLIC_READONLY_LINK"]'::jsonb
  where code in ('PRO', 'ORGANIZATION', 'FOUNDER');

-- Parte 10 (Auditoria): who exported what, from where.
create table if not exists export_audit_log (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references churches(id),
  user_id uuid not null references auth.users(id),
  format text not null, -- 'csv' | 'pdf' | 'image' | 'print'
  period text,          -- e.g. the month requested, free-form
  filters jsonb not null default '{}',
  created_at bigint not null
);

alter table export_audit_log enable row level security;
drop policy if exists "export_audit_log: admin read own church" on export_audit_log;
drop policy if exists "export_audit_log: admin insert own church" on export_audit_log;
create policy "export_audit_log: admin read own church" on export_audit_log for select
  using (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = export_audit_log.church_id));
create policy "export_audit_log: admin insert own church" on export_audit_log for insert
  with check (exists (select 1 from profiles where id = auth.uid() and is_admin and church_id = export_audit_log.church_id));

grant select, insert on public.export_audit_log to authenticated;

-- Parte 16 (Outbox pattern) - domain events queued here whenever something Atlas-relevant
-- changes, processed asynchronously by a future worker rather than a synchronous call inside the
-- request that made the change (so a slow/down Atlas never blocks or fails a save). Nothing in
-- this codebase writes to this table yet - see lib/atlas/types.ts for the StellaDomainEvent shape
-- this row's `payload` is meant to hold, and AtlasEventOutbox for the (currently no-op) writer.
create table if not exists integration_outbox (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references churches(id),
  app_code text not null default 'STELLA_SCALE',
  event_type text not null,   -- e.g. 'scale.created', see lib/atlas/types.ts
  entity_type text not null,  -- e.g. 'scale'
  entity_id uuid not null,
  payload jsonb not null default '{}',
  status text not null default 'PENDING', -- PENDING | SENT | FAILED
  attempts integer not null default 0,
  created_at bigint not null,
  processed_at bigint,
  last_error text
);

alter table integration_outbox enable row level security;
-- No client-facing policy on purpose - this table is only ever touched by a future service-role
-- worker, mirroring how the Stripe webhook is the only writer of `subscriptions.status`. No
-- grant to anon/authenticated either, unlike export_audit_log above.
