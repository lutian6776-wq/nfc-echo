package com.echo.lutian.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放状态数据类
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,      // 当前播放位置（毫秒）
    val duration: Long = 0,             // 总时长（毫秒）
    val progress: Float = 0f,           // 播放进度 (0-1)
    val audioPath: String? = null,
    val audioId: Long? = null
)

class AudioPlayerService(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var guidePlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /**
     * 播放引导语音
     * @param rawResourceId 原始资源 ID (如 R.raw.play_start)
     * @param onComplete 播放完成回调
     */
    fun playGuideAudio(rawResourceId: Int, onComplete: () -> Unit) {
        releaseGuidePlayer()

        guidePlayer = ExoPlayer.Builder(context).build().apply {
            val uri = "android.resource://${context.packageName}/$rawResourceId"
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onComplete()
                        releaseGuidePlayer()
                    }
                }
            })
        }
    }

    /**
     * 播放音频文件
     * @param filePath 音频文件路径
     * @param audioId 音频记录 ID
     */
    fun playAudio(filePath: String, audioId: Long? = null) {
        // 如果正在播放同一个文件，从头重放
        if (_playbackState.value.audioPath == filePath && exoPlayer != null) {
            exoPlayer?.seekTo(0)
            exoPlayer?.play()
            return
        }

        releasePlayer()

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(filePath))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackState()
                    if (isPlaying) {
                        // 开始播放时启动进度更新
                        startProgressUpdates()
                    } else {
                        // 停止播放时取消进度更新
                        progressJob?.cancel()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            progressJob?.cancel()
                            _playbackState.value = PlaybackState()
                        }
                        Player.STATE_READY -> {
                            updatePlaybackState()
                        }
                        else -> {
                            updatePlaybackState()
                        }
                    }
                }
            })
        }

        _playbackState.value = _playbackState.value.copy(
            audioPath = filePath,
            audioId = audioId
        )

        // 启动进度更新
        startProgressUpdates()
    }

    /**
     * 暂停播放
     */
    fun pause() {
        exoPlayer?.pause()
        updatePlaybackState()
    }

    /**
     * 恢复播放
     */
    fun resume() {
        exoPlayer?.play()
        updatePlaybackState()
    }

    /**
     * 停止播放
     */
    fun stop() {
        progressJob?.cancel()
        releasePlayer()
        _playbackState.value = PlaybackState()
    }

    /**
     * 跳转到指定位置
     * @param position 位置（毫秒）
     */
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        updatePlaybackState()
    }

    /**
     * 更新播放状态
     */
    private fun updatePlaybackState() {
        exoPlayer?.let { player ->
            val currentPos = player.currentPosition
            val duration = player.duration.coerceAtLeast(1)
            val progress = (currentPos.toFloat() / duration).coerceIn(0f, 1f)

            _playbackState.value = _playbackState.value.copy(
                isPlaying = player.isPlaying,
                currentPosition = currentPos,
                duration = duration,
                progress = progress
            )
        }
    }

    /**
     * 启动进度更新
     */
    private fun startProgressUpdates() {
        // 取消之前的更新任务
        progressJob?.cancel()

        // 启动新的更新任务
        progressJob = scope.launch {
            while (isActive && exoPlayer != null) {
                updatePlaybackState()

                // 如果播放器不在播放状态，停止更新
                if (exoPlayer?.isPlaying != true) {
                    break
                }

                delay(100) // 每 100ms 更新一次进度
            }
        }
    }

    /**
     * 释放播放器资源
     */
    private fun releasePlayer() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }

    /**
     * 释放引导语音播放器
     */
    private fun releaseGuidePlayer() {
        guidePlayer?.release()
        guidePlayer = null
    }

    /**
     * 释放所有资源
     */
    fun release() {
        progressJob?.cancel()
        releasePlayer()
        releaseGuidePlayer()
        scope.cancel()
    }

    /**
     * 获取当前播放进度（百分比）
     */
    fun getProgressPercentage(): Int {
        return (_playbackState.value.progress * 100).toInt()
    }

    /**
     * 格式化时间显示
     */
    fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000).toInt()
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
    }
}
