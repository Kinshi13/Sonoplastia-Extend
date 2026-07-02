package com.escalachurch.app.ui.screens.doxology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.DoxologyCard
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.TimePickerField

/**
 * Create/edit form for a [DoxologyItem]. All doxologies are official (admin-managed) today, so
 * [isAdmin] is a hard gate: a non-admin caller only ever gets a read-only preview, never the
 * form - this mirrors ScaleEditScreen's guarantee that members can't edit official data, even
 * if a future navigation path reaches this screen directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoxologyEditScreen(
    existing: DoxologyItem?,
    isAdmin: Boolean,
    onSave: (DoxologyItem) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    if (!isAdmin) {
        ReadOnlyOfficialDoxology(existing, onBack)
        return
    }

    var title by remember { mutableStateOf(existing?.title ?: "Ordem do Culto") }
    var date by remember { mutableStateOf(existing?.date) }
    var startTime by remember { mutableStateOf(existing?.startTime) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val steps = remember {
        mutableStateListOf<ProgramStep>().apply {
            addAll(existing?.programOrder?.sortedBy { it.order } ?: defaultOrderOfService())
        }
    }
    var newStepTitle by remember { mutableStateOf("") }

    var showTitleError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova doxologia" else "Editar doxologia") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppTextField(
                value = title,
                onValueChange = { title = it; showTitleError = false },
                label = "Título da programação",
                isError = showTitleError
            )
            DatePickerField(label = "Data", date = date, onDateSelected = { date = it; showDateError = false })
            if (showDateError) Text("Selecione a data", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            TimePickerField(label = "Horário", time = startTime, onTimeSelected = { startTime = it; showTimeError = false })
            if (showTimeError) Text("Selecione o horário", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            Text("Ordem da programação", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            steps.forEachIndexed { index, step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${step.title}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (index > 0) steps.swap(index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Mover para cima")
                        }
                        IconButton(onClick = { if (index < steps.lastIndex) steps.swap(index, index + 1) }, enabled = index < steps.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Mover para baixo")
                        }
                        IconButton(onClick = { steps.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover etapa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = newStepTitle,
                    onValueChange = { newStepTitle = it },
                    label = "Nova etapa",
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Adicionar",
                    onClick = {
                        if (newStepTitle.isNotBlank()) {
                            steps.add(ProgramStep(order = steps.size + 1, title = newStepTitle.trim()))
                            newStepTitle = ""
                        }
                    }
                )
            }

            AppTextField(value = notes, onValueChange = { notes = it }, label = "Observações (opcional)", singleLine = false, minLines = 3)

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Salvar",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val d = date
                    val st = startTime
                    var hasError = false
                    if (title.isBlank()) { showTitleError = true; hasError = true }
                    if (d == null) { showDateError = true; hasError = true }
                    if (st == null) { showTimeError = true; hasError = true }
                    if (hasError) return@PrimaryButton

                    onSave(
                        DoxologyItem(
                            id = existing?.id ?: 0L,
                            date = d!!,
                            startTime = st!!,
                            title = title,
                            notes = notes,
                            programOrder = steps.mapIndexed { i, s -> s.copy(order = i + 1) },
                            sourceType = com.escalachurch.app.domain.model.SourceType.OFFICIAL,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            )

            if (onDelete != null) {
                SecondaryButton(text = "Excluir doxologia", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteConfirm = true })
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDialog(
            title = "Excluir doxologia",
            message = "Tem certeza que deseja excluir esta programação?",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadOnlyOfficialDoxology(doxology: DoxologyItem?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doxologia oficial") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "Apenas administradores podem criar ou editar a doxologia oficial. Entre em \"Modo administrador\" nas Configurações para alterar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (doxology != null) {
                Spacer(Modifier.height(16.dp))
                DoxologyCard(doxology)
            }
        }
    }
}

private fun <T> androidx.compose.runtime.snapshots.SnapshotStateList<T>.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}

private fun defaultOrderOfService(): List<ProgramStep> = listOf(
    "Prelúdio", "Boas-vindas", "Hino inicial", "Oração", "Dízimos e ofertas",
    "Mensagem musical", "Sermão", "Hino final", "Oração final", "Poslúdio"
).mapIndexed { index, title -> ProgramStep(order = index + 1, title = title) }
