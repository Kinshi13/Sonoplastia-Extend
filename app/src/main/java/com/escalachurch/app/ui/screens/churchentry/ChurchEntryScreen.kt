package com.escalachurch.app.ui.screens.churchentry

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.RecentChurch
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.AdminLoginDialog
import com.escalachurch.app.ui.components.CelestialBackground
import com.escalachurch.app.ui.components.CelestialFrame
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.stellacore.FourPointStar
import kotlin.math.roundToLong

/**
 * Fase 11.9B Bloco 7 - first screen shown when there's no active church yet (see NavGraph):
 * cosmic backdrop (CelestialBackground), code entry + "Continuar em" for recent churches (each in
 * a CelestialFrame mini card) + an Admin login entry point. Logic unchanged from Fase 11.9A - this
 * is a visual pass only, see ChurchEntryViewModel for the actual behavior.
 */
@Composable
fun ChurchEntryScreen() {
    val viewModel = appViewModel { container ->
        ChurchEntryViewModel(
            container.churchRepository,
            container.recentChurchStore,
            container.activeChurchManager,
            container.adminSession
        )
    }
    val state by viewModel.uiState.collectAsState()

    CelestialBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FourPointStar(
                        modifier = Modifier.size(34.dp),
                        color = MaterialTheme.colorScheme.primary,
                        glowColor = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Escala Church",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Encontre sua organização",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))

                CelestialFrame(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        AppTextField(
                            value = state.code,
                            onValueChange = viewModel::onCodeChange,
                            label = "Código da igreja",
                            placeholder = "ex: minha-igreja",
                            isError = state.errorMessage != null,
                            supportingText = state.errorMessage
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PrimaryButton(
                                text = "Entrar",
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.code.isNotBlank() && !state.isSubmitting,
                                onClick = viewModel::submitCode
                            )
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }

                if (state.recentChurches.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "Continuar em",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(state.recentChurches, key = { it.churchId }) { recent ->
                RecentChurchCard(
                    recent = recent,
                    onOpen = { viewModel.continueWith(recent) },
                    onToggleFavorite = { viewModel.setFavorite(recent.churchId, !recent.isFavorite) },
                    onRemove = { viewModel.removeRecent(recent.churchId) }
                )
            }

            item {
                Spacer(Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = viewModel::openAdminLogin) {
                        Text("Sou administrador")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (state.showAdminLogin) {
        AdminLoginDialog(
            errorMessage = state.adminLoginError,
            isLoading = state.adminLoginBusy,
            onConfirm = { email, password -> viewModel.adminSignIn(email, password) },
            onDismiss = viewModel::dismissAdminLogin,
            onForgotPassword = { email -> viewModel.forgotPassword(email) },
            forgotPasswordMessage = state.forgotPasswordMessage
        )
    }
}

@Composable
private fun RecentChurchCard(
    recent: RecentChurch,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    CelestialFrame(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
                    Column {
                        Text(recent.churchName.ifBlank { recent.churchCode }, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${recent.churchCode} · ${lastAccessedLabel(recent.lastAccessedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (recent.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (recent.isFavorite) "Remover dos favoritos" else "Marcar como favorita",
                    tint = if (recent.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remover", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "Hoje" / "há N dia(s)" - no new date-formatting dependency, just a coarse relative label. */
private fun lastAccessedLabel(lastAccessedAt: Long): String {
    val days = ((System.currentTimeMillis() - lastAccessedAt) / 86_400_000.0).roundToLong()
    return when {
        days <= 0 -> "hoje"
        days == 1L -> "há 1 dia"
        else -> "há $days dias"
    }
}
