package com.jyhs.educheck.data.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Geofences are cleared on reboot and app updates: register them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManagerPresenceTracker.enqueueSetup(context)
        }
    }
}
