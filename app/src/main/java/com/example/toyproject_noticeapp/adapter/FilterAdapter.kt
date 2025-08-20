package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.databinding.ItemNotificationFilterBinding

data class FilterItem(
    val name: String,
    val isFavoriteFilter: Boolean = false, // 이 속성은 이제 사용되지 않지만, 다른 코드에 영향을 주지 않으므로 그대로 둡니다.
    var isSelected: Boolean = false
)

class FilterAdapter(
    private val filters: List<FilterItem>,
    private val onFilterSelected: (FilterItem) -> Unit
) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onFilterSelected)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount(): Int = filters.size

    class ViewHolder(
        private val binding: ItemNotificationFilterBinding,
        private val onFilterSelected: (FilterItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // ##### 이 부분이 수정되었습니다! #####
        fun bind(filter: FilterItem) {
            // 모든 필터에 대해 텍스트와 선택 상태만 설정합니다.
            // 아이콘 관련 로직을 모두 제거했습니다.
            binding.chipFilter.text = filter.name
            binding.chipFilter.isChecked = filter.isSelected
            binding.chipFilter.chipIcon = null // 아이콘을 항상 제거하도록 명시

            binding.chipFilter.setOnClickListener {
                onFilterSelected(filter)
            }
        }
    }
}