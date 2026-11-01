package com.jyhs.educheck.data.push

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.core.ui.rejectionReasonRes
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.repository.EntryNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** RF-A05: local notification on the student's own phone when the entry is confirmed. */
@Singleton
class LocalEntryNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : EntryNotifier {

    @SuppressLint("MissingPermission") // checked with areNotificationsEnabled()
    override fun notifyEntry(entryTimeMs: Long, method: PresenceMethod) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val time = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(entryTimeMs))
        val detail = context.getString(
            if (method == PresenceMethod.GEOFENCE_WIFI) R.string.entry_method_wifi else R.string.entry_method_gps,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHECK_INS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_entry_title))
            .setContentText(context.getString(R.string.notification_entry_text, time, detail))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** The queued entry could not be sent and was discarded: tell the student why. */
    @SuppressLint("MissingPermission") // checked with areNotificationsEnabled()
    override fun notifyEntryRejected(entryTimeMs: Long, reason: ErrorReason) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val time = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(entryTimeMs))
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHECK_INS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_rejected_title))
            .setContentText(context.getString(R.string.notification_rejected_text, time, context.getString(reason.rejectionReasonRes())))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(REJECTED_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val REJECTED_NOTIFICATION_ID = 1002
    }
}
