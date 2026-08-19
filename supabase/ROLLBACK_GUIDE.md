# Guia de rollback — Escala Church

Nenhum rollback automático é inventado aqui. Cada migration existente está
documentada abaixo com o rollback real disponível hoje. Se algo não está listado
como "rollback possível", é porque não é seguro reverter sem uma decisão humana
sobre o que fazer com os dados.

## Regra geral

Toda migration existente até agora (`002`-`017`) é **puramente aditiva**: cria
tabela nova, adiciona coluna com `default`, cria índice, cria RPC nova, ou faz
backfill idempotente sem sobrescrever nada. Nenhuma delas remove ou renomeia algo
que já existia. Por isso, para todas elas, o "rollback" é sempre o mesmo:
**parar de usar a coluna/tabela nova no código** — os dados antigos nunca foram
tocados, então não há nada para restaurar.

## Por migration

| Migration | Rollback possível? | Como |
|---|---|---|
| 002-009 | Sim, trivial | Nenhuma removeria dado existente. Reverter = não usar a coluna/tabela nova. |
| 010 (organization_roles/people/teams/scale_assignments/scale_templates) | Sim, mas com cuidado | São tabelas novas — um `DROP` reverteria limpo *se* nada tiver escrito nelas ainda. Depois que existirem dados reais (pessoas cadastradas, escalas com assignments), um DROP perde esses dados — nesse ponto o rollback deixa de ser "possível sem perda" e vira uma decisão de produto, não técnica. As 5 colunas legadas de `scales` continuam intactas em paralelo (nunca foram alteradas), então mesmo revertendo 010 o app não quebra — só volta a depender só das colunas legadas. |
| 011 (backfill scale_assignments) | Sim, mas manual | É um `INSERT` idempotente que nunca sobrescreve. Reverter = `DELETE FROM scale_assignments WHERE created_at >= <momento do backfill>` — precisa do timestamp exato de quando rodou, não há uma forma automática de distinguir "veio do backfill" de "veio de uso normal depois". Não incluído como script pronto porque é uma operação destrutiva por definição (DANGEROUS, categoria da Fase 13) — só ser preparado sob pedido explícito. |
| 012/013 (RPCs) | Sim, trivial | `DROP FUNCTION public.get_church_subscription(uuid);` / `DROP FUNCTION public.get_church_by_code(text);`. Sem dados envolvidos — são funções puras de leitura. Quebraria o Android (essas RPCs são usadas lá), então só reverter junto com uma mudança no app. |
| 014-016 (worship_songs, web_push_subscriptions) | Sim, mas com cuidado | Tabelas novas — DROP limpo antes de terem dados reais. Depois que igrejas cadastrarem músicas/inscrições de push, vira decisão de produto (perde conteúdo real), não técnica. |
| 017 (scales.is_temporary, scale_templates.is_protected) | Sim, trivial | Colunas novas com default `false` — `ALTER TABLE scales DROP COLUMN is_temporary;` não afeta nenhuma outra coluna. Perde apenas a marcação "temporária" das escalas que já tiverem sido marcadas (dado real, mas de baixo impacto — reverte para "não marcada", não corrompe nada). |

## Quando um backup é necessário antes de aplicar

Nenhuma migration atual precisa de backup prévio (todas são aditivas, sem dado
existente em risco). Isso muda no momento em que uma migration futura for
classificada como REVIEW ou DANGEROUS (ver `MIGRATION_POLICY.md` §3) — nesses
casos, o guia é:

1. **REVIEW** (ex.: `NOT NULL` numa coluna existente, mudança de tipo, FK nova
   sobre tabela com dados): confirmar via `validation/` que não há linha que
   violaria a nova regra, *antes* de aplicar. Se houver, a migration precisa de
   um passo de backfill/limpeza junto, não só o `ALTER TABLE`.
2. **DANGEROUS** (`DROP`, `TRUNCATE`, `DELETE` em massa, reset): exportar a
   tabela afetada via Supabase Dashboard → Database → Backups (ou
   `pg_dump` de uma tabela específica, se você tiver acesso direto ao Postgres)
   antes de qualquer execução. Isso é sempre uma ação sua, nunca automática.

## O que este projeto ainda não tem

- Um mecanismo de "point-in-time recovery" automatizado documentado aqui — o
  Supabase Dashboard tem backups diários no plano pago; confirmar com você se o
  projeto de produção está nesse plano antes de depender disso como rede de
  segurança.
- Um script de rollback pronto para 011 (backfill) — deliberadamente não criado
  nesta sessão por ser uma operação DANGEROUS; preparo isso só sob pedido
  explícito, com o timestamp real do backfill em mãos.
