package com.escalachurch.app.ui.components

import android.app.ActivityManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.util.EffectiveVisualSettings
import com.escalachurch.app.domain.util.VisualQualityInputs
import com.escalachurch.app.domain.util.resolveEffectiveVisualSettings

/**
 * Fase 11.9B Bloco 14 - the only place that reads device/OS signals (RAM tier, battery saver,
 * the system's "remover animações" accessibility setting, whether the screen is actually visible)
 * and feeds them into [resolveEffectiveVisualSettings]. Every Celestial decorative component calls
 * this instead of re-deriving [EffectiveVisualSettings] from [AppSettings] itself, so the
 * precedence rules only live in one pure function (see VisualQualityResolverTest).
 */
@Composable
fun rememberEffectiveVisualSettings(appSettings: AppSettings): EffectiveVisualSettings {
    val context = LocalContext.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    val isLowRamDevice = remember {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val isPowerSaveMode = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    val systemReducedMotion = remember {
        // Android's "Remove animations" accessibility toggle drives this scale to 0 - there's no
        // dedicated "reduced motion" API, this is the same signal the OS itself uses.
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    return resolveEffectiveVisualSettings(
        VisualQualityInputs(
            qualityPreference = appSettings.visualQuality,
            parallaxPreference = appSettings.parallaxEnabled,
            animationsEnabled = appSettings.animationsEnabled,
            systemReducedMotion = systemReducedMotion,
            isLowRamDevice = isLowRamDevice,
            isPowerSaveMode = isPowerSaveMode,
            isAppVisible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        )
    )
}
