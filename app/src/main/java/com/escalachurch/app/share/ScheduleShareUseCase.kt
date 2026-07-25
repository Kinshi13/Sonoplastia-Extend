package com.escalachurch.app.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android replica of the web's schedule "Compartilhar" action (Export Studio). Same
 * nullable-file/always-text-fallback shape as [ChurchShareImageUseCase] - the FREE plan can always
 * share the text/link; the visual PNG card ([imageFile]) is only ever present when the caller
 * already generated one (gated by FeatureKey.EXPORT, same as the existing PDF/JPEG export - see
 * ScheduleShareBottomSheet), never generated implicitly here.
 */
object ScheduleShareUseCase {

    fun share(context: Context, imageFile: File?, message: String) {
        val intent = if (imageFile != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
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
        context.startActivity(Intent.createChooser(intent, "Compartilhar escala"))
    }
}
