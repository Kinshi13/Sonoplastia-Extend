# Relatório de reconciliação estrutural — migrations 002-017

Baseado no resultado real de `REMOTE_SCHEMA_REPORT.sql` rodado em produção
(colado por você), cruzado com a leitura de todas as migrations 002-017,
`MIGRATION_INVENTORY.md`, `MIGRATION_POLICY.md`, os scripts em
`supabase/manual/`, e o código atual (`web/app`, Android). **Nenhum SQL foi
executado nesta sessão** — classificação puramente por evidência estrutural.

## Limitação importante sobre os dados de entrada

Você me passou um **resumo curado** (bullets PRESENT/MISSING), não a grade
completa das 318 linhas que `REMOTE_SCHEMA_REPORT.sql` produz. Para tudo que
não apareceu explicitamente em nenhuma das duas listas (ex.: `bulletins`,
`doxologies.end_time`, `shared_files.is_pinned`, colunas Stripe específicas),
classifico como **UNKNOWN** abaixo — não presumo PRESENT só por ausência de
menção. Se você colar a grade completa (ou rodar de novo e exportar o CSV
inteiro), refino essas linhas.

## Achado principal: dois dos quatro "MISSING" extras não são bugs

Antes da tabela — isto muda a leitura de duas linhas inteiras do relatório:

### `subscriptions: public read` — divergência intencional, já documentada no repo

`supabase/manual/RUN_PENDING_MIGRATIONS_006_010_IDEMPOTENT_SAFE.sql` (linhas
70-80) **remove deliberadamente** a policy `"subscriptions: public read"`
(criada pela migration `006` crua, `using (true)` — qualquer um lê o plano/
billing de qualquer igreja) e a substitui por `"subscriptions: read own
church"`, exigindo `auth.uid()` casado com `profiles.church_id`. O comentário
da migration `012` (`012_public_subscription_lookup.sql`, linhas 6-8) **já
citava esse nome de policy** ("SAFE migration 006-010: 'subscriptions: read
own church' exige auth.uid()") — ou seja, o autor de `012` já sabia que a
versão SAFE (não a migration `006` crua) era o que estava rodando em
produção. Isso bate exatamente com o que seu relatório mostrou.

**Verifiquei o código atual**: toda leitura de `subscriptions` no site
(`web/app/admin/billing-actions.ts`, `app/admin/planos/page.tsx`,
`lib/entitlements.ts`) passa por rotas `/admin` com sessão autenticada — nunca
por um visitante anônimo. O Android nunca lê `subscriptions` diretamente, só
via a RPC `get_church_subscription` (confirmada PRESENT). **A policy
restrita não quebra nada hoje.** Isto é uma correção de segurança real
("subscriptions" é dado de billing, não devia ser world-readable) que já foi
aplicada e nunca foi documentada como uma migration própria.

### `organization_people: public read` — mesma história, mesmo arquivo

O mesmo script SAFE (linhas 363-370) remove `"organization_people: public
read"` (da migration `010` crua) e cria `"organization_people: admin read own
church"` no lugar — nomes/pessoas cadastradas deixam de ser legíveis por
qualquer visitante anônimo, só por admin autenticado da própria igreja.
`organization_roles`, `organization_teams` e `scale_assignments` **mantêm**
leitura pública nesse mesmo script (linhas 335-356, 392-407) — o que bate
exatamente com você não ter reportado essas três como MISSING.

**Verifiquei o código atual**: toda leitura de `organization_people`
(`web/app/admin/escalas/*`, `web/app/admin/pessoas/*`) é sempre dentro de
`/admin`, sempre autenticada. **Não quebra nada.**

**Conclusão para as duas:** Categoria D (diferença intencional) — não
Categoria E, porque há evidência escrita e específica no próprio repositório
apontando exatamente para essa mudança, não é uma suposição.

### As duas foreign keys (`profiles.id -> users`, `export_audit_log.user_id -> users`) — inconclusivo, provável falso positivo do meu script

Você está certo em desconfiar: "users" ali significa `auth.users`, não uma
tabela `public.users` (que não existe e nunca existiu neste projeto). O
`REMOTE_SCHEMA_REPORT.sql` verifica a FK por nome de tabela referenciada
(`ccu.table_name = 'users'`), sem checar o schema do lado referenciado — em
teoria isso deveria casar com `auth.users` normalmente (testei localmente
contra um Postgres vanilla e funcionou depois de eu corrigir um bug de JOIN
na sessão anterior). Mas o Supabase gerencia o schema `auth` de um jeito
específico, e é conhecido que `information_schema.constraint_column_usage`
pode não expor de forma confiável uma FK cujo lado referenciado está fora do
schema atual, dependendo de `search_path`/privilégios da role que roda a
consulta no SQL Editor. **Não tenho como confirmar qual dos dois é o caso sem
rodar mais uma consulta.** Ver Categoria E abaixo para a consulta de
follow-up (não executada).

---

## Tabela de reconciliação

| Migration | Feature | Estado | Evidência remota | Ausências | Próxima ação | Risco |
|---|---|---|---|---|---|---|
| (baseline) `schema.sql` | churches, profiles, scales, doxologies, announcements, retrospective_items, bulletins, plans, subscriptions, export_audit_log, integration_outbox, shared_files | **APPLIED** | `scales.is_temporary`, `plans`, `subscriptions`, `announcements`, `retrospective_items`, `export_audit_log`, `integration_outbox`, `shared_files` todos confirmados PRESENT | Nenhuma confirmada | Nenhuma | — |
| 002 `multi_tenant` | `church_id NOT NULL` + RLS por igreja nas tabelas antigas | **SUPERSEDED** (conteúdo em schema.sql) | Tabelas afetadas (`doxologies`, `announcements`, `retrospective_items`, `shared_files`) confirmadas PRESENT; `NOT NULL` específico não verificável no resumo | `is_nullable` de `church_id` não confirmado (precisa da grade completa) | Nenhuma — já é o baseline | SAFE |
| 003 `sonoplastia_pins` | `shared_files.is_pinned` | **UNKNOWN** | `shared_files` (tabela) PRESENT | Coluna `is_pinned` não mencionada no resumo | Confirmar com a grade completa | SAFE |
| 004 `bulletins` | Tabela `bulletins` | **UNKNOWN** | Não mencionada em nenhuma das duas listas | Presença da tabela não confirmada | Confirmar com a grade completa | SAFE |
| 005 `doxology_sessions` | `doxologies.end_time` | **UNKNOWN** | Não mencionada | Coluna `end_time` não confirmada (distinta dos "doxology reuse fields" que você confirmou, que são de 009) | Confirmar com a grade completa | SAFE |
| 006 `plans_entitlements` | Tabelas `plans`/`subscriptions` + RLS original (`public read`) | **PARTIALLY_APPLIED** | Tabelas PRESENT. RLS original (`subscriptions: public read`) ausente — **substituída deliberadamente** pela versão SAFE (`subscriptions: read own church`), ver acima | `subscriptions: public read` (esperado por este arquivo cru; não pelo que está de fato em produção) | Nenhuma — a versão em produção (SAFE) é a correta, mais segura que este arquivo | — (arquivo cru desatualizado, não é risco em si) |
| 007 `stripe_billing` | Colunas Stripe em `plans`/`subscriptions` + índice único | **UNKNOWN** | `plans`/`subscriptions` (tabelas) PRESENT | Colunas `stripe_price_id_monthly`, `stripe_customer_id`, etc. e o índice `subscriptions_stripe_subscription_id_key` não mencionados | Confirmar com a grade completa (billing via Stripe já funciona em produção? Se sim, é evidência indireta forte de que isto está aplicado) | SAFE |
| 008 `export_and_atlas_prep` | `export_audit_log`, `integration_outbox` | **APPLIED** | Ambas as tabelas confirmadas PRESENT | Nenhuma confirmada | Nenhuma | — |
| 009 `doxology_reuse` | `is_favorite`/`reused_from_doxology_id`/`times_reused` em `doxologies` | **APPLIED** | "doxology reuse fields" confirmado PRESENT por você | Nenhuma confirmada | Nenhuma | — |
| 010 `organization_roles_people` | `organization_roles`, `organization_teams`, `organization_people`, `person_team_memberships`, `scale_assignments`, `scale_templates` + RLS original | **PARTIALLY_APPLIED** | Todas as 6 tabelas confirmadas PRESENT. RLS de `organization_people` **substituída deliberadamente** pela versão SAFE (ver acima); RLS de `organization_roles`/`organization_teams`/`scale_assignments` permanece igual ao arquivo cru | `organization_people: public read` (esperado pelo arquivo cru; produção tem a versão SAFE, mais restrita) | Nenhuma — versão em produção é a correta | — |
| 011 `backfill_legacy_scale_assignments` | Converte colunas legadas de `scales` em linhas de `scale_assignments` | **UNKNOWN** | Este é um script de **dados**, não de schema — `REMOTE_SCHEMA_REPORT.sql` não consegue detectar se rodou (não cria/altera tabela/coluna nenhuma) | N/A — precisa de uma consulta de contagem, não de schema | Preparar uma query read-only separada (ex.: `select count(*) from scale_assignments` vs. quantas escalas têm os 5 campos legados preenchidos) se quisermos confirmar | REVIEW se algum dia precisar re-rodar (ver `ROLLBACK_GUIDE.md`) |
| 012 `public_subscription_lookup` | RPC `get_church_subscription(uuid)` | **APPLIED** | RPC confirmada PRESENT | Nenhuma | Nenhuma (recomendação separada: adicionar nota-ponteiro em `schema.sql`, é documentação, não schema) | — |
| 013 `public_church_lookup` | RPC `get_church_by_code(text)` | **APPLIED** | RPC confirmada PRESENT | Nenhuma | Nenhuma (mesma recomendação de documentação) | — |
| **014** `worship_songs` | Tabela `worship_songs` + índices + RLS + grants | **NOT_APPLIED** | `public.worship_songs` confirmada **MISSING** — tabela inteira ausente | Toda a tabela e sua estrutura | Ver Categoria C | SAFE (aditiva, tabela nova) |
| **015** `worship_recommendation_and_push` | Colunas de recomendação/notificação em `worship_songs` + tabela `web_push_subscriptions` | **NOT_APPLIED** | `public.web_push_subscriptions` confirmada **MISSING**; as colunas de `worship_songs` dependem de `014` (impossível ter rodado sem a tabela existir) | `web_push_subscriptions` inteira + todas as colunas que este arquivo adiciona a `worship_songs` | Ver Categoria C | SAFE (aditiva), mas **bloqueada por 014** |
| **016** `worship_recommendation_message` | `worship_songs.recommendation_message` + índice único (1 recomendação/dia) | **NOT_APPLIED** | Depende de `worship_songs` (014) já existir — `alter table` falharia sem ela | `recommendation_message` + o índice único | Ver Categoria C | REVIEW (índice único — mas **bloqueada por 014**, ver auditoria de risco abaixo) |
| **017** `scale_templates_and_temporary` | `scales.is_temporary`, `scale_templates.is_protected` | **APPLIED** | Ambas as colunas confirmadas **PRESENT** por você | Nenhuma | Nenhuma — já está feito | — |

---

## Plano dividido

### A — Estruturalmente já aplicadas, candidatas a "migration repair APPLIED" (se algum dia habilitarmos o CLI)

`002`, `003`\*, `004`\*, `005`\*, `006` (via SAFE), `007`\*, `008`, `009`,
`010` (via SAFE), `012`, `013`, `017`.

\* = classificados UNKNOWN aqui por falta da grade completa, mas são
candidatos fortes a esta categoria assim que confirmados — nenhum motivo
estrutural para suspeitar do contrário.

**Nota importante para quando isso virar `migration repair` de verdade**: `006`
e `010` não podem ser marcadas "repair APPLIED" apontando para os arquivos
crus (`006_plans_entitlements.sql`, `010_organization_roles_people.sql`) sem
mais explicação, porque o RLS realmente aplicado é o da versão SAFE, não o do
arquivo cru. Um "repair" ingênuo aqui criaria uma mentira no histórico (diria
"`010` rodou" quando na verdade rodou uma variante diferente dela). Ver
recomendação na sequência final.

### B — Parcialmente aplicadas, precisam de migration corretiva (só a nível de *documentação/histórico*, não de schema — o schema já está correto)

`006`, `010` — não porque falta algo no banco (não falta), mas porque **o
arquivo de migration em `supabase/migrations/` não reflete o que está em
produção**. A correção aqui não é rodar SQL — é atualizar os arquivos-fonte
(ou criar uma migration `018` documentando o hardening) para que o próximo
`schema.sql`/instalação limpa já nasça com a versão SAFE, não a insegura.
**Isto é uma proposta de mudança em arquivo de migration, não uma execução —
preciso da sua autorização antes de tocar nisso, e nesta tarefa você pediu só
classificação, então não fiz.**

### C — Realmente não aplicadas, podem ser executadas

`014`, `015`, `016` — nesta ordem exata, porque `015`/`016` dependem de `014`
ter rodado primeiro (ambas fazem `alter table worship_songs`, que falha se a
tabela não existir). As três já têm um script consolidado e idempotente
pronto no repositório: **`supabase/manual/RUN_PENDING_MIGRATIONS_014_017.sql`**
— inclui `017` também, mas como `017` já está aplicada, essa parte roda como
no-op seguro (`add column if not exists`, sem nenhum `create policy` nessa
seção). Não preparei um script novo separado porque este já existe, já é
idempotente, e já foi revisado — recriar um `_014_016` redundante seria
duplicação sem necessidade.

**Auditoria de risco pedida especificamente para as NOT_APPLIED:**

| | 014 | 015 | 016 |
|---|---|---|---|
| Idempotente (rodar 2x)? | **Não** — 4 `create policy` sem `drop policy if exists` antes (achado confirmado por teste local na sessão anterior) | **Não** — mesmo problema, 3 `create policy` em `web_push_subscriptions` | **Sim** — só `alter table add column if not exists` + `create unique index if not exists`, nenhuma policy |
| Pressupõe objetos anteriores? | `scales` (FK de `schedule_id`) e `profiles` (nas policies) — ambas já existem | **`worship_songs` (de 014)** — sem ela, `alter table` falha imediatamente | **`worship_songs` (de 014)** — mesma dependência |
| CREATE POLICY sem IF NOT EXISTS? | **Sim** (4 ocorrências) | **Sim** (3 ocorrências) | Não se aplica (nenhuma policy) |
| Pode falhar na segunda execução? | **Sim**, no `create policy` | **Sim**, no `create policy` | Não — totalmente seguro re-rodar |
| Altera dados existentes? | Não — tabela nova, zero linhas pré-existentes | Não — tabela nova + colunas novas com default | Não — coluna nova com default, índice não destrutivo |
| Risco de regressão? | **Baixo** — aditiva pura, nenhuma tabela/coluna existente é tocada, nenhum código hoje espera que `worship_songs` *não* exista | **Baixo**, mesma razão, bloqueada por 014 | **Baixo**, mesma razão, bloqueada por 014. Único risco teórico: o índice único falharia se já existissem 2+ "recomendações principais" no mesmo dia — impossível hoje porque a tabela nem existe ainda |

**Rodando o arquivo consolidado (`RUN_PENDING_MIGRATIONS_014_017.sql`) em vez
dos arquivos crus, o problema de "sem IF NOT EXISTS" desaparece** — ele já
tem `drop policy if exists` antes de cada `create policy` (conferido:
7 ocorrências, cobrindo as 4 de `worship_songs` + as 3 de
`web_push_subscriptions`), então é seguro mesmo se precisar rodar mais de uma
vez por engano.

### D — Diferenças intencionais entre migration antiga e arquitetura atual

1. `subscriptions: public read` → `subscriptions: read own church` (ver
   achado principal acima) — segurança, não bug.
2. `organization_people: public read` → `organization_people: admin read own
   church` (ver achado principal acima) — segurança, não bug.
3. `scales.reused_from_scale_id` — **não existe em nenhuma migration, não
   deve ser criada nesta reconciliação**, confirmado de novo nesta sessão.
   Nenhuma ação.
4. RPCs `012`/`013` ausentes de `schema.sql` — gap de documentação já
   registrado em `MIGRATION_INVENTORY.md` (seção NEEDS_REVIEW), não uma
   divergência de comportamento.

### E — Possíveis falsos positivos do diagnóstico

1. **`profiles.id -> auth.users` e `export_audit_log.user_id -> auth.users`
   reportadas MISSING** — provável limitação de
   `information_schema.constraint_column_usage` com FKs cross-schema no
   Supabase, não uma FK realmente ausente (ver explicação técnica acima).
   Query de follow-up sugerida (**não executada, só proposta** — somente
   leitura, usa `pg_constraint`/`pg_class` direto em vez de
   `information_schema`, que costuma ser mais confiável para isto):

   ```sql
   select
     conname,
     conrelid::regclass as from_table,
     confrelid::regclass as to_table,
     pg_get_constraintdef(oid) as definition
   from pg_constraint
   where contype = 'f'
     and conrelid::regclass::text in ('public.profiles', 'public.export_audit_log');
   ```

   Se você rodar isso e as duas FKs aparecerem apontando para `auth.users`,
   confirma que é um falso positivo do meu relatório (conserto a consulta).
   Se não aparecerem, é uma ausência real e viramos isso um achado de
   verdade — mas prefiro essa confirmação antes de propor qualquer coisa.

---

## Sequência proposta para reconciliar o histórico — SEM EXECUTAR NADA AGORA

1. **Você decide** se quer rodar a query de follow-up da Categoria E (só
   leitura) para fechar a dúvida das duas FKs. Opcional, baixo risco.
2. **Você decide** se quer que eu prepare uma query read-only para checar o
   estado de `011` (backfill de dados) por contagem, já que o diagnóstico de
   schema não alcança isso.
3. Se você confirmar que quer seguir com `014`/`015`/`016`: você roda
   `supabase/manual/RUN_PENDING_MIGRATIONS_014_017.sql` no SQL Editor
   (mesmo arquivo que já te entreguei antes — continua válido, `017` roda
   como no-op seguro). Isto é uma ação sua, explícita, quando você decidir.
4. Depois disso, rodar `REMOTE_SCHEMA_REPORT.sql` de novo para confirmar
   `worship_songs`/`web_push_subscriptions` e as colunas de
   recomendação/notificação todas PRESENT.
5. **Separado disso** (Categoria B): decidir se queremos atualizar
   `supabase/migrations/006_plans_entitlements.sql` e
   `010_organization_roles_people.sql` (ou documentar via uma migration nova,
   ex. `018_harden_subscriptions_and_people_rls.sql`) para que os arquivos
   crus parem de descrever um RLS mais aberto do que o que está de fato em
   produção — isto evita que uma instalação nova (ou um `migration repair`
   futuro) reintroduza a policy insegura por engano. **Não fiz isso agora —
   é uma mudança de arquivo de migration, preciso do seu aval específico.**
6. Só depois de 3-5 resolvidos é que faria sentido considerar `migration
   repair` via CLI (Fase 15 de `MIGRATION_POLICY.md`) — e mesmo assim, `006`/
   `010` precisariam apontar para a versão SAFE, não para o arquivo cru,
   conforme a nota na Categoria A.

Nenhum destes passos foi executado nesta sessão.

PRODUCTION UNCHANGED
