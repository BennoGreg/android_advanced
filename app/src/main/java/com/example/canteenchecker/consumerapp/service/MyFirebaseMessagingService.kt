package com.example.canteenchecker.consumerapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.canteenchecker.consumerapp.proxy.Broadcasting
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val REMOTE_MESSAGE_TOPIC = "CanteenUpdates"
        private const val REMOTE_MESSAGE_CANTEEN_ID_KEY = "canteenId"

        fun subscribeToCanteenUpdates() {
            FirebaseMessaging.getInstance().subscribeToTopic(REMOTE_MESSAGE_TOPIC)
        }
    }

    override fun onNewToken(token: String) {
        subscribeToCanteenUpdates()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.containsKey(REMOTE_MESSAGE_CANTEEN_ID_KEY)) {
            val canteenId = data[REMOTE_MESSAGE_CANTEEN_ID_KEY]
            if (canteenId != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    Broadcasting.invokeEvent(canteenId)
                }
            }
        }
    }

}