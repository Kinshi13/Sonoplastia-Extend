package com.escalachurch.app.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.export.ScaleExporter

/**
 * Fase 11.10 - "Compartilhar" in ShareChurchAccessBottomSheet: image (when there's a next scale to
 * render) + the church-access message text, in one native share sheet. Reuses ScaleExporter's
 * existing JPEG renderer (see its `writeJpeg`) instead of a second one - the phase's own
 * instruction ("utilizar a mesma imagem gerada pela exportação, não criar outro renderer").
 *
 * Falls back to text-only when there's no scale to render (Bloco: "compartilhar apenas texto" is
 * an explicit test case, not an error state).
 */
object ChurchShareImageUseCase {

    fun share(context: Context, nextScale: ScaleItem?, message: String) {
        val intent = if (nextScale != null) {
            val file = ScaleExporter.writeJpeg(context, nextScale)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar acesso da igreja"))
    }
}
