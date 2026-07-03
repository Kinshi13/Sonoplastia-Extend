package com.escalachurch.app.ui.screens.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.ui.components.CardCarousel
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.ProgramCard
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.rememberEntranceVisible

/**
 * "Programar": the member's own programações - other than the official Escala Geral. Uses the
 * same flash-card + swipe pattern as Início/Doxologia (always opens on the next upcoming item,
 * drag right/left for future/past ones) so the whole app feels like one consistent system.
 */
@Composable
fun ProgramScreen() {
    val viewModel = appViewModel { container -> ProgramViewModel(container.customEventRepository) }
    val state by viewModel.uiState.collectAsState()
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val cardsVisible = rememberEntranceVisible(appSettings.animationsEnabled)

    var editingTarget by remember { mutableStateOf<ProgramEditTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }

    if (isEditing) {
        ProgramEditScreen(
            existing = editingTarget?.item,
            onSave = { event, extraDates, weeklyOccurrences ->
                viewModel.save(event, extraDates, weeklyOccurrences)
                isEditing = false
            },
            onDelete = editingTarget?.item?.let { item -> { viewModel.delete(item); isEditing = false } },
            onBack = { isEditing = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Programar", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Suas programações: eventos especiais, Semana de Oração, ensaios e outros avulsos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        if (state.events.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.EventNote,
                title = "Nenhuma programação cadastrada",
                message = "Adicione a primeira programação pessoal ou evento especial.",
                modifier = Modifier.weight(1f)
            ) {
                PrimaryButton(text = "Adicionar programação", onClick = { editingTarget = ProgramEditTarget(null); isEditing = true })
            }
        } else {
            PulledUpEntrance(visible = cardsVisible, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    CardCarousel(
                        items = state.events,
                        initialPage = state.startIndex ?: 0,
                        onPageChanged = { currentPage = it }
                    ) { event ->
                        ProgramCard(event)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val current: CustomEvent? = state.events.getOrNull(currentPage)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Editar",
                    modifier = Modifier.weight(1f),
                    enabled = current != null,
                    onClick = { current?.let { editingTarget = ProgramEditTarget(it); isEditing = true } }
                )
                PrimaryButton(
                    text = "Adicionar programação",
                    modifier = Modifier.weight(1f),
                    onClick = { editingTarget = ProgramEditTarget(null); isEditing = true }
                )
            }
        }
    }
}

private data class ProgramEditTarget(val item: CustomEvent?)
