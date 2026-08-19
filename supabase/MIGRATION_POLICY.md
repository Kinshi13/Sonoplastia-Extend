# Política de migrations — Escala Church

Este documento é a regra permanente. `CLAUDE.md` (raiz do repo) aponta para cá.

## 1. Toda alteração de schema exige migration

Nenhuma feature que crie/altere tabela, coluna, RPC, constraint, enum ou
relacionamento pode ser considerada concluída só porque o frontend funciona.
Isso já causou incidentes reais neste projeto (`worship_songs`, `scales.is_temporary`,
006-010, RPCs 012/013 — ver `MIGRATION_INVENTORY.md`).

**Regra:** se o código (site ou Android) referencia uma tabela/coluna/RPC nova,
a migration correspondente é criada **na mesma entrega**, nunca depois.

## 2. Quatro estados possíveis — nunca confundir

| Estado | Significa |
|---|---|
| `CREATED` | O arquivo de migration existe no Git. |
| `TESTED LOCALLY` | Validado por leitura/typecheck/build, sem banco real disponível. |
| `APPLIED TO PRODUCTION` | Alguém rodou o SQL contra o banco real (SQL Editor ou CLI). |
| `VERIFIED IN PRODUCTION` | `check-supabase-schema.mjs` (ou consulta equivalente) confirmou, ao vivo, que a tabela/coluna/RPC existe. |

Uma migration só é "concluída" no estado `VERIFIED IN PRODUCTION`. Uma entrega
pode terminar em `CREATED`/`TESTED LOCALLY` quando não há credenciais disponíveis
no ambiente de execução — nesse caso, a entrega deve dizer isso explicitamente,
nunca implicar que está pronta.

## 3. Classificação de risco

### SAFE — pode ser proposta livremente

- Adicionar tabela nova;
- Adicionar coluna nullable ou com `default` seguro;
- Adicionar índice;
- Criar RPC nova (`security definer`, sem side-effects, `stable`/`immutable`);
- Adicionar policy nova e específica (não substituir uma existente).

### REVIEW — precisa de atenção antes de aplicar, mas não é bloqueada

- Adicionar `NOT NULL` numa coluna existente (precisa de backfill antes, ou
  `default` + `not null` juntos numa tabela pequena);
- Adicionar foreign key numa tabela com dados existentes (pode falhar se houver
  linha órfã — validar antes);
- Mudar o tipo de uma coluna existente;
- Alterar RLS de uma tabela existente;
- Alterar o corpo de uma função existente (RPC já em uso);
- `unique index` sobre dados existentes (pode falhar se já houver duplicata).

### DANGEROUS — nunca aplicada automaticamente, exige autorização explícita por escrito

- `DROP TABLE` / `DROP COLUMN`;
- `TRUNCATE`;
- `DELETE` em massa;
- `supabase db reset` / reset de histórico de migrations;
- Remoção de policy sem substituta imediata;
- Qualquer migração de dados irreversível.

Migrations REVIEW ou DANGEROUS **nunca** são aplicadas por uma sessão automatizada
sem uma frase explícita do usuário autorizando aquela migration específica.

## 4. Idempotência

Toda migration nova deve ser segura para rodar mais de uma vez:

```sql
create table if not exists ...
alter table ... add column if not exists ...
create index if not exists ...
drop policy if exists "..." on ...; create policy "..." on ...
```

**`IF NOT EXISTS` nunca é desculpa para não pensar no impacto.** Ele evita erro de
"já existe" — não torna uma mudança semanticamente perigosa em segura. Uma
migration que muda o *significado* de uma coluna existente (não só adiciona)
continua sendo REVIEW ou DANGEROUS mesmo se tecnicamente idempotente.

## 5. Schema cache do PostgREST

Um erro `Could not find ... in the schema cache` (`PGRST204`/`PGRST205`) **não
significa automaticamente "preciso de `NOTIFY pgrst, 'reload schema'`"**. Ordem
de diagnóstico correta:

1. A tabela/coluna/função realmente existe no banco? (`check-supabase-schema.mjs`
   ou `information_schema` via SQL Editor — ver `supabase/validation/`.)
2. Se não existe: a causa é migration não aplicada. Aplique a migration. O
   PostgREST normalmente recarrega o cache sozinho pouco depois de um DDL via
   conexão direta; via SQL Editor/`NOTIFY` isso é imediato.
3. Só se a coluna **já existir** e o erro persistir, considere
   `NOTIFY pgrst, 'reload schema';` como o problema (cache desatualizado, não
   schema ausente).

Nunca trate o reload como substituto de criar a coluna.

## 6. Estrutura de diretórios

```
supabase/
  migrations/         # fonte oficial de evolução do banco (nunca reordenar/mover arquivos existentes)
  validation/          # queries somente-leitura para conferir produção
  manual/               # scripts para colar no SQL Editor quando o CLI não pode ser usado
  schema.sql            # baseline de instalação limpa (não é migration numerada)
  MIGRATION_POLICY.md
  MIGRATION_INVENTORY.md
  ROLLBACK_GUIDE.md
```

Migrations antigas (`002`...`017`) **não foram renumeradas nem movidas** — fazer
isso quebraria a ordem que o Supabase CLI (se algum dia usado) espera. Scripts
soltos que já existiam na raiz de `supabase/` (`RUN_*`, `VALIDATE_*`,
`GRANT_TEST_PREMIUM.sql`) foram movidos para `manual/`/`validation/` conforme o
que fazem — isso é reorganização de arquivos auxiliares, não de migrations.

## 7. Nomenclatura — nota sobre o Supabase CLI

As migrations hoje usam `NNN_nome.sql` (numeração sequencial). O Supabase CLI
espera `<timestamp_YYYYMMDDHHMMSS>_nome.sql` para comandos como
`supabase migration list` e `supabase db push` funcionarem corretamente contra o
histórico remoto. **Isso ainda não foi feito** — é um pré-requisito da Fase 15
(automação futura), não desta fase. Não renomear os arquivos existentes sem
primeiro confirmar, com `supabase migration list` contra o projeto linkado, se
o histórico remoto usa nomes ou apenas os timestamps numéricos internos (a CLI
mantém isso numa tabela própria, `supabase_migrations.schema_migrations`) —
renomear às cegas pode fazer a CLI achar que tudo precisa ser reaplicado.

## 8. Pipeline definitivo de uma feature dependente de banco

```
Feature solicitada
  → Claude audita schema (grep por tabela/coluna/RPC referenciada + MIGRATION_INVENTORY.md)
  → Implementação (frontend/backend)
  → Migration criada (mesma entrega, nunca depois)
  → Script em manual/ preparado, se ainda não há acesso a CLI/credenciais
  → Validation query em validation/ preparada
  → typecheck
  → lint
  → tests (quando existirem)
  → build
  → check-supabase-schema.mjs (SCHEMA OK esperado seria falso aqui — a migration
    ainda não foi aplicada; a checagem serve para confirmar que o drift
    detectado é exatamente o esperado, nada a mais)
  → Migration aplicada (por você, ou pelo Claude só com autorização explícita)
  → check-supabase-schema.mjs de novo → SCHEMA OK
  → Deploy
```

**Nenhuma feature dependente de banco é "concluída" antes do passo de validação
remota confirmar `VERIFIED IN PRODUCTION`.** Uma entrega pode — e deve — parar
antes disso e dizer exatamente isso, quando não há credenciais no ambiente.

## 9. Backup / rollback

Ver `ROLLBACK_GUIDE.md`. Regra geral: nenhuma migration SAFE hoje tem rollback
não-trivial (todas são aditivas — o rollback é sempre "não usar a coluna nova",
sem precisar reverter nada). Migrations REVIEW/DANGEROUS futuras devem documentar
rollback explicitamente antes de serem propostas para aplicação.

## 10. Arquitetura para automação futura (preparada, não habilitada)

Objetivo futuro: `Claude → cria migration → testa → aplica migration segura →
valida Supabase → testa site → testa Android → informa resultado`, sem execução
autônoma de produção habilitada ainda. Pré-requisitos, na ordem:

1. **Link do projeto**: `supabase link --project-ref <ref>` rodado por você (ou
   com um `SUPABASE_ACCESS_TOKEN` fornecido explicitamente para a sessão) — ver
   Fase 3 do relatório de entrega.
2. **Renomear migrations** para o formato de timestamp do CLI (item 7 acima),
   feito com `supabase migration list` primeiro para não perder histórico.
3. **Toda migration nova classificada** (SAFE/REVIEW/DANGEROUS) automaticamente
   pela própria sessão antes de propor aplicação — heurística: presença de
   `drop `, `truncate`, `delete from` sem `where`, `not null` sobre coluna
   existente sem default, mudança de tipo (`alter column ... type`).
4. **Só migrations SAFE** poderiam, no futuro, ser aplicadas por uma sessão sem
   confirmação turno-a-turno — e mesmo assim, sempre com o passo de validação
   remota rodando logo depois e um resumo claro do que mudou.
5. **REVIEW e DANGEROUS continuam exigindo autorização explícita por escrito
   para cada aplicação individual**, para sempre — isso não é um passo
   temporário do rollout, é a regra final.

Nada disso está habilitado agora. Esta seção documenta o caminho, não liga
nada.
