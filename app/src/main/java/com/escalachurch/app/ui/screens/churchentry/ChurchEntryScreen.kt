package com.escalachurch.app.ui.screens.churchentry

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.RecentChurch
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.AdminLoginDialog

/**
 * Fase 11.9A - first screen shown when there's no active church yet (see NavGraph): code entry +
 * "Continuar em" for recent churches + an Admin login entry point. Deliberately plain Material3
 * for now - the Celestial visual pass is out of scope for this phase.
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                Text("Escala Church", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Digite o código da sua igreja para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

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

                if (state.recentChurches.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Text("Continuar em", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(state.recentChurches, key = { it.churchId }) { recent ->
                RecentChurchRow(
                    recent = recent,
                    onOpen = { viewModel.continueWith(recent) },
                    onToggleFavorite = { viewModel.setFavorite(recent.churchId, !recent.isFavorite) },
                    onRemove = { viewModel.removeRecent(recent.churchId) }
                )
            }

            item {
                Spacer(Modifier.height(40.dp))
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
            onConfirm = { email, password -> viewModel.adminSignIn(email, password) },
            onDismiss = viewModel::dismissAdminLogin
        )
    }
}

@Composable
private fun RecentChurchRow(
    recent: RecentChurch,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            TextButton(onClick = onOpen, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Column {
                    Text(recent.churchName.ifBlank { recent.churchCode }, style = MaterialTheme.typography.bodyLarge)
                    Text(recent.churchCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
