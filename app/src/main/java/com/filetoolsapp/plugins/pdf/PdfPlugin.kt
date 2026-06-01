package com.filetoolsapp.plugins.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream

class PdfPlugin : BasePlugin() {

    override val id = "pdf"
    override val name = "PDF Tools"
    override val description = "Merge, split, compress PDF files"
    override val icon = R.drawable.ic_pdf
    override val accentColor = R.color.accent_pdf

    override val tools = listOf(
        ToolItem("pdf_merge", "Copy PDF", "Save a clean PDF copy", R.drawable.ic_merge),
        ToolItem("pdf_split", "Extract First Page", "Save page 1 as PDF", R.drawable.ic_split),
        ToolItem("pdf_compress", "Optimize PDF", "Rewrite PDF structure", R.drawable.ic_compress),
        ToolItem("pdf_to_image", "PDF to Image", "Render first page to JPG", R.drawable.ic_convert),
        ToolItem("img_to_pdf", "Image to PDF", "Convert image to PDF", R.drawable.ic_convert),
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
                "pdf_merge" -> copyPdf(inputPath, outputPath, onProgress)
                "pdf_split" -> splitFirstPage(inputPath, outputPath, onProgress)
                "pdf_compress" -> copyPdf(inputPath, outputPath, onProgress)
                "pdf_to_image" -> pdfFirstPageToImage(inputPath, outputPath, onProgress)
                "img_to_pdf" -> imageToPdf(inputPath, outputPath, onProgress)
                else -> Result.failure(Exception("Unknown tool: $toolId"))
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

    private fun copyPdf(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        PdfReader(inputPath).use { reader ->
            PdfWriter(outputPath).use { writer ->
                ITextPdfDocument(reader).use { source ->
                    if (source.numberOfPages < 1) {
                        return Result.failure(Exception("PDF has no pages"))
                    }

                    ITextPdfDocument(writer).use { target ->
                        onProgress(60)
                        source.copyPagesTo(1, source.numberOfPages, target)
                    }
                }
            }
        }
        onProgress(100)
        return Result.success(outputPath)
    }

    private fun splitFirstPage(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        PdfReader(inputPath).use { reader ->
            PdfWriter(outputPath).use { writer ->
                ITextPdfDocument(reader).use { source ->
                    if (source.numberOfPages < 1) {
                        return Result.failure(Exception("PDF has no pages"))
                    }

                    ITextPdfDocument(writer).use { target ->
                        onProgress(60)
                        source.copyPagesTo(1, 1, target)
                    }
                }
            }
        }
        onProgress(100)
        return Result.success(outputPath)
    }

    private fun pdfFirstPageToImage(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val inputFile = File(inputPath)
        ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount < 1) {
                    return Result.failure(Exception("PDF has no pages"))
                }

                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    onProgress(60)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    FileOutputStream(outputPath).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    bitmap.recycle()
                }
            }
        }
        onProgress(100)
        return Result.success(outputPath)
    }
}
