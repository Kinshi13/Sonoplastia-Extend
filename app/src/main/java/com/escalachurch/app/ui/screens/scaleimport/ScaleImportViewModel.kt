package com.escalachurch.app.ui.screens.scaleimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

sealed class ScaleImportSaveState {
    data object Idle : ScaleImportSaveState()
    data object Saving : ScaleImportSaveState()
    data class Error(val message: String) : ScaleImportSaveState()
    /** Fase 11.14 - [skipped] used to be computed and then thrown away; a partial result (some
     *  drafts created, some skipped for an unparseable date/time) looked identical to full
     *  success. Surfaced now so "sucesso" is never claimed when part of it silently wasn't. */
    data class Done(val created: Int, val skipped: Int) : ScaleImportSaveState()
}

class ScaleImportViewModel(private val scaleRepository: ScaleRepository) : ViewModel() {

    private val _saveState = MutableStateFlow<ScaleImportSaveState>(ScaleImportSaveState.Idle)
    val saveState: StateFlow<ScaleImportSaveState> = _saveState

    /** Bloco B3's "Criar escalas" - only place in the whole import flow that actually writes
     *  anything, and only after the admin has reviewed every draft in the preview. Skips a draft
     *  whose date/time can't be parsed rather than crashing or guessing - matches the parser's own
     *  "never decide silently" rule.
     *
     *  Fase 11.14 - [onDraftSaved] fires once per draft that actually got written, as it happens
     *  (not batched at the end), so the caller can remove exactly those from the list it's
     *  showing. That's what makes a retry after a partial failure/skip safe: the drafts still on
     *  screen afterward are only the ones never saved, so tapping "Criar escalas" again can never
     *  create a duplicate of one that already succeeded.
     */
    fun createSchedules(drafts: List<ScheduleDraft>, onDraftSaved: (ScheduleDraft) -> Unit) {
        viewModelScope.launch {
            _saveState.value = ScaleImportSaveState.Saving
            var created = 0
            var skipped = 0
            for (draft in drafts) {
                val date = runCatching { LocalDate.parse(draft.date) }.getOrNull()
                val time = runCatching { LocalTime.parse(draft.startTime.ifBlank { "00:00" }) }.getOrNull()
                if (date == null || time == null) {
                    skipped++
                    continue
                }
                val scale = ScaleItem(
                    date = date,
                    startTime = time,
                    type = ProgramType.COMMON_SCALE,
                    title = draft.type.ifBlank { "Escala importada" },
                    receptionPerson = draft.receptionPerson,
                    soundPerson = draft.soundPerson,
                    preachingPerson = draft.preachingPerson,
                    conductingPerson = draft.conductingPerson,
                    musicalMessagePerson = draft.musicalMessagePerson,
                    sourceType = SourceType.OFFICIAL
                )
                runCatching { scaleRepository.save(scale) }
                    .onSuccess {
                        created++
                        onDraftSaved(draft)
                    }
                    .onFailure {
                        // Bloco I - stop here: drafts already saved were already reported via
                        // onDraftSaved above (caller already removed them from the list), every
                        // draft from this one onward is still on screen, untouched, safe to fix
                        // and retry without creating anything twice.
                        _saveState.value = ScaleImportSaveState.Error(
                            if (created > 0) "$created escala(s) criada(s). Uma escala falhou ao salvar - as demais continuam na lista para tentar de novo."
                            else "Não foi possível criar a escala. Tente novamente."
                        )
                        return@launch
                    }
            }
            _saveState.value = ScaleImportSaveState.Done(created = created, skipped = skipped)
        }
    }
}
