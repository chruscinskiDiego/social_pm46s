package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.Group
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val groupCollection = firestore.collection("groups")

    suspend fun getGroup(groupId: String): Group? {
        return try {
            groupCollection.document(groupId).get().await().toObject(Group::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllGroups(): List<Group> {
        return try {
            groupCollection.get().await().toObjects(Group::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveGroup(group: Group): Boolean {
        return try {
            val documentRef = if (group.id.isNotEmpty()) {
                groupCollection.document(group.id)
            } else {
                groupCollection.document()
            }

            documentRef.set(group).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        return try {
            groupCollection.document(groupId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}