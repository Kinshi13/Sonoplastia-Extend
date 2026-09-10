package com.escalachurch.app.ui.screens.scaleimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.importer.EscalaChurchImportParser
import com.escalachurch.app.domain.importer.ImportResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Bloco B2/B3 - "Importar escala por texto". Parsing is always local (EscalaChurchImportParser,
 *  no LLM, no network) - nothing here is saved until the admin reviews the preview and taps
 *  "Criar escalas". */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScaleImportScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> ScaleImportViewModel(container.scaleRepository) }
    val saveState by viewModel.saveState.collectAsState()

    var rawText by remember { mutableStateOf("") }
    var result: ImportResult? by remember { mutableStateOf(null) }
    val drafts = remember { mutableStateListOf<ScheduleDraft>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar escala por texto") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        if (result == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Cole abaixo uma escala no formato ESCALA_CHURCH_IMPORT_V1. Nada é salvo automaticamente - " +
                        "você revisa tudo na próxima tela antes de confirmar.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Cole aqui a escala preparada") },
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
                Button(onClick = {
                    val parsed = EscalaChurchImportParser.parse(rawText)
                    result = parsed
                    drafts.clear()
                    drafts.addAll(parsed.schedules.map { it.toDraft() })
                }) { Text("Analisar escala") }
            }
        } else {
            val current = result!!
            if (current.errors.isNotEmpty() && current.schedules.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.errors.forEach { Text(it.message, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(onClick = { result = null }) { Text("Tentar novamente") }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Text(
                        "Prévia da importação",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    if (current.errors.isNotEmpty() || current.warnings.isNotEmpty()) {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            current.errors.forEach { Text("⚠ ${it.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            current.warnings.forEach { Text("• ${it.message}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(drafts, key = { it.key }) { draft ->
                            ScheduleDraftCard(draft = draft, onRemove = { drafts.remove(draft) })
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { result = null }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            onClick = { viewModel.createSchedules(drafts) { onBack() } },
                            enabled = drafts.isNotEmpty() && saveState !is ScaleImportSaveState.Saving,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (saveState is ScaleImportSaveState.Saving) CircularProgressIndicator(Modifier.height(20.dp))
                            else Text("Criar escalas (${drafts.size})")
                        }
                    }
                    (saveState as? ScaleImportSaveState.Error)?.let {
                        Text(it.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDraftCard(draft: ScheduleDraft, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(draft.summaryTitle(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Excluir escala") }
            }
            if (draft.dateAssumedYear) {
                Text("Ano assumido automaticamente - confira a data.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = draft.date, onValueChange = { draft.date = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = draft.startTime, onValueChange = { draft.startTime = it }, label = { Text("Início") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = draft.type, onValueChange = { draft.type = it }, label = { Text("Tipo") }, modifier = Modifier.fillMaxWidth())
            draft.roles.forEach { role ->
                OutlinedTextField(
                    value = role.peopleText,
                    onValueChange = { role.peopleText = it },
                    label = { Text("${role.roleName} (separe vários nomes com |)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun com.escalachurch.app.domain.importer.ImportedSchedule.toDraft() = ScheduleDraft(
    date = date?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: rawDate,
    dateAssumedYear = dateAssumedYear,
    startTime = startTime ?: "",
    type = type,
    roles = roles.map { RoleDraft(roleName = it.roleName, peopleText = it.people.joinToString("|") { p -> p.rawName }) }
)

private fun ScheduleDraft.summaryTitle(): String {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val weekday = parsedDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))?.replaceFirstChar { it.uppercase() }
    return if (parsedDate != null) "$weekday — ${parsedDate.dayOfMonth} de ${parsedDate.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))}"
    else date.ifBlank { "Data não informada" }
}
