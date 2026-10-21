package com.escalachurch.app.data.remote

object SupabaseTables {
    const val CHURCHES = "churches"
    const val PROFILES = "profiles"
    const val SCALES = "scales"
    const val DOXOLOGIES = "doxologies"
    const val ANNOUNCEMENTS = "announcements"
    const val SHARED_FILES = "shared_files"
    const val BULLETINS = "bulletins"
    const val PLANS = "plans"
    const val SUBSCRIPTIONS = "subscriptions"
    const val WORSHIP_SONGS = "worship_songs"
    // Fase 11.15 - read-only so far (see OrganizationRoleRepository/OrganizationPersonRepository):
    // these tables already exist in production (migration 010, confirmed applied - see
    // supabase/MIGRATION_INVENTORY.md) and the web admin already writes to them. Android only
    // reads from them for now, to reconcile import participants/roles - no write path yet.
    const val ORGANIZATION_ROLES = "organization_roles"
    const val ORGANIZATION_PEOPLE = "organization_people"
}

/** Public Storage bucket used for both Anúncios media and files shared from Sonoplastia. */
const val CHURCH_FILES_BUCKET = "church-files"
