package com.example.canteenchecker.consumerapp.proxy

object ServiceProxyFactory {

    fun createProxy(): ServiceProxy {
        return ServiceProxyImpl()
    }
}