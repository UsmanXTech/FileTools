package com.filetoolsapp.ui.tool

import android.content.ActivityNotFoundException
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
        binding.btnPickFile.setOnClickListener { filePickerLauncher.launch(getPickerMimeType()) }
        binding.btnProcess.setOnClickListener { processFile() }
        binding.btnProcess.isEnabled = false
    }

    private fun setupToolList() {
        val adapter = ToolAdapter(plugin.tools) { tool ->
            selectedTool = tool
            binding.tvSelectedTool.text = "Selected: ${tool.name}"
            binding.btnPickFile.visibility = View.VISIBLE
            binding.successLayout.visibility = View.GONE
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

                val baseName = FileUtils.removeExtension(selectedFileName)
                val outputDir = FileUtils.getOutputDir(this@ToolActivity, plugin.id)
                val outputFile = createOutputFile(outputDir, baseName, tool.id)

                val result = withContext(Dispatchers.IO) {
                    plugin.executeTool(
                        this@ToolActivity,
                        tool.id,
                        inputFile.absolutePath,
                        outputFile.absolutePath,
                        onProgress = { progress ->
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

    private fun createOutputFile(outputDir: File, baseName: String, toolId: String): File {
        val safeBaseName = baseName.ifBlank { "file" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val extension = when (toolId) {
            "img_convert", "img_compress", "img_resize", "img_rotate", "img_crop" -> "jpg"
            "img_to_pdf", "pdf_merge", "pdf_split", "pdf_compress" -> "pdf"
            "pdf_to_image" -> "jpg"
            "zip_create" -> "zip"
            "zip_extract" -> ""
            "zip_view", "file_size" -> "txt"
            "audio_convert", "audio_compress", "audio_extract", "audio_trim", "video_extract_audio" -> "m4a"
            "video_compress", "video_convert", "video_trim", "video_remove_audio" -> "mp4"
            else -> FileUtils.getExtension(selectedFileName).ifBlank { "bin" }
        }

        return if (extension.isEmpty()) {
            File(outputDir, "${safeBaseName}_extracted")
        } else {
            File(outputDir, "${safeBaseName}_${toolId}_output.$extension")
        }
    }

    private fun getPickerMimeType(): String {
        selectedTool?.let { tool ->
            return when (tool.id) {
                "img_to_pdf" -> "image/*"
                "pdf_merge", "pdf_split", "pdf_compress", "pdf_to_image" -> "application/pdf"
                "zip_extract", "zip_view" -> "application/zip"
                "audio_extract" -> "video/*"
                else -> when (plugin.id) {
                    "image" -> "image/*"
                    "audio" -> "audio/*"
                    "video" -> "video/*"
                    else -> "*/*"
                }
            }
        }

        return when (plugin.id) {
            "image" -> "image/*"
            "audio" -> "audio/*"
            "video" -> "video/*"
            "pdf" -> "*/*"
            "archive" -> "*/*"
            else -> "*/*"
        }
    }

    private fun showSuccessDialog(outputFile: File) {
        binding.successLayout.visibility = View.VISIBLE
        binding.tvOutputFile.text = outputFile.name
        binding.tvOutputSize.text = FileUtils.formatFileSize(outputFile.length())

        binding.btnShare.setOnClickListener {
            if (outputFile.isDirectory) {
                showToast("Extracted folder is saved in app storage")
                return@setOnClickListener
            }

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
            if (outputFile.isDirectory) {
                showToast("Extracted folder is saved in app storage")
                return@setOnClickListener
            }

            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                outputFile
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, FileUtils.getMimeType(outputFile))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(openIntent)
            } catch (e: ActivityNotFoundException) {
                showToast("No app found to open this file")
            }
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
