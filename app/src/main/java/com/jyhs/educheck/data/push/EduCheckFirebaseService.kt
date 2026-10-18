package com.jyhs.educheck.data.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Esqueleto del servicio FCM. En la siguiente etapa:
 *  - onNewToken guardará el token del dispositivo en Firestore/backend tras el login.
 *  - onMessageReceived actualizará la caché local y mostrará la notificación por canal.
 */
class EduCheckFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nuevo token FCM recibido")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Mensaje FCM recibido: tipo=" + message.data["tipo"])
    }

    private companion object {
        const val TAG = "EduCheckFcm"
    }
}
