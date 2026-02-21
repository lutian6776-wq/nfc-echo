package com.echo.lutian.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * 录音状态数据类
 */
data class RecordingState(
    val isRecording: Boolean = false,
    val duration: Int = 0,           // 已录制时长（秒）
    val remainingTime: Int = 30,     // 剩余时间（秒）
    val amplitude: Int = 0,          // 当前振幅 (0-32767)
    val filePath: String? = null
)

class AudioService(private val context: Context) {

    companion object {
        private const val TAG = "AudioService"
        private const val MAX_RECORDING_DURATION = 30 // 最大录音时长 30 秒
        private const val AMPLITUDE_UPDATE_INTERVAL = 100L // 振幅更新间隔（毫秒）
    }

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /**
     * 开始录音
     * @return 录音文件路径，如果失败返回 null
     */
    fun startRecording(): String? {
        if (_recordingState.value.isRecording) {
            Log.w(TAG, "Recording already in progress")
            return null
        }

        try {
            // 创建录音文件
            val audioFile = createAudioFile()
            val filePath = audioFile.absolutePath

            // 初始化 MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(filePath)
                setMaxDuration(MAX_RECORDING_DURATION * 1000) // 设置最大时长

                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }

                prepare()
                start()
            }

            // 更新状态
            _recordingState.value = RecordingState(
                isRecording = true,
                duration = 0,
                remainingTime = MAX_RECORDING_DURATION,
                amplitude = 0,
                filePath = filePath
            )

            // 启动倒计时和振幅监控
            startRecordingMonitor()

            Log.d(TAG, "Recording started: $filePath")
            return filePath

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 停止录音
     * @return 录音文件路径
     */
    fun stopRecording(): String? {
        if (!_recordingState.value.isRecording) {
            Log.w(TAG, "No recording in progress")
            return null
        }

        val filePath = _recordingState.value.filePath

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Error stopping MediaRecorder, may be already stopped", e)
                }
                reset()
            }
            Log.d(TAG, "Recording stopped: $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            releaseRecorder()
            recordingJob?.cancel()

            _recordingState.value = RecordingState(
                isRecording = false,
                filePath = filePath
            )
        }

        return filePath
    }

    /**
     * 取消录音（删除文件）
     */
    fun cancelRecording() {
        val filePath = _recordingState.value.filePath
        stopRecording()

        filePath?.let {
            try {
                File(it).delete()
                Log.d(TAG, "Recording file deleted: $it")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete recording file", e)
            }
        }
    }

    /**
     * 启动录音监控（倒计时和振幅）
     */
    private fun startRecordingMonitor() {
        recordingJob = scope.launch {
            val startTime = System.currentTimeMillis()

            while (isActive) {
                delay(AMPLITUDE_UPDATE_INTERVAL)

                // 计算已录制时长（秒）
                val elapsedMillis = System.currentTimeMillis() - startTime
                val elapsedSeconds = (elapsedMillis / 1000).toInt()

                // 获取当前振幅
                val amplitude = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }

                // 更新状态
                _recordingState.value = _recordingState.value.copy(
                    duration = elapsedSeconds,
                    remainingTime = (MAX_RECORDING_DURATION - elapsedSeconds).coerceAtLeast(0),
                    amplitude = amplitude
                )

                // 达到最大时长自动停止
                if (elapsedSeconds >= MAX_RECORDING_DURATION) {
                    stopRecording()
                    break
                }
            }
        }
    }

    /**
     * 创建录音文件
     */
    private fun createAudioFile(): File {
        val audioDir = File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        return File(audioDir, "recording_$timestamp.m4a")
    }

    /**
     * 释放 MediaRecorder 资源
     */
    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    /**
     * 获取当前振幅（0-100 的归一化值）
     */
    fun getNormalizedAmplitude(): Int {
        val maxAmplitude = 32767 // MediaRecorder.getMaxAmplitude() 的最大值
        return (_recordingState.value.amplitude * 100 / maxAmplitude).coerceIn(0, 100)
    }

    /**
     * 清理资源
     */
    fun release() {
        recordingJob?.cancel()
        releaseRecorder()
        scope.cancel()
    }
}
