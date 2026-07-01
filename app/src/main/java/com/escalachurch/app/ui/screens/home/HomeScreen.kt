package com.escalachurch.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
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
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.CardCarousel
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.ScaleCard
import com.escalachurch.app.ui.components.SecondaryButton

@Composable
fun HomeScreen() {
    val viewModel = appViewModel { container -> HomeViewModel(container.scaleRepository) }
    val state by viewModel.uiState.collectAsState()

    var editingTarget by remember { mutableStateOf<EditTarget?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }

    editingTarget?.let { target ->
        ScaleEditScreen(
            existing = target.item,
            onSave = { item ->
                viewModel.save(item)
                editingTarget = null
            },
            onDelete = target.item?.let { item -> { viewModel.delete(item); editingTarget = null } },
            onBack = { editingTarget = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Próxima Escala", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))

        if (state.scales.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.EventBusy,
                title = "Nenhuma escala futura cadastrada",
                message = "Adicione a primeira escala para começar a organizar a programação da igreja.",
                modifier = Modifier.weight(1f)
            ) {
                PrimaryButton(text = "Adicionar escala", onClick = { editingTarget = EditTarget(null) })
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                CardCarousel(
                    items = state.scales,
                    initialPage = state.startIndex ?: 0,
                    onPageChanged = { currentPage = it }
                ) { scale ->
                    ScaleCard(scale)
                }
            }

            Spacer(Modifier.height(16.dp))

            val current: ScaleItem? = state.scales.getOrNull(currentPage)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Editar",
                    modifier = Modifier.weight(1f),
                    enabled = current != null,
                    onClick = { current?.let { editingTarget = EditTarget(it) } }
                )
                PrimaryButton(
                    text = "Adicionar escala",
                    modifier = Modifier.weight(1f),
                    onClick = { editingTarget = EditTarget(null) }
                )
            }
        }
    }
}

private data class EditTarget(val item: ScaleItem?)
