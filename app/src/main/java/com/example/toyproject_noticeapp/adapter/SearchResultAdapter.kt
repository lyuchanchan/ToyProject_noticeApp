package com.example.toyproject_noticeapp.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.ItemSearchResultBinding
import java.util.regex.Pattern

class SearchResultAdapter : ListAdapter<DataNotificationItem, SearchResultAdapter.ViewHolder>(DiffCallback()) {

    private var searchQuery: String = ""

    fun updateQuery(query: String) {
        searchQuery = query
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), searchQuery)
    }

    class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DataNotificationItem, query: String) {
            binding.textviewSearchCategory.text = item.category
            binding.textviewSearchTitle.text = highlightText(item.title, query)
            binding.textviewSearchDescription.text = highlightText(item.description, query)
        }

        private fun highlightText(text: String, query: String): SpannableString {
            val spannableString = SpannableString(text)
            if (query.isNotEmpty()) {
                val pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    spannableString.setSpan(
                        BackgroundColorSpan(Color.YELLOW),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return spannableString
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DataNotificationItem>() {
        override fun areItemsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: DataNotificationItem, newItem: DataNotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}