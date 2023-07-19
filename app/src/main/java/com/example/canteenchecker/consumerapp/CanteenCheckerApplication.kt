package com.example.canteenchecker.consumerapp

import android.app.Application
import com.example.canteenchecker.consumerapp.service.MyFirebaseMessagingService

class CanteenCheckerApplication: Application() {

    @get:Synchronized
    @set:Synchronized
    var authenticationToken: String? = null

    @get:Synchronized
    val isAuthenticated: Boolean
        get() = authenticationToken != null

    override fun onCreate() {
        super.onCreate()

        MyFirebaseMessagingService.subscribeToCanteenUpdates()
    }

}