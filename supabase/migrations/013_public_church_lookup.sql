-- ===========================================================================
-- 013_public_church_lookup.sql
-- Fase 11.9 Parte 3 - RPC de busca de igreja por codigo/slug para a entrada
-- inicial do Android (ChurchEntryScreen).
--
-- churches ja e publicamente legivel na integra ("churches: public read"
-- using (true), ver schema.sql/002_multi_tenant.sql) - essa RPC nao abre
-- nada de novo, so evita que o Android dependa diretamente do nome/formato
-- das colunas e centraliza a normalizacao da entrada do usuario (trim +
-- lower) num unico lugar em vez de duplicar essa logica em cada tela.
--
-- "codigo da igreja" = churches.slug hoje - nao existe (e esta migration nao
-- cria) uma coluna separada de codigo. O mesmo slug usado nas URLs publicas
-- do site (/c/<slug>) e o que o usuario digita no Android.
-- ===========================================================================

create or replace function public.get_church_by_code(p_code text)
returns table (
  id uuid,
  slug text,
  name text,
  is_active boolean
)
language sql
security definer
set search_path = ''
stable
as $$
  select c.id, c.slug, c.name, c.is_active
  from public.churches c
  where c.slug = lower(trim(p_code))
  limit 1;
$$;

revoke all
on function public.get_church_by_code(text)
from public;

grant execute
on function public.get_church_by_code(text)
to anon, authenticated;
