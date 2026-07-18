package com.escalachurch.app.share

/**
 * Fase 11.10 (correção) - builds the public church link shared by "Compartilhar acesso da
 * igreja", from [PUBLIC_WEB_BASE_URL] (the one and only source of the domain) + the website's
 * actual public route (`web/app/c/[slug]/page.tsx`, i.e. `/c/SLUG` - NOT `/igreja/`, per this
 * correction's own instruction to never invent a route the site doesn't already use).
 *
 * Root cause of the original bug: this used to take `baseUrl` as a plain parameter and always
 * returned a String, even when [PUBLIC_WEB_BASE_URL] was blank (no domain configured in this
 * project - see PublicWebConfig) - producing a relative path like "/c/slug" that looked like a
 * broken/missing link everywhere it was shared or copied. Returning `null` when either the domain
 * or the slug is missing forces every caller to handle that explicitly (see
 * ShareChurchAccessViewModel's error states) instead of silently shipping a broken URL.
 *
 * Never takes a church id - only the public [slug] (Bloco: "nunca utilizar church_id, sempre o
 * slug público"). [baseUrl] defaults to [PUBLIC_WEB_BASE_URL] so no real call site ever needs to
 * pass it (and can't accidentally pass a different one) - it's a parameter at all only so this
 * stays a pure, directly-testable function instead of reading BuildConfig internally. Pure - see
 * ChurchShareLinkBuilderTest.
 */
fun buildPublicChurchUrl(slug: String, baseUrl: String = PUBLIC_WEB_BASE_URL): String? {
    val base = baseUrl.trim().trimEnd('/')
    val cleanSlug = slug.trim().trim('/')
    if (base.isBlank() || cleanSlug.isBlank()) return null
    return "$base/c/$cleanSlug"
}
