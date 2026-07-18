package com.escalachurch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationElevation

/**
 * Fase 11.9B Bloco 3/11 - the shared "ornamented card" frame used across the site's Celestial
 * cards: an ambient shadow, a soft gradient surface, and a hairline border - reused here instead
 * of each screen re-deriving its own card look. Deliberately no glow/blur *inside* the frame
 * (Bloco 11: "não aplicar glow intenso em listas longas") - just the frame itself, cheap enough
 * to use anywhere, including lists.
 *
 * [tone] only tints the shadow (ADMIN leans gold/"comet" instead of the calm primary/"polaris") -
 * same frame, a slightly more sober glow, per Bloco 8.
 */
@Composable
fun CelestialFrame(
    modifier: Modifier = Modifier,
    tone: CelestialTone = CelestialTone.PUBLIC,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val palette = if (isDark) ConstellationColors.Dark else ConstellationColors.Light
    val glowColor = if (tone == CelestialTone.ADMIN) palette.comet else palette.polaris
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation = ConstellationElevation.floatingDp, shape = shape, ambientColor = glowColor, spotColor = glowColor)
            .background(Brush.verticalGradient(listOf(palette.nebulaElevated, palette.nebula)), shape)
            .border(1.dp, palette.horizon, shape),
        content = content
    )
}
