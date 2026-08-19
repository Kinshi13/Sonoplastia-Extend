# Escala Church — instrução persistente: Supabase

Monorepo: `app/` (Android/Kotlin), `web/` (Next.js), `supabase/` (schema/migrations,
backend comum aos dois).

## REGRA SUPABASE (leia antes de tocar em qualquer tabela/coluna/RPC)

Este projeto já teve vários incidentes de produção causados por frontend
atualizado antes do banco: `worship_songs` inexistente, `scales.is_temporary`
inexistente, migrations 006-010 não aplicadas, RPCs 012/013 aplicadas
manualmente sem registro. Ver `supabase/MIGRATION_INVENTORY.md` para o histórico
completo.

**Antes de adicionar ao código qualquer referência a tabela nova, coluna nova,
RPC nova, constraint nova, enum novo ou relacionamento novo:**

1. Verifique se já existe uma migration correspondente (`grep` em
   `supabase/migrations/` e consulte `supabase/MIGRATION_INVENTORY.md`).
2. Se não existir: **crie a migration na mesma entrega**. Nunca implemente só o
   frontend e deixe o banco para depois.
3. Adicione uma linha em `supabase/MIGRATION_INVENTORY.md` para a migration nova.
4. Classifique a migration como SAFE, REVIEW ou DANGEROUS (regras em
   `supabase/MIGRATION_POLICY.md` §3). Migrations REVIEW/DANGEROUS nunca são
   aplicadas em produção sem autorização explícita do usuário, por escrito, para
   aquela migration específica.
5. Prepare um script em `supabase/manual/` (para colar no SQL Editor) e, se fizer
   sentido, uma query em `supabase/validation/` para confirmar depois.

**Nunca afirme que uma alteração "existe em produção" só porque a migration foi
criada no Git.** Distinga sempre, na sua resposta ao usuário:

- `CREATED` — o arquivo de migration existe no repositório.
- `TESTED LOCALLY` — validado por typecheck/lint/build, sem banco real disponível.
- `APPLIED TO PRODUCTION` — alguém rodou o SQL contra o banco real.
- `VERIFIED IN PRODUCTION` — confirmado ao vivo (ex.: `node
  scripts/check-supabase-schema.mjs`, ou uma query em `supabase/validation/`).

Uma feature dependente de banco só está "concluída" no estado `VERIFIED IN
PRODUCTION`. Se o ambiente de execução não tem credenciais do Supabase (comum
neste projeto — containers efêmeros não carregam `web/.env.local`), diga isso
explicitamente em vez de presumir que está tudo certo.

## Erro "Could not find ... in the schema cache" (PGRST204/PGRST205)

Não é automaticamente "preciso de `NOTIFY pgrst, 'reload schema'`". Primeiro
confirme se a tabela/coluna/função realmente existe (`check-supabase-schema.mjs`
ou uma query em `supabase/validation/`). Se não existir, a causa é migration não
aplicada — aplique-a. Só considere o reload do cache se a coluna já existir e o
erro persistir mesmo assim. Detalhe completo em `supabase/MIGRATION_POLICY.md` §5.

## Nunca

- Nunca rode `DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, `DELETE` em massa, reset de
  banco ou reset de histórico de migrations sem autorização explícita e
  específica do usuário para aquela operação.
- Nunca commit de `SUPABASE_SERVICE_ROLE_KEY`, senha de banco, access token ou
  qualquer secret. `.env*` já está no `.gitignore` de `web/` — mantenha assim.
- Nunca aplique uma migration em produção só porque ela existe no Git — aplicar
  é sempre uma ação explícita (sua ou, quando autorizado, do Claude).

## Onde ver mais

- `supabase/MIGRATION_POLICY.md` — regra completa, pipeline, classificação de
  risco, arquitetura de automação futura.
- `supabase/MIGRATION_INVENTORY.md` — estado de cada migration hoje.
- `supabase/ROLLBACK_GUIDE.md` — o que é reversível e como, migration por
  migration.
- `web/CLAUDE.md` — instruções específicas do Next.js usado neste projeto.
