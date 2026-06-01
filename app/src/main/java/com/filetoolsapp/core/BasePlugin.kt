package com.filetoolsapp.core

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class ToolItem(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val icon: Int,
    val isPro: Boolean = false
)

abstract class BasePlugin {
    abstract val id: String
    abstract val name: String
    abstract val description: String
    @get:DrawableRes abstract val icon: Int
    @get:ColorRes abstract val accentColor: Int
    abstract val tools: List<ToolItem>

    abstract suspend fun executeTool(
        context: Context,
        toolId: String,
        inputPath: String,
        outputPath: String,
        params: Map<String, Any> = emptyMap(),
        onProgress: (Int) -> Unit = {}
    ): Result<String>
}
