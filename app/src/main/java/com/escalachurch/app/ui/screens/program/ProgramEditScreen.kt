package com.escalachurch.app.ui.screens.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.RepeatRule
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.TimePickerField
import com.escalachurch.app.ui.components.dayOfWeekLabel
import com.escalachurch.app.ui.components.toDisplayString

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgramEditScreen(
    existing: CustomEvent?,
    onSave: (CustomEvent, additionalDates: List<java.time.LocalDate>, weeklyOccurrences: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(existing?.date) }
    var startTime by remember { mutableStateOf(existing?.startTime) }
    var endTime by remember { mutableStateOf(existing?.endTime) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var type by remember { mutableStateOf(existing?.eventType ?: ProgramType.SPECIAL_EVENT) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var isWeeklyRecurring by remember { mutableStateOf(existing?.repeatRule == RepeatRule.WEEKLY) }
    var weeklyOccurrences by remember { mutableStateOf("4") }
    val additionalDates = remember { mutableStateListOf<java.time.LocalDate>() }
    var pendingExtraDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    var showTitleError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova programação" else "Editar programação") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
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
                label = "Nome da programação",
                isError = showTitleError
            )

            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                OutlinedTextField(
                    value = type.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo da programação") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    ProgramType.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { type = option; typeMenuExpanded = false })
                    }
                }
            }

            DatePickerField(label = "Data principal", date = date, onDateSelected = { date = it; showDateError = false })
            if (date != null) {
                Text("Dia da semana: ${date!!.dayOfWeekLabel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showDateError) Text("Selecione a data", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

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
            if (showTimeError) Text("Selecione o horário inicial", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            AppTextField(value = description, onValueChange = { description = it }, label = "Breve descrição", singleLine = false, minLines = 3)

            Text(
                "Dias adicionais (Semana de Oração, dias avulsos, etc.)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            DatePickerField(
                label = "Adicionar outro dia",
                date = pendingExtraDate,
                onDateSelected = {
                    if (it !in additionalDates) additionalDates.add(it)
                    pendingExtraDate = null
                }
            )
            if (additionalDates.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    additionalDates.forEach { extra ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(extra.toDisplayString()) },
                            trailingIcon = {
                                IconButton(onClick = { additionalDates.remove(extra) }, modifier = Modifier.height(18.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remover dia")
                                }
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = isWeeklyRecurring, onCheckedChange = { isWeeklyRecurring = it })
                Text("Repetir semanalmente", style = MaterialTheme.typography.bodyMedium)
            }
            if (isWeeklyRecurring) {
                AppTextField(
                    value = weeklyOccurrences,
                    onValueChange = { weeklyOccurrences = it.filter { c -> c.isDigit() } },
                    label = "Quantas semanas seguidas (além da data principal)?",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
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

                    val base = CustomEvent(
                        id = existing?.id ?: 0L,
                        title = title,
                        date = d!!,
                        startTime = st!!,
                        endTime = endTime,
                        description = description,
                        eventType = type,
                        repeatRule = if (isWeeklyRecurring) RepeatRule.WEEKLY else RepeatRule.NONE,
                        groupId = existing?.groupId,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(base, additionalDates.toList(), weeklyOccurrences.toIntOrNull()?.takeIf { isWeeklyRecurring } ?: 0)
                }
            )

            if (onDelete != null) {
                SecondaryButton(text = "Excluir programação", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteConfirm = true })
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDialog(
            title = "Excluir programação",
            message = "Tem certeza que deseja excluir esta programação?",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
