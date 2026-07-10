package com.escalachurch.app.ui.screens.plans

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.entitlements.Plan

private fun formatPrice(cents: Int?, currency: String): String {
    if (cents == null) return "-"
    if (cents == 0) return "Grátis"
    return "R$ ${"%.2f".format(cents / 100.0)}".replace(".", ",")
}

@Composable
fun PlansScreen(onBack: () -> Unit = {}) {
    val viewModel = appViewModel { container ->
        PlansViewModel(container.planRepository, container.entitlementService)
    }
    val plans by viewModel.plans.collectAsState()
    val current by viewModel.current.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Planos e recursos", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Plano atual da igreja: ${current.planName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(plans.filter { it.isPublic }) { plan ->
                PlanCard(plan = plan, isCurrent = plan.code == current.planCode)
            }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, isCurrent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (isCurrent) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Atual") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(
                if (plan.billingPeriod == "one_time") "${formatPrice(plan.monthlyPriceCents, plan.currency)} (pagamento único)"
                else "${formatPrice(plan.monthlyPriceCents, plan.currency)}/mês",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            plan.features.take(6).forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                    Text(feature.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
