package com.filetoolsapp.ui.tool

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.PluginManager
import com.filetoolsapp.core.ToolItem
import com.filetoolsapp.databinding.ActivityToolBinding
import com.filetoolsapp.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ToolActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLUGIN_ID = "plugin_id"
    }

    private lateinit var binding: ActivityToolBinding
    private lateinit var plugin: BasePlugin
    private var selectedTool: ToolItem? = null
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = FileUtils.getFileName(this, it)
            val fileSize = FileUtils.getFileSize(this, it)
            binding.tvFileName.text = selectedFileName
            binding.tvFileSize.text = FileUtils.formatFileSize(fileSize)
            binding.fileInfoCard.visibility = View.VISIBLE
            binding.btnProcess.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pluginId = intent.getStringExtra(EXTRA_PLUGIN_ID) ?: run { finish(); return }
        plugin = PluginManager.getPlugin(pluginId) ?: run { finish(); return }

        setupUI()
        setupToolList()
    }

    private fun setupUI() {
        binding.tvPluginName.text = plugin.name
        binding.tvPluginDesc.text = plugin.description
        binding.ivPluginIcon.setImageResource(plugin.icon)

        val accent = ContextCompat.getColor(this, plugin.accentColor)
        binding.headerLayout.setBackgroundColor(accent)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPickFile.setOnClickListener { filePickerLauncher.launch("*/*") }
        binding.btnProcess.setOnClickListener { processFile() }
        binding.btnProcess.isEnabled = false
    }

    private fun setupToolList() {
        val adapter = ToolAdapter(plugin.tools) { tool ->
            selectedTool = tool
            binding.tvSelectedTool.text = "Selected: ${tool.name}"
            binding.btnPickFile.visibility = View.VISIBLE
        }
        binding.rvTools.apply {
            layoutManager = LinearLayoutManager(this@ToolActivity)
            this.adapter = adapter
        }
    }

    private fun processFile() {
        val tool = selectedTool ?: return showToast("Please select a tool")
        val uri = selectedFileUri ?: return showToast("Please select a file")

        if (tool.isPro) {
            showToast("This is a Pro feature. Upgrade to unlock!")
            return
        }

        lifecycleScope.launch {
            binding.progressLayout.visibility = View.VISIBLE
            binding.btnProcess.isEnabled = false

            try {
                val inputFile = withContext(Dispatchers.IO) {
                    FileUtils.copyUriToCache(this@ToolActivity, uri, selectedFileName)
                }

                val ext = FileUtils.getExtension(selectedFileName)
                val baseName = FileUtils.removeExtension(selectedFileName)
                val outputDir = FileUtils.getOutputDir(this@ToolActivity, plugin.id)
                val outputFile = File(outputDir, "${baseName}_output.$ext")

                var lastProgress = 0
                val result = withContext(Dispatchers.IO) {
                    plugin.executeTool(
                        this@ToolActivity,
                        tool.id,
                        inputFile.absolutePath,
                        outputFile.absolutePath,
                        onProgress = { progress ->
                            lastProgress = progress
                            lifecycleScope.launch {
                                binding.progressBar.progress = progress
                                binding.tvProgress.text = "$progress%"
                            }
                        }
                    )
                }

                binding.progressLayout.visibility = View.GONE
                binding.btnProcess.isEnabled = true

                result.fold(
                    onSuccess = { path ->
                        showSuccessDialog(File(path))
                    },
                    onFailure = { e ->
                        showToast("Error: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                binding.progressLayout.visibility = View.GONE
                binding.btnProcess.isEnabled = true
                showToast("Failed: ${e.message}")
            }
        }
    }

    private fun showSuccessDialog(outputFile: File) {
        binding.successLayout.visibility = View.VISIBLE
        binding.tvOutputFile.text = outputFile.name
        binding.tvOutputSize.text = FileUtils.formatFileSize(outputFile.length())

        binding.btnShare.setOnClickListener {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                outputFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share file"))
        }

        binding.btnOpenFile.setOnClickListener {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                outputFile
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, contentResolver.getType(fileUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(openIntent)
        }

        binding.btnDone.setOnClickListener {
            binding.successLayout.visibility = View.GONE
            binding.fileInfoCard.visibility = View.GONE
            selectedFileUri = null
            binding.btnProcess.isEnabled = false
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
