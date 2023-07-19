package com.example.canteenchecker.consumerapp.core

data class ReviewData(val ratingsOne: Int, val ratingsTwo: Int, val ratingsThree: Int, val ratingsFour: Int, val ratingsFive: Int) {

    val totalRatings: Int = ratingsOne + ratingsTwo + ratingsThree + ratingsFour + ratingsFive
    val averageRating: Float

    init {
        averageRating = if (totalRatings == 0) 0F
        else (ratingsOne + ratingsTwo*2 + ratingsThree*3 + ratingsFour*4 + ratingsFive*5) / totalRatings.toFloat()
    }
}
