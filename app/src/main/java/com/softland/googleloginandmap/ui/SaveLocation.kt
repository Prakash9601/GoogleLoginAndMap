package com.softland.googleloginandmap.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.softland.googleloginandmap.adapter.LocationAdapter
import com.softland.googleloginandmap.databinding.ActivitySaveLocationBinding
import com.softland.googleloginandmap.viewmodel.LocationViewModel
import com.softland.googleloginandmap.viewmodel.MainActivityViewModel
import com.softland.googleloginandmap.viewmodel.MainActivityViewModelFactory
import com.softland.googleloginandmap.viewmodel.OnSignInStartedListener
import com.softland.googleloginandmap.worker.LocationService

class SaveLocation : AppCompatActivity() {
    private lateinit var binding: ActivitySaveLocationBinding
    private lateinit var viewModel: LocationViewModel
    private lateinit var viewModelMain: MainActivityViewModel
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaveLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAndPromptForGPS()


        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        sharedPreferences= getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val username = sharedPreferences!!.getString("username", "")


        val application = requireNotNull(this).application
        val factory = MainActivityViewModelFactory(application, object : OnSignInStartedListener {
            override fun onSignInStarted(client: GoogleSignInClient?) {
                client?.signInIntent?.let { startActivityForResult(it, MainActivity.RC_SIGN_IN) }
            }
        })
        viewModelMain = ViewModelProvider(this, factory)[MainActivityViewModel::class.java]


        viewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        // Set the layout manager to the RecyclerView
        binding.recyclerView.layoutManager = layoutManager




        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            ),
            0
        )

        viewModel.getLocationsForUser(username?:"").observe(this@SaveLocation){
            val adapter = LocationAdapter(application, it,this@SaveLocation)
            binding.recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        binding.logOut.setOnClickListener {
            viewModelMain.signOut()
            showLogoutConfirmationDialog()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location layer on map and start location updates
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }


            } else {
                // Permission denied, show a toast message
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == MainActivity.REQUEST_CHECK_SETTINGS) {
            when (requestCode) {
                RESULT_OK -> {
                    // User agreed to make required location settings changes.
                    Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show()
                }

                RESULT_CANCELED -> {
                    // User chose not to make required location settings changes.
                    Toast.makeText(this, "GPS not enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { dialog, which ->
            // Handle the logout process here
            stopLocationService()
        }
        builder.setNegativeButton("No") { dialog, which ->
            // Dismiss the dialog
            dialog.dismiss()
        }
        builder.setCancelable(false) // Prevent dismissing by tapping outside the dialog
        builder.show()
    }

    private fun stopLocationService() {
        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val clearEditor = sharedPreferences.edit()
        clearEditor.clear()
        clearEditor.apply()

        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            startService(this)
        }

        Intent(applicationContext, MainActivity::class.java).apply {
            startActivity(this)
            finish()
        }
    }

    private fun checkAndPromptForGPS() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            if (states?.isLocationPresent == true && states.isLocationUsable) {
                // GPS is already enabled
            } else {
                // GPS is not enabled but this block won't be called due to addOnSuccessListener
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    exception.startResolutionForResult(this, MainActivity.REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            } else {
                Snackbar.make(binding.root, "Location settings are inadequate, and cannot be fixed here. Fix in Settings.", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}