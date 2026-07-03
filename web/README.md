# Escala Church - Website

Public site (escala, doxologia, anúncios - no login needed) + admin dashboard, both backed by
the same Supabase project as the Android app.

## Setup

```bash
npm install
cp .env.local.example .env.local   # fill in the Supabase URL/anon key (Project Settings -> API)
npm run dev
```

Open http://localhost:3000.

## Structure

- `app/` - public pages (`/`, `/doxologia`, `/anuncios`) and `app/admin/*` (protected dashboard:
  `/admin/escalas`, `/admin/doxologia`, `/admin/anuncios`, `/admin/sonoplastia`).
- `app/login` - admin sign-in. No self-serve sign-up - admin accounts are created in the
  Supabase Dashboard (Authentication -> Add user) and promoted via the SQL at the bottom of
  `../supabase/schema.sql`, same as the Android app.
- `lib/supabase/` - browser/server/middleware Supabase clients (`@supabase/ssr` pattern) and
  `getAdminStatus()`, which checks the signed-in user's `profiles.is_admin` flag.
- `lib/types/database.ts` - TypeScript types mirroring `supabase/schema.sql`.

Admin writes go through Server Actions in `app/admin/actions.ts`; Postgres row-level security
(see `supabase/schema.sql`) is the real enforcement, the `is_admin` check in the actions is just
there to surface a friendly error instead of a raw Postgres one.

## Deploying

This is a stock Next.js App Router project - deploy on [Vercel](https://vercel.com/new) (free
tier is enough for this), setting `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`
as environment variables in the Vercel project settings.
