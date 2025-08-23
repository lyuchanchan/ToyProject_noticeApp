package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.databinding.ItemNotificationFilterBinding

enum class FilterType {
    COMMAND,
    CATEGORY
}

data class FilterItem(
    val name: String,
    val type: FilterType,
    var isSelected: Boolean = false
)

class FilterAdapter(
    private val filters: List<FilterItem>,
    private val onFilterSelected: (FilterItem) -> Unit
) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: ItemNotificationFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(filter: FilterItem) {
            binding.chipFilter.text = filter.name
            binding.chipFilter.isChecked = filter.isSelected
            binding.chipFilter.chipIcon = null

            if (filter.type == FilterType.COMMAND) {
                binding.chipFilter.isCheckable = false
            } else {
                binding.chipFilter.isCheckable = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)

        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onFilterSelected(filters[position])
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount(): Int = filters.size
}