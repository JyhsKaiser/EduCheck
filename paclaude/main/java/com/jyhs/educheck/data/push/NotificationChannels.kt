package com.jyhs.educheck.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.jyhs.educheck.R

/** Notification channels; high FCM priority maps to IMPORTANCE_HIGH. */
object NotificationChannels {
    const val CHECK_INS = "check_ins"
    const val ABSENCES = "absences"
    const val JUSTIFICATIONS = "justifications"
    const val EMERGENCIES = "emergencies"
    const val ADMINISTRATION = "administration"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHECK_INS, context.getString(R.string.channel_check_ins), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(ABSENCES, context.getString(R.string.channel_absences), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(JUSTIFICATIONS, context.getString(R.string.channel_justifications), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(EMERGENCIES, context.getString(R.string.channel_emergencies), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(ADMINISTRATION, context.getString(R.string.channel_administration), NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }
}
