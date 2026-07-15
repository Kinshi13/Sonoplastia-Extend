-- Fase 11.8.3 (Bloco N-S): "Reutilizar programação recente" for Doxologia. All additive/nullable
-- with safe defaults - no existing row is touched, nothing destructive. Safe to run once on the
-- existing production database.

-- Bloco S: favoriting a Doxologia so it sorts to the top of the reuse picker.
alter table doxologies add column if not exists is_favorite boolean not null default false;

-- Bloco R: an optional, non-authoritative pointer back to whatever Doxologia this one was cloned
-- from - `on delete set null` so deleting the original never blocks/cascades into deleting a copy
-- that has since taken on a life of its own. Never required (see Bloco R: "não transformar esse
-- campo em dependência obrigatória").
alter table doxologies add column if not exists reused_from_doxology_id uuid references doxologies(id) on delete set null;

-- Bloco P: "mais reutilizadas" sort - incremented on the *source* row every time it's used as the
-- base for a new Doxologia (never on the clone itself).
alter table doxologies add column if not exists times_reused integer not null default 0;

create index if not exists doxologies_church_favorite_idx on doxologies (church_id, is_favorite, date desc);
