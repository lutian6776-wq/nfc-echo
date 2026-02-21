package com.echo.lutian.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用状态枚举
 */
enum class AppState {
    INITIALIZING,   // 初始化状态（新增）
    IDLE,       // 空闲状态
    PLAYING,    // 播放状态
    COUNTDOWN,  // 录音倒计时
    RECORDING,  // 录音状态
    CONFIRMING, // 确认状态
    SYNCING,    // 同步中（检查更新/下载）
    ADMIN,      // 管理员模式
    SELECTING_RECEIVER,  // 选择接收者
    USER_CONVERSATION,   // 用户对话详情
    USER_HISTORY         // 普通用户历史对话
}

/**
 * UI 状态数据类
 */
data class MainUiState(
    val appState: AppState = AppState.INITIALIZING,  // 默认为初始化状态
    val currentAudioPath: String? = null,
    val currentAudioId: Long? = null,
    val recordingDuration: Int = 0,  // 录音时长（秒）
    val amplitude: Int = 0,           // 当前振幅
    val errorMessage: String? = null,
    val nfcAction: String? = null,
    val syncMessage: String? = null,   // 同步状态消息
    val previousState: AppState? = null, // 播放前的状态
    val adminInitialTab: Int = 0 // 管理员模式初始标签页
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * 完成初始化，进入主界面
     */
    fun completeInitialization() {
        _uiState.value = MainUiState(appState = AppState.IDLE)
    }

    /**
     * 更新初始化消息
     */
    fun updateInitMessage(message: String) {
        if (_uiState.value.appState == AppState.INITIALIZING) {
            _uiState.value = _uiState.value.copy(syncMessage = message)
        }
    }

    /**
     * 切换到空闲状态
     */
    fun setIdleState() {
        _uiState.value = MainUiState(appState = AppState.IDLE)
    }

    /**
     * 切换到播放状态
     */
    fun setPlayingState(audioPath: String, audioId: Long? = null) {
        val currentState = _uiState.value.appState
        android.util.Log.d("StateDebug", "setPlayingState: currentState=$currentState -> PLAYING, previousState will be=$currentState")
        _uiState.value = _uiState.value.copy(
            appState = AppState.PLAYING,
            currentAudioPath = audioPath,
            currentAudioId = audioId,
            previousState = currentState // 记住当前状态
        )
    }

    /**
     * 从播放状态返回到之前的状态
     */
    fun returnFromPlaying() {
        val previousState = _uiState.value.previousState
        val currentState = _uiState.value.appState
        android.util.Log.d("StateDebug", "returnFromPlaying: currentState=$currentState, previousState=$previousState")
        if (previousState != null && previousState != AppState.PLAYING) {
            android.util.Log.d("StateDebug", "returnFromPlaying: returning to $previousState")
            _uiState.value = _uiState.value.copy(
                appState = previousState,
                previousState = null
            )
        } else {
            android.util.Log.d("StateDebug", "returnFromPlaying: no valid previousState, going to IDLE")
            setIdleState()
        }
    }

    /**
     * 恢复到指定状态并准备播放
     */
    fun restoreStateForPlaying(targetState: AppState) {
        val currentState = _uiState.value.appState
        android.util.Log.d("StateDebug", "restoreStateForPlaying: currentState=$currentState -> targetState=$targetState")
        _uiState.value = _uiState.value.copy(
            appState = targetState,
            previousState = targetState
        )
    }

    /**
     * 切换到倒计时状态
     */
    fun setCountdownState() {
        _uiState.value = _uiState.value.copy(
            appState = AppState.COUNTDOWN
        )
    }

    /**
     * 切换到录音状态
     */
    fun setRecordingState() {
        _uiState.value = _uiState.value.copy(
            appState = AppState.RECORDING,
            recordingDuration = 0,
            amplitude = 0
        )
    }

    /**
     * 切换到确认状态
     */
    fun setConfirmingState(audioPath: String) {
        _uiState.value = _uiState.value.copy(
            appState = AppState.CONFIRMING,
            currentAudioPath = audioPath
        )
    }

    /**
     * 更新录音时长
     */
    fun updateRecordingDuration(duration: Int) {
        if (_uiState.value.appState == AppState.RECORDING) {
            _uiState.value = _uiState.value.copy(recordingDuration = duration)
        }
    }

    /**
     * 更新振幅
     */
    fun updateAmplitude(amplitude: Int) {
        if (_uiState.value.appState == AppState.RECORDING) {
            _uiState.value = _uiState.value.copy(amplitude = amplitude)
        }
    }

    /**
     * 设置错误消息
     */
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 处理 NFC 动作
     */
    fun handleNfcAction(action: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(nfcAction = action)

            // 根据动作类型处理不同逻辑
            when {
                action.startsWith("play/") -> {
                    val audioId = action.removePrefix("play/")
                    // TODO: 从数据库加载音频并播放
                }
                action == "record" -> {
                    setRecordingState()
                }
            }
        }
    }

    /**
     * 清除 NFC 动作
     */
    fun clearNfcAction() {
        _uiState.value = _uiState.value.copy(nfcAction = null)
    }

    /**
     * 进入管理员模式
     */
    fun enterAdminMode() {
        _uiState.value = _uiState.value.copy(
            appState = AppState.ADMIN,
            adminInitialTab = 0
        )
    }

    /**
     * 进入管理员模式的用户管理标签页
     */
    fun enterAdminUserManagement() {
        _uiState.value = _uiState.value.copy(
            appState = AppState.ADMIN,
            adminInitialTab = 2 // 用户管理是第3个标签页（索引2）
        )
    }

    /**
     * 切换到同步状态
     */
    fun setSyncingState(message: String) {
        val currentState = _uiState.value.appState
        android.util.Log.d("StateDebug", "setSyncingState: currentState=$currentState -> SYNCING, previousState will be=$currentState")
        _uiState.value = _uiState.value.copy(
            appState = AppState.SYNCING,
            syncMessage = message,
            previousState = currentState // 记住当前状态
        )
    }

    /**
     * 更新同步消息
     */
    fun updateSyncMessage(message: String) {
        _uiState.value = _uiState.value.copy(syncMessage = message)
    }

    /**
     * 退出管理员模式
     */
    fun exitAdminMode() {
        _uiState.value = MainUiState(appState = AppState.IDLE)
    }

    /**
     * 进入用户对话详情
     */
    fun enterUserConversation() {
        _uiState.value = _uiState.value.copy(appState = AppState.USER_CONVERSATION)
    }

    /**
     * 显示接收者选择界面
     */
    fun showReceiverSelection(audioPath: String) {
        _uiState.value = _uiState.value.copy(
            appState = AppState.SELECTING_RECEIVER,
            currentAudioPath = audioPath
        )
    }

    /**
     * 进入用户历史对话
     */
    fun enterUserHistory() {
        _uiState.value = _uiState.value.copy(appState = AppState.USER_HISTORY)
    }
}
