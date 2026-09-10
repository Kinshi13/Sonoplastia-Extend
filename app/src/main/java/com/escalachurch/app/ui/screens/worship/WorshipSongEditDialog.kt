package com.escalachurch.app.ui.screens.worship

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.WorshipSong
import java.time.LocalDate

/** Add/edit form for one Música e Louvor item (Bloco A3) - same screen, no separate Admin
 *  dashboard, per the task's own instruction. */
@Composable
fun WorshipSongEditDialog(
    existing: WorshipSong?,
    viewModel: WorshipViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var artist by remember { mutableStateOf(existing?.artist ?: "") }
    var youtubeUrl by remember { mutableStateOf(existing?.youtubeUrl ?: "") }
    var momentLabel by remember { mutableStateOf(existing?.momentLabel ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var programDate by remember { mutableStateOf(existing?.programDate?.toString() ?: LocalDate.now().toString()) }
    var isDailyRecommendation by remember { mutableStateOf(existing?.isDailyRecommendation ?: false) }
    var recommendationMessage by remember { mutableStateOf(existing?.recommendationMessage ?: "") }
    var isPublished by remember { mutableStateOf(existing?.isPublished ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Adicionar música" else "Editar música") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artista/canal") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = { youtubeUrl = it },
                    label = { Text("Link do YouTube") },
                    supportingText = {
                        val resolved = viewModel.resolveYoutubeLink(youtubeUrl)
                        Text(if (youtubeUrl.isBlank()) "" else if (resolved != null) "Link reconhecido" else "Link não reconhecido")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = momentLabel, onValueChange = { momentLabel = it }, label = { Text("Momento da programação") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = programDate,
                    onValueChange = { programDate = it },
                    label = { Text("Data (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isDailyRecommendation, onCheckedChange = { isDailyRecommendation = it })
                    Text("Recomendação do dia")
                }
                if (isDailyRecommendation) {
                    OutlinedTextField(
                        value = recommendationMessage,
                        onValueChange = { recommendationMessage = it },
                        label = { Text("Mensagem da recomendação") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isPublished, onCheckedChange = { isPublished = it })
                    Text("Publicado")
                }
                error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = "Informe o título."
                    return@TextButton
                }
                val date = runCatching { LocalDate.parse(programDate) }.getOrNull()
                if (programDate.isNotBlank() && date == null) {
                    error = "Data inválida - use o formato AAAA-MM-DD."
                    return@TextButton
                }
                val resolved = viewModel.resolveYoutubeLink(youtubeUrl)
                val song = (existing ?: WorshipSong(title = title)).copy(
                    title = title,
                    artist = artist,
                    youtubeUrl = youtubeUrl,
                    youtubeVideoId = resolved?.first ?: existing?.youtubeVideoId ?: "",
                    thumbnailUrl = resolved?.second ?: existing?.thumbnailUrl ?: "",
                    momentLabel = momentLabel,
                    notes = notes,
                    programDate = date,
                    isDailyRecommendation = isDailyRecommendation,
                    recommendationDate = if (isDailyRecommendation) LocalDate.now() else null,
                    recommendationMessage = recommendationMessage.ifBlank { null },
                    isPublished = isPublished,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.save(song) { result ->
                    if (result.isSuccess) onDismiss() else error = "Não foi possível salvar. Tente novamente."
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
