package com.escalachurch.app.share

/**
 * Fase 11.10 - builds the public church link shared by "Compartilhar acesso da igreja". Uses the
 * website's actual public route (`web/app/c/[slug]/page.tsx`, i.e. `/c/SLUG`) - NOT the `/igreja/`
 * pattern from the original spec, per its own instruction to prefer whatever route already exists.
 *
 * Never takes a church id - only the public [slug] (Bloco: "nunca utilizar church_id, sempre o
 * slug público"). Pure - see ChurchShareLinkBuilderTest.
 */
fun buildChurchLink(baseUrl: String, slug: String): String {
    val trimmedBase = baseUrl.trimEnd('/')
    return "$trimmedBase/c/$slug"
}
