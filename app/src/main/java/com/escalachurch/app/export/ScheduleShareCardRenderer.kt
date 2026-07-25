package com.escalachurch.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.dayOfWeekLabel
import com.escalachurch.app.ui.components.toDisplayString
import java.io.File
import java.io.FileOutputStream

/**
 * Android replica of the web's `renderScaleCardPng` (web/app/admin/escalas/export-png.ts,
 * "Compartilhar escala"). Same 1080x1350 (4:5) share-card shape, same Constellation Calm palette
 * (here [com.escalachurch.app.ui.theme.ConstellationColors.Dark] instead of the web's `--cc-*` CSS
 * vars, since a Canvas render can't read those), same content: church name, program title, date/
 * time, up to 5 roles, and a footer with the public link + church code + Escala Church branding.
 *
 * Deliberately a fresh renderer rather than a reuse of [ScaleExporter]'s: that one draws the
 * app's older light/white "próxima escala" card (see its own doc comment - a different, pre-
 * existing visual language), while this one exists specifically to match the new Stella/Celestial
 * identity the web version just shipped ("mesmo visual aprovado no site"). Not screenshot-based -
 * every element is drawn at fixed, resolution-independent coordinates.
 */
object ScheduleShareCardRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val MARGIN = 90f

    // Constellation Calm Dark tokens (see ConstellationTokens.kt) - copied as plain ARGB ints
    // since Canvas paints don't read Compose Color objects directly.
    private const val VOID = 0xFF090B14.toInt()
    private const val NEBULA = 0xFF12162A.toInt()
    private const val STARLIGHT = 0xFFF3F4FA.toInt()
    private const val STARDUST = 0xFFA6ABC7.toInt()
    private const val MUTED = 0xFF5C6280.toInt()
    private const val ACCENT = 0xFF7C9CFF.toInt() // polaris

    private val roleFields: (ScaleItem) -> List<Pair<String, String>> = { scale ->
        listOf(
            "Recepção" to scale.receptionPerson,
            "Sonoplastia" to scale.soundPerson,
            "Pregação" to scale.preachingPerson,
            "Regência" to scale.conductingPerson,
            "Mensagem musical" to scale.musicalMessagePerson
        ).filter { it.second.isNotBlank() }
    }

    fun renderBitmap(scale: ScaleItem, churchName: String, churchCode: String, publicUrl: String, isFreePlan: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Layer 1: gradient ground (void -> nebula, top to bottom - same direction as the web card).
        canvas.drawColor(VOID)
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), VOID, NEBULA, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), groundPaint)

        // Layer 2: soft accent glow, top-right - a radial gradient fading to transparent, echoing
        // the web card's `createRadialGradient` accent glow.
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                WIDTH * 0.82f, HEIGHT * 0.1f, 460f,
                ColorUtils.setAlphaComponent(ACCENT, 60), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glowPaint)

        // Layer 3: corner-cut frame (top-left and bottom-right corners clipped, matching the web
        // card's CelestialCard-style cut-corner border) - Android's theme has no existing
        // corner-cut Shape, so this is drawn as its own Path here.
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = ColorUtils.setAlphaComponent(ACCENT, 140)
        }
        canvas.drawPath(cutCornerPath(MARGIN, MARGIN, WIDTH - MARGIN, HEIGHT - MARGIN, cut = 34f), framePaint)

        // Layer 4: discreet constellation watermark (a handful of dots + connecting lines), bottom-right.
        drawConstellation(canvas, WIDTH - 240f, HEIGHT - 330f)

        // Layer 5: corner star mark, top-left.
        drawStar(canvas, MARGIN + 46f, MARGIN + 50f, 17f, ACCENT)

        var y = MARGIN + 150f
        val left = MARGIN + 46f
        val maxTextWidth = WIDTH - left - MARGIN

        val labelPaint = textPaint(ACCENT, 30f, bold = true)
        canvas.drawText("ESCALA OFICIAL  ·  ${churchName.uppercase()}", left, y, labelPaint)
        y += 76f

        val titlePaint = textPaint(STARLIGHT, 68f, bold = true)
        y = drawWrapped(canvas, scale.title, left, y, maxTextWidth, 78f, titlePaint, maxLines = 2)
        y += 12f

        val dateLine = buildString {
            append(scale.date.dayOfWeekLabel()).append(" · ").append(scale.date.toDisplayString())
            append(" às ").append(scale.startTime.toDisplayString())
            scale.endTime?.let { append(" - ").append(it.toDisplayString()) }
        }
        val datePaint = textPaint(STARDUST, 34f)
        canvas.drawText(dateLine, left, y, datePaint)
        y += 90f

        // Fixed 5-role list (same as web's ROLE_FIELDS) - no dynamic overflow risk, so no
        // multi-page/summarized fallback is needed for "escalas grandes" here.
        val roleLabelPaint = textPaint(MUTED, 26f)
        val rolePersonPaint = textPaint(STARLIGHT, 40f, bold = true)
        roleFields(scale).forEach { (label, person) ->
            canvas.drawText(label.uppercase(), left, y, roleLabelPaint)
            y += 44f
            canvas.drawText(person, left, y, rolePersonPaint)
            y += 78f
        }

        // Footer: public link + church code + Escala Church branding - never IDs/tokens/admin data.
        val footerLinkPaint = textPaint(ColorUtils.setAlphaComponent(STARDUST, 220), 26f)
        canvas.drawText(publicUrl.removePrefix("https://").removePrefix("http://"), left, HEIGHT - 150f, footerLinkPaint)
        val footerCodePaint = textPaint(MUTED, 24f)
        canvas.drawText("Código da igreja: $churchCode", left, HEIGHT - 112f, footerCodePaint)
        val brandPaint = textPaint(MUTED, 26f)
        canvas.drawText("✦ Escala Church", left, HEIGHT - 62f, brandPaint)

        // FREE plan watermark - discreet, per "marca d'água discreta no plano FREE"; paid plans
        // omit it entirely rather than showing a faded/disabled version of it.
        if (isFreePlan) {
            val watermarkPaint = textPaint(ColorUtils.setAlphaComponent(STARDUST, 90), 24f)
            canvas.drawText("Gerado com o app Escala Church", WIDTH - MARGIN - 360f, HEIGHT - 62f, watermarkPaint)
        }

        return bitmap
    }

    fun writePng(context: Context, scale: ScaleItem, churchName: String, churchCode: String, publicUrl: String, isFreePlan: Boolean): File {
        val bitmap = renderBitmap(scale, churchName, churchCode, publicUrl, isFreePlan)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeSlug = churchCode.ifBlank { "igreja" }.replace(Regex("[^A-Za-z0-9]+"), "-").lowercase()
        val file = File(dir, "escala-$safeSlug-${scale.date}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    private fun cutCornerPath(left: Float, top: Float, right: Float, bottom: Float, cut: Float): Path = Path().apply {
        moveTo(left, top + cut)
        lineTo(left + cut, top)
        lineTo(right, top)
        lineTo(right, bottom - cut)
        lineTo(right - cut, bottom)
        lineTo(left, bottom)
        close()
    }

    private fun drawConstellation(canvas: Canvas, cx: Float, cy: Float) {
        val points = listOf(0f to 0f, 60f to -90f, -40f to -70f, 90f to -30f, 130f to -110f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = ColorUtils.setAlphaComponent(ACCENT, 55)
        }
        for (i in 0 until points.size - 1) {
            canvas.drawLine(cx + points[i].first, cy + points[i].second, cx + points[i + 1].first, cy + points[i + 1].second, linePaint)
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(ACCENT, 130) }
        points.forEach { (dx, dy) -> canvas.drawCircle(cx + dx, cy + dy, 5f, dotPaint) }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val inner = r * 0.32f
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + inner, cy - inner)
            lineTo(cx + r, cy)
            lineTo(cx + inner, cy + inner)
            lineTo(cx, cy + r)
            lineTo(cx - inner, cy + inner)
            lineTo(cx - r, cy)
            lineTo(cx - inner, cy - inner)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun textPaint(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        isFakeBoldText = bold
    }

    /** Word-wraps into at most [maxLines] lines, truncating the last with an ellipsis rather than
     *  letting a very long title overflow past the footer - "nunca esconder completamente o nome
     *  responsável" is about role names (a fixed, short list), not the free-text title. */
    private fun drawWrapped(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, lineHeight: Float, paint: Paint, maxLines: Int): Float {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current)
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines.add(current)

        var y = startY
        lines.take(maxLines).forEachIndexed { index, line ->
            val isLastRenderedLine = index == maxLines - 1 && lines.size > maxLines
            val displayLine = if (isLastRenderedLine) line.trimEnd() + "…" else line
            canvas.drawText(displayLine, x, y, paint)
            y += lineHeight
        }
        return y
    }
}
