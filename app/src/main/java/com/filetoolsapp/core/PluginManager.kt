package com.filetoolsapp.core

import com.filetoolsapp.plugins.audio.AudioPlugin
import com.filetoolsapp.plugins.image.ImagePlugin
import com.filetoolsapp.plugins.pdf.PdfPlugin
import com.filetoolsapp.plugins.video.VideoPlugin
import com.filetoolsapp.plugins.archive.ArchivePlugin

object PluginManager {

    private val _plugins = mutableListOf<BasePlugin>()
    val plugins: List<BasePlugin> get() = _plugins.toList()

    fun init() {
        _plugins.clear()
        registerPlugin(ImagePlugin())
        registerPlugin(AudioPlugin())
        registerPlugin(VideoPlugin())
        registerPlugin(PdfPlugin())
        registerPlugin(ArchivePlugin())
    }

    private fun registerPlugin(plugin: BasePlugin) {
        _plugins.add(plugin)
    }

    fun getPlugin(id: String): BasePlugin? = _plugins.find { it.id == id }

    fun getTool(pluginId: String, toolId: String): ToolItem? =
        getPlugin(pluginId)?.tools?.find { it.id == toolId }
}
