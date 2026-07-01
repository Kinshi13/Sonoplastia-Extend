package com.escalachurch.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.domain.model.AppFont
import com.escalachurch.app.domain.model.FontSizeOption
import com.escalachurch.app.domain.model.ThemeMode

@Composable
fun SettingsScreen() {
    val viewModel = appViewModel { container -> SettingsViewModel(container.settingsRepository) }
    val settings by viewModel.settings.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            SettingsSection(title = "Lembretes de escala") {
                Text(
                    "Informe seu nome como ele aparece nas escalas para receber um aviso quando estiver escalado(a).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(value = settings.myName, onValueChange = { viewModel.update { s -> s.copy(myName = it) } }, label = "Meu nome")
                Spacer(Modifier.height(4.dp))
                SwitchRow("Ativar lembretes", settings.remindersEnabled) { checked ->
                    viewModel.update { it.copy(remindersEnabled = checked) }
                }
                if (settings.remindersEnabled) {
                    SwitchRow("Avisar 1 dia antes", settings.notifyDayBefore) { checked ->
                        viewModel.update { it.copy(notifyDayBefore = checked) }
                    }
                    SwitchRow("Avisar poucas horas antes", settings.notifyHoursBefore) { checked ->
                        viewModel.update { it.copy(notifyHoursBefore = checked) }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Aparência") {
                Text("Tamanho da fonte", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontSizeOption.entries.forEach { option ->
                        FilterChip(
                            selected = settings.fontSize == option,
                            onClick = { viewModel.update { it.copy(fontSize = option) } },
                            label = { Text(option.label) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.update { it.copy(themeMode = mode) } },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Fontes disponíveis") {
                AppFont.entries.filter { !it.isPremium }.forEach { font ->
                    FontRow(font, selected = settings.selectedFont == font) {
                        viewModel.update { it.copy(selectedFont = font) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Fontes premium em breve", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                AppFont.entries.filter { it.isPremium }.forEach { font ->
                    FontRow(font, selected = false, enabled = false, onSelect = {})
                }
            }
        }

        item {
            SettingsSection(title = "Áudio") {
                Text("Volume da música", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = settings.musicVolume,
                    onValueChange = { value -> viewModel.update { it.copy(musicVolume = value) } },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(8.dp))
                Text("Volume dos efeitos sonoros", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = settings.effectsVolume,
                    onValueChange = { value -> viewModel.update { it.copy(effectsVolume = value) } },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        item {
            SettingsSection(title = "Modo de uso") {
                val isStandalone = settings.syncMode == com.escalachurch.app.domain.model.SyncMode.STANDALONE
                Text(
                    if (isStandalone) "Modo pessoal (local) — os dados ficam só neste aparelho."
                    else "Conectado à igreja: ${settings.workspaceName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Em breve: um administrador poderá montar a escala e cada pessoa incluída recebe o aviso automaticamente, sem poder alterar a escala principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = "Conectar à minha igreja (em breve)", enabled = false, onClick = {})
            }
        }

        item {
            SettingsSection(title = "Interações") {
                SwitchRow("Animações suaves entre cards", settings.animationsEnabled) { checked ->
                    viewModel.update { it.copy(animationsEnabled = checked) }
                }
                SwitchRow("Efeitos visuais", settings.visualEffectsEnabled) { checked ->
                    viewModel.update { it.copy(visualEffectsEnabled = checked) }
                }
                SwitchRow("Vibração ao trocar de card", settings.vibrationEnabled) { checked ->
                    viewModel.update { it.copy(vibrationEnabled = checked) }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun FontRow(font: AppFont, selected: Boolean, enabled: Boolean = true, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            font.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilterChip(selected = selected, onClick = onSelect, enabled = enabled, label = { Text(if (selected) "Selecionada" else "Selecionar") })
    }
}
