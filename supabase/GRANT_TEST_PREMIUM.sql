-- ===========================================================================
-- GRANT_TEST_PREMIUM.sql
-- ===========================================================================
-- Hotfix, Parte 1-3: grants the PRO plan to whichever church the test user
-- (maikoncraftgames@gmail.com) administers - for testing PNG export, Export
-- Studio, and other paid Feature Entitlements. Not a global unlock: it only
-- ever touches the one `subscriptions` row for that user's own church.
--
-- REQUIRES: RUN_PENDING_MIGRATIONS_006_010.sql must already be applied -
-- `subscriptions`/`plans` don't exist in production yet (confirmed live,
-- see the hotfix report), so this script will fail with "relation does not
-- exist" until that one runs first.
--
-- Run this in the Supabase SQL Editor with your own (elevated) access - the
-- app's anon/service keys available in this session cannot resolve an email
-- to a user_id (auth.users isn't exposed to anon, and `profiles` has no
-- email column), so this step genuinely has to be run by you, not me.
-- ===========================================================================


-- STEP 1 - DIAGNOSTIC. Run this first and read the result before doing
-- anything else. It resolves the email to every church that user profile is
-- tied to and shows the plan/subscription state as it is right now.
select
  u.id as user_id,
  u.email,
  p.church_id,
  p.is_admin,
  c.slug as church_slug,
  c.name as church_name,
  s.id as subscription_id,
  s.status as current_status,
  s.expires_at as current_expires_at,
  pl.code as current_plan_code
from auth.users u
left join profiles p on p.id = u.id
left join churches c on c.id = p.church_id
left join subscriptions s on s.church_id = p.church_id
left join plans pl on pl.id = s.plan_id
where u.email = 'maikoncraftgames@gmail.com';

-- Expected: exactly one row, with a non-null church_id and is_admin = true.
--   - Zero rows: the user hasn't signed up yet (or signed up with a
--     different email) - nothing to grant, stop here.
--   - church_id is null / is_admin is false: this user isn't an admin of
--     any church yet (the Stripe webhook normally sets both together on a
--     successful checkout) - grant a plan here won't do anything useful
--     until that's resolved separately; don't proceed blindly.
--   - More than one row: shouldn't happen given profiles.church_id is a
--     single column, but if it does, stop and figure out why before
--     continuing - don't guess which one.


-- STEP 2 - GRANT. Only run this after Step 1 confirmed exactly one church_id.
-- Copy that church_id into the two <CHURCH_ID> placeholders below.
--
-- PRO is the real plan code from the catalog (006_plans_entitlements.sql) -
-- it's the one that includes ADVANCED_MEDIA, EXPORT, ADVANCED_ADMIN and
-- REPORTS, which covers PNG export / Export Studio / the admin features
-- mentioned in the request. Expires in 90 days so this is clearly temporary,
-- not a silent permanent upgrade.
insert into subscriptions (church_id, plan_id, status, started_at, expires_at, source, updated_at)
values (
  '<CHURCH_ID>',
  (select id from plans where code = 'PRO'),
  'ACTIVE',
  extract(epoch from now()) * 1000,
  (extract(epoch from now()) * 1000) + (90 * 24 * 60 * 60 * 1000),
  'manual_test',
  extract(epoch from now()) * 1000
)
on conflict (church_id) do update set
  plan_id = excluded.plan_id,
  status = 'ACTIVE',
  expires_at = excluded.expires_at,
  source = 'manual_test',
  updated_at = excluded.updated_at
returning *;

-- STEP 3 - VERIFY. Re-run the Step 1 query (same email) - current_plan_code
-- should now read 'PRO' and current_status 'ACTIVE'. The web app reads this
-- table fresh on every server render (lib/entitlements.ts has no server-side
-- cache), so a normal page reload/relogin on the site is enough to see it -
-- no separate cache-busting step needed there. Android's EntitlementCache
-- picks it up on its own next sync/pull-to-refresh per its existing TTL.


-- ROLLBACK - to remove the temporary grant later (falls back to whatever the
-- church had before - for a brand-new church with no prior row, this puts it
-- back to no subscription row at all, which lib/entitlements.ts already
-- treats as the FREE floor, not an error):
-- delete from subscriptions where church_id = '<CHURCH_ID>' and source = 'manual_test';
