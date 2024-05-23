package com.softland.googleloginandmap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.softland.googleloginandmap.data.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job


class MainActivityViewModel(private val app: Application, private val listener: OnSignInStartedListener): AndroidViewModel(app) {
    private var auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableLiveData<FirebaseUser>()

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val currentUser: LiveData<FirebaseUser> = _currentUser

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("329989568700-1abqss58ucb5p2kfmhtnoe9qnn8di2s1.apps.googleusercontent.com")
        .requestEmail()
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(app, gso)

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                _currentUser.value = auth.currentUser
            } else {
                _currentUser.value = null
            }
        }
    }

    fun signIn() {
        // Sign out any existing sessions to force the email chooser dialog
        googleSignInClient.signOut().addOnCompleteListener {
            listener.onSignInStarted(googleSignInClient)
        }
    }

    fun signOut(){
        auth.signOut()
        googleSignInClient.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}





@Suppress("UNCHECKED_CAST")
class MainActivityViewModelFactory(
        private val application: Application,
        private val listener: OnSignInStartedListener
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {

            return MainActivityViewModel(application, listener) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

interface OnSignInStartedListener {
    fun onSignInStarted(client: GoogleSignInClient?)
}