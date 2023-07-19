package com.example.canteenchecker.consumerapp.proxy

import com.example.canteenchecker.consumerapp.core.Canteen
import com.example.canteenchecker.consumerapp.core.CanteenDetails
import com.example.canteenchecker.consumerapp.core.ReviewData
import java.io.IOException

interface ServiceProxy {

    @Throws(IOException::class)
    suspend fun getCanteens(filter: String?): Collection<Canteen>?

    @Throws(IOException::class)
    suspend fun getCanteen(canteenId: String?): CanteenDetails?

    @Throws(IOException::class)
    suspend fun getReviewsDataForCanteen(canteenId: String?): ReviewData?

    @Throws(IOException::class)
    suspend fun authenticate(userName: String?, password: String?): String?

    @Throws(IOException::class)
    suspend fun createReview(authToken: String?, canteenId: String?, rating: Int, remark: String?)
}