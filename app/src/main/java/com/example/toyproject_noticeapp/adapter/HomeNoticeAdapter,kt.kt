package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.Notice
import com.example.toyproject_noticeapp.databinding.ItemHomeNotificationBinding

class HomeNoticeAdapter(private val items: List<Notice>) :
    RecyclerView.Adapter<HomeNoticeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemHomeNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notice: Notice) {
            binding.textviewItemCategory.text = notice.category
            binding.textviewItemTitle.text = notice.title
            binding.textviewItemDate.text = notice.date

            // isNew 값에 따라 NEW 태그 보이기/숨기기
            binding.textviewItemNew.visibility = if (notice.isNew) View.VISIBLE else View.GONE
        }
    }
}