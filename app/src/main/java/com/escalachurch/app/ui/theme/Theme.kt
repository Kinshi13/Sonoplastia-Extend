package com.escalachurch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.FontSizeOption
import com.escalachurch.app.domain.model.ThemeMode

// Fase 6 (Master Plan): the app's Material color scheme now sources every value from the
// Constellation Calm tokens (ConstellationTokens.kt, Fase 4) instead of the old standalone
// MediumBlue/DeepBlue/... constants below - one edit here recolors every screen that reads
// MaterialTheme.colorScheme (effectively the whole app; see Color.kt). Same role structure as
// before (primary/onPrimary/...), so this is a re-skin, not a rewrite: no screen's layout,
// navigation, or behavior changes, only the palette they render with.
// Fase 11.9B Bloco 17 audit - errorContainer/onErrorContainer (ErrorBanner, used app-wide for
// save/load failures) and tertiary (CalendarScreen's event-type dot) were left unset, so Material3
// filled them with its own generic baseline instead of a Constellation-tuned tone - not a contrast
// break (Material3's own defaults are pre-checked), but a "cor fixa fora do tema" gap. Soft-red
// container tones below are nova blended toward each theme's own surface, not arbitrary colors.
private val LightColors = lightColorScheme(
    primary = ConstellationColors.Light.polaris,
    onPrimary = ConstellationColors.Light.nebula,
    primaryContainer = ConstellationColors.Light.polarisSoft,
    onPrimaryContainer = ConstellationColors.Light.starlight,
    secondary = ConstellationColors.Light.aurora,
    onSecondary = ConstellationColors.Light.nebula,
    tertiary = ConstellationColors.Light.comet,
    background = ConstellationColors.Light.void,
    onBackground = ConstellationColors.Light.starlight,
    surface = ConstellationColors.Light.nebula,
    onSurface = ConstellationColors.Light.starlight,
    surfaceVariant = ConstellationColors.Light.nebulaElevated,
    onSurfaceVariant = ConstellationColors.Light.stardust,
    outline = ConstellationColors.Light.horizon,
    error = ConstellationColors.Light.nova,
    errorContainer = Color(0xFFF8E0E4),
    onErrorContainer = Color(0xFF6C1422)
)

private val DarkColors = darkColorScheme(
    primary = ConstellationColors.Dark.polaris,
    onPrimary = ConstellationColors.Dark.void,
    primaryContainer = ConstellationColors.Dark.polarisSoft,
    onPrimaryContainer = ConstellationColors.Dark.starlight,
    secondary = ConstellationColors.Dark.aurora,
    onSecondary = ConstellationColors.Dark.void,
    tertiary = ConstellationColors.Dark.comet,
    background = ConstellationColors.Dark.void,
    onBackground = ConstellationColors.Dark.starlight,
    surface = ConstellationColors.Dark.nebula,
    onSurface = ConstellationColors.Dark.starlight,
    surfaceVariant = ConstellationColors.Dark.nebulaElevated,
    onSurfaceVariant = ConstellationColors.Dark.stardust,
    outline = ConstellationColors.Dark.horizon,
    error = ConstellationColors.Dark.nova,
    errorContainer = Color(0xFF5C3246),
    onErrorContainer = Color(0xFFF3AFBA)
)

/**
 * App-wide theme, reacting live to [AppSettings] (theme mode, font, font size) so
 * changes made in Configurações apply immediately without restarting the app.
 */
@Composable
fun EscalaChurchTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val fontFamily = settings.selectedFont.toFontFamily()
    val fontScale = settings.fontSize.scale
    val typography = buildTypography(fontFamily, fontScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}
