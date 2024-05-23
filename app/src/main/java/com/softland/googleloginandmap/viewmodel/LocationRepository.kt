package com.softland.googleloginandmap.viewmodel

import androidx.lifecycle.LiveData
import com.softland.googleloginandmap.data.LocationData
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

class LocationRepository {
    private val realm: Realm = Realm.getDefaultInstance()


    fun getAllLocations(): LiveData<RealmResults<LocationData>> {
        val results: RealmResults<LocationData> = realm.where(LocationData::class.java).findAllAsync()
        return RealmLiveData(results)
    }

    fun getUserLocations(username: String): LiveData<RealmResults<LocationData>> {
        val results: RealmResults<LocationData> = realm.where(LocationData::class.java)
            .equalTo("username", username)
            .findAllAsync()
        return RealmLiveData(results)
    }


    fun close() {
        realm.close()
    }
}

class RealmLiveData<T : RealmModel>(private val realmResults: RealmResults<T>) : LiveData<RealmResults<T>>() {
    private val listener = RealmChangeListener<RealmResults<T>> { results ->
        value = results
    }

    override fun onActive() {
        super.onActive()
        realmResults.addChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        realmResults.removeChangeListener(listener)
    }
}
