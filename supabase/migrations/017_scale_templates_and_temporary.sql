-- ===========================================================================
-- 017_scale_templates_and_temporary.sql
-- Duas peças pedidas para a atualização de Escalas:
--
-- 1. "Escala Temporária" - `scales.is_temporary`, mesmo padrão booleano já
--    usado por `is_special_event` (mesma tabela, mesmo tipo, mesmo default
--    false) - marca uma escala que não segue o ciclo oficial normal (feita
--    "às pressas"), sem mexer em `type`/`source_type` (ambos já usados/
--    reservados para outra coisa - ver auditoria).
--
-- 2. "Escalas padrão" (modelos fixos de quarta/sábado/domingo) - reaproveita
--    `scale_templates`, que já existe desde 010_organization_roles_people.sql
--    mas nunca foi usada por nenhuma tela (auditado: zero leituras/escritas
--    em web/app). Só precisa de `is_protected`, para os 3 modelos semeados
--    automaticamente não poderem ser excluídos por engano - Admin continua
--    podendo editar o conteúdo deles livremente.
-- ===========================================================================

alter table scales add column if not exists is_temporary boolean not null default false;

alter table scale_templates add column if not exists is_protected boolean not null default false;
