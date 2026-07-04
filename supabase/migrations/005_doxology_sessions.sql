-- Adds an optional end_time to doxologies, so multiple same-day entries (Escola Sabatina,
-- Culto Divino, JA...) can define their own time range - that's what lets the site/app know
-- which one is happening "now". Safe to run once on the existing production database.
alter table doxologies add column if not exists end_time time;
