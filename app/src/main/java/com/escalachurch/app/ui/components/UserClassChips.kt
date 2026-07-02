package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.UserClass

/** Multi-select chip row for [UserClass] - used in Configurações and when tagging escalas/anúncios. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserClassChips(
    selected: Set<UserClass>,
    onToggle: (UserClass) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UserClass.entries.forEach { userClass ->
            FilterChip(
                selected = userClass in selected,
                onClick = { onToggle(userClass) },
                label = { Text(userClass.label) }
            )
        }
    }
}
