package com.escalachurch.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.components.dayOfWeekLabel
import com.escalachurch.app.ui.components.toDisplayString
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat { PDF, JPEG }

/**
 * Renders a [ScaleItem] as a shareable image or PDF, matching the app's card look (rounded
 * corners, blue accents) via plain Canvas drawing - independent of whichever Compose version is
 * in use, so it doesn't need the newer graphicsLayer capture APIs.
 */
object ScaleExporter {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1400
    private const val MARGIN = 64f

    private val roles: (ScaleItem) -> List<Pair<String, String>> = { scale ->
        listOf(
            "Recepção" to scale.receptionPerson,
            "Sonoplastia" to scale.soundPerson,
            "Pregação" to scale.preachingPerson,
            "Regência" to scale.conductingPerson,
            "Mensagem musical" to scale.musicalMessagePerson
        ).filter { it.second.isNotBlank() }
    }

    /** Shares the scale as the requested [format] via the Android share sheet. */
    fun share(context: Context, scale: ScaleItem, format: ExportFormat) {
        val file = when (format) {
            ExportFormat.JPEG -> writeJpeg(context, scale)
            ExportFormat.PDF -> writePdf(context, scale)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (format == ExportFormat.JPEG) "image/jpeg" else "application/pdf"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar escala"))
    }

    private fun writeJpeg(context: Context, scale: ScaleItem): File {
        val bitmap = renderBitmap(scale)
        val file = exportFile(context, scale, "jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        return file
    }

    private fun writePdf(context: Context, scale: ScaleItem): File {
        val bitmap = renderBitmap(scale)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(CARD_WIDTH, CARD_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        val file = exportFile(context, scale, "pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    private fun exportFile(context: Context, scale: ScaleItem, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = scale.title.ifBlank { "escala" }.replace(Regex("[^A-Za-z0-9]+"), "-").lowercase()
        return File(dir, "$safeName-${scale.date}.$extension")
    }

    private fun renderBitmap(scale: ScaleItem): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Color.parseColor("#F7F9FC")
        val cardColor = Color.WHITE
        val primary = Color.parseColor("#3B7DDD")
        val textPrimary = Color.parseColor("#17233D")
        val textSecondary = Color.parseColor("#64748B")

        canvas.drawColor(background)

        val cardRect = RectF(MARGIN, MARGIN, CARD_WIDTH - MARGIN, CARD_HEIGHT - MARGIN)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardColor; setShadowLayer(24f, 0f, 8f, Color.parseColor("#22000000")) }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        var y = MARGIN + 96f
        val left = MARGIN + 56f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary; textSize = 34f; isFakeBoldText = true }
        canvas.drawText("PRÓXIMA ESCALA", left, y, labelPaint)
        y += 70f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textPrimary; textSize = 56f; isFakeBoldText = true }
        canvas.drawText(scale.title, left, y, titlePaint)
        y += 70f

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textSecondary; textSize = 36f }
        canvas.drawText("${scale.date.dayOfWeekLabel()} · ${scale.date.toDisplayString()}", left, y, subtitlePaint)
        y += 50f
        canvas.drawText(scale.startTime.toDisplayString(), left, y, subtitlePaint)
        y += 90f

        val roleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textSecondary; textSize = 28f }
        val rolePersonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textPrimary; textSize = 40f }

        roles(scale).forEach { (label, person) ->
            canvas.drawText(label, left, y, roleLabelPaint)
            y += 46f
            canvas.drawText(person, left, y, rolePersonPaint)
            y += 64f
        }

        if (scale.notes.isNotBlank()) {
            y += 20f
            canvas.drawText("Observações", left, y, labelPaint.apply { textSize = 30f })
            y += 44f
            canvas.drawText(scale.notes, left, y, subtitlePaint)
        }

        return bitmap
    }
}
