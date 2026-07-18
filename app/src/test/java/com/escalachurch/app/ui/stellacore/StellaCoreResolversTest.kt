package com.escalachurch.app.ui.stellacore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.escalachurch.app.domain.model.AccessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Fase 11.9B Bloco 22 - StellaCorePermissionResolver was untested at the unit level despite being
 *  the exact rule "Admin" vs "Público" actions rely on (Bloco 22: "testar Admin / Público").
 *  StellaCoreFeatureResolver (the PRO/FREE half) needs a real EntitlementService to exercise -
 *  building a fake for that would mean touching the entitlements architecture, which this bloco
 *  explicitly must not reopen; its PRO/FREE/lockedPreview branches were instead re-read and
 *  confirmed correct by inspection (see the Bloco 22 delivery notes). */
class StellaCoreResolversTest {

    private fun action(id: String, requiresRole: AccessLevel? = null) =
        StellaCoreAction(id = id, label = id, icon = Icons.Filled.Add, requiresRole = requiresRole) {}

    @Test
    fun publicAction_visibleToEveryRole() {
        val actions = listOf(action("public_action"))
        assertEquals(actions, StellaCorePermissionResolver.resolve(actions, AccessLevel.MEMBER))
        assertEquals(actions, StellaCorePermissionResolver.resolve(actions, AccessLevel.ADMIN))
    }

    @Test
    fun adminOnlyAction_hiddenFromMember_visibleToAdmin() {
        val actions = listOf(action("admin_action", requiresRole = AccessLevel.ADMIN))
        assertEquals(emptyList<StellaCoreAction>(), StellaCorePermissionResolver.resolve(actions, AccessLevel.MEMBER))
        assertEquals(actions, StellaCorePermissionResolver.resolve(actions, AccessLevel.ADMIN))
    }

    @Test
    fun mixedList_memberSeesOnlyPublicActions() {
        val public = action("public_action")
        val admin = action("admin_action", requiresRole = AccessLevel.ADMIN)
        val resolved = StellaCorePermissionResolver.resolve(listOf(public, admin), AccessLevel.MEMBER)
        assertEquals(listOf(public), resolved)
    }

    @Test
    fun mixedList_adminSeesEverything() {
        val public = action("public_action")
        val admin = action("admin_action", requiresRole = AccessLevel.ADMIN)
        val resolved = StellaCorePermissionResolver.resolve(listOf(public, admin), AccessLevel.ADMIN)
        assertEquals(listOf(public, admin), resolved)
    }

    @Test
    fun emptyList_resolvesToEmpty() {
        assertEquals(emptyList<StellaCoreAction>(), StellaCorePermissionResolver.resolve(emptyList(), AccessLevel.MEMBER))
    }
}
