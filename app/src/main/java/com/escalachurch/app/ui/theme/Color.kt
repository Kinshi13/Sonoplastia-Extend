package com.escalachurch.app.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy pre-Constellation-Calm palette. No longer wired into Theme.kt (see ConstellationTokens.kt
// and Fase 6 of the Master Plan) - kept only because SpecialGold is still the app's one "special
// event" gold accent (same hue as ConstellationColors.comet). Prefer MaterialTheme.colorScheme.*
// or ConstellationColors in new code instead of adding call sites for the constants below.
val White = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF7F9FC)
val LightBlue = Color(0xFFDCEBFF)
val SoftBlue = Color(0xFFA9CBFF)
val MediumBlue = Color(0xFF3B7DDD)
val DeepBlue = Color(0xFF1E3A8A)
val TextPrimaryLight = Color(0xFF17233D)
val TextSecondaryLight = Color(0xFF64748B)
val DividerLight = Color(0xFFE2E8F0)
val ErrorRed = Color(0xFFDC2626)
val SpecialGold = Color(0xFFC98A2C)

// Dark theme counterparts, keeping the same blue family for brand consistency.
val DarkBackground = Color(0xFF0F1626)
val DarkSurface = Color(0xFF17223B)
val DarkSurfaceAlt = Color(0xFF1E2C4A)
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFFA9B6CC)
val DividerDark = Color(0xFF2A3A5C)
val SoftBlueDark = Color(0xFF3E5B99)
