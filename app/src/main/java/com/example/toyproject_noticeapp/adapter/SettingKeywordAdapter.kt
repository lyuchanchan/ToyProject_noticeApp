package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.Keyword
import com.example.toyproject_noticeapp.databinding.ItemSettingKeywordBinding

class SettingKeywordAdapter(
    private val onDeleteClicked: (Keyword) -> Unit
) : ListAdapter<Keyword, SettingKeywordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingKeywordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onDeleteClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSettingKeywordBinding,
        private val onDeleteClicked: (Keyword) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Keyword) {
            binding.textviewKeywordText.text = item.text
            binding.imageKeywordDelete.setOnClickListener {
                onDeleteClicked(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Keyword>() {
        override fun areItemsTheSame(oldItem: Keyword, newItem: Keyword): Boolean {
            return oldItem.text == newItem.text
        }
        override fun areContentsTheSame(oldItem: Keyword, newItem: Keyword): Boolean {
            return oldItem == newItem
        }
    }
}