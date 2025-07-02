package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val userCollection = firestore.collection("users")
    suspend fun getUser(userId: String): User? {
        return try {
            userCollection.document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUser(user: User): Boolean {
        return try {
            val documentRef = if (user.id.isNotEmpty()) {
                userCollection.document(user.id)
            } else {
                userCollection.document()
            }

            documentRef.set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            userCollection.get().await().toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveOrUpdateUserFromGoogle(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String
    ) {
        try {
            val user = getUser(userId) ?: User(
                id = userId,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                nomeUsuario = displayName
            )

            userCollection.document(userId).set(user).await()
        } catch (e: Exception) {
            throw Exception("Erro ao salvar ou atualizar o usuário: ${e.message}")
        }
    }
}