package com.escalachurch.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import java.io.File
import java.io.FileOutputStream

/**
 * Android replica of the web's `AnnouncementShareButton` (Web Fase - "Compartilhamento de
 * anúncios"). Reuses Coil (already the app's image-loading dependency, via [imageLoader]) to fetch
 * the announcement's own remote image into a local cache file instead of adding a second HTTP
 * client - the same "não criar outro renderer" spirit as [ScaleExporter] being reused by
 * [ChurchShareImageUseCase] instead of a parallel one.
 *
 * Always falls back to a text-only share (never throws, never blocks on a failed image): a video
 * announcement, a missing image, a failed download, or offline all degrade to the same text/link
 * share that always works, per "compartilhamento básico" never being allowed to fail silently.
 */
object AnnouncementShareUseCase {

    suspend fun share(context: Context, announcement: Announcement, message: String) {
        val imageFile = if (announcement.mediaType == MediaType.IMAGE && !announcement.mediaUrl.isNullOrBlank()) {
            downloadToCache(context, announcement.mediaUrl, announcement.id)
        } else {
            null
        }

        val intent = if (imageFile != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
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
        context.startActivity(Intent.createChooser(intent, "Compartilhar anúncio"))
    }

    /** Returns null (never throws) on any failure - a bad/expired/offline image URL degrades to a
     *  text-only share instead of blocking it. */
    private suspend fun downloadToCache(context: Context, url: String, announcementId: String): File? {
        return try {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val result = context.imageLoader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: result.drawable?.toBitmap() ?: return null

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "anuncio-$announcementId.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            file
        } catch (_: Exception) {
            null
        }
    }
}
