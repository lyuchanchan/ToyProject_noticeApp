package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.Subscription
import com.example.toyproject_noticeapp.databinding.ItemSettingSubscriptionBinding

class SettingSubscriptionAdapter(
    private val onToggleChanged: (Subscription, Boolean) -> Unit
) : ListAdapter<Subscription, SettingSubscriptionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onToggleChanged)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSettingSubscriptionBinding,
        private val onToggleChanged: (Subscription, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Subscription) {
            binding.textviewSubscriptionName.text = item.name
            // 리사이클러뷰의 재사용으로 인한 리스너 중복 호출을 방지하기 위해 초기화
            binding.switchSubscriptionToggle.setOnCheckedChangeListener(null)
            binding.switchSubscriptionToggle.isChecked = item.isEnabled
            binding.switchSubscriptionToggle.setOnCheckedChangeListener { _, isChecked ->
                item.isEnabled = isChecked
                onToggleChanged(item, isChecked)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Subscription>() {
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem.name == newItem.name
        }
        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem == newItem
        }
    }
}