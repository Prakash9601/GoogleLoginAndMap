package com.softland.googleloginandmap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.softland.googleloginandmap.data.LocationData
import io.realm.RealmResults

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LocationRepository = LocationRepository()
    val allLocations: LiveData<RealmResults<LocationData>> = repository.getAllLocations()

    fun getLocationsForUser(username: String): LiveData<RealmResults<LocationData>> {
        return repository.getUserLocations(username)
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
