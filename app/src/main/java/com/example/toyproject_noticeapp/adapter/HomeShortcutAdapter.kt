package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.Shortcut
import com.example.toyproject_noticeapp.databinding.ItemHomeShortcutBinding

class HomeShortcutAdapter(
    private val items: List<Shortcut>,
    private val onClick: (Shortcut) -> Unit
) : RecyclerView.Adapter<HomeShortcutAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeShortcutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemHomeShortcutBinding,
        private val onClick: (Shortcut) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Shortcut) {
            // 아이콘과 텍스트를 각각의 뷰에 설정
            binding.imageShortcutIcon.setImageResource(item.iconResId)
            binding.textShortcutName.text = item.name

            itemView.setOnClickListener {
                onClick(item)
            }
        }
    }
}