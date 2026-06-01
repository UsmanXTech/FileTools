package com.filetoolsapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.databinding.ItemPluginCardBinding

class PluginAdapter(
    private val plugins: List<BasePlugin>,
    private val onClick: (BasePlugin) -> Unit
) : RecyclerView.Adapter<PluginAdapter.PluginViewHolder>() {

    inner class PluginViewHolder(val binding: ItemPluginCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
        val binding = ItemPluginCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PluginViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PluginViewHolder, position: Int) {
        val plugin = plugins[position]
        val ctx = holder.itemView.context

        holder.binding.apply {
            tvPluginName.text = plugin.name
            tvPluginDescription.text = plugin.description
            tvToolCount.text = "${plugin.tools.size} tools"
            ivPluginIcon.setImageResource(plugin.icon)

            val accent = ContextCompat.getColor(ctx, plugin.accentColor)
            iconBackground.setCardBackgroundColor(accent)
            pluginCard.setOnClickListener { onClick(plugin) }

            // Stagger animation
            pluginCard.alpha = 0f
            pluginCard.translationY = 60f
            pluginCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((position * 80).toLong())
                .setDuration(400)
                .start()
        }
    }

    override fun getItemCount() = plugins.size
}
