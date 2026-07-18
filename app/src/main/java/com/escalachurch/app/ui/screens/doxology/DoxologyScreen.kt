package com.escalachurch.app.ui.screens.doxology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.ui.components.CardCarousel
import com.escalachurch.app.ui.components.DoxologyCard
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ErrorBanner
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.rememberEntranceVisible

@Composable
fun DoxologyScreen() {
    val viewModel = appViewModel { container -> DoxologyViewModel(container.doxologyRepository, container.adminSession) }
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val cardsVisible = rememberEntranceVisible(appSettings.animationsEnabled)

    var editingTarget by remember { mutableStateOf<DoxologyEditTarget?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        com.escalachurch.app.ui.stellacore.StellaCoreBus.events().collect { command ->
            if (command == com.escalachurch.app.ui.stellacore.StellaCoreCommand.NewDoxology) {
                editingTarget = DoxologyEditTarget(null)
            }
        }
    }

    editingTarget?.let { target ->
        DoxologyEditScreen(
            existing = target.item,
            isAdmin = state.isAdmin,
            onSave = { item -> viewModel.save(item); editingTarget = null },
            onDelete = target.item?.let { item -> { viewModel.delete(item); editingTarget = null } },
            onBack = { editingTarget = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Doxologia", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))

        errorMessage?.let { message ->
            ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
            Spacer(Modifier.height(12.dp))
        }

        if (state.isLoading) {
            com.escalachurch.app.ui.components.CelestialLoadingState(
                variant = com.escalachurch.app.ui.components.SkeletonVariant.HERO,
                modifier = Modifier.weight(1f)
            )
        } else if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.MusicOff,
                title = "Nenhuma programação futura cadastrada",
                message = if (state.isAdmin) "Adicione a ordem do culto para a próxima programação." else "Fale com um administrador para cadastrar a doxologia.",
                modifier = Modifier.weight(1f)
            ) {
                if (state.isAdmin) {
                    PrimaryButton(text = "Adicionar doxologia", onClick = { editingTarget = DoxologyEditTarget(null) })
                }
            }
        } else {
            PulledUpEntrance(visible = cardsVisible, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    CardCarousel(
                        items = state.items,
                        initialPage = state.startIndex ?: 0,
                        reducedMotion = !appSettings.animationsEnabled,
                        onPageChanged = { currentPage = it }
                    ) { item -> DoxologyCard(item, isLive = item.id == state.liveItemId) }
                }
            }

            Spacer(Modifier.height(16.dp))

            val current: DoxologyItem? = state.items.getOrNull(currentPage)
            if (state.isAdmin) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Editar",
                        modifier = Modifier.weight(1f),
                        enabled = current != null,
                        onClick = { current?.let { editingTarget = DoxologyEditTarget(it) } }
                    )
                    PrimaryButton(
                        text = "Adicionar doxologia",
                        modifier = Modifier.weight(1f),
                        onClick = { editingTarget = DoxologyEditTarget(null) }
                    )
                }
            }
        }
    }
}

private data class DoxologyEditTarget(val item: DoxologyItem?)
