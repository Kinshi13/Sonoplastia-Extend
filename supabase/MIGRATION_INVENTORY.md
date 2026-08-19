# Inventário de migrations — Escala Church

Gerado por auditoria estática (código + arquivos locais). **Nenhuma migration foi
considerada aplicada apenas por existir no Git.**

## Estado da conexão usada nesta auditoria

Este ambiente de execução **não tinha nenhuma credencial do Supabase disponível**
(sem `web/.env.local`, sem variável de ambiente, sem `supabase/config.toml`, sem
Supabase CLI logado). Por isso a coluna **Estado remoto** abaixo é `UNKNOWN` para
tudo, exceto onde um comentário no próprio código documenta uma verificação ao
vivo feita em uma sessão anterior (citada explicitamente, com a data/contexto que
consta no arquivo). Rode `node scripts/check-supabase-schema.mjs` (ver
`MIGRATION_POLICY.md`) com as credenciais reais para obter o estado remoto atual.

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
| (baseline) | Schema base: churches, profiles, scales, doxologies, announcements, retrospective_items, worship_songs, web_push_subscriptions, bulletins, plans, subscriptions, export_audit_log, integration_outbox, shared_files, storage policies | `schema.sql` | CONFIRMED_APPLIED* | UNKNOWN | — | Nenhuma. *Presumido pela app estar em produção há tempo, não verificado ao vivo nesta sessão. |
| 002 | Multi-tenant: `church_id` em todas as tabelas, `churches`, RLS por igreja | `002_multi_tenant.sql` | SUPERSEDED (conteúdo já está em `schema.sql`) | UNKNOWN | SAFE | Nenhuma — schema.sql já é o alvo de instalação limpa. |
| 003 | `shared_files.is_pinned` | `003_sonoplastia_pins.sql` | SUPERSEDED | UNKNOWN | SAFE | Nenhuma. |
| 004 | Tabela `bulletins` | `004_bulletins.sql` | SUPERSEDED | UNKNOWN | SAFE | Nenhuma. |
| 005 | Sessões de doxologia (colunas de horário) | `005_doxology_sessions.sql` | SUPERSEDED | UNKNOWN | SAFE | Nenhuma. |
| 006 | Tabelas `plans` + `subscriptions` | `006_plans_entitlements.sql` | SUPERSEDED (já em schema.sql) | UNKNOWN — comentário em `011` registra que, na época daquele hotfix, 006-010 **não** estavam aplicadas em produção | SAFE | Rodar `check-supabase-schema.mjs` para confirmar estado atual antes de assumir aplicada. |
| 007 | Colunas Stripe em `plans`/`subscriptions` | `007_stripe_billing.sql` | SUPERSEDED (já em schema.sql) | UNKNOWN (mesma ressalva de 006) | SAFE | Idem 006. |
| 008 | `export_audit_log`, `integration_outbox` | `008_export_and_atlas_prep.sql` | SUPERSEDED | UNKNOWN | SAFE | Idem 006. |
| 009 | `doxologies.is_favorite`/`reused_from_doxology_id`/`times_reused` | `009_doxology_reuse.sql` | SUPERSEDED | UNKNOWN | SAFE | Idem 006. |
| 010 | `organization_roles`, `organization_teams`, `organization_people`, `person_team_memberships`, `scale_assignments`, `scale_templates` + RLS + seed de roles legadas | `010_organization_roles_people.sql` | **LOCAL_ONLY** — DDL completa só existe na migration, `schema.sql` deliberadamente só resume (ver comentário nas linhas 472-496) | UNKNOWN — mesmo hotfix (011) registra "confirmado ao vivo contra produção que 006-010 não foram aplicadas" na época | REVIEW (cria FKs novas: `organization_roles.team_id`, `scale_assignments.role_id`/`person_id`, todas com `on delete` definido — não é DROP/destrutivo, mas é uma peça grande, revisar antes de reaplicar às cegas) | **Alto risco de estar pendente.** Confirmar com `check-supabase-schema.mjs` antes de qualquer feature nova que dependa de `organization_roles`/`scale_assignments`. |
| 011 | Backfill de dados: converte as 5 colunas legadas de `scales` em `scale_assignments` | `011_backfill_legacy_scale_assignments.sql` | MANUAL_HOTFIX (script de dados, não de schema — não pertence a `schema.sql`) | UNKNOWN — depende de 006-010 já aplicadas | REVIEW (`insert` em massa, mas idempotente via `not exists`; não é destrutivo, não sobrescreve nada existente) | Só rodar depois de confirmar que 006-010 estão aplicadas. |
| 012 | RPC `get_church_subscription(uuid)` — usada só pelo Android | `012_public_subscription_lookup.sql` | **LOCAL_ONLY**, ausente de `schema.sql` (sem nota-ponteiro, ao contrário de 010) | UNKNOWN — o próprio arquivo documenta que uma versão *anterior* desta RPC foi confirmada ausente em produção (`PGRST202`) quando este arquivo foi escrito, e foi substituída sem deixar histórico | SAFE (função `security definer`, `stable`, somente `select`, sem grant a `public`, só `anon`/`authenticated`) | Confirmar presença via RPC probe (`check-supabase-schema.mjs`). Recomendado: adicionar nota-ponteiro em `schema.sql` (ver NEEDS_REVIEW abaixo). |
| 013 | RPC `get_church_by_code(text)` — usada só pelo Android | `013_public_church_lookup.sql` | **LOCAL_ONLY**, ausente de `schema.sql` | UNKNOWN | SAFE (mesma forma de 012) | Idem 012. |
| 014 | Tabela `worship_songs` | `014_worship_songs.sql` | SUPERSEDED (já em `schema.sql`, adicionada numa sessão anterior) | **Confirmado ausente** por chamada REST direta (`GET .../worship_songs` → HTTP 404 `PGRST205`) numa sessão anterior desta mesma conversa, antes deste hotfix. Presumidamente ainda pendente — só você pode confirmar se já rodou `supabase/manual/RUN_PENDING_MIGRATIONS_014_017.sql`. | SAFE | **Confirmar se você já rodou `supabase/manual/RUN_PENDING_MIGRATIONS_014_017.sql`** (entregue numa sessão anterior). Se sim, remoto passa a CONFIRMED_APPLIED. |
| 015 | `worship_songs` colunas de recomendação/notificação + tabela `web_push_subscriptions` | `015_worship_recommendation_and_push.sql` | SUPERSEDED (já em schema.sql) | Mesmo status de 014 (mesmo script consolidado) | SAFE | Idem 014. |
| 016 | `worship_songs.recommendation_message` + índice único (1 recomendação/dia) | `016_worship_recommendation_message.sql` | SUPERSEDED | Mesmo status de 014 | REVIEW (cria `unique index` — se já existirem duas linhas "principais" no mesmo dia por alguma inconsistência anterior, a criação falharia; não há razão para isso acontecer dado o app nunca permitiu isso, mas vale confirmar antes) | Idem 014. |
| 017 | `scales.is_temporary`, `scale_templates.is_protected` | `017_scale_templates_and_temporary.sql` | SUPERSEDED (já em schema.sql) | **Confirmado ausente** — erro relatado por você (`Could not find the 'is_temporary' column`) nesta mesma sessão de trabalho. | SAFE | **Você ainda precisa rodar `supabase/manual/RUN_TEMPORARY_AND_REUSED_SCALES.sql` (ou `supabase/manual/RUN_PENDING_MIGRATIONS_014_017.sql`)** no SQL Editor. |

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

## O que fazer com este arquivo

Toda vez que uma migration nova for criada, adicione uma linha aqui **antes** de
considerar a feature pronta (ver `MIGRATION_POLICY.md`). Atualize "Estado remoto"
sempre que `check-supabase-schema.mjs` for rodado com credenciais reais.
