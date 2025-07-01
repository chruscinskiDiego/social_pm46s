package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.Group
import br.edu.utfpr.social_pm46s.data.model.User
import br.edu.utfpr.social_pm46s.data.model.UsuarioGrupo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioGrupoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuarioGrupoCollection = firestore.collection("usuarios_grupos")

    suspend fun getUserGroups(userId: String): List<UsuarioGrupo> {
        return try {
            usuarioGrupoCollection.whereEqualTo("idUsuario", userId)
                .get().await().toObjects(UsuarioGrupo::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGroupMembers(groupId: String): List<UsuarioGrupo> {
        return try {
            usuarioGrupoCollection.whereEqualTo("idGrupo", groupId)
                .get().await().toObjects(UsuarioGrupo::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addUserToGroup(usuarioGrupo: UsuarioGrupo): Boolean {
        return try {
            // Create composite ID to ensure uniqueness
            val docId = "${usuarioGrupo.idUsuario}_${usuarioGrupo.idGrupo}"
            usuarioGrupoCollection.document(docId).set(usuarioGrupo).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeUserFromGroup(userId: String, groupId: String): Boolean {
        return try {
            val docId = "${userId}_${groupId}"
            usuarioGrupoCollection.document(docId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isUserInGroup(userId: String, groupId: String): Boolean {
        return try {
            val docId = "${userId}_${groupId}"
            val document = usuarioGrupoCollection.document(docId).get().await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }
}