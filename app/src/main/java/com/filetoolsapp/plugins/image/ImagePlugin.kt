package com.filetoolsapp.plugins.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import java.io.File
import java.io.FileOutputStream

class ImagePlugin : BasePlugin() {

    override val id = "image"
    override val name = "Image Tools"
    override val description = "Convert, compress, resize images"
    override val icon = R.drawable.ic_image
    override val accentColor = R.color.accent_image

    override val tools = listOf(
        ToolItem("img_convert", "Convert Format", "PNG, JPG, WEBP, BMP", R.drawable.ic_convert),
        ToolItem("img_compress", "Compress Image", "Reduce file size", R.drawable.ic_compress),
        ToolItem("img_resize", "Resize Image", "Change dimensions", R.drawable.ic_resize),
        ToolItem("img_rotate", "Rotate & Flip", "Rotate or mirror", R.drawable.ic_rotate),
        ToolItem("img_crop", "Crop Image", "Crop to selection", R.drawable.ic_crop),
        ToolItem("img_batch", "Batch Convert", "Convert multiple files", R.drawable.ic_batch, isPro = true)
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
                "img_convert" -> convertImage(inputPath, outputPath, params, onProgress)
                "img_compress" -> compressImage(inputPath, outputPath, params, onProgress)
                "img_resize" -> resizeImage(inputPath, outputPath, params, onProgress)
                "img_rotate" -> rotateImage(inputPath, outputPath, params, onProgress)
                else -> Result.failure(Exception("Unknown tool: $toolId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun convertImage(
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val bitmap = BitmapFactory.decodeFile(inputPath)
            ?: return Result.failure(Exception("Could not decode image"))

        onProgress(50)
        val format = when (params["format"] as? String ?: "jpg") {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
            else -> Bitmap.CompressFormat.JPEG
        }
        val quality = (params["quality"] as? Int) ?: 90

        onProgress(70)
        FileOutputStream(outputPath).use { out ->
            bitmap.compress(format, quality, out)
        }
        bitmap.recycle()
        onProgress(100)
        return Result.success(outputPath)
    }

    private fun compressImage(
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(20)
        val bitmap = BitmapFactory.decodeFile(inputPath)
            ?: return Result.failure(Exception("Could not decode image"))

        onProgress(60)
        val quality = (params["quality"] as? Int) ?: 70
        FileOutputStream(outputPath).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        onProgress(100)
        return Result.success(outputPath)
    }

    private fun resizeImage(
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val bitmap = BitmapFactory.decodeFile(inputPath)
            ?: return Result.failure(Exception("Could not decode image"))

        val targetWidth = (params["width"] as? Int) ?: (bitmap.width / 2)
        val targetHeight = (params["height"] as? Int) ?: (bitmap.height / 2)

        onProgress(50)
        val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        bitmap.recycle()

        onProgress(80)
        FileOutputStream(outputPath).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        resized.recycle()
        onProgress(100)
        return Result.success(outputPath)
    }

    private fun rotateImage(
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(20)
        val bitmap = BitmapFactory.decodeFile(inputPath)
            ?: return Result.failure(Exception("Could not decode image"))

        val degrees = (params["degrees"] as? Float) ?: 90f
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }

        onProgress(60)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()

        FileOutputStream(outputPath).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        rotated.recycle()
        onProgress(100)
        return Result.success(outputPath)
    }
}
