package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.ItemNotificationListBinding

class AdapterNotificationList(
    // 아이템 클릭과 즐겨찾기 클릭을 처리할 람다 함수를 추가
    private val onItemClick: (DataNotificationItem) -> Unit,
    private val onFavoriteClick: (DataNotificationItem) -> Unit
) : ListAdapter<DataNotificationItem, AdapterNotificationList.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding, onItemClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationListBinding,
        private val onItemClick: (DataNotificationItem) -> Unit,
        private val onFavoriteClick: (DataNotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DataNotificationItem) {
            binding.textviewNotificationCategory.text = item.category
            binding.textviewNotificationDate.text = item.date
            binding.textviewNotificationTitle.text = item.title
            binding.textviewNotificationDescription.text = item.description

            val starIconRes = if (item.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            binding.imageNotificationFavorite.setImageResource(starIconRes)

            // 클릭 리스너 설정
            itemView.setOnClickListener { onItemClick(item) }
            binding.imageNotificationFavorite.setOnClickListener { onFavoriteClick(item) }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<DataNotificationItem>() {
        override fun areItemsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem.id == newItem.id // 고유 ID로 비교하는 것이 더 안전
        }

        override fun areContentsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}