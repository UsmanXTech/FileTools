package com.filetoolsapp.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.filetoolsapp.core.PluginManager
import com.filetoolsapp.databinding.ActivityHomeBinding
import com.filetoolsapp.ui.tool.ToolActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
    }

    private fun setupRecyclerView() {
        val adapter = PluginAdapter(PluginManager.plugins) { plugin ->
            val intent = Intent(this, ToolActivity::class.java).apply {
                putExtra(ToolActivity.EXTRA_PLUGIN_ID, plugin.id)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.rvPlugins.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            this.adapter = adapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.searchCard.cardElevation = 12f
            } else {
                binding.searchCard.cardElevation = 4f
            }
        }
    }
}
