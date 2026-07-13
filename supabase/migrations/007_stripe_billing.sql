-- Fase 10 - real recurring billing for the 3 paid recurring plans (Essencial/Pro/Organização).
-- FREE stays unpaid and FOUNDER stays the existing one-time Checkout flow (cadastrar-igreja) -
-- neither needs a Stripe price here. Safe to run once on the existing database.

-- Each plan carries its own Stripe Price IDs so the checkout/webhook code never hardcodes one -
-- set these from the Stripe Dashboard (Products -> a plan's monthly/yearly price -> "API ID").
-- Null on FREE/FOUNDER on purpose (FOUNDER already has its own price via STRIPE_PRICE_ID env var,
-- unrelated to this table).
alter table plans add column if not exists stripe_price_id_monthly text;
alter table plans add column if not exists stripe_price_id_yearly text;

-- Needed to resolve subscription.updated/deleted webhook events back to a church_id (those events
-- only carry Stripe's own IDs, not ours) and to open the Stripe Billing Portal for a church.
alter table subscriptions add column if not exists stripe_customer_id text;
alter table subscriptions add column if not exists stripe_subscription_id text;

create unique index if not exists subscriptions_stripe_subscription_id_key
  on subscriptions (stripe_subscription_id)
  where stripe_subscription_id is not null;

-- Stripe IDs are never read by an anon/authenticated client directly (checkout/portal are server
-- actions using the service-role client for writes), but the row itself is still public-select
-- per the existing "subscriptions: public read" policy from 006 - a member seeing their own
-- church's stripe_customer_id isn't a secret (it identifies a Customer object, not a payment
-- method), same trust level as knowing which plan the church is on.

-- Fill in your real Price IDs after creating the Products/Prices in the Stripe Dashboard, e.g.:
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ESSENTIAL';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'PRO';
--   update plans set stripe_price_id_monthly = 'price_...', stripe_price_id_yearly = 'price_...' where code = 'ORGANIZATION';
