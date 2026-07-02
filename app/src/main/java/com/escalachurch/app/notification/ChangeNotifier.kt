package com.escalachurch.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.escalachurch.app.MainActivity
import com.escalachurch.app.R
import com.escalachurch.app.domain.model.ChangeLogEntry

private const val CHANNEL_ID = "escala_changes"

/** Fires a local notification right when an admin saves a change relevant to the user's classes. */
object ChangeNotifier {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alterações na escala",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos quando a escala oficial ou anúncios da sua função forem alterados."
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyChange(context: Context, entry: ChangeLogEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            entry.id.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(entry.title)
            .setContentText(entry.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(entry.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(entry.id.toInt(), notification)
    }
}
