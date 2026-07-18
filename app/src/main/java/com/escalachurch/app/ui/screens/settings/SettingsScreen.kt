package com.escalachurch.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.escalachurch.app.domain.model.AppFont
import com.escalachurch.app.domain.model.FontSizeOption
import com.escalachurch.app.domain.model.ThemeMode
import com.escalachurch.app.domain.model.VisualQuality
import com.escalachurch.app.ui.components.AdminLoginDialog
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.UserClassChips
import com.escalachurch.app.ui.components.rememberEntranceVisible
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenGeneralScale: () -> Unit = {},
    onOpenAnnouncements: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val viewModel = appViewModel { container ->
        SettingsViewModel(container.settingsRepository, container.userProfileRepository, container.adminSession)
    }
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScopeCompat()
    val sectionsVisible = rememberEntranceVisible(settings.animationsEnabled)

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(20.dp))
        PulledUpEntrance(visible = sectionsVisible) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
        item {
            SettingsSection(title = "Modo administrador") {
                Text(
                    "Só quem faz login com a conta de administrador (criada no Supabase pela liderança) " +
                        "pode editar escalas, doxologia e anúncios oficiais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (uiState.isAdmin) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (uiState.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.isAdmin) "Modo administrador ativo" else "Modo administrador inativo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (uiState.isAdmin) {
                    SecondaryButton(text = "Sair do modo administrador", onClick = { scope.launch { viewModel.adminSession.signOut() } })
                } else {
                    SecondaryButton(
                        text = "Entrar no modo administrador",
                        onClick = { loginError = null; showLoginDialog = true }
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Escala e Anúncios") {
                NavRow(icon = Icons.AutoMirrored.Filled.EventNote, label = "Escala geral", onClick = onOpenGeneralScale)
                NavRow(icon = Icons.Filled.Campaign, label = "Anúncios", onClick = onOpenAnnouncements)
            }
        }

        item {
            SettingsSection(title = "Assinatura") {
                NavRow(icon = Icons.Filled.AutoAwesome, label = "Planos e recursos", onClick = onOpenPlans)
            }
        }

        item {
            SettingsSection(title = "Classe de usuário") {
                Text(
                    "Selecione suas funções na igreja para receber destaques e avisos quando a escala mudar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                UserClassChips(selected = uiState.profile.selectedClasses, onToggle = { viewModel.toggleClass(it) })
                if (uiState.profile.selectedClasses.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Você ainda não selecionou suas classes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Notificações de alterações") {
                SwitchRow("Ativar notificações de alterações", settings.changeNotificationsEnabled) { checked ->
                    viewModel.update { it.copy(changeNotificationsEnabled = checked) }
                }
                if (settings.changeNotificationsEnabled) {
                    Text(
                        "Escopo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.notifyOnlyMyClasses,
                            onClick = { viewModel.update { it.copy(notifyOnlyMyClasses = true) } },
                            label = { Text("Só das minhas classes") }
                        )
                        FilterChip(
                            selected = !settings.notifyOnlyMyClasses,
                            onClick = { viewModel.update { it.copy(notifyOnlyMyClasses = false) } },
                            label = { Text("Todas as alterações") }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow("Mostrar pop-up de novidades ao abrir o app", settings.showNewsPopupOnOpen) { checked ->
                    viewModel.update { it.copy(showNewsPopupOnOpen = checked) }
                }
            }
        }

        item {
            SettingsSection(title = "Lembretes de escala") {
                Text(
                    "Informe seu nome como ele aparece nas escalas para receber um aviso quando estiver escalado(a).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                // Local-first: typed text is the source of truth for what's on screen, and is only
                // pushed to DataStore as a side effect. Binding the field's value directly to the
                // DataStore round-trip (read-after-write on every keystroke) races with fast typing
                // on real devices, dropping characters while the write/read-back is still in flight.
                var localName by remember { mutableStateOf(settings.myName) }
                AppTextField(
                    value = localName,
                    onValueChange = { localName = it; viewModel.update { s -> s.copy(myName = it) } },
                    label = "Meu nome"
                )
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
                Text(
                    "Conectado à nuvem da igreja — a escala geral, doxologia e anúncios são os mesmos para todo mundo, em tempo real.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Suas programações pessoais (aba Programar) e suas classes/lembretes continuam só neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsSection(title = "Interações") {
                SwitchRow("Animações suaves entre cards", settings.animationsEnabled) { checked ->
                    viewModel.update { it.copy(animationsEnabled = checked) }
                }
                SwitchRow("Efeitos visuais (parallax, brilhos)", settings.visualEffectsEnabled) { checked ->
                    viewModel.update { it.copy(visualEffectsEnabled = checked) }
                }
                if (settings.visualEffectsEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text("Qualidade visual", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualQuality.entries.forEach { option ->
                            FilterChip(
                                selected = settings.visualQuality == option,
                                onClick = { viewModel.update { it.copy(visualQuality = option) } },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
                SwitchRow("Vibração ao trocar de card", settings.vibrationEnabled) { checked ->
                    viewModel.update { it.copy(vibrationEnabled = checked) }
                }
            }
        }
    }
        }
    }

    if (showLoginDialog) {
        AdminLoginDialog(
            errorMessage = loginError,
            onConfirm = { email, password ->
                scope.launch {
                    val result = viewModel.adminSession.signIn(email, password)
                    if (result.isSuccess) {
                        showLoginDialog = false
                    } else {
                        loginError = "Não foi possível entrar. Confira o e-mail e a senha."
                    }
                }
            },
            onDismiss = { showLoginDialog = false }
        )
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
