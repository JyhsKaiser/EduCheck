package com.jyhs.educheck.data.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM service skeleton. Pushes are sent by a Supabase Edge Function (FCM HTTP v1); the client never sends them.
 * Next stage:
 *  - onNewToken stores the device token in Firestore after sign-in.
 *  - onMessageReceived updates the local cache and shows the notification on its channel.
 */
class EduCheckFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received: type=" + message.data["type"])
    }

    private companion object {
        const val TAG = "EduCheckFcm"
    }
}
