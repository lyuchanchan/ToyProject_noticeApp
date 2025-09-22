package com.example.toyproject_noticeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.data.Shortcut
import com.example.toyproject_noticeapp.databinding.ItemHomeShortcutBinding
import java.util.Collections

class HomeShortcutAdapter(
    internal val items: MutableList<Shortcut>, // MutableList로 변경하고 internal로 접근 가능하게 함
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

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun addItem(item: Shortcut, position: Int = items.size) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    fun removeItem(position: Int): Shortcut? {
        if (position < 0 || position >= items.size) return null
        val removedItem = items.removeAt(position)
        notifyItemRemoved(position)
        return removedItem
    }

    // ViewHolder는 동일하게 유지
    class ViewHolder(
        private val binding: ItemHomeShortcutBinding,
        private val onClick: (Shortcut) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Shortcut) {
            binding.imageShortcutIcon.setImageResource(item.iconResId)
            binding.textShortcutName.text = item.name
            itemView.setOnClickListener {
                onClick(item)
            }
        }
    }
}