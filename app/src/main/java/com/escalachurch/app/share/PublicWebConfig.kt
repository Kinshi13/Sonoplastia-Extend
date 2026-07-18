package com.escalachurch.app.share

import com.escalachurch.app.BuildConfig

/**
 * Fase 11.10 (correção) - the ONE place that reads the public website's base URL. Root cause of
 * the original bug: nothing enforced this - ShareChurchAccessBottomSheet read
 * `BuildConfig.SITE_URL` directly and passed it into the ViewModel's constructor as a plain
 * String, so a future call site could easily pass a different/wrong value (or nothing at all)
 * without anyone noticing. Everything that needs the domain now goes through
 * [PUBLIC_WEB_BASE_URL] - see ChurchShareLinkBuilder, the only consumer.
 */
val PUBLIC_WEB_BASE_URL: String get() = BuildConfig.SITE_URL
