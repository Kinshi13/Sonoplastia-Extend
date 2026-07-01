package com.escalachurch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.FontSizeOption
import com.escalachurch.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = MediumBlue,
    onPrimary = White,
    primaryContainer = LightBlue,
    onPrimaryContainer = DeepBlue,
    secondary = SoftBlue,
    onSecondary = DeepBlue,
    background = OffWhite,
    onBackground = TextPrimaryLight,
    surface = White,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightBlue,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = SoftBlue,
    onPrimary = DeepBlue,
    primaryContainer = SoftBlueDark,
    onPrimaryContainer = White,
    secondary = SoftBlueDark,
    onSecondary = White,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    error = ErrorRed
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
