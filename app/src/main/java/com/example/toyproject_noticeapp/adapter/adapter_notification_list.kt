package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.ItemNotificationListBinding

class AdapterNotificationList : ListAdapter<DataNotificationItem, AdapterNotificationList.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(private val binding: ItemNotificationListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DataNotificationItem) {
            binding.textviewNotificationCategory.text = item.category
            binding.textviewNotificationDate.text = item.date
            binding.textviewNotificationTitle.text = item.title
            binding.textviewNotificationDescription.text = item.description

            // 즐겨찾기 여부에 따라 별 아이콘 변경
            val starIconRes = if (item.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            binding.imageNotificationFavorite.setImageResource(starIconRes)
        }
    }

    // 리스트 업데이트 시 효율적인 처리를 위한 DiffUtil
    private class NotificationDiffCallback : DiffUtil.ItemCallback<DataNotificationItem>() {
        override fun areItemsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem.title == newItem.title && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}