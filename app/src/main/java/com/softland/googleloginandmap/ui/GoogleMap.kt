package com.softland.googleloginandmap.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.softland.googleloginandmap.R
import com.softland.googleloginandmap.databinding.ActivityGoogleMapBinding
import com.softland.googleloginandmap.viewmodel.LocationViewModel
import com.softland.googleloginandmap.viewmodel.MainActivityViewModel
import com.softland.googleloginandmap.viewmodel.MainActivityViewModelFactory
import com.softland.googleloginandmap.viewmodel.OnSignInStartedListener
import com.softland.googleloginandmap.worker.LocationService
import io.realm.Realm

class GoogleMap : AppCompatActivity() , OnMapReadyCallback {
    private lateinit var binding: ActivityGoogleMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    var latLng: LatLng?= null
    private lateinit var realm: Realm
    private lateinit var locationViewModel: LocationViewModel
    private val squarePoints = mutableListOf<LatLng>()
    private lateinit var viewModel: MainActivityViewModel
    var displayName=""
    private var sharedPreferences: SharedPreferences? = null
    private var isSquareDrawn = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        val application = requireNotNull(this).application
        val factory = MainActivityViewModelFactory(application, object : OnSignInStartedListener {
            override fun onSignInStarted(client: GoogleSignInClient?) {
                client?.signInIntent?.let { startActivityForResult(it, MainActivity.RC_SIGN_IN) }
            }
        })
        viewModel = ViewModelProvider(this, factory)[MainActivityViewModel::class.java]

        // Create location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds interval
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        binding.btnSave.setOnClickListener {
            viewModel.signOut()
            stopLocationService()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            ),
            0
        )

        // Initialize location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations) {
                    // Update map with new location
                     latLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng!!, 15f))

                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.myMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            return
        }
        // Enable location layer on map
        mMap.isMyLocationEnabled = true
        // Start location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)



        sharedPreferences= getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val username = sharedPreferences!!.getString("username", "")

      //  Toast.makeText(applicationContext,displayName,Toast.LENGTH_SHORT).show()

        // Observer for location data
        locationViewModel.getLocationsForUser(username?:"").observe(this) { locations ->
            // Check if the locations data is not null
            if (locations.isNotEmpty() && !isSquareDrawn) {
                // Clear the existing points to avoid duplication
                squarePoints.clear()
                // Loop through each location and add it to squarePoints
                for (location in locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    squarePoints.add(latLng)
                }
                drawSquare(googleMap,squarePoints)
                isSquareDrawn = true

            }
        }
    }


    private fun drawSquare(googleMap: GoogleMap, squarePoints: MutableList<LatLng>) {
        val lineOptions = PolylineOptions()
            .addAll(squarePoints)
            .color(Color.RED)
            .width(5f)
        // Remove any existing polygons before adding a new one
        googleMap.let {
            it.addPolyline(lineOptions)
        }

        // Move the camera to the first point in the list with a specific zoom level
        if (squarePoints.isNotEmpty()) {
            val firstPoint = squarePoints[0]
            val zoomLevel = 15f
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPoint, zoomLevel))
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
                mMap.isMyLocationEnabled = true
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } else {
                // Permission denied, show a toast message
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun stopLocationService() {
        Intent(this, MainActivity::class.java).apply {
            putExtra("DISPLAY_NAME", displayName)
            startActivity(this)
            finish() // Optional: finish MainActivity so the user can't navigate back to it
        }
    }




}