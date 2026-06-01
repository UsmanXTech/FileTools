package com.filetoolsapp.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.filetoolsapp.core.ToolItem
import com.filetoolsapp.databinding.ItemToolBinding

class ToolAdapter(
    private val tools: List<ToolItem>,
    private val onClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ToolViewHolder>() {

    private var selectedPosition = -1

    inner class ToolViewHolder(val binding: ItemToolBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.binding.apply {
            tvToolName.text = tool.name
            tvToolDesc.text = tool.description
            ivToolIcon.setImageResource(tool.icon)

            if (tool.isPro) {
                tvProBadge.visibility = android.view.View.VISIBLE
            } else {
                tvProBadge.visibility = android.view.View.GONE
            }

            val isSelected = selectedPosition == position
            toolCard.isSelected = isSelected
            toolCard.cardElevation = if (isSelected) 8f else 2f

            toolCard.setOnClickListener {
                val prev = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPosition)
                onClick(tool)
            }
        }
    }

    override fun getItemCount() = tools.size
}
