package com.softland.googleloginandmap.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*


open class LocationData(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var username:String ="",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var timestamp: Date = Date()
) : RealmObject()
