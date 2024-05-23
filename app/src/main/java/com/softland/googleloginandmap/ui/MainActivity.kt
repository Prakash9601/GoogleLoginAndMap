package com.softland.googleloginandmap.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.softland.googleloginandmap.databinding.ActivityMainBinding
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.softland.googleloginandmap.viewmodel.MainActivityViewModel
import com.softland.googleloginandmap.viewmodel.MainActivityViewModelFactory
import com.softland.googleloginandmap.viewmodel.OnSignInStartedListener
import com.softland.googleloginandmap.worker.LocationService

class MainActivity : AppCompatActivity() {
    companion object {
        private const val RC_LOCATION_PERMISSION = 1
        private const val RC_NOTIFICATION_PERMISSION = 2
        const val RC_SIGN_IN = 9001
    }
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private var sharedPreferences: SharedPreferences? = null

    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        checkPermissions()


        // Get an instance of SharedPreferences
        sharedPreferences= getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val username = sharedPreferences!!.getString("username", "")


        if(!username.isNullOrEmpty()){
            startLocationService(username)
            navigateToGoogleMap(username)
        }


        val application = requireNotNull(this).application
        val factory = MainActivityViewModelFactory(application, object : OnSignInStartedListener {
            override fun onSignInStarted(client: GoogleSignInClient?) {
                client?.signInIntent?.let { startActivityForResult(it, RC_SIGN_IN) }
            }
        })
        viewModel = ViewModelProvider(this, factory)[MainActivityViewModel::class.java]



        binding.button.setOnClickListener {
            viewModel.signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK && data != null) {
            // this task is responsible for getting ACCOUNT SELECTED
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                viewModel.firebaseAuthWithGoogle(account.idToken!!)
                viewModel.currentUser.observe(this){
                    // it.displayName
                    startLocationService(it.uid)
                    navigateToGoogleMap(it.uid)
                }
                Toast.makeText(this, "Signed In Successfully", Toast.LENGTH_SHORT).show()

            } catch (e: ApiException) {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToGoogleMap(displayName: String?) {
        // Save data to SharedPreferences
        val editor = sharedPreferences?.edit()
        editor?.putString("username",displayName )
        editor?.apply()


        Intent(this, SaveLocation::class.java).apply {
            putExtra("DISPLAY_NAME", displayName)
            startActivity(this)
            finish() // Optional: finish MainActivity so the user can't navigate back to it
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_SIGN_IN) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService(displayName: String?) {
        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra("DISPLAY_NAME", displayName)
            startService(this)
        }
    }

    fun checkPermissions() {
        // Check Location Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RC_LOCATION_PERMISSION
            )
        }

        // Check Notification Permission (for Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    RC_NOTIFICATION_PERMISSION
                )
            }
        } else {
            // For earlier Android versions, ensure the notifications are enabled via the system settings.
            // Typically handled by your notification logic elsewhere in your app.
        }
    }

}

