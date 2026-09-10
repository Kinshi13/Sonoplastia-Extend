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
}

class ScaleImportViewModel(private val scaleRepository: ScaleRepository) : ViewModel() {

    private val _saveState = MutableStateFlow<ScaleImportSaveState>(ScaleImportSaveState.Idle)
    val saveState: StateFlow<ScaleImportSaveState> = _saveState

    /** Bloco B3's "Criar escalas" - only place in the whole import flow that actually writes
     *  anything, and only after the admin has reviewed every draft in the preview. Skips a draft
     *  whose date/time can't be parsed rather than crashing or guessing - matches the parser's own
     *  "never decide silently" rule. */
    fun createSchedules(drafts: List<ScheduleDraft>, onDone: () -> Unit) {
        viewModelScope.launch {
            _saveState.value = ScaleImportSaveState.Saving
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
                runCatching { scaleRepository.save(scale) }.onFailure {
                    _saveState.value = ScaleImportSaveState.Error("Não foi possível criar uma das escalas. Tente novamente.")
                    return@launch
                }
            }
            _saveState.value = ScaleImportSaveState.Idle
            onDone()
        }
    }
}
