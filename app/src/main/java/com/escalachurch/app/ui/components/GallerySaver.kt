package com.escalachurch.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/**
 * "Baixar imagem" for a generated schedule/announcement share card - saves straight to the public
 * gallery via MediaStore (scoped storage, Android 10+ `RELATIVE_PATH`), the same mechanism the web
 * spec asked for ("usar MediaStore... não solicitar permissão ampla... não usar
 * MANAGE_EXTERNAL_STORAGE"). No storage permission is requested anywhere in this file - scoped
 * storage doesn't need one for an app writing its own MediaStore entries.
 */
object GallerySaver {

    /** Returns true on success. Never throws - a failed save should show a friendly error, not
     *  crash the share flow. */
    fun savePngToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Escala Church")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
