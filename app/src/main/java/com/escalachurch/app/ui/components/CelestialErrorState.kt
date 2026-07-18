package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Fase 11.9B Entrega 3 Bloco 6 - CelestialErrorState/OfflineState/PermissionState. A full-state
 * response (icon + title + short message + primary action + optional secondary action) for when a
 * screen's whole feed failed to load - distinct from [ErrorBanner] (a transient inline notice for
 * a single save/delete failure) and from [EmptyState] (there genuinely being nothing to show).
 * Never shown with raw Supabase/network exception text - see domain/util/ErrorMessages.kt.
 */
@Composable
fun CelestialErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.CloudOff,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(PaddingValues(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (secondaryLabel != null && onSecondary != null) {
                SecondaryButton(text = secondaryLabel, onClick = onSecondary)
            }
            PrimaryButton(text = "Tentar novamente", onClick = onRetry)
        }
    }
}

/** Specialization of [CelestialErrorState] for "no network, showing cached data" - never implies
 *  the user's plan/access was lost, just that the feed couldn't refresh right now. */
@Composable
fun CelestialOfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    lastSyncedLabel: String? = null
) {
    CelestialErrorState(
        title = "Sem conexão",
        message = lastSyncedLabel?.let { "Mostrando dados salvos · última sincronização: $it" }
            ?: "Mostrando os últimos dados salvos neste aparelho.",
        onRetry = onRetry,
        icon = Icons.Filled.WifiOff,
        modifier = modifier
    )
}

/** For an action blocked by permission (e.g. a non-admin reaching an admin-only path some other
 *  way) - not wired to any concrete flow yet in this entrega (none of today's screens can actually
 *  reach a permission-denied state client-side, RLS already prevents the write server-side before
 *  this would ever render), kept ready for when one exists. */
@Composable
fun CelestialPermissionState(
    message: String = "Você não tem permissão para acessar este recurso.",
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(PaddingValues(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Acesso restrito", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onBack != null) {
            Spacer(Modifier.height(20.dp))
            SecondaryButton(text = "Voltar", onClick = onBack)
        }
    }
}
