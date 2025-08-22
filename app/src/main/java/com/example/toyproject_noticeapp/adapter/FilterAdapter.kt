package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.databinding.ItemNotificationFilterBinding

// FilterType Enum과 FilterItem data class는 변경 없습니다.
enum class FilterType {
    COMMAND,
    FAVORITE,
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

    // ##### 이 부분이 수정되었습니다! #####
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 오타를 ItemNotificationFilterBinding 으로 수정했습니다.
        val binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ViewHolder(binding)

        // ViewHolder가 생성될 때 클릭 리스너를 단 한 번만 설정하여 재사용 오류를 방지합니다.
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            // 유효한 위치인지 반드시 확인
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