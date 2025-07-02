package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val userCollection = firestore.collection("users")

    // Obtém um usuário pelo ID
    suspend fun getUser(userId: String): User? {
        return try {
            userCollection.document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Salva ou atualiza um usuário
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

    // Obtém todos os usuários
    suspend fun getAllUsers(): List<User> {
        return try {
            userCollection.get().await().toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createUserIfNotExists(firebaseUser: FirebaseUser): User? {
        return try {
            val userId = firebaseUser.uid

            val existingUser = getUser(userId)

            if (existingUser != null) {
                existingUser
            } else {
                val newUser = User(
                    id = userId,
                    nomeUsuario = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    nivelAtividadeAcumulado = 0.0,
                    dataCadastro = Timestamp.now()
                )

                val success = saveUser(newUser)

                if (success) {
                    newUser
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Salva ou atualiza um usuário com base nos dados do Google OAuth2
    suspend fun saveOrUpdateUserFromGoogle(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String
    ) {
        try {
            val user = User(
                id = userId,
                nomeUsuario = displayName,
                email = email,
                photoUrl = photoUrl,
                displayName = displayName,
                nivelAtividadeAcumulado = 0.0,
                dataCadastro = Timestamp.now()
            )

            userCollection.document(userId).set(user).await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Método para atualizar o nível de atividade do usuário
    suspend fun updateActivityLevel(userId: String, additionalLevel: Double): Boolean {
        return try {
            val user = getUser(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    nivelAtividadeAcumulado = user.nivelAtividadeAcumulado + additionalLevel
                )
                saveUser(updatedUser)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Método para atualizar o nome de usuário
    suspend fun updateUserName(userId: String, newName: String): Boolean {
        return try {
            val user = getUser(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    nomeUsuario = newName,
                    displayName = newName
                )
                saveUser(updatedUser)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Método para buscar usuários por nome (para funcionalidades sociais)
    suspend fun searchUsersByName(query: String): List<User> {
        return try {
            userCollection
                .whereGreaterThanOrEqualTo("nomeUsuario", query)
                .whereLessThan("nomeUsuario", query + '\uf8ff')
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Método para deletar usuário
    suspend fun deleteUser(userId: String): Boolean {
        return try {
            userCollection.document(userId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}