package com.escalachurch.app.ui.screens.scaleimport

import androidx.compose.runtime.mutableStateOf
import com.escalachurch.app.domain.importer.RoleAliasResolver
import java.util.UUID

/** One editable card in the import preview (Bloco B3) - a mutable draft of an [com.escalachurch.app.domain.importer.ImportedSchedule],
 *  never itself persisted; [ScaleImportViewModel.createSchedules] converts a confirmed list of
 *  these into real `ScaleItem`s only when the admin taps "Criar escalas". */
class ScheduleDraft(
    date: String,
    val dateAssumedYear: Boolean,
    startTime: String,
    type: String,
    val roles: List<RoleDraft>
) {
    val key: String = UUID.randomUUID().toString()
    private val dateState = mutableStateOf(date)
    var date: String get() = dateState.value; set(value) { dateState.value = value }
    private val startTimeState = mutableStateOf(startTime)
    var startTime: String get() = startTimeState.value; set(value) { startTimeState.value = value }
    private val typeState = mutableStateOf(type)
    var type: String get() = typeState.value; set(value) { typeState.value = value }
}

class RoleDraft(val roleName: String, peopleText: String) {
    private val peopleTextState = mutableStateOf(peopleText)
    var peopleText: String get() = peopleTextState.value; set(value) { peopleTextState.value = value }

    /** All names typed for this role, trimmed, blanks dropped - in the order the admin typed them. */
    fun people(): List<String> = peopleText.split("|").map { it.trim() }.filter { it.isNotBlank() }

    /** Fase 11.14 - every name, not just the first (see [ScheduleDraft.firstPersonFor]'s removal
     *  below): joined the same way `web/app/admin/actions.ts` already aggregates multiple
     *  `scale_assignments` rows back into a legacy column ("Ana e Pedro"), so a schedule saved
     *  from Android reads the same way a schedule saved from the site already does. */
    fun peopleOrBlank(): String = people().joinToString(" e ")
}

/** Maps a draft's canonical role names back onto the five legacy `scales` columns Android still
 *  writes (see ScaleItem). Fase 11.14 - used to keep only the first name per role, silently
 *  dropping everyone else the admin had already seen and confirmed in the preview; now every name
 *  is preserved (joined with "e", same convention the web admin already uses for the same
 *  columns) - see [RoleDraft.peopleOrBlank]. */
fun ScheduleDraft.allPeopleFor(roleName: String): String =
    roles.firstOrNull { it.roleName == roleName }?.peopleOrBlank().orEmpty()

/** True when any role on this draft has more than one name - the preview surfaces this so the
 *  admin knows those names will be saved together in one legacy field, not as separate people
 *  (Fase 11.14: no longer silent either way). */
fun ScheduleDraft.hasMultiPersonRole(): Boolean = roles.any { it.people().size > 1 }

val ScheduleDraft.receptionPerson get() = allPeopleFor(RoleAliasResolver.RECEPCAO)
val ScheduleDraft.soundPerson get() = allPeopleFor(RoleAliasResolver.SONOPLASTIA)
val ScheduleDraft.preachingPerson get() = allPeopleFor(RoleAliasResolver.PREGACAO)
val ScheduleDraft.conductingPerson get() = allPeopleFor(RoleAliasResolver.REGENCIA)
val ScheduleDraft.musicalMessagePerson get() = allPeopleFor(RoleAliasResolver.MENSAGEM_MUSICAL)
