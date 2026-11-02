package com.jyhs.educheck.data.push

import android.annotation.SuppressLint
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives the pushes sent by the backend (FCM HTTP v1, data messages with `type`, `title` and `body`) and shows
 * them in the channel of their type. The client never sends pushes.
 */
class EduCheckFirebaseService : FirebaseMessagingService() {

    /** FCM rotated the token: register the new one for the signed-in user. */
    override fun onNewToken(token: String) {
        WorkManagerPushTokenScheduler.enqueue(applicationContext)
    }

    @SuppressLint("MissingPermission") // checked with areNotificationsEnabled()
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()

        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return

        // Tapping the notification opens the app.
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(this, NotificationChannels.forType(data["type"]))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        // One notification per event (the same push delivered twice replaces itself).
        manager.notify((data["presenceId"] ?: (title + body)).hashCode(), notification)
    }
}
