package com.example.canteenchecker.consumerapp.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.canteenchecker.consumerapp.CanteenCheckerApplication
import com.example.canteenchecker.consumerapp.R
import com.example.canteenchecker.consumerapp.databinding.FragmentReviewsBinding
import com.example.canteenchecker.consumerapp.proxy.Broadcasting
import com.example.canteenchecker.consumerapp.proxy.ServiceProxyFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CANTEEN_ID_KEY = "CanteenId"

class ReviewsFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(canteenId: String) =
            ReviewsFragment().apply {
                arguments = Bundle().apply {
                    putString(CANTEEN_ID_KEY, canteenId)
                }
            }
    }

    private lateinit var binding: FragmentReviewsBinding

    private var canteenId: String? = null

    private var loginActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            createReview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            canteenId = it.getString(CANTEEN_ID_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentReviewsBinding.inflate(inflater, container, false)

        binding.btnAddReview.setOnClickListener { createReview() }

        lifecycleScope.launch {
            Broadcasting.events
                .filter { it == canteenId }
                .collectLatest { updateReviews() }
        }

        updateReviews()

        return binding.root
    }

    private fun updateReviews() {
        // kotlin coroutines
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            error("Exception: ${throwable.localizedMessage}")
        }
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            // background work here
            val reviewData = ServiceProxyFactory.createProxy().getReviewsDataForCanteen(canteenId)
            withContext(Dispatchers.Main) {
                // UI thread work here
                if (reviewData != null) {
                    binding.txvAverageRating.text = "%.2f".format(reviewData.averageRating)
                    binding.rtbAverageRating.rating = reviewData.averageRating
                    binding.txvTotalRatings.text = reviewData.totalRatings.toString()
                    binding.prbRatingsOne.max = reviewData.totalRatings
                    binding.prbRatingsTwo.max = reviewData.totalRatings
                    binding.prbRatingsThree.max = reviewData.totalRatings
                    binding.prbRatingsFour.max = reviewData.totalRatings
                    binding.prbRatingsFive.max = reviewData.totalRatings
                    binding.prbRatingsOne.progress = reviewData.ratingsOne
                    binding.prbRatingsTwo.progress = reviewData.ratingsTwo
                    binding.prbRatingsThree.progress = reviewData.ratingsThree
                    binding.prbRatingsFour.progress = reviewData.ratingsFour
                    binding.prbRatingsFive.progress = reviewData.ratingsFive
                } else {
                    binding.txvAverageRating.text = null
                    binding.rtbAverageRating.rating = 0f
                    binding.txvTotalRatings.text = null
                    binding.prbRatingsOne.max = 1
                    binding.prbRatingsTwo.max = 1
                    binding.prbRatingsThree.max = 1
                    binding.prbRatingsFour.max = 1
                    binding.prbRatingsFive.max = 1
                    binding.prbRatingsOne.progress = 0
                    binding.prbRatingsTwo.progress = 0
                    binding.prbRatingsThree.progress = 0
                    binding.prbRatingsFour.progress = 0
                    binding.prbRatingsFive.progress = 0
                }
            }
        }
    }

    private fun createReview() {
        // check if user is logged in
        val isAuthenticated = (activity?.application as CanteenCheckerApplication).isAuthenticated
        if (!isAuthenticated) {
            // ask user to log in
            loginActivityResultLauncher.launch(LoginActivity.createIntent(this.context))
        } else {
            // user is authenticated
            // show dialog to collect review data
            val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_add_review, null)
            AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_add_review)
                .setView(view)
                .setPositiveButton(R.string.text_send) { dialog, which ->
                    // dismiss dialog and submit review
                    dialog.dismiss()

                    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                        Toast.makeText(activity, R.string.message_review_not_created, Toast.LENGTH_SHORT).show()
                        error("Exception: ${throwable.localizedMessage}")
                    }
                    CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
                        // background work here
                        val authToken = (activity?.application as CanteenCheckerApplication).authenticationToken
                        val rating = view.findViewById<RatingBar>(R.id.rtbRating).rating.toInt()
                        val remark = view.findViewById<EditText>(R.id.edtRemark).text.toString()
                        ServiceProxyFactory.createProxy().createReview(authToken, canteenId, rating, remark)
                        withContext(Dispatchers.Main) {
                            // UI thread work here
                            Toast.makeText(activity, R.string.message_review_created, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .create()
                .show()
        }

    // kotlin coroutines

    }
}