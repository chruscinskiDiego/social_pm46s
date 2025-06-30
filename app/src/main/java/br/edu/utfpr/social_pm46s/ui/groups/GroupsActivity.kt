package br.edu.utfpr.social_pm46s.ui.groups

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsBinding
    private var selectedImageUri: Uri? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                createGroupDialogBinding?.imageViewGroupIcon?.setImageURI(uri)
            }
        }

    private var createGroupDialogBinding: DialogCreateGroupBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // RecyclerView setup
        binding.recyclerViewGroups.layoutManager = LinearLayoutManager(this)
        val adapter = GroupsAdapter()
        binding.recyclerViewGroups.adapter = adapter

        loadGroups(adapter)

        binding.fabCreateGroup.setOnClickListener {
            showCreateGroupDialog(adapter)
        }
    }

    private fun loadGroups(adapter: GroupsAdapter) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        firestore.collection("groups")
            .limit(10)
            .get()
            .addOnSuccessListener { docs ->
                val groups = docs.map { it.toObject(Group::class.java).apply { id = it.id } }
                adapter.submitList(groups)
                binding.progressBar.visibility = android.view.View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar grupos", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = android.view.View.GONE
            }
    }

    private fun showCreateGroupDialog(adapter: GroupsAdapter) {
        createGroupDialogBinding =
            DialogCreateGroupBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Criar Grupo")
            .setView(createGroupDialogBinding?.root)
            .setPositiveButton("Criar") { _, _ ->
                val name = createGroupDialogBinding?.editTextGroupName?.text.toString()
                uploadGroup(name, adapter)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        createGroupDialogBinding?.buttonSelectImage?.setOnClickListener {
            imagePicker.launch("image/*")
        }

        dialog.show()
    }

    private fun uploadGroup(name: String, adapter: GroupsAdapter) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Digite o nome do grupo", Toast.LENGTH_SHORT).show()
            return
        }

        val groupId = UUID.randomUUID().toString()
        if (selectedImageUri != null) {
            val ref = storage.reference.child("groups/$groupId/icon.jpg")
            ref.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception!!
                    }
                    ref.downloadUrl
                }.addOnSuccessListener { uri ->
                    saveGroupToFirestore(groupId, name, uri.toString(), adapter)
                }
        } else {
            saveGroupToFirestore(groupId, name, "", adapter)
        }
    }

    private fun saveGroupToFirestore(
        groupId: String,
        name: String,
        iconUrl: String,
        adapter: GroupsAdapter
    ) {
        val data = mapOf(
            "name" to name,
            "iconUrl" to iconUrl,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("groups")
            .document(groupId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Grupo criado!", Toast.LENGTH_SHORT).show()
                loadGroups(adapter)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao criar grupo", Toast.LENGTH_SHORT).show()
            }
    }
}
