package br.edu.utfpr.social_pm46s.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    // Web Client ID from Firebase console (Authentication -> Sign-in method -> Google)
    // Or from your google-services.json: projects[].oauth_client[].client_id where client_type is 3
    private val webClientId = "YOUR_WEB_CLIENT_ID" // Make sure to replace this

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun createGetGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            // .setAutoSelectEnabled(true)
            .build()
    }

    suspend fun signInWithGoogleCredential(credential: Any): FirebaseUser? {
        if (credential is GoogleIdTokenCredential) {
            val googleIdToken = credential.idToken
            return try {
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(firebaseCredential).await().user
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        } else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                return auth.signInWithCredential(firebaseCredential).await().user
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Log exceptions
                null
            }
        }
        return null
    }

    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
        }
    }
}