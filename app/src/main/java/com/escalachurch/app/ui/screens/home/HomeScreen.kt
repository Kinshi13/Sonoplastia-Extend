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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.AnnouncementSpotlight
import com.escalachurch.app.ui.components.ChangeNewsDialog
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ParallaxStarfield
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.rememberEffectiveVisualSettings

/**
 * Fase 11.11 (Home 2.0) - answers "o que está acontecendo, e o que importa agora": one Hero (the
 * next official schedule), a short "Agora na Igreja" editorial section, and "Acesso Rápido" -
 * deliberately NOT an admin dashboard (no sync/debug/plan-promotion content lives here). Escalas,
 * Anúncios, Calendário keep their own screens untouched - this file only composes what Home shows
 * of them, it never reimplements them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenGeneralScale: (java.time.LocalDate?) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenBulletins: () -> Unit = {},
    onOpenSonoplastia: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    onOpenAnnouncements: () -> Unit = {},
    onOpenWorship: () -> Unit = {},
    onOpenDoxology: () -> Unit = {},
    onOpenScaleImport: () -> Unit = {}
) {
    val viewModel = appViewModel { container ->
        HomeViewModel(
            container.scaleRepository,
            container.generalScaleRepository,
            container.userProfileRepository,
            container.changeLogRepository,
            container.adminSession,
            container.settingsRepository,
            container.announcementRepository,
            container.worshipSongRepository,
            container.activeChurchManager
        )
    }
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
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
    // Fase 11.10 - "Compartilhar acesso da igreja" from Stella Core (admin only) - Home is the
    // first screen that needs to react to a Stella Core command without navigating away.
    var showShareChurchAccess by remember { mutableStateOf(false) }
    var showShareSchedule by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        com.escalachurch.app.ui.stellacore.StellaCoreBus.events().collect { command ->
            when (command) {
                com.escalachurch.app.ui.stellacore.StellaCoreCommand.ShareChurchAccess -> showShareChurchAccess = true
                com.escalachurch.app.ui.stellacore.StellaCoreCommand.ShareSchedule -> showShareSchedule = true
                else -> Unit
            }
        }
    }

    val effectiveVisualSettings = rememberEffectiveVisualSettings(appSettings)
    val scrollState = rememberScrollState()
    // Bloco 15/16 - soft scroll-based parallax, no sensors: the existing starfield already reads
    // a 0f..1f scroll fraction, reused here against Home's own vertical scroll instead of the old
    // carousel's page-scroll fraction.
    val scrollFraction = if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) viewModel.refresh()
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullState.endRefresh()
    }

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

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullState.nestedScrollConnection)) {
        ParallaxStarfield(
            scrollFraction = scrollFraction,
            reducedMotion = !effectiveVisualSettings.parallaxActive,
            starCount = effectiveVisualSettings.starDensity
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp)) {
            HomeHeader(
                churchName = state.churchName,
                isAdmin = state.isAdmin,
                onOpenSettings = onOpenSettings
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                com.escalachurch.app.ui.components.ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
            }

            Spacer(Modifier.height(20.dp))

            if (state.isLoading) {
                com.escalachurch.app.ui.components.CelestialLoadingState(
                    variant = com.escalachurch.app.ui.components.SkeletonVariant.HERO
                )
            } else if (state.hasLoadError) {
                // Bloco 18 - only the schedule feed failing degrades the whole screen; a
                // discrete retry, never a technical snackbar every time Home opens.
                com.escalachurch.app.ui.components.CelestialOfflineState(onRetry = viewModel::retry)
            } else {
                val nextScale = state.nextScale
                if (nextScale == null) {
                    EmptyState(
                        icon = Icons.Filled.EventBusy,
                        title = "Tudo tranquilo por enquanto",
                        message = "Nenhuma programação futura foi publicada."
                    ) {
                        SecondaryButton(text = "Ver calendário", onClick = onOpenAnnouncements)
                        if (state.isAdmin) {
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton(text = "Adicionar escala", onClick = { editingTarget = EditTarget(null) })
                        }
                    }
                } else {
                    HeroCard(
                        scale = nextScale,
                        isAdmin = state.isAdmin,
                        onOpenGeneralScale = { onOpenGeneralScale(nextScale.date) },
                        onEditOrDetails = { editingTarget = EditTarget(nextScale) }
                    )
                }

                val agoraItems = rankAgoraNaIgreja(state.pinnedAnnouncement, state.nextScale, state.todaysRecommendation)
                if (agoraItems.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Agora na Igreja",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        agoraItems.forEach { item ->
                            AgoraNaIgrejaCard(item = item, onOpenAnnouncements = onOpenAnnouncements, onOpenWorship = onOpenWorship, onOpenGeneralScale = onOpenGeneralScale)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "Acesso Rápido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(quickAccessItems(state.isAdmin)) { entry ->
                        QuickAccessTile(entry = entry) {
                            when (entry) {
                                QuickAccessEntry.WORSHIP -> onOpenWorship()
                                QuickAccessEntry.DOXOLOGY -> onOpenDoxology()
                                QuickAccessEntry.SCALE_IMPORT -> onOpenScaleImport()
                                QuickAccessEntry.SONOPLASTIA -> onOpenSonoplastia()
                                QuickAccessEntry.BULLETINS -> onOpenBulletins()
                            }
                        }
                    }
                }

                if (state.isAdmin) {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(
                            onClick = { editingTarget = EditTarget(null) },
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Nova escala", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = onOpenAnnouncements,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        ) {
                            Icon(Icons.Filled.Campaign, contentDescription = "Novo anúncio", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
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

    if (showShareChurchAccess) {
        com.escalachurch.app.ui.components.ShareChurchAccessBottomSheet(
            nextScale = state.nextScale,
            onDismiss = { showShareChurchAccess = false }
        )
    }

    if (showShareSchedule) {
        val entitlements by container.entitlementService.entitlements.collectAsState()
        com.escalachurch.app.ui.components.ScheduleShareBottomSheet(
            nextScale = state.nextScale,
            canExport = container.entitlementService.has(com.escalachurch.app.entitlements.FeatureKey.EXPORT),
            isFreePlan = entitlements.planCode == com.escalachurch.app.entitlements.PlanCode.FREE,
            onSeePlans = { showShareSchedule = false; onOpenPlans() },
            onDismiss = { showShareSchedule = false }
        )
    }
}

/** Bloco 3/4 - compact header: church name (never hardcoded - see [HomeUiState.churchName], read
 *  from ActiveChurchManager), a discrete "ADMIN" indicator, and settings access. No big banner. */
@Composable
private fun HomeHeader(churchName: String, isAdmin: Boolean, onOpenSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                churchName.ifBlank { "Escala Church" },
                style = MaterialTheme.typography.titleLarge,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isAdmin) {
                Text(
                    "ADMIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Bloco 5/6 - the single next official schedule, up to 3 role lines + "+ N funções", tapping the
 *  card opens the existing detail/edit screen (no duplicate flow built here). */
@Composable
private fun HeroCard(scale: ScaleItem, isAdmin: Boolean, onOpenGeneralScale: () -> Unit, onEditOrDetails: () -> Unit) {
    val hero = heroSummaryFor(scale)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            hero.specialLabel?.let { label ->
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }
            Text(scale.title.ifBlank { "Culto" }, style = MaterialTheme.typography.titleLarge)
            Text(
                "${scale.date} · ${scale.startTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            hero.visibleRoles.forEach { line ->
                Text("${line.roleLabel}: ${line.personName}", style = MaterialTheme.typography.bodyMedium)
            }
            if (hero.hiddenRoleCount > 0) {
                Text(
                    "+ ${hero.hiddenRoleCount} funções",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onOpenGeneralScale, modifier = Modifier.size(44.dp).clip(CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Escala Geral", tint = MaterialTheme.colorScheme.primary)
                }
                SecondaryButton(
                    text = if (isAdmin) "Editar" else "Ver detalhes",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditOrDetails
                )
            }
        }
    }
}

@Composable
private fun AgoraNaIgrejaCard(
    item: AgoraNaIgrejaItem,
    onOpenAnnouncements: () -> Unit,
    onOpenWorship: () -> Unit,
    onOpenGeneralScale: (java.time.LocalDate?) -> Unit
) {
    val (title, subtitle, action, onClick) = when (item) {
        is AgoraNaIgrejaItem.PinnedAnnouncement -> AgoraCardContent(
            "AVISO", item.announcement.title, "Ver anúncio →", onOpenAnnouncements
        )
        is AgoraNaIgrejaItem.SpecialSchedule -> AgoraCardContent(
            item.label, item.scale.title.ifBlank { "Culto" }, "Ver escala →", { onOpenGeneralScale(item.scale.date) }
        )
        is AgoraNaIgrejaItem.DailyMusic -> AgoraCardContent(
            "MÚSICA DO DIA", item.song.title, "Ouvir →", onOpenWorship
        )
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(action, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private data class AgoraCardContent(val title: String, val subtitle: String, val action: String, val onClick: () -> Unit)

private enum class QuickAccessEntry(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    WORSHIP("Música e Louvor", Icons.Filled.MusicNote),
    DOXOLOGY("Doxologia", Icons.Filled.MusicNote),
    SCALE_IMPORT("Importar escala", Icons.Filled.Add),
    SONOPLASTIA("Sonoplastia", Icons.Filled.Settings),
    BULLETINS("Boletins", Icons.Filled.Campaign)
}

/** Bloco 12 - deliberately excludes Início/Escalas/Anúncios/Calendário: they already live in the
 *  bottom nav, listing them again here would just be redundant. Scale import is admin-only. */
private fun quickAccessItems(isAdmin: Boolean): List<QuickAccessEntry> = buildList {
    add(QuickAccessEntry.WORSHIP)
    add(QuickAccessEntry.DOXOLOGY)
    if (isAdmin) add(QuickAccessEntry.SCALE_IMPORT)
    add(QuickAccessEntry.SONOPLASTIA)
    add(QuickAccessEntry.BULLETINS)
}

@Composable
private fun QuickAccessTile(entry: QuickAccessEntry, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        modifier = Modifier.size(88.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(entry.icon, contentDescription = entry.label, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(entry.label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

private data class EditTarget(val item: ScaleItem?)
