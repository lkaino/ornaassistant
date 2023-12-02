package com.rockethat.ornaassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.databinding.ItemAssessedBinding

class AssessedItemsAdapter(private val items: List<String>) : RecyclerView.Adapter<AssessedItemsAdapter.ViewHolder>() {

    class ViewHolder(binding: ItemAssessedBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView: TextView = binding.tvItemData
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssessedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position]
    }

    override fun getItemCount() = items.size
}
