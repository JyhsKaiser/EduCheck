package com.jyhs.educheck.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.jyhs.educheck.R

/** Canales de notificación; la prioridad FCM alta se corresponde con IMPORTANCE_HIGH. */
object NotificationChannels {
    const val INGRESOS = "ingresos"
    const val INASISTENCIAS = "inasistencias"
    const val JUSTIFICANTES = "justificantes"
    const val EMERGENCIAS = "emergencias"
    const val ADMINISTRACION = "administracion"

    fun crear(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(INGRESOS, context.getString(R.string.canal_ingresos), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(INASISTENCIAS, context.getString(R.string.canal_inasistencias), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(JUSTIFICANTES, context.getString(R.string.canal_justificantes), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(EMERGENCIAS, context.getString(R.string.canal_emergencias), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(ADMINISTRACION, context.getString(R.string.canal_administracion), NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }
}
