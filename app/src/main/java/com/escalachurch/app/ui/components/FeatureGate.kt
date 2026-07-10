package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.escalachurch.app.entitlements.FeatureKey
import com.escalachurch.app.ui.theme.MediumBlue

/**
 * Wraps [content] and only renders it when [service] grants [feature]; otherwise renders
 * [locked] (defaults to nothing - callers that need a visible locked state, e.g. a menu item that
 * should still show but open the Premium Preview sheet on tap, pass their own).
 */
@Composable
fun FeatureGate(
    feature: FeatureKey,
    service: com.escalachurch.app.entitlements.EntitlementService,
    locked: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (service.has(feature)) content() else locked()
}

/**
 * Elegant upsell sheet shown instead of a dry "recurso bloqueado" error - Fase 3's "Premium
 * Preview" pattern. Explains what the feature does and offers a path to Planos e recursos,
 * without blocking navigation or throwing a hard error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPreviewSheet(
    featureName: String,
    featureDescription: String,
    onSeePlans: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MediumBlue, modifier = Modifier.size(28.dp))
                Text("Recurso premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(featureName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                featureDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onSeePlans,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MediumBlue)
            ) {
                Text("Ver planos e recursos")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Agora não")
            }
        }
    }
}
