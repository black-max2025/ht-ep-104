package app.ht104.control.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.ht104.control.MainActivity
import app.ht104.control.R

object Notifier {
    private const val CHANNEL = "alerts"

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, title: String, text: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        if (!PermissionHelper.allGranted(context, PermissionHelper.notificationRuntime())) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(id, n)
    }
}
