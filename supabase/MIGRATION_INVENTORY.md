# Inventário de migrations — Escala Church

Gerado por auditoria estática (código + arquivos locais). **Nenhuma migration foi
considerada aplicada apenas por existir no Git.**

## Estado da conexão usada nesta auditoria

Este ambiente de execução **não tem credenciais do Supabase disponíveis**
diretamente (sem `web/.env.local`, sem variável de ambiente, sem
`supabase/config.toml`, sem Supabase CLI logado) — mas a coluna **Estado
remoto** abaixo já reflete dados reais: o usuário rodou
`supabase/validation/REMOTE_SCHEMA_REPORT.sql` e
`supabase/validation/VALIDATE_MIGRATIONS_002_007_011.sql` no SQL Editor de
produção e colou os resultados de volta. Ver
`supabase/MIGRATION_RECONCILIATION_REPORT.md` para a análise completa por
trás de cada classificação abaixo (inclui achados como "subscriptions:
public read"/"organization_people: public read" terem sido deliberadamente
endurecidas por `supabase/manual/RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql`,
não uma migration ausente). Migrations 005 permanece `UNKNOWN` porque a
coluna específica que ela adiciona (`doxologies.end_time`) não foi
confirmada explicitamente nos resultados recebidos até agora.

## Convenção de numeração

As migrations usam numeração sequencial (`002`...`017`), não o padrão
`<timestamp>_nome.sql` que o Supabase CLI espera para `supabase migration list` /
`supabase db push`. Isso funciona hoje porque nada aqui usa o CLI para aplicar
migrations (tudo é colado manualmente no SQL Editor). Se decidirmos habilitar o
CLI (Fase 15), as migrations precisarão ser renomeadas para o formato de
timestamp — ver nota em `MIGRATION_POLICY.md`.

`supabase/schema.sql` funciona como a migration `001` implícita: é o baseline de
instalação limpa. Migrations `002+` são deltas aditivos sobre ele. **Importante:**
`schema.sql` não é 100% completo — ele documenta explicitamente (comentário nas
linhas ~472-496) que omite a DDL completa das tabelas da migration `010`
(`organization_roles`, `organization_teams`, `organization_people`,
`person_team_memberships`, `scale_assignments`, `scale_templates`), apontando para
a migration como fonte autoritativa. As RPCs das migrations `012`/`013` **não têm
nem essa nota-ponteiro** — ver linha "NEEDS_REVIEW" na tabela.

## Tabela

| Migration | Feature | Arquivo | Estado local | Estado remoto | Risco | Ação |
|---|---|---|---|---|---|---|
| (baseline) | Schema base: churches, profiles, scales, doxologies, announcements, retrospective_items, worship_songs, web_push_subscriptions, bulletins, plans, subscriptions, export_audit_log, integration_outbox, shared_files, storage policies | `schema.sql` | CONFIRMED_APPLIED | **APPLIED** (via `REMOTE_SCHEMA_REPORT.sql` + `VALIDATE_MIGRATIONS_002_007_011.sql`) | — | Nenhuma. |
| 002 | Multi-tenant: `church_id` em todas as tabelas, `churches`, RLS por igreja | `002_multi_tenant.sql` | SUPERSEDED (conteúdo já está em `schema.sql`) | **APPLIED** — `churches`, `church_id NOT NULL` e as 14 policies com filtro de `church_id` confirmadas presentes | SAFE | Nenhuma. |
| 003 | `shared_files.is_pinned` | `003_sonoplastia_pins.sql` | SUPERSEDED | **APPLIED** — coluna e policy `shared_files: admin update` confirmadas | SAFE | Nenhuma. |
| 004 | Tabela `bulletins` | `004_bulletins.sql` | SUPERSEDED | **APPLIED** — tabela, todas as colunas e as 4 policies confirmadas | SAFE | Nenhuma. |
| 005 | Sessões de doxologia (colunas de horário) | `005_doxology_sessions.sql` | SUPERSEDED | **UNKNOWN** — `doxologies.end_time` especificamente não foi confirmada em nenhum dos resultados recebidos até agora | SAFE | Rodar de novo `VALIDATE_MIGRATIONS_002_007_011.sql` e conferir a linha `COLUMN \| public.doxologies.end_time`. |
| 006 | Tabelas `plans` + `subscriptions` + RLS original (`public read`) | `006_plans_entitlements.sql` | SUPERSEDED (já em schema.sql) | **SUPERSEDED_SAFE** — tabelas aplicadas; RLS real é a versão endurecida de `RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql` (`subscriptions: read own church`), não a policy aberta do arquivo cru | SAFE | Nenhuma — produção já está na versão correta (mais segura que o arquivo cru). |
| 007 | Colunas Stripe em `plans`/`subscriptions` | `007_stripe_billing.sql` | SUPERSEDED (já em schema.sql) | **APPLIED** — as 4 colunas e o índice único confirmados | SAFE | Nenhuma. |
| 008 | `export_audit_log`, `integration_outbox` | `008_export_and_atlas_prep.sql` | SUPERSEDED | **APPLIED** — ambas as tabelas confirmadas | SAFE | Nenhuma. |
| 009 | `doxologies.is_favorite`/`reused_from_doxology_id`/`times_reused` | `009_doxology_reuse.sql` | SUPERSEDED | **APPLIED** — "doxology reuse fields" confirmados | SAFE | Nenhuma. |
| 010 | `organization_roles`, `organization_teams`, `organization_people`, `person_team_memberships`, `scale_assignments`, `scale_templates` + RLS original | `010_organization_roles_people.sql` | **LOCAL_ONLY** — DDL completa só existe na migration, `schema.sql` deliberadamente só resume (ver comentário nas linhas 472-496) | **SUPERSEDED_SAFE** — todas as 6 tabelas aplicadas; RLS de `organization_people` é a versão endurecida (`admin read own church`), não a `public read` do arquivo cru; `organization_roles`/`organization_teams`/`scale_assignments` mantêm `public read` (inalterado) | SAFE | Nenhuma — produção já está correta. |
| 011 | Backfill de dados: converte as 5 colunas legadas de `scales` em `scale_assignments` | `011_backfill_legacy_scale_assignments.sql` | MANUAL_HOTFIX (script de dados, não de schema — não pertence a `schema.sql`) | **APPLIED** — confirmado por query de dados dedicada: 34 linhas em `scale_assignments`, 7 scales com dado legado, 0 scales com dado legado e zero assignments (`BACKFILL_COMPLETE`) | SAFE (já concluído) | Nenhuma. |
| 012 | RPC `get_church_subscription(uuid)` — usada só pelo Android | `012_public_subscription_lookup.sql` | **LOCAL_ONLY**, ausente de `schema.sql` (sem nota-ponteiro, ao contrário de 010) | **APPLIED** — RPC confirmada, `security definer` | SAFE | Recomendado (documentação, não schema): adicionar nota-ponteiro em `schema.sql` (ver NEEDS_REVIEW abaixo). |
| 013 | RPC `get_church_by_code(text)` — usada só pelo Android | `013_public_church_lookup.sql` | **LOCAL_ONLY**, ausente de `schema.sql` | **APPLIED** — RPC confirmada | SAFE | Idem 012. |
| 014 | Tabela `worship_songs` | `014_worship_songs.sql` | SUPERSEDED (já em `schema.sql`) | **NOT_APPLIED** — confirmado ausente via `REMOTE_SCHEMA_REPORT.sql` | SAFE | Rodar `supabase/manual/APPLY_PENDING_014_016_SAFE.sql` (ação sua, ainda não executada). |
| 015 | `worship_songs` colunas de recomendação/notificação + tabela `web_push_subscriptions` | `015_worship_recommendation_and_push.sql` | SUPERSEDED (já em schema.sql) | **NOT_APPLIED** — `web_push_subscriptions` confirmada ausente | SAFE, mas depende de 014 | Idem 014 — mesmo script consolidado cobre as três. |
| 016 | `worship_songs.recommendation_message` + índice único (1 recomendação/dia) | `016_worship_recommendation_message.sql` | SUPERSEDED | **NOT_APPLIED** — depende de `worship_songs` (014) | REVIEW→SAFE no script consolidado (idempotência corrigida ali) | Idem 014. |
| 017 | `scales.is_temporary`, `scale_templates.is_protected` | `017_scale_templates_and_temporary.sql` | SUPERSEDED (já em schema.sql) | **APPLIED** — ambas as colunas confirmadas presentes | SAFE | Nenhuma — já está feito. |

## NEEDS_REVIEW (não é uma migration pendente, é uma lacuna de documentação)

- `schema.sql` tem uma nota-ponteiro para a migration `010` (linhas ~472-496)
  explicando por que aquelas 6 tabelas não têm DDL completa ali. **As RPCs `012` e
  `013` não têm nota equivalente** — alguém lendo só `schema.sql` não saberia que
  elas existem. Recomendo adicionar um comentário similar (não fiz isso nesta
  sessão para não misturar mudança de schema com auditoria; é uma edição de
  1 comentário, baixo risco, mas prefiro seu aval antes de tocar em `schema.sql`
  de novo).

## Resumo por risco

- **SAFE** (13): 002, 003, 004, 005, 006, 007, 008, 009, 012, 013, 014, 015, 017
- **REVIEW** (3): 010 (FKs novas), 011 (backfill em massa, idempotente), 016 (unique index)
- **DANGEROUS** (0): nenhuma migration existente hoje é destrutiva.

## Resumo do estado remoto (confirmado ao vivo)

- **APPLIED** (11): baseline, 002, 003, 004, 007, 008, 009, 011, 012, 013, 017
- **SUPERSEDED_SAFE** (2): 006, 010 — schema 100% aplicado, RLS é a versão
  endurecida (`RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql`), não o
  arquivo cru
- **NOT_APPLIED** (3): 014, 015, 016 — script pronto em
  `supabase/manual/APPLY_PENDING_014_016_SAFE.sql`, ainda não executado
- **UNKNOWN** (1): 005 — só falta confirmar `doxologies.end_time`

Ver `supabase/MIGRATION_RECONCILIATION_REPORT.md` para a análise completa
por trás de cada uma.

## O que fazer com este arquivo

Toda vez que uma migration nova for criada, adicione uma linha aqui **antes** de
considerar a feature pronta (ver `MIGRATION_POLICY.md`). Atualize "Estado remoto"
sempre que uma consulta de `supabase/validation/` for rodada com credenciais reais.
