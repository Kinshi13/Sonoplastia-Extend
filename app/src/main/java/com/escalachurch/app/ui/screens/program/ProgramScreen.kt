package com.escalachurch.app.ui.screens.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.ui.components.CustomEventCard
import com.escalachurch.app.ui.components.EmptyState

@Composable
fun ProgramScreen() {
    val viewModel = appViewModel { container -> ProgramViewModel(container.customEventRepository) }
    val events by viewModel.events.collectAsState()

    var editingTarget by remember { mutableStateOf<ProgramEditTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Programar", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(
                "Crie eventos especiais, Semana de Oração, ensaios e outras programações personalizadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            if (events.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    title = "Nenhuma programação personalizada",
                    message = "Toque no botão + para criar seu primeiro evento especial.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(events, key = { it.id }) { event: CustomEvent ->
                        CustomEventCard(
                            event = event,
                            onClick = { editingTarget = ProgramEditTarget(event); isEditing = true }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingTarget = ProgramEditTarget(null); isEditing = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nova programação")
        }
    }
}

private data class ProgramEditTarget(val item: CustomEvent?)
