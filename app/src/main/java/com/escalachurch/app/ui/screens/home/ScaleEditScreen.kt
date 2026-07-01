package com.escalachurch.app.ui.screens.home

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleEditScreen(
    existing: ScaleItem?,
    onSave: (ScaleItem) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(existing?.date) }
    var startTime by remember { mutableStateOf(existing?.startTime) }
    var endTime by remember { mutableStateOf(existing?.endTime) }
    var reception by remember { mutableStateOf(existing?.receptionPerson ?: "") }
    var sound by remember { mutableStateOf(existing?.soundPerson ?: "") }
    var preaching by remember { mutableStateOf(existing?.preachingPerson ?: "") }
    var conducting by remember { mutableStateOf(existing?.conductingPerson ?: "") }
    var musicalMessage by remember { mutableStateOf(existing?.musicalMessagePerson ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var isSpecial by remember { mutableStateOf(existing?.isSpecialEvent ?: false) }

    var showTitleError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova escala" else "Editar escala") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
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
                label = "Nome da escala",
                isError = showTitleError,
                supportingText = if (showTitleError) "Informe um nome para a escala" else null
            )

            DatePickerField(
                label = "Data",
                date = date,
                onDateSelected = { date = it; showDateError = false }
            )
            if (showDateError) {
                Text("Selecione a data", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePickerField(
                    label = "Horário inicial",
                    time = startTime,
                    onTimeSelected = { startTime = it; showTimeError = false },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = "Horário final (opcional)",
                    time = endTime,
                    onTimeSelected = { endTime = it },
                    modifier = Modifier.weight(1f)
                )
            }
            if (showTimeError) {
                Text("Selecione o horário inicial", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Text("Funções", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            AppTextField(value = reception, onValueChange = { reception = it }, label = "Recepção")
            AppTextField(value = sound, onValueChange = { sound = it }, label = "Sonoplastia")
            AppTextField(value = preaching, onValueChange = { preaching = it }, label = "Pregação")
            AppTextField(value = conducting, onValueChange = { conducting = it }, label = "Regência")
            AppTextField(value = musicalMessage, onValueChange = { musicalMessage = it }, label = "Mensagem musical")

            AppTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Observações (opcional)",
                singleLine = false,
                minLines = 3
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSpecial, onCheckedChange = { isSpecial = it })
                Text("É uma programação especial", style = MaterialTheme.typography.bodyMedium)
            }

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
                        ScaleItem(
                            id = existing?.id ?: 0L,
                            date = d!!,
                            startTime = st!!,
                            endTime = endTime,
                            title = title,
                            receptionPerson = reception,
                            soundPerson = sound,
                            preachingPerson = preaching,
                            conductingPerson = conducting,
                            musicalMessagePerson = musicalMessage,
                            notes = notes,
                            isSpecialEvent = isSpecial,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            )

            if (onDelete != null) {
                SecondaryButton(
                    text = "Excluir escala",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDialog(
            title = "Excluir escala",
            message = "Tem certeza que deseja excluir esta escala? Esta ação não pode ser desfeita.",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
