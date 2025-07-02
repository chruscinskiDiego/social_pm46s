package br.edu.utfpr.social_pm46s.data.repository

import android.content.Context
import br.edu.utfpr.social_pm46s.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseUser

class AuthRepositoryAdapter(private val context: Context) {
    private val firebaseManager = FirebaseManager()
    private val authRepository = AuthRepository(context)

    fun getCurrentUser(): FirebaseUser? = authRepository.getCurrentUser()

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    suspend fun signInWithGoogle(): FirebaseUser? {
        return authRepository.signInWithGoogle()
    }

    suspend fun signOut() {
        authRepository.signOut()
        firebaseManager.signOut()
    }

    suspend fun saveUserData(userId: String, data: Map<String, Any>): Result<String> {
        return firebaseManager.saveDocumentToFirestore("users", userId, data)
    }

    suspend fun getUserData(userId: String): Result<Map<String, Any>?> {
        return firebaseManager.getDocumentFromFirestore("users", userId)
    }
}