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
            binding.textviewNotificationViewCount.text = item.viewCount.toString()

            // ⬇️ isFavorite 상태에 따라 아이콘 리소스를 직접 변경 ⬇️
            if (item.isFavorite) {
                binding.imageNotificationFavorite.setImageResource(R.drawable.ic_star_filled)
            } else {
                binding.imageNotificationFavorite.setImageResource(R.drawable.ic_star_outline)
            }

            itemView.setOnClickListener { onItemClick(item) }
            binding.imageNotificationFavorite.setOnClickListener { onFavoriteClick(item) }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<DataNotificationItem>() {
        override fun areItemsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return "${oldItem.category}_${oldItem.id}" == "${newItem.category}_${newItem.id}"
        }

        override fun areContentsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}