package br.edu.utfpr.social_pm46s.ui.groups

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class GroupsAdapter : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    private var groups = listOf<br.edu.utfpr.social_pm46s.ui.groups.Group>()

    fun submitList(list: List<br.edu.utfpr.social_pm46s.ui.groups.Group>) {
        groups = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(private val binding: ItemGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: br.edu.utfpr.social_pm46s.ui.groups.Group) {
            binding.textViewGroupName.text = group.name
            binding.textViewGroupMembers.text = "membros: ???"

            Glide.with(binding.root.context)
                .load(group.iconUrl)
                .placeholder(R.drawable.ic_group_placeholder)
                .into(binding.imageViewGroupIcon)

            binding.root.setOnClickListener {
                // abrir tela de detalhes do grupo (membros + ranking)
            }
        }
    }
}
