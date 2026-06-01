package com.filetoolsapp.plugins.archive

import android.content.Context
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ArchivePlugin : BasePlugin() {

    override val id = "archive"
    override val name = "Archive Tools"
    override val description = "ZIP, unzip and manage archives"
    override val icon = R.drawable.ic_archive
    override val accentColor = R.color.accent_archive

    override val tools = listOf(
        ToolItem("zip_create", "Create ZIP", "Compress files to ZIP", R.drawable.ic_compress),
        ToolItem("zip_extract", "Extract ZIP", "Unzip archive files", R.drawable.ic_extract),
        ToolItem("zip_view", "View Contents", "Preview archive contents", R.drawable.ic_eye),
        ToolItem("file_size", "File Size Checker", "Check and compare sizes", R.drawable.ic_info)
    )

    override suspend fun executeTool(
        context: Context,
        toolId: String,
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return try {
            when (toolId) {
                "zip_create" -> createZip(inputPath, outputPath, onProgress)
                "zip_extract" -> extractZip(inputPath, outputPath, onProgress)
                else -> Result.failure(Exception("Unknown tool: $toolId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createZip(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val inputFile = File(inputPath)
        val files = if (inputFile.isDirectory) inputFile.listFiles()?.toList() ?: emptyList()
                    else listOf(inputFile)

        onProgress(30)
        ZipOutputStream(FileOutputStream(outputPath)).use { zos ->
            files.forEachIndexed { index, file ->
                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
                onProgress(30 + ((index + 1) * 60 / files.size))
            }
        }

        onProgress(100)
        return Result.success(outputPath)
    }

    private fun extractZip(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val outputDir = File(outputPath)
        if (!outputDir.exists()) outputDir.mkdirs()

        onProgress(30)
        ZipInputStream(FileInputStream(inputPath)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        onProgress(100)
        return Result.success(outputDir.absolutePath)
    }
}
