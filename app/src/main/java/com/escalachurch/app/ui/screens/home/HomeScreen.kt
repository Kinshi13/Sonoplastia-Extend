package com.escalachurch.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.AnnouncementSpotlight
import com.escalachurch.app.ui.components.CardCarousel
import com.escalachurch.app.ui.components.ChangeNewsDialog
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ParallaxStarfield
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.ScaleCard
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.rememberEffectiveVisualSettings
import com.escalachurch.app.ui.components.rememberEntranceVisible
import androidx.compose.runtime.mutableFloatStateOf

@Composable
fun HomeScreen(
    onOpenGeneralScale: (java.time.LocalDate?) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenBulletins: () -> Unit = {},
    onOpenSonoplastia: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    onOpenAnnouncements: () -> Unit = {}
) {
    val viewModel = appViewModel { container ->
        HomeViewModel(
            container.scaleRepository,
            container.generalScaleRepository,
            container.userProfileRepository,
            container.changeLogRepository,
            container.adminSession,
            container.settingsRepository
        )
    }
    val state by viewModel.uiState.collectAsState()
    val pendingNews by viewModel.pendingNewsEntry.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val spotlightViewModel = appViewModel { container ->
        AnnouncementSpotlightViewModel(
            container.announcementRepository,
            container.announcementSpotlightStore,
            container.activeChurchManager,
            container.userProfileRepository
        )
    }
    val spotlightState by spotlightViewModel.uiState.collectAsState()

    val container = rememberAppContainer()
    val appSettings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    var editingTarget by remember { mutableStateOf<EditTarget?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var carouselScrollFraction by remember { mutableFloatStateOf(0f) }

    val cardsVisible = rememberEntranceVisible(appSettings.animationsEnabled)
    val reducedMotion = !appSettings.animationsEnabled
    // Fase 11.9B Bloco 14 - single central resolver decides whether the carousel's scroll-linked
    // starfield reacts at all and how many stars it draws (see VisualEffectsController).
    val effectiveVisualSettings = rememberEffectiveVisualSettings(appSettings)

    editingTarget?.let { target ->
        ScaleEditScreen(
            existing = target.item,
            isAdmin = state.isAdmin,
            onSave = { item ->
                viewModel.save(item)
                editingTarget = null
            },
            onDelete = target.item?.let { item -> { viewModel.delete(item); editingTarget = null } },
            onBack = { editingTarget = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ParallaxStarfield(
            scrollFraction = carouselScrollFraction,
            reducedMotion = !effectiveVisualSettings.parallaxActive,
            starCount = effectiveVisualSettings.starDensity
        )

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Próxima Escala",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = onOpenSonoplastia) {
                    Icon(Icons.Filled.Computer, contentDescription = "Sonoplastia (arquivos remotos)", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOpenBulletins) {
                    Icon(Icons.Filled.Description, contentDescription = "Boletins", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            com.escalachurch.app.ui.components.ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
        }
        Spacer(Modifier.height(20.dp))

        if (state.isLoading) {
            com.escalachurch.app.ui.components.CelestialLoadingState(
                variant = com.escalachurch.app.ui.components.SkeletonVariant.HERO,
                modifier = Modifier.weight(1f)
            )
        } else if (state.hasLoadError) {
            com.escalachurch.app.ui.components.CelestialOfflineState(onRetry = viewModel::retry, modifier = Modifier.weight(1f))
        } else if (state.scales.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.EventBusy,
                title = "Nenhuma escala futura cadastrada",
                message = if (state.isAdmin) {
                    "Adicione a primeira escala para começar a organizar a programação da igreja."
                } else {
                    "Fale com um administrador para cadastrar a escala, ou crie uma programação pessoal em \"Programar\"."
                },
                modifier = Modifier.weight(1f)
            ) {
                if (state.isAdmin) {
                    PrimaryButton(text = "Adicionar escala", onClick = { editingTarget = EditTarget(null) })
                }
            }
        } else {
            PulledUpEntrance(
                visible = cardsVisible,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    CardCarousel(
                        items = state.scales,
                        initialPage = state.startIndex ?: 0,
                        reducedMotion = reducedMotion,
                        onPageChanged = { currentPage = it },
                        onScrollFractionChanged = { carouselScrollFraction = it }
                    ) { scale ->
                        ScaleCard(
                            scale,
                            highlightClasses = state.myClasses,
                            entitlementService = container.entitlementService,
                            onSeePlans = onOpenPlans
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val current: ScaleItem? = state.scales.getOrNull(currentPage)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = { onOpenGeneralScale(current?.date) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Escala Geral", tint = MaterialTheme.colorScheme.primary)
                }
                SecondaryButton(
                    text = if (state.isAdmin) "Editar" else "Ver detalhes",
                    modifier = Modifier.weight(1f),
                    enabled = current != null,
                    onClick = { current?.let { editingTarget = EditTarget(it) } }
                )
                if (state.isAdmin) {
                    PrimaryButton(
                        text = "Adicionar escala",
                        modifier = Modifier.weight(1f),
                        onClick = { editingTarget = EditTarget(null) }
                    )
                }
            }
        }
        }
    }

    pendingNews?.let { entry ->
        ChangeNewsDialog(
            title = entry.title,
            message = entry.message,
            onViewScale = {
                viewModel.dismissNews(markSeen = true)
                onOpenGeneralScale(entry.relatedDateIso?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() })
            },
            onDismiss = { viewModel.dismissNews(markSeen = true) }
        )
    } ?: spotlightState.current?.let { announcement ->
        // Sequential priority - a pending scale-change dialog (existing, above) takes precedence
        // over the Spotlight so they never stack on the same open.
        AnnouncementSpotlight(
            announcement = announcement,
            index = spotlightState.currentIndex,
            total = spotlightState.total,
            onView = { spotlightViewModel.viewCurrent(); onOpenAnnouncements() },
            onSnooze = spotlightViewModel::snoozeCurrent,
            onConfirm = spotlightViewModel::confirmCurrent
        )
    }
}

private data class EditTarget(val item: ScaleItem?)
