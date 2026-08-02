-- ===========================================================================
-- RUN_TEMPORARY_AND_REUSED_SCALES.sql
--
-- Hotfix: "Could not find the 'is_temporary' column of 'scales' in the
-- schema cache". Causa raiz: a migration 017_scale_templates_and_temporary.sql
-- já existe no repositório (já commitada), mas nunca foi aplicada no banco de
-- produção - este ambiente de execução não tem credenciais de banco (sem
-- service role key, sem senha do Postgres, sem projeto Supabase CLI
-- vinculado), então não é possível aplicar migrations diretamente aqui.
--
-- Cole este arquivo inteiro no Supabase Dashboard -> SQL Editor e execute.
--
-- O QUE ESTE SCRIPT FAZ (e só isso):
--   1. scales.is_temporary            - "Escala temporária" (já usada pelo
--      formulário/checkbox/badge no admin e no site público).
--   2. scale_templates.is_protected   - protege os 3 modelos padrão
--      (Quarta/Sábado/Domingo) contra exclusão acidental (já usado pela
--      tela "Modelos").
--   3. notify pgrst, 'reload schema'  - força o PostgREST a recarregar o
--      cache de esquema imediatamente, sem esperar o próximo deploy/restart.
--
-- O QUE ESTE SCRIPT *NÃO* FAZ:
--   - Não cria `reused_from_scale_id` (ou qualquer coluna de rastreamento de
--     reutilização). Auditoria confirmou zero referências a esse conceito em
--     todo o frontend (web/app) - "Reutilizar" e "Aplicar modelo" já criam
--     um registro novo com id novo, sem tocar na escala de origem, sem
--     precisar de nenhuma coluna nova para isso. Adicionar uma coluna sem
--     uso real violaria a própria instrução deste hotfix.
--   - Não apaga dados, não dropa colunas, não recria public.scales, não
--     trunca nada.
--
-- Idempotente: seguro rodar mais de uma vez (todo `add column`/`create
-- index` usa `if not exists`).
-- ===========================================================================

begin;

alter table public.scales
  add column if not exists is_temporary boolean not null default false;

alter table public.scale_templates
  add column if not exists is_protected boolean not null default false;

commit;

-- Recarrega o cache de esquema do PostgREST imediatamente (fora da
-- transação acima de propósito - é um sinal assíncrono, não uma alteração
-- de dados, e não precisa fazer parte do commit/rollback da DDL).
notify pgrst, 'reload schema';
