package com.escalachurch.app.ui.screens.generalscale

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.TimePickerField
import com.escalachurch.app.ui.components.toDisplayString
import java.time.LocalDate
import java.time.LocalTime

/**
 * Creates several official scale days at once from one shared template - e.g. Semana de Oração
 * (multiple consecutive days) or any other set of manually-picked extra dates.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtraDaysScreen(
    onCreate: (template: ScaleItem, dates: List<LocalDate>) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("Semana de Oração") }
    var startTime by remember { mutableStateOf<LocalTime?>(LocalTime.of(19, 30)) }
    var preaching by remember { mutableStateOf("") }
    var conducting by remember { mutableStateOf("") }
    var sound by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val dates = remember { mutableStateListOf<LocalDate>() }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar dias extras") },
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
            Text(
                "Use para Semana de Oração ou qualquer programação especial em vários dias. O mesmo horário e funções serão aplicados a todas as datas escolhidas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AppTextField(value = title, onValueChange = { title = it }, label = "Nome da programação")
            TimePickerField(label = "Horário", time = startTime, onTimeSelected = { startTime = it })

            Text("Funções (opcional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            AppTextField(value = preaching, onValueChange = { preaching = it }, label = "Pregação")
            AppTextField(value = conducting, onValueChange = { conducting = it }, label = "Regência")
            AppTextField(value = sound, onValueChange = { sound = it }, label = "Sonoplastia")
            AppTextField(value = notes, onValueChange = { notes = it }, label = "Observações (opcional)", singleLine = false, minLines = 2)

            Text("Datas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            DatePickerField(
                label = "Adicionar dia",
                date = pendingDate,
                onDateSelected = { if (it !in dates) dates.add(it); pendingDate = null; showError = false }
            )
            if (dates.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.sorted().forEach { date ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(date.toDisplayString()) },
                            trailingIcon = {
                                IconButton(onClick = { dates.remove(date) }, modifier = Modifier.height(18.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remover dia")
                                }
                            }
                        )
                    }
                }
            }
            if (showError) {
                Text("Adicione ao menos um dia", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Criar ${dates.size} dia(s)",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val st = startTime
                    if (dates.isEmpty() || st == null) { showError = true; return@PrimaryButton }
                    val template = ScaleItem(
                        date = dates.first(),
                        startTime = st,
                        title = title.ifBlank { "Semana de Oração" },
                        preachingPerson = preaching,
                        conductingPerson = conducting,
                        soundPerson = sound,
                        notes = notes,
                        sourceType = SourceType.OFFICIAL
                    )
                    onCreate(template, dates.toList())
                }
            )
        }
    }
}
