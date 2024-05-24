package com.softland.googleloginandmap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.realm.Realm
import io.realm.RealmConfiguration


class LocationApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config: RealmConfiguration =
            RealmConfiguration.Builder().name("Location.db").schemaVersion(1)
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .build()
        Realm.setDefaultConfiguration(config)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel("location", "location", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}