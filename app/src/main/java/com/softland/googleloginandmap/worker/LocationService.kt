package com.softland.googleloginandmap.worker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.softland.googleloginandmap.R
import com.softland.googleloginandmap.data.LocationData
import com.softland.googleloginandmap.interfaces.MyLocationClient
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class LocationService: Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var  myLocationClient: MyLocationClient


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        myLocationClient = MyDefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> {
                val displayName = intent.getStringExtra("DISPLAY_NAME")
                start(displayName)
            }
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(displayName: String?) {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking Location")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        myLocationClient.getLocationUpdates(1500000)
            .catch { e -> e.printStackTrace() }
            .onEach {
                val lat = it.latitude.toString()
                val long = it.longitude.toString()
                val updateNotification = notification.setContentText("Location: ($lat, $long)")
                notificationManager?.notify(1, updateNotification.build())

                insertLocationData(it, displayName)
            }.launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop(){
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    fun insertLocationData(location: Location, displayName: String?) {
        if (location != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    withContext(Dispatchers.IO) {
                        Realm.getDefaultInstance().use { realm ->
                            realm.executeTransaction { transactionRealm ->
                                val locationData = LocationData().apply {
                                    this.latitude = location.latitude
                                    this.longitude = location.longitude
                                    this.timestamp = Date()
                                    this.username = displayName ?: ""
                                }
                                transactionRealm.insert(locationData)
                            }
                        }
                    }
                    // Switch to Main thread to show the toast
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Data saved successfully.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    companion object{
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}