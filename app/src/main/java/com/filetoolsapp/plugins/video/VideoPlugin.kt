package com.filetoolsapp.plugins.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import java.nio.ByteBuffer

class VideoPlugin : BasePlugin() {

    override val id = "video"
    override val name = "Video Tools"
    override val description = "Compress, trim, convert videos"
    override val icon = R.drawable.ic_video
    override val accentColor = R.color.accent_video

    override val tools = listOf(
        ToolItem("video_compress", "Compress Video", "Reduce video size", R.drawable.ic_compress),
        ToolItem("video_trim", "Trim Video", "Cut video clips", R.drawable.ic_trim),
        ToolItem("video_convert", "Convert Format", "MP4, MKV, AVI, MOV", R.drawable.ic_convert),
        ToolItem("video_extract_audio", "Extract Audio", "Get audio from video", R.drawable.ic_extract),
        ToolItem("video_remove_audio", "Remove Audio", "Mute video", R.drawable.ic_mute),
        ToolItem("video_thumbnail", "Extract Thumbnail", "Get video thumbnail", R.drawable.ic_image, isPro = true)
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
                "video_remove_audio" -> removeAudio(inputPath, outputPath, onProgress)
                "video_trim" -> trimVideo(inputPath, outputPath, params, onProgress)
                else -> Result.failure(Exception("Tool: $toolId — add FFmpeg for full support"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun removeAudio(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = mutableMapOf<Int, Int>()

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackMap[i] = muxer.addTrack(format)
            }
        }

        onProgress(30)
        muxer.start()

        val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            val trackIndex = extractor.sampleTrackIndex
            if (trackMap.containsKey(trackIndex)) {
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(trackMap[trackIndex]!!, buffer, bufferInfo)
            }
            extractor.advance()
        }

        onProgress(90)
        muxer.stop()
        muxer.release()
        extractor.release()
        onProgress(100)

        return Result.success(outputPath)
    }

    private fun trimVideo(
        inputPath: String,
        outputPath: String,
        params: Map<String, Any>,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val startMs = (params["start_ms"] as? Long) ?: 0L
        val endMs = (params["end_ms"] as? Long) ?: Long.MAX_VALUE

        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = mutableMapOf<Int, Int>()

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            extractor.selectTrack(i)
            trackMap[i] = muxer.addTrack(format)
        }

        extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        onProgress(30)
        muxer.start()

        val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        val startUs = startMs * 1000

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            val sampleTime = extractor.sampleTime
            if (sampleTime > endMs * 1000) break

            val trackIndex = extractor.sampleTrackIndex
            bufferInfo.presentationTimeUs = sampleTime - startUs
            bufferInfo.flags = extractor.sampleFlags
            bufferInfo.offset = 0
            muxer.writeSampleData(trackMap[trackIndex]!!, buffer, bufferInfo)
            extractor.advance()
        }

        onProgress(90)
        muxer.stop()
        muxer.release()
        extractor.release()
        onProgress(100)

        return Result.success(outputPath)
    }
}
