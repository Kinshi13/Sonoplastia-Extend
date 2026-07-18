package com.escalachurch.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.stellacore.FourPointStar
import java.time.DayOfWeek

/**
 * Fase 11.9B Entrega 2 - the four "day constellations," semantically equivalent to the web's
 * (Farol/Coroa/Aurora/Peregrina). Never spelled out as text on a card (Bloco 3 rule) - the shape
 * and color alone identify the day; the actual weekday/date text next to it is what makes it
 * accessible (see Bloco 22 - constellation is a complementary cue, never the only one).
 *
 * Deliberately abstract dot-and-line geometry via Canvas, no raster assets, no per-frame
 * animation - cheap enough for a compact list row, not just the Hero card (see [intensity]).
 */
enum class DayConstellationKind { FAROL, COROA, AURORA, PEREGRINA }

/** Wednesday -> Farol, Saturday -> Coroa (the week's main service), Sunday -> Aurora, any
 *  isSpecialEvent -> Peregrina regardless of weekday (a special day's own identity takes over). */
fun ScaleItem.constellationKind(): DayConstellationKind = when {
    isSpecialEvent -> DayConstellationKind.PEREGRINA
    date.dayOfWeek == DayOfWeek.WEDNESDAY -> DayConstellationKind.FAROL
    date.dayOfWeek == DayOfWeek.SATURDAY -> DayConstellationKind.COROA
    date.dayOfWeek == DayOfWeek.SUNDAY -> DayConstellationKind.AURORA
    else -> DayConstellationKind.FAROL
}

/** Hero = full detail with the four-point star accent (Sábado's "maior presença"); Compact = same
 *  silhouette, thinner strokes, no star accent - for list rows where many are on screen at once
 *  (Bloco 21: "listas longas devem usar uma versão mais leve"). */
enum class ConstellationIntensity { HERO, COMPACT }

/** Per-kind hue, per Entrega 2's spec (Farol: azul frio/ciano, Coroa: azul profundo + dourado,
 *  Aurora: azul claro/lavanda, Peregrina: violeta/azul estelar) - a fixed identity independent of
 *  light/dark theme, since the accent needs to read the same way in both. */
fun DayConstellationKind.tintColor(): Color = when (this) {
    DayConstellationKind.FAROL -> Color(0xFF5BC8D6)
    DayConstellationKind.COROA -> Color(0xFF3A5AC9)
    DayConstellationKind.AURORA -> Color(0xFFA9C2F0)
    DayConstellationKind.PEREGRINA -> Color(0xFF8B6BE0)
}

fun DayConstellationKind.accentColor(): Color = when (this) {
    DayConstellationKind.COROA -> Color(0xFFD4A94A) // "dourado suave"
    else -> tintColor()
}

@Composable
fun DayConstellationGlyph(
    kind: DayConstellationKind,
    modifier: Modifier = Modifier,
    intensity: ConstellationIntensity = ConstellationIntensity.COMPACT,
    tint: Color,
    accentColor: Color
) {
    val strokeWidth = if (intensity == ConstellationIntensity.HERO) 2.2f else 1.4f
    val dotRadius = if (intensity == ConstellationIntensity.HERO) 3.2f else 2f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Each kind is a small fixed set of fractional points + which pairs connect - the actual
        // "identity" is this layout, not any icon/label.
        val (points, edges) = when (kind) {
            DayConstellationKind.FAROL -> // vertical composition - a lighthouse-like column
                listOf(
                    Offset(0.5f, 0.05f), Offset(0.5f, 0.35f), Offset(0.5f, 0.65f), Offset(0.5f, 0.95f),
                    Offset(0.25f, 0.5f), Offset(0.75f, 0.5f)
                ) to listOf(0 to 1, 1 to 2, 2 to 3, 1 to 4, 1 to 5)
            DayConstellationKind.COROA -> // a wide crown-like arc, larger presence
                listOf(
                    Offset(0.1f, 0.7f), Offset(0.3f, 0.25f), Offset(0.5f, 0.05f), Offset(0.7f, 0.25f), Offset(0.9f, 0.7f),
                    Offset(0.5f, 0.55f)
                ) to listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 1 to 5, 3 to 5)
            DayConstellationKind.AURORA -> // ascending arc
                listOf(
                    Offset(0.08f, 0.85f), Offset(0.3f, 0.6f), Offset(0.55f, 0.4f), Offset(0.78f, 0.22f), Offset(0.95f, 0.1f)
                ) to listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
            DayConstellationKind.PEREGRINA -> // asymmetric trajectory
                listOf(
                    Offset(0.12f, 0.2f), Offset(0.4f, 0.5f), Offset(0.3f, 0.85f), Offset(0.7f, 0.65f), Offset(0.92f, 0.15f)
                ) to listOf(0 to 1, 1 to 2, 1 to 3, 3 to 4)
        }
        val resolved = points.map { Offset(it.x * w, it.y * h) }
        edges.forEach { (from, to) ->
            drawLine(
                color = tint.copy(alpha = 0.55f),
                start = resolved[from],
                end = resolved[to],
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        resolved.forEach { p -> drawCircle(color = tint, radius = dotRadius, center = p) }
    }

    if (kind == DayConstellationKind.COROA && intensity == ConstellationIntensity.HERO) {
        // Saturday's "estrela central de quatro pontas" - the one kind that gets the extra accent,
        // smaller than the glyph itself so it reads as one star within the constellation, not a
        // replacement for it.
        FourPointStar(modifier = Modifier.size(14.dp), color = accentColor, glowColor = tint)
    }
    }
}
