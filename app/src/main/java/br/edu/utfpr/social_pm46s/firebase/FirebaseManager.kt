package br.edu.utfpr.social_pm46s.firebase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Classe utilitária para gerenciar todas as operações do Firebase
 * Centraliza a lógica de Authentication (Google), Firestore e Realtime Database
 */
class FirebaseManager {

    // Instâncias do Firebase
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance()

    // Google Sign-In
    private var googleSignInClient: GoogleSignInClient? = null

    // ==================== GOOGLE SIGN-IN SETUP ====================

    /**
     * Configura o Google Sign-In
     * Deve ser chamado na MainActivity ou Application
     */
    fun setupGoogleSignIn(context: Context, webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Obtém o Intent para iniciar o Google Sign-In
     */
    fun getGoogleSignInIntent(): Intent? = googleSignInClient?.signInIntent

    // ==================== FIREBASE AUTHENTICATION (GOOGLE) ====================

    /**
     * Processa o resultado do Google Sign-In
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()

            result.user?.let { user ->
                // Salvar informações do usuário no Firestore
                saveUserToFirestore(user, account)
                Result.success(user)
            } ?: Result.failure(Exception("Falha na autenticação"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva informações do usuário no Firestore após login
     */
    private suspend fun saveUserToFirestore(user: FirebaseUser, account: GoogleSignInAccount) {
        val userData = mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "googleId" to (account.id ?: ""),
            "lastLogin" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )

        saveDocumentToFirestore("users", user.uid, userData)
    }

    /**
     * Fazer logout
     */
    fun signOut() {
        auth.signOut()
        googleSignInClient?.signOut()
    }

    /**
     * Obter usuário atual
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Verificar se usuário está logado
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Obter informações do usuário logado
     */
    fun getCurrentUserInfo(): Map<String, Any?>? = auth.currentUser?.let {
        mapOf(
            "uid" to it.uid,
            "email" to it.email,
            "displayName" to it.displayName,
            "photoUrl" to it.photoUrl?.toString()
        )
    }

    // ==================== FIRESTORE ====================

    /**
     * Salvar documento no Firestore
     */
    suspend fun saveDocumentToFirestore(
        collection: String,
        documentId: String? = null,
        data: Map<String, Any>
    ): Result<String> {
        return try {
            val docRef = if (documentId != null) {
                firestore.collection(collection).document(documentId)
            } else {
                firestore.collection(collection).document()
            }

            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ler documento do Firestore
     */
    suspend fun getDocumentFromFirestore(
        collection: String,
        documentId: String
    ): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection(collection).document(documentId).get().await()
            if (document.exists()) {
                Result.success(document.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualizar documento no Firestore
     */
    suspend fun updateDocumentInFirestore(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ): Result<Unit> {
        return try {
            firestore.collection(collection).document(documentId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletar documento do Firestore
     */
    suspend fun deleteDocumentFromFirestore(
        collection: String,
        documentId: String
    ): Result<Unit> {
        return try {
            firestore.collection(collection).document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== REALTIME DATABASE ====================

    /**
     * Salvar dados no Realtime Database
     */
    suspend fun saveDataToRealtimeDatabase(
        path: String,
        data: Any
    ): Result<Unit> {
        return try {
            realtimeDatabase.getReference(path).setValue(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ler dados do Realtime Database
     */
    suspend fun getDataFromRealtimeDatabase(path: String): Result<Any?> {
        return try {
            val snapshot = realtimeDatabase.getReference(path).get().await()
            Result.success(snapshot.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualizar dados no Realtime Database
     */
    suspend fun updateDataInRealtimeDatabase(
        path: String,
        data: Map<String, Any>
    ): Result<Unit> {
        return try {
            realtimeDatabase.getReference(path).updateChildren(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletar dados do Realtime Database
     */
    suspend fun deleteDataFromRealtimeDatabase(path: String): Result<Unit> {
        return try {
            realtimeDatabase.getReference(path).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== STORAGE ====================
    suspend fun uploadGroupImage(imageUri: Uri, groupId: String): String? {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("groups/$groupId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }
} 