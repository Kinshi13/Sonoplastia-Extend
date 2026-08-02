-- ===========================================================================
-- VALIDATE_TEMPORARY_AND_REUSED_SCALES.sql
--
-- Somente leitura - rode depois de RUN_TEMPORARY_AND_REUSED_SCALES.sql para
-- confirmar que tudo foi aplicado corretamente. Nenhuma instrução aqui
-- altera dados.
-- ===========================================================================

-- 1. As colunas existem, com o tipo/default/nullable esperados -------------
select
  table_name,
  column_name,
  data_type,
  is_nullable,
  column_default
from information_schema.columns
where table_schema = 'public'
  and (
    (table_name = 'scales' and column_name = 'is_temporary')
    or (table_name = 'scale_templates' and column_name = 'is_protected')
  )
order by table_name, column_name;
-- Esperado: 2 linhas, ambas com data_type = 'boolean', is_nullable = 'NO',
-- column_default = 'false'.

-- 2. Nenhuma constraint/índice extra foi criado por engano ------------------
-- (Este hotfix não cria nenhum índice - não há coluna reused_from_scale_id
-- nem query que filtre por is_temporary sozinho hoje, então um índice novo
-- seria especulativo. Esta consulta é só para confirmar que nada além do
-- esperado apareceu.)
select indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and tablename in ('scales', 'scale_templates')
order by tablename, indexname;

-- 3. Escalas antigas continuam íntegras (nenhuma foi apagada/alterada) -----
select count(*) as total_scales from public.scales;

-- 4. Quantas escalas já estão marcadas como temporárias ---------------------
select count(*) as total_temporary_scales
from public.scales
where is_temporary;

-- 5. Escalas antigas devem ter recebido is_temporary = false por default ---
select count(*) as scales_with_null_or_unexpected_is_temporary
from public.scales
where is_temporary is null;
-- Esperado: 0 (a coluna é `not null default false`, então isto é só uma
-- checagem de sanidade - não deveria haver nenhuma linha aqui).

-- 6. Quantos modelos padrão existem e quantos estão protegidos --------------
select count(*) filter (where is_protected) as protected_templates,
       count(*) as total_templates
from public.scale_templates;

-- 7. Referências de reutilização inválidas / cross-church ------------------
-- Não aplicável: este hotfix não cria `reused_from_scale_id` (ver o
-- cabeçalho de RUN_TEMPORARY_AND_REUSED_SCALES.sql - o fluxo de reutilização
-- já cria um registro novo sem nenhuma coluna de rastreamento, então não há
-- o que validar aqui). Se essa coluna for adicionada em um trabalho futuro,
-- a validação correta seria:
--
--   select r.id as reused_scale_id, r.church_id as reused_church_id,
--          o.id as origin_scale_id, o.church_id as origin_church_id
--   from public.scales r
--   join public.scales o on o.id = r.reused_from_scale_id
--   where r.church_id <> o.church_id;
--   -- Esperado: 0 linhas (reutilização nunca deveria atravessar igrejas).
