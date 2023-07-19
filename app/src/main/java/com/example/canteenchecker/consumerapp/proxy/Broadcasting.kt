package com.example.canteenchecker.consumerapp.proxy

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object Broadcasting {

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    suspend fun invokeEvent(canteenId: String) {
        _events.emit(canteenId)
    }
}