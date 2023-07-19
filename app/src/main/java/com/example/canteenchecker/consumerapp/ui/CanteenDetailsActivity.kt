package com.example.canteenchecker.consumerapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.canteenchecker.consumerapp.R
import com.example.canteenchecker.consumerapp.core.CanteenDetails
import com.example.canteenchecker.consumerapp.databinding.ActivityCanteenDetailsBinding
import com.example.canteenchecker.consumerapp.proxy.Broadcasting
import com.example.canteenchecker.consumerapp.proxy.ServiceProxyFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

class CanteenDetailsActivity : AppCompatActivity() {

    companion object {
        private const val CANTEEN_ID_KEY = "CanteenId"
        private const val MAKE_PHONE_CALL_REQUEST_CODE = 1
        private const val DEFAULT_MAP_ZOOM_FACTOR = 15F

        fun createIntent(context: Context?, canteenId: String?): Intent {
            val intent = Intent(context, CanteenDetailsActivity::class.java)
            intent.putExtra(CANTEEN_ID_KEY, canteenId)
            return intent
        }
    }

    private lateinit var binding: ActivityCanteenDetailsBinding

    private var canteen: CanteenDetails? = null

    private var mpfMap: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCanteenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // google maps
        mpfMap = supportFragmentManager.findFragmentById(R.id.mpfMap) as SupportMapFragment?
        mpfMap?.getMapAsync { googleMap: GoogleMap ->
            // disable gestures (since map is in a scroll viewer) and enable zoom controls instead
            val uiSettings = googleMap.uiSettings
            uiSettings.setAllGesturesEnabled(false)
            uiSettings.isZoomControlsEnabled = true
        }

        // create and add reviews fragment
        supportFragmentManager.beginTransaction().replace(R.id.lnlReviews, ReviewsFragment.newInstance(getCanteenId()!!)).commit()

        lifecycleScope.launch {
            Broadcasting.events
                .filter { it == getCanteenId() }
                .collectLatest { updateCanteenDetails() }
        }

        updateCanteenDetails()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_canteen_details, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        //super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.mniCall)!!.isVisible = canteen?.phoneNumber != null && canteen!!.phoneNumber!!.isNotEmpty()
        menu.findItem(R.id.mniShowWebsite)!!.isVisible = canteen?.website != null && canteen!!.website!!.isNotEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mniCall -> {
                makePhoneCall()
                true
            }
            R.id.mniShowWebsite -> {
                showWebsite()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MAKE_PHONE_CALL_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall()
            } else {
                Toast.makeText(this, R.string.message_no_call_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun makePhoneCall() {
        // check if permission for making phone cass has been granted
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), MAKE_PHONE_CALL_REQUEST_CODE)
        } else {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse(String.format("tel:%s", canteen!!.phoneNumber)))
            startActivity(intent)
        }*/

        // no permission CALL_PHONE in AndroidManifest needed
        //val intent = Intent(Intent.ACTION_DIAL, Uri.parse(String.format("tel:%s", canteen!!.phoneNumber)))
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${canteen!!.phoneNumber}"))
        startActivity(intent)
    }

    private fun showWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(canteen!!.website))
        // permission QUERY_ALL_PACKAGES in AndroidManifest
        if (packageManager.queryIntentActivities(intent, 0).size > 0) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.message_show_website_not_possible, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCanteenId(): String? {
        return intent.getStringExtra(CANTEEN_ID_KEY)
    }

    private fun updateCanteenDetails() {
        // kotlin coroutines
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            error("Exception: ${throwable.localizedMessage}")
        }
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            // background work here
            val canteenDetails = ServiceProxyFactory.createProxy().getCanteen(getCanteenId())
            withContext(Dispatchers.Main) {
                // UI thread work here
                // make UI visible
                binding.viwProgress.visibility = View.GONE
                binding.viwContent.visibility = View.VISIBLE

                // update action bar
                invalidateOptionsMenu()

                // display details
                if (canteenDetails != null) {
                    canteen = canteenDetails
                    title = canteenDetails.name
                    binding.txvName.text = canteenDetails.name
                    binding.txvDish.text = canteenDetails.dish
                    binding.txvDishPrice.text = canteenDetails.dishPrice.toString()
                    binding.txvLocation.text = canteenDetails.location
                    binding.txvWaitingTime.text = canteenDetails.waitingTime.toString()
                    binding.prbWaitingTime.progress = canteenDetails.waitingTime

                    // determine location and update map
                    CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
                        // background work here
                        val locationString = canteenDetails.location
                        // try to determine canteen location
                        var location: LatLng? = null
                        val geocoder = Geocoder(this@CanteenDetailsActivity)
                        val addresses = if (locationString == null) null
                                        else geocoder.getFromLocationName(locationString, 1)
                        if (addresses != null && addresses.size > 0) {
                            val address = addresses[0]
                            location = LatLng(address.latitude, address.longitude)
                        } else {
                            Log.w(CanteenDetailsActivity::class.java.name, "Resolving of location for location name $locationString failed.")
                        }

                        withContext(Dispatchers.Main) {
                            // UI thread work here
                            // update map
                            mpfMap?.getMapAsync { googleMap: GoogleMap ->
                                googleMap.clear()
                                if (location != null) {
                                    googleMap.addMarker(MarkerOptions().position(location))
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_MAP_ZOOM_FACTOR))
                                } else {
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 0F))
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, R.string.message_canteen_not_found, Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


}