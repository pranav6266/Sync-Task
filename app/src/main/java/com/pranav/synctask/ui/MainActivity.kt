package com.pranav.synctask.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pranav.synctask.R
import com.pranav.synctask.data.UserRepository

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    // Old-school Google Sign In setup (migrated to Kotlin)
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Handle error (Log it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        val startDestination = if (auth.currentUser != null) "main" else "login"

        setContent {
            // This is your new App Entry Point
            AppNavigation(
                startDestination = startDestination,
                onLoginClick = { launchGoogleSignIn() }
            )
        }
    }

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sync user to your Firestore (Using your existing Java Repo)
                    UserRepository.getInstance().createOrUpdateUser(auth.currentUser)
                    // Refresh UI (Compose will react to auth state change if we observed it,
                    // but for now let's just recreate the activity to keep it simple)
                    recreate()
                }
            }
    }

}