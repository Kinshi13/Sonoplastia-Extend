-- ===========================================================================
-- 016_worship_recommendation_message.sql
-- Música e Louvor - Parte 2 "Recomendação do dia": a short admin-written message
-- shown with the recommendation (distinct from notification_title/body, which
-- are for the future push, not the public card) + a DB-level guarantee that a
-- church can only have one *main* recommendation per date.
--
-- Auditoria: is_daily_recommendation/recommendation_date/notification_* já
-- existem (015_worship_recommendation_and_push.sql) - reaproveitados, não
-- duplicados. Só recommendation_message é novo aqui.
-- ===========================================================================

alter table worship_songs add column if not exists recommendation_message text;

-- Bloco 8/13: "para a mesma igreja e mesma data, deve existir apenas uma recomendação principal
-- do dia" - enforced as a real constraint (not just app-level discipline) via a partial unique
-- index, so a race between two admin tabs can't silently create two "main" recommendations for
-- the same day. Only applies to rows that are both flagged AND have a date - two songs can still
-- both have is_daily_recommendation=false, or a date-less draft, without tripping this.
create unique index if not exists worship_songs_one_recommendation_per_day
  on worship_songs (church_id, recommendation_date)
  where is_daily_recommendation and recommendation_date is not null;
