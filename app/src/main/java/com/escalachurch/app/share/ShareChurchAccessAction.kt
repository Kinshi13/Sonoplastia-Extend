package com.escalachurch.app.share

/** Fase 11.10 - which affordance in ShareChurchAccessBottomSheet the admin used; feeds directly
 *  into ChurchAccessAnalytics' `share_type` field. */
enum class ShareChurchAccessAction(val analyticsLabel: String) {
    SHARE_SHEET("share_sheet"),
    COPY_LINK("copy_link"),
    COPY_CODE("copy_code"),
    OPEN_SITE("open_site")
}
