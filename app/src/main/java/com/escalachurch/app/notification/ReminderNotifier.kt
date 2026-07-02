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
import com.escalachurch.app.MainActivity
import com.escalachurch.app.R
import com.escalachurch.app.domain.util.ScaleMatch

private const val CHANNEL_ID = "escala_reminders"

/**
 * Creates the notification channel and shows "you're on the scale" reminders.
 *
 * TODO(push): reminders are computed locally today by [com.escalachurch.app.notification.ReminderWorker]
 * polling the local database. Once a backend exists, consider moving the scheduling server-side
 * (e.g. a scheduled Cloud Function) and delivering via Firebase Cloud Messaging, so reminders
 * keep firing even if the app isn't opened for a while and battery-saver doesn't delay WorkManager.
 */
object ReminderNotifier {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de escala",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos quando você estiver escalado em uma função do culto."
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyMatch(context: Context, match: ScaleMatch, leadDescription: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val rolesText = match.roles.joinToString(", ")
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Você está escalado(a) — ${match.scale.title}")
            .setContentText("$rolesText · $leadDescription")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Função: $rolesText\n$leadDescription"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
