package com.filetoolsapp.plugins.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.filetoolsapp.R
import com.filetoolsapp.core.BasePlugin
import com.filetoolsapp.core.ToolItem
import java.io.File
import java.nio.ByteBuffer

class AudioPlugin : BasePlugin() {

    override val id = "audio"
    override val name = "Audio Tools"
    override val description = "Convert, trim, merge audio files"
    override val icon = R.drawable.ic_audio
    override val accentColor = R.color.accent_audio

    override val tools = listOf(
        ToolItem("audio_convert", "Convert to M4A", "Save audio copy", R.drawable.ic_convert),
        ToolItem("audio_trim", "Trim Audio", "Cut start and end", R.drawable.ic_trim),
        ToolItem("audio_merge", "Merge Audio", "Join multiple files", R.drawable.ic_merge, isPro = true),
        ToolItem("audio_extract", "Extract from Video", "Get audio from video", R.drawable.ic_extract),
        ToolItem("audio_compress", "Optimize Audio", "Repackage as M4A", R.drawable.ic_compress),
        ToolItem("audio_batch", "Batch Convert", "Convert multiple files", R.drawable.ic_batch, isPro = true)
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
                "audio_convert", "audio_compress" -> copyAudio(inputPath, outputPath, onProgress)
                "audio_extract" -> extractAudioFromVideo(inputPath, outputPath, onProgress)
                "audio_trim" -> trimAudio(inputPath, outputPath, params, onProgress)
                else -> Result.failure(Exception("Unknown tool: $toolId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyAudio(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            return Result.failure(Exception("No audio track found"))
        }

        extractor.selectTrack(audioTrackIndex)
        onProgress(30)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(audioFormat)
        muxer.start()

        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        onProgress(90)
        muxer.stop()
        muxer.release()
        extractor.release()
        onProgress(100)

        return Result.success(outputPath)
    }

    private fun extractAudioFromVideo(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        onProgress(10)
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            return Result.failure(Exception("No audio track found in video"))
        }

        extractor.selectTrack(audioTrackIndex)
        onProgress(30)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(audioFormat)
        muxer.start()

        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        onProgress(50)

        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        onProgress(90)
        muxer.stop()
        muxer.release()
        extractor.release()
        onProgress(100)

        return Result.success(outputPath)
    }

    private fun trimAudio(
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

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            return Result.failure(Exception("No audio track found"))
        }

        extractor.selectTrack(audioTrackIndex)
        extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        onProgress(30)
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(audioFormat)
        muxer.start()

        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        val startUs = startMs * 1000

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            val sampleTime = extractor.sampleTime
            if (sampleTime > endMs * 1000) break

            bufferInfo.presentationTimeUs = sampleTime - startUs
            bufferInfo.flags = extractor.sampleFlags
            bufferInfo.offset = 0
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
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
