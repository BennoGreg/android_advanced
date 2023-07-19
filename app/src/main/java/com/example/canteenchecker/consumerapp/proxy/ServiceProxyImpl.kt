package com.example.canteenchecker.consumerapp.proxy

import com.example.canteenchecker.consumerapp.core.Canteen
import com.example.canteenchecker.consumerapp.core.CanteenDetails
import com.example.canteenchecker.consumerapp.core.ReviewData
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

// proxy implementation for API version 1.1
// https://moc5.projekte.fh-hagenberg.at/CanteenChecker/swagger/index.html
class ServiceProxyImpl : ServiceProxy {

    private val SERVICE_BASE_URL = "https://moc5.projekte.fh-hagenberg.at/CanteenChecker/api/"
    private val ARTIFICIAL_DELAY: Long = 100

    private val proxy: ServiceProxyImpl.Proxy = Retrofit.Builder()
        .baseUrl(SERVICE_BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create()) // for simple responses (auth token string)
        .addConverterFactory(GsonConverterFactory.create()) // for JSON responses
        .build()
        .create(ServiceProxyImpl.Proxy::class.java)

    private fun causeDelay() {
        try {
            Thread.sleep(ARTIFICIAL_DELAY)
        } catch (ignored: InterruptedException) {
        }
    }

    override suspend fun getCanteens(filter: String?): Collection<Canteen>? {
        causeDelay() // for testing only

        val canteens = proxy.getCanteens(filter)!!.execute().body() ?: return null
        val result: MutableCollection<Canteen> = ArrayList(canteens.size)
        for (canteen in canteens) {
            canteen?.let { result.add(it.toCanteen()) }
        }
        return result
    }

    override suspend fun getCanteen(canteenId: String?): CanteenDetails? {
        causeDelay() // for testing only

        val canteen = proxy.getCanteen(canteenId)?.execute()?.body()
        return canteen?.toCanteenDetails()
    }

    override suspend fun getReviewsDataForCanteen(canteenId: String?): ReviewData? {
        causeDelay() // for testing only

        val reviewData = proxy.getReviewStatisticsForCanteen(canteenId)?.execute()?.body()
        return reviewData?.toReviewData()
    }

    override suspend fun authenticate(userName: String?, password: String?): String? {
        causeDelay(); // for testing only
        return proxy.postAuthenticate(userName, password)?.execute()?.body();
    }

    override suspend fun createReview(authToken: String?, canteenId: String?, rating: Int, remark: String?) {
        causeDelay() // for testing only

        proxy.postCanteenReview(String.format("Bearer %s", authToken), canteenId, rating, remark)?.execute()
    }

    private interface Proxy {
        @POST("authenticate")
        fun postAuthenticate(
            @Query("userName") userName: String?,
            @Query("password") password: String?
        ): Call<String?>?

        @GET("canteens")
        fun getCanteens(@Query("name") name: String?): Call<Collection<ServiceProxyImpl.Proxy_CanteenData?>?>?

        @GET("canteens/{canteenId}")
        fun getCanteen(@Path("canteenId") canteenId: String?): Call<ServiceProxyImpl.Proxy_CanteenDetails?>?

        @GET("canteens/{canteenId}/review-statistics")
        fun getReviewStatisticsForCanteen(@Path("canteenId") canteenId: String?): Call<ServiceProxyImpl.Proxy_CanteenReviewStatistics?>?

        @POST("canteens/{canteenId}/reviews")
        fun postCanteenReview(
            @Header("Authorization") authenticationToken: String?,
            @Path("canteenId") canteenId: String?,
            @Query("rating") rating: Int,
            @Query("remark") remark: String?
        ): Call<Void?>?
    }

    private class Proxy_CanteenData {
        var id: String? = null
        var name: String? = null
        var dish: String? = null
        var dishPrice = 0f
        var averageRating = 0f
        fun toCanteen(): Canteen {
            return Canteen(id, name, dish, dishPrice, averageRating)
        }
    }

    private class Proxy_CanteenDetails {
        var name: String? = null
        var address: String? = null
        var phoneNumber: String? = null
        var website: String? = null
        var dish: String? = null
        var dishPrice = 0f
        var waitingTime = 0
        fun toCanteenDetails(): CanteenDetails {
            return CanteenDetails(
                name,
                phoneNumber, website, dish, dishPrice, address, waitingTime
            )
        }
    }

    private class Proxy_CanteenReviewStatistics {
        var countOneStar = 0
        var countTwoStars = 0
        var countThreeStars = 0
        var countFourStars = 0
        var countFiveStars = 0
        fun toReviewData(): ReviewData {
            return ReviewData(
                countOneStar,
                countTwoStars,
                countThreeStars,
                countFourStars,
                countFiveStars
            )
        }
    }
}