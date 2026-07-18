package com.escalachurch.app.ui.screens.generalscale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.components.ConstellationIntensity
import com.escalachurch.app.ui.components.DayConstellationGlyph
import com.escalachurch.app.ui.components.accentColor
import com.escalachurch.app.ui.components.constellationKind
import com.escalachurch.app.ui.components.tintColor
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ErrorBanner
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.SourceBadge
import com.escalachurch.app.ui.components.SpecialBadge
import com.escalachurch.app.ui.components.dayOfWeekLabel
import com.escalachurch.app.ui.components.rememberEntranceVisible
import com.escalachurch.app.ui.components.toDisplayString
import com.escalachurch.app.ui.screens.home.ScaleEditScreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralScaleScreen(
    initialDate: LocalDate? = null,
    onBack: () -> Unit
) {
    val viewModel = appViewModel { container ->
        GeneralScaleViewModel(container.generalScaleRepository, container.userProfileRepository, container.adminSession)
    }
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val onlyMyClasses by viewModel.onlyMyClassesFlow.collectAsState()
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val listVisible = rememberEntranceVisible(appSettings.animationsEnabled)

    var editingTarget by remember { mutableStateOf<ScaleItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isCreatingExtraDays by remember { mutableStateOf(false) }
    var duplicateSource by remember { mutableStateOf<ScaleItem?>(null) }
    var deleteTarget by remember { mutableStateOf<ScaleItem?>(null) }

    LaunchedEffect(initialDate) {
        initialDate?.let { viewModel.setMonth(YearMonth.from(it)) }
    }

    LaunchedEffect(Unit) {
        com.escalachurch.app.ui.stellacore.StellaCoreBus.events().collect { command ->
            when (command) {
                com.escalachurch.app.ui.stellacore.StellaCoreCommand.NewOfficialScale -> {
                    isCreatingNew = true; editingTarget = null; isEditing = true
                }
                com.escalachurch.app.ui.stellacore.StellaCoreCommand.ToggleOnlyMyClassesGeneralScale -> {
                    viewModel.setOnlyMyClasses(!onlyMyClasses)
                }
                com.escalachurch.app.ui.stellacore.StellaCoreCommand.GoToTodayGeneralScale -> {
                    viewModel.setMonth(YearMonth.now())
                }
                else -> Unit
            }
        }
    }

    if (isEditing) {
        ScaleEditScreen(
            existing = if (isCreatingNew) null else editingTarget,
            isAdmin = true,
            onSave = { item -> viewModel.save(item); isEditing = false; editingTarget = null; isCreatingNew = false },
            onDelete = editingTarget?.let { item -> { viewModel.delete(item); isEditing = false; editingTarget = null } },
            onBack = { isEditing = false; editingTarget = null; isCreatingNew = false }
        )
        return
    }

    if (isCreatingExtraDays) {
        ExtraDaysScreen(
            onCreate = { template, dates -> viewModel.createExtraDays(template, dates); isCreatingExtraDays = false },
            onBack = { isCreatingExtraDays = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escala Geral") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(
                    onClick = { isCreatingNew = true; editingTarget = null; isEditing = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Filled.Add, contentDescription = "Nova escala oficial") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            errorMessage?.let { message ->
                ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
                Spacer(Modifier.height(12.dp))
            }
            MonthSelector(state.month, onPrevious = { viewModel.setMonth(state.month.minusMonths(1)) }, onNext = { viewModel.setMonth(state.month.plusMonths(1)) })
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = onlyMyClasses, onClick = { viewModel.setOnlyMyClasses(!onlyMyClasses) }, label = { Text("Somente minhas classes") })
                if (state.isAdmin) {
                    FilterChip(selected = false, onClick = { isCreatingExtraDays = true }, label = { Text("Criar Semana de Oração / dias extras") })
                }
            }
            Spacer(Modifier.height(12.dp))

            if (state.isLoading) {
                com.escalachurch.app.ui.components.CelestialLoadingState(variant = com.escalachurch.app.ui.components.SkeletonVariant.COMPACT)
            } else if (state.hasLoadError) {
                com.escalachurch.app.ui.components.CelestialOfflineState(onRetry = viewModel::retry)
            } else if (state.scales.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    title = "Nenhuma escala geral cadastrada para este mês.",
                    message = if (state.isAdmin) "Toque no botão + para criar a primeira escala oficial." else "Fale com um administrador para cadastrar a escala."
                )
            } else {
                PulledUpEntrance(visible = listVisible) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(state.scales, key = { it.id }) { scale ->
                            GeneralScaleRow(
                                scale = scale,
                                isAdmin = state.isAdmin,
                                highlightClasses = state.myClasses,
                                onEdit = { editingTarget = scale; isCreatingNew = false; isEditing = true },
                                onDuplicate = { duplicateSource = scale },
                                onDelete = { deleteTarget = scale }
                            )
                        }
                    }
                }
            }
        }
    }

    duplicateSource?.let { source ->
        DuplicateDateDialog(
            onConfirm = { date -> viewModel.duplicateTo(source, date); duplicateSource = null },
            onDismiss = { duplicateSource = null }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Excluir escala",
            message = "Tem certeza que deseja excluir esta escala oficial? Membros não verão mais essa data na Escala Geral.",
            onConfirm = { viewModel.delete(target); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior") }
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } + " " + month.year,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês") }
    }
}

@Composable
private fun GeneralScaleRow(
    scale: ScaleItem,
    isAdmin: Boolean,
    highlightClasses: Set<com.escalachurch.app.domain.model.UserClass>,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(scale.date.dayOfWeekLabel(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${scale.date.toDisplayString()} · ${scale.startTime.toDisplayString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(scale.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(scale.sourceType)
                    if (scale.isSpecialEvent) SpecialBadge()
                    val kind = scale.constellationKind()
                    DayConstellationGlyph(
                        kind = kind,
                        modifier = Modifier.size(22.dp),
                        intensity = ConstellationIntensity.COMPACT,
                        tint = kind.tintColor(),
                        accentColor = kind.accentColor()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            RoleChip("Recepção", scale.receptionPerson, com.escalachurch.app.domain.model.UserClass.RECEPCIONISTA in highlightClasses)
            RoleChip("Sonoplastia", scale.soundPerson, com.escalachurch.app.domain.model.UserClass.SONOPLASTA in highlightClasses)
            RoleChip("Pregação", scale.preachingPerson, com.escalachurch.app.domain.model.UserClass.PREGADOR in highlightClasses)
            RoleChip("Regência", scale.conductingPerson, com.escalachurch.app.domain.model.UserClass.REGENTE in highlightClasses)
            RoleChip("Mensagem musical", scale.musicalMessagePerson, com.escalachurch.app.domain.model.UserClass.CANTOR in highlightClasses)
            if (scale.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(scale.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                    IconButton(onClick = onDuplicate) { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar") }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun RoleChip(label: String, person: String, highlighted: Boolean) {
    if (person.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            person,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateDateDialog(onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicar escala") },
        text = {
            Column {
                Text("Escolha a data para a cópia desta escala.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                DatePickerField(label = "Nova data", date = date, onDateSelected = { date = it })
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { date?.let(onConfirm) }, enabled = date != null) { Text("Duplicar") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
