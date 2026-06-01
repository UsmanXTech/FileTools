package com.filetoolsapp.plugins.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import java.io.File
import java.io.FileOutputStream

class PdfPlugin : BasePlugin() {

    override val id = "pdf"
    override val name = "PDF Tools"
    override val description = "Merge, split, compress PDF files"
    override val icon = R.drawable.ic_pdf
    override val accentColor = R.color.accent_pdf

    override val tools = listOf(
        ToolItem("pdf_merge", "Merge PDFs", "Combine multiple PDFs", R.drawable.ic_merge),
        ToolItem("pdf_split", "Split PDF", "Split into pages", R.drawable.ic_split),
        ToolItem("pdf_compress", "Compress PDF", "Reduce PDF size", R.drawable.ic_compress),
        ToolItem("pdf_to_image", "PDF to Image", "Convert pages to images", R.drawable.ic_convert),
        ToolItem("img_to_pdf", "Image to PDF", "Convert images to PDF", R.drawable.ic_convert),
        ToolItem("pdf_protect", "Protect PDF", "Add password protection", R.drawable.ic_lock, isPro = true)
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
                "img_to_pdf" -> imageToPdf(inputPath, outputPath, onProgress)
                else -> Result.failure(Exception("Tool: $toolId — requires iTextPDF integration"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun imageToPdf(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val bitmap = BitmapFactory.decodeFile(inputPath)
            ?: return Result.failure(Exception("Could not decode image"))

        onProgress(40)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            bitmap.width, bitmap.height, 1
        ).create()

        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        onProgress(80)
        FileOutputStream(outputPath).use { out ->
            document.writeTo(out)
        }
        document.close()
        bitmap.recycle()

        onProgress(100)
        return Result.success(outputPath)
    }
}
