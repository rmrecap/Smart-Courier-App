package com.smartcourier.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartcourier.core.data.sync.SyncScheduler

class SmartCourierMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val action = message.data[ACTION_KEY]
        val priority = message.priority

        log("Received FCM message: action=$action, priority=$priority, data=${message.data}")

        if (action == ACTION_TRIGGER_SYNC && priority == RemoteMessage.PRIORITY_HIGH) {
            log("High-priority TRIGGER_SYNC received — launching immediate sync")
            SyncScheduler.scheduleImmediate(applicationContext)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        log("FCM token refreshed: $token")
    }

    private fun log(msg: String) = Log.d(TAG, msg)

    companion object {
        private const val TAG = "FCMessagingService"
        private const val ACTION_KEY = "action"
        private const val ACTION_TRIGGER_SYNC = "TRIGGER_SYNC"
    }
}
