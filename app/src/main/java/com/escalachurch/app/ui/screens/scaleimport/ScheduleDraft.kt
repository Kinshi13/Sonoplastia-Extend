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

    fun firstPersonOrBlank(): String = peopleText.split("|").map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
}

/** Maps a draft's canonical role names back onto the five legacy `scales` columns Android still
 *  writes (see ScaleItem) - only the first name per role, since the legacy schema has one slot per
 *  function. Extra names beyond the first are dropped with a warning surfaced by the caller. */
fun ScheduleDraft.firstPersonFor(roleName: String): String =
    roles.firstOrNull { it.roleName == roleName }?.firstPersonOrBlank().orEmpty()

val ScheduleDraft.receptionPerson get() = firstPersonFor(RoleAliasResolver.RECEPCAO)
val ScheduleDraft.soundPerson get() = firstPersonFor(RoleAliasResolver.SONOPLASTIA)
val ScheduleDraft.preachingPerson get() = firstPersonFor(RoleAliasResolver.PREGACAO)
val ScheduleDraft.conductingPerson get() = firstPersonFor(RoleAliasResolver.REGENCIA)
val ScheduleDraft.musicalMessagePerson get() = firstPersonFor(RoleAliasResolver.MENSAGEM_MUSICAL)
