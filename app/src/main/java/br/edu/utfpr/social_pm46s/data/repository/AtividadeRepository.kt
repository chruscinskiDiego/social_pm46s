package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.RegistroAtividade
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AtividadeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val atividadeCollection = firestore.collection("atividades")

    suspend fun getAtividade(atividadeId: String): RegistroAtividade? {
        return try {
            atividadeCollection.document(atividadeId).get().await()
                .toObject(RegistroAtividade::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAtividadesByUser(userId: String): List<RegistroAtividade> {
        return try {
            atividadeCollection.whereEqualTo("idUsuario", userId)
                .get().await().toObjects(RegistroAtividade::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAtividade(atividade: RegistroAtividade): Boolean {
        return try {
            val documentRef = if (atividade.id.isNotEmpty()) {
                atividadeCollection.document(atividade.id)
            } else {
                atividadeCollection.document()
            }

            documentRef.set(atividade).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAtividade(atividadeId: String): Boolean {
        return try {
            atividadeCollection.document(atividadeId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}