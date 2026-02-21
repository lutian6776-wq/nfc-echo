package com.echo.lutian

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.echo.lutian.data.database.HeartEchoDatabase
import com.echo.lutian.data.entity.AudioRecord
import com.echo.lutian.nfc.NfcManager
import com.echo.lutian.network.NetworkRepository
import com.echo.lutian.service.AudioPlayerService
import com.echo.lutian.service.AudioService
import com.echo.lutian.ui.screen.AdminModeScreen
import com.echo.lutian.ui.screen.MainScreen
import com.echo.lutian.ui.screen.ReceiverSelectionScreen
import com.echo.lutian.ui.screen.SetupServerScreen
import com.echo.lutian.ui.screen.SetupAdminScreen
import com.echo.lutian.ui.screen.UserConversationScreen
import com.echo.lutian.ui.screen.UserHistoryScreen
import com.echo.lutian.ui.theme.MyApplicationTheme
import com.echo.lutian.util.DebugUtil
import com.echo.lutian.util.AppPreferences
import com.echo.lutian.viewmodel.AppState
import com.echo.lutian.viewmodel.MainViewModel
import com.echo.lutian.viewmodel.UserViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var audioService: AudioService
    private lateinit var audioPlayerService: AudioPlayerService
    private lateinit var nfcManager: NfcManager
    private lateinit var database: HeartEchoDatabase
    private lateinit var networkRepository: NetworkRepository
    private var nfcAdapter: NfcAdapter? = null
    private var vibrator: Vibrator? = null

    // NFC 写入状态（使用 mutableStateOf 以触发重组）
    private var isWritingNfc by mutableStateOf(false)
    private var nfcWriteSuccess by mutableStateOf(false)
    private var nfcActionToWrite: String? = null
    private var writeSuccessJob: Job? = null

    // 录音权限请求
    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "需要录音权限才能使用此功能", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化服务
        audioService = AudioService(this)
        audioPlayerService = AudioPlayerService(this)
        nfcManager = NfcManager()
        database = HeartEchoDatabase.getDatabase(this)
        networkRepository = NetworkRepository(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // 初始化震动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // 检查 NFC 是否可用
        if (nfcAdapter == null) {
            Toast.makeText(this, "设备不支持 NFC", Toast.LENGTH_LONG).show()
        }

        // 启动初始化流程
        performInitialization()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val recordingState by audioService.recordingState.collectAsState()
                val playbackState by audioPlayerService.playbackState.collectAsState()
                val audioRecords by database.audioRecordDao().getAllRecords().collectAsState(initial = emptyList())
                val currentUser by userViewModel.currentUser.collectAsState()
                val users by userViewModel.users.collectAsState()
                val selectedUser by userViewModel.selectedUser.collectAsState()
                val conversationMessages by userViewModel.conversationMessages.collectAsState()
                val isLoadingMessages by userViewModel.isLoading.collectAsState()
                val latestMessageRead by userViewModel.latestSentMessageRead.collectAsState()
                val hasUnreadNewMessage by userViewModel.hasUnreadNewMessage.collectAsState()

                val appPreferences = remember { AppPreferences(this@MainActivity) }
                var serverUrl by remember { mutableStateOf(appPreferences.serverUrl) }

                // 定期检查最新消息状态（仅普通用户）
                LaunchedEffect(currentUser) {
                    if (currentUser?.role != "admin") {
                        while (true) {
                            userViewModel.checkLatestSentMessageStatus()
                            kotlinx.coroutines.delay(10000) // 每10秒检查一次
                        }
                    }
                }

                // 返回键处理
                var showExitDialog by remember { mutableStateOf(false) }

                // 当前接收者逻辑
                var localLastRecipientId by remember { mutableStateOf(appPreferences.lastRecipientId) }
                val currentRecipientId = remember(users, localLastRecipientId) {
                    localLastRecipientId ?: users.firstOrNull { it.role == "admin" }?.userId ?: users.firstOrNull { it.userId != currentUser?.userId }?.userId
                }
                val currentRecipientName = remember(users, currentRecipientId) {
                    val user = users.firstOrNull { it.userId == currentRecipientId }
                    if (user != null) {
                        "${user.name} (${if (user.role == "admin") "管理员" else "普通用户"})"
                    } else {
                        "未知接收者"
                    }
                }

                // 管理员密码验证对话框
                var showAdminPasswordDialog by remember { mutableStateOf(false) }

                BackHandler {
                    handleBackPress(
                        uiState = uiState,
                        recordingState = recordingState,
                        playbackState = playbackState,
                        onShowExitDialog = { showExitDialog = true }
                    )
                }

                if (uiState.appState == AppState.SETUP_SERVER) {
                    SetupServerScreen(
                        currentUrl = serverUrl,
                        onSaveUrl = { newUrl ->
                            serverUrl = newUrl
                            networkRepository.updateServerUrl(newUrl)
                            // 重新开始初始化
                            viewModel.setInitializingState()
                            performInitialization()
                        }
                    )
                } else if (uiState.appState == AppState.SETUP_ADMIN) {
                    var isSettingAdmin by remember { mutableStateOf(false) }
                    SetupAdminScreen(
                        isLoading = isSettingAdmin,
                        onSetupAdmin = { name, password ->
                            isSettingAdmin = true
                            lifecycleScope.launch {
                                val deviceId = android.provider.Settings.Secure.getString(
                                    contentResolver,
                                    android.provider.Settings.Secure.ANDROID_ID
                                ) ?: "default_user"
                                val success = networkRepository.initAdmin(deviceId, password, name)
                                withContext(Dispatchers.Main) {
                                    isSettingAdmin = false
                                    if (success) {
                                        Toast.makeText(this@MainActivity, "初始化成功！", Toast.LENGTH_SHORT).show()
                                        // 重新开始初始化以便登录流程
                                        viewModel.setInitializingState()
                                        performInitialization()
                                    } else {
                                        Toast.makeText(this@MainActivity, "初始化失败，请重试", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                } else if (uiState.appState == AppState.ADMIN) {
                    // 管理员模式界面
                    AdminModeScreen(
                        audioRecords = audioRecords,
                        isWritingNfc = isWritingNfc,
                        nfcWriteSuccess = nfcWriteSuccess,
                        onBack = { viewModel.exitAdminMode() },
                        onWriteNfcTag = { action -> startNfcWrite(action) },
                        onCancelNfcWrite = { cancelNfcWrite() },
                        onPlayAudio = { record -> playAudio(record) },
                        onDeleteAudio = { record -> deleteAudio(record) },
                        onTestRecord = { requestRecordingPermission() },
                        users = users,
                        onUpdateUserName = { userId, newName ->
                            userViewModel.updateUser(userId, name = newName)
                        },
                        onUserSelected = { user ->
                            userViewModel.selectUser(user)
                            viewModel.enterUserConversation()
                        },
                        currentUserId = currentUser?.userId,
                        initialTab = uiState.adminInitialTab,
                        serverUrl = serverUrl,
                        onUpdateServerUrl = { newUrl ->
                            serverUrl = newUrl
                            networkRepository.updateServerUrl(newUrl)
                        }
                    )
                } else if (uiState.appState == AppState.USER_CONVERSATION) {
                    // 用户对话详情界面
                    selectedUser?.let { user ->
                        UserConversationScreen(
                            user = user,
                            currentUserId = currentUser?.userId,
                            messages = conversationMessages,
                            isLoading = isLoadingMessages,
                            onBack = {
                                userViewModel.clearSelectedUser()
                                viewModel.enterAdminMode()
                            },
                            onPlayMessage = { message ->
                                lifecycleScope.launch {
                                    playCloudMessage(message)
                                }
                            },
                            onDeleteMessage = { message ->
                                lifecycleScope.launch {
                                    deleteCloudMessage(message)
                                }
                            },
                            onRefresh = {
                                userViewModel.refreshConversation()
                            }
                        )
                    }
                } else if (uiState.appState == AppState.SELECTING_RECEIVER) {
                    // 接收者选择界面
                    ReceiverSelectionScreen(
                        users = users.filter { it.userId != currentUser?.userId },
                        currentUserId = currentUser?.userId,
                        onReceiverSelected = { receiver ->
                            // 不直接发送，而是更新当前接收者并回到确认发送界面
                            localLastRecipientId = receiver.userId
                            appPreferences.lastRecipientId = receiver.userId
                            viewModel.setConfirmingState(uiState.currentAudioPath ?: "")
                        },
                        onCancel = {
                            viewModel.setConfirmingState(uiState.currentAudioPath ?: "")
                        }
                    )
                } else if (uiState.appState == AppState.USER_HISTORY) {
                    // 普通用户历史对话界面
                    UserHistoryScreen(
                        currentUserId = currentUser?.userId,
                        messages = conversationMessages,
                        isLoading = isLoadingMessages,
                        onBack = {
                            viewModel.setIdleState()
                        },
                        onPlayMessage = { message ->
                            lifecycleScope.launch {
                                playCloudMessage(message)
                            }
                        },
                        onRefresh = {
                            userViewModel.loadMyHistory()
                        }
                    )
                } else {
                    // 普通用户界面
                    val userInfoText = currentUser?.let {
                        """
                        === 用户信息 ===
                        用户名: ${it.name}
                        用户ID: ${it.userId}
                        设备ID: ${it.deviceId}
                        角色: ${it.role}
                        是否管理员: ${it.role == "admin"}
                        ==================
                        """.trimIndent()
                    }

                    MainScreen(
                        uiState = uiState,
                        recordingState = recordingState,
                        playbackState = playbackState,
                        onStartRecording = { requestRecordingPermission() },
                        onStopRecording = { stopRecording() },
                        onCancelRecording = { cancelRecording() },
                        onConfirmSend = { 
                            if (currentRecipientId != null) {
                                confirmSendToReceiver(currentRecipientId) 
                            } else {
                                Toast.makeText(this@MainActivity, "没有可用的接收者", Toast.LENGTH_SHORT).show()
                            }
                        },
                        currentRecipientName = currentRecipientName,
                        onChangeRecipient = {
                            viewModel.showReceiverSelection(uiState.currentAudioPath ?: "")
                        },
                        onPlayAudio = { lifecycleScope.launch { playLatestAudio() } },
                        onStopPlayback = { stopPlayback() },
                        onPausePlayback = { pausePlayback() },
                        onResumePlayback = { resumePlayback() },
                        onSeekTo = { position -> seekToPosition(position) },
                        onEnterAdminMode = {
                            // 打印调试信息
                            DebugUtil.logUserDebugInfo(this@MainActivity, currentUser)

                            // 权限检查：只有管理员才能进入管理员模式
                            if (currentUser?.role == "admin") {
                                viewModel.enterAdminUserManagement()
                                // 同步用户列表
                                userViewModel.syncAllUsers()
                            } else {
                                // 非管理员设备，显示密码验证对话框
                                showAdminPasswordDialog = true
                            }
                        },
                        onCountdownComplete = { startRecordingInternal() },
                        isAdmin = currentUser?.role == "admin",
                        onViewHistory = {
                            // 进入历史对话界面
                            viewModel.enterUserHistory()
                            userViewModel.loadMyHistory()
                        },
                        latestMessageRead = latestMessageRead,
                        hasUnreadNewMessage = hasUnreadNewMessage,
                        userName = currentUser?.name,
                        userInfo = userInfoText,
                        onCancelCountdown = {
                            viewModel.setIdleState()
                        },
                        onCancelSync = {
                            viewModel.returnFromPlaying()
                        }
                    )
                }

                // 退出确认对话框
                if (showExitDialog) {
                    // 判断是否在录音或播放中
                    val isInActivity = recordingState.isRecording || playbackState.isPlaying

                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = {
                            androidx.compose.material3.Text(
                                text = when {
                                    recordingState.isRecording -> "录音进行中"
                                    playbackState.isPlaying -> "播放进行中"
                                    else -> "退出应用"
                                },
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        },
                        text = {
                            androidx.compose.material3.Text(
                                text = when {
                                    recordingState.isRecording -> "录音进行中，确定要停止并返回主界面吗？录音将被取消。"
                                    playbackState.isPlaying -> "播放进行中，确定要停止并返回主界面吗？"
                                    else -> "确定要退出应用吗？"
                                }
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    showExitDialog = false
                                    if (recordingState.isRecording) {
                                        cancelRecording()
                                    }
                                    if (playbackState.isPlaying) {
                                        stopPlayback()
                                    }
                                    // 如果在录音或播放中，返回主界面；否则退出应用
                                    if (isInActivity) {
                                        viewModel.setIdleState()
                                    } else {
                                        finish()
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFF44336)
                                )
                            ) {
                                androidx.compose.material3.Text(if (isInActivity) "返回主界面" else "退出")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showExitDialog = false }
                            ) {
                                androidx.compose.material3.Text("取消")
                            }
                        }
                    )
                }

                // 管理员密码验证对话框
                if (showAdminPasswordDialog) {
                    var password by remember { mutableStateOf("") }
                    var showError by remember { mutableStateOf(false) }

                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            showAdminPasswordDialog = false
                            password = ""
                            showError = false
                        },
                        title = {
                            androidx.compose.material3.Text(
                                text = "管理员验证",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                androidx.compose.material3.Text(
                                    text = "您不是已标记的管理员设备，请输入管理员密码："
                                )

                                androidx.compose.foundation.layout.Spacer(
                                    modifier = androidx.compose.ui.Modifier.height(16.dp)
                                )

                                androidx.compose.material3.OutlinedTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        showError = false
                                    },
                                    label = { androidx.compose.material3.Text("密码") },
                                    singleLine = true,
                                    isError = showError,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = {
                                            if (password == "Lu950909") {
                                                showAdminPasswordDialog = false
                                                password = ""
                                                showError = false
                                                viewModel.enterAdminMode()
                                                userViewModel.syncAllUsers()
                                            } else {
                                                showError = true
                                            }
                                        }
                                    ),
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                                )

                                if (showError) {
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = androidx.compose.ui.Modifier.height(8.dp)
                                    )
                                    androidx.compose.material3.Text(
                                        text = "密码错误，请重试",
                                        color = androidx.compose.ui.graphics.Color(0xFFF44336),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (password == "Lu950909") {
                                        showAdminPasswordDialog = false
                                        password = ""
                                        showError = false
                                        viewModel.enterAdminMode()
                                        userViewModel.syncAllUsers()
                                    } else {
                                        showError = true
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            ) {                        androidx.compose.material3.Text("确认")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showAdminPasswordDialog = false
                                    password = ""
                                    showError = false
                                }
                            ) {
                                androidx.compose.material3.Text("取消")
                            }
                        }
                    )
                }
            }
        }

        // 处理 NFC Intent
        handleNfcIntent(intent)
    }

    /**
     * 执行应用初始化
     */
    private fun performInitialization() {
        lifecycleScope.launch {
            try {
                val appPreferences = AppPreferences(this@MainActivity)
                val currentUrl = appPreferences.serverUrl

                if (currentUrl.isBlank()) {
                    viewModel.setSetupServerState()
                    return@launch
                }

                viewModel.updateInitMessage("检查系统状态...")
                val status = networkRepository.getSystemStatus()
                if (status != null && !status.isInitialized) {
                    viewModel.setSetupAdminState()
                    return@launch
                }

                // 1. 用户识别
                viewModel.updateInitMessage("正在识别用户...")
                android.util.Log.d("Init", "开始用户识别: ${System.currentTimeMillis()}")
                userViewModel.identifyUser()

                // 等待用户识别完成（增加到 20 秒，给网络请求足够时间）
                var attempts = 0
                while (userViewModel.currentUser.value == null && attempts < 200) {
                    if (attempts % 10 == 0 && attempts > 0) {
                        android.util.Log.d("Init", "等待用户识别... 已等待 ${attempts * 100}ms")
                    }
                    delay(100)
                    attempts++
                }

                android.util.Log.d("Init", "用户识别完成: ${System.currentTimeMillis()}, 耗时: ${attempts * 100}ms")

                val currentUser = userViewModel.currentUser.value
                if (currentUser != null) {
                    // 2. 同步用户列表（获取管理员信息等）
                    viewModel.updateInitMessage("同步用户信息...")
                    userViewModel.syncAllUsers()
                    delay(800)

                    // 3. 如果是普通用户，预加载最新消息
                    if (currentUser.role != "admin") {
                        viewModel.updateInitMessage("检查新消息...")

                        // 获取管理员用户ID
                        var adminUserId: String? = null
                        attempts = 0
                        while (adminUserId == null && attempts < 30) {
                            adminUserId = userViewModel.users.value.firstOrNull { it.role == "admin" }?.userId
                            if (adminUserId == null) {
                                delay(100)
                                attempts++
                            }
                        }

                        if (adminUserId != null) {
                            // 尝试获取最新消息（不阻塞，失败也继续）
                            try {
                                val latestMessage = networkRepository.getLatestMessage(
                                    userId = currentUser.userId,
                                    fromUserId = adminUserId
                                )

                                if (latestMessage != null) {
                                    viewModel.updateInitMessage("发现新消息，准备下载...")

                                    // 检查本地是否已有
                                    val localRecord = database.audioRecordDao().getRecordByCloudId(latestMessage.id)

                                    if (localRecord == null || !File(localRecord.filePath).exists()) {
                                        // 下载最新消息
                                        val localFileName = "cloud_${latestMessage.id}.m4a"
                                        val localPath = File(filesDir, "audio/$localFileName").absolutePath

                                        val downloadSuccess = networkRepository.downloadAudio(latestMessage.fileUrl, localPath)

                                        if (downloadSuccess) {
                                            // 保存到数据库
                                            val newRecord = AudioRecord(
                                                filePath = localPath,
                                                duration = latestMessage.duration,
                                                createdAt = System.currentTimeMillis(),
                                                nfcTagId = "default",
                                                cloudId = latestMessage.id,
                                                cloudUrl = latestMessage.fileUrl,
                                                isUploaded = true
                                            )
                                            database.audioRecordDao().insertRecord(newRecord)
                                            viewModel.updateInitMessage("消息已准备就绪")
                                            delay(500)
                                        }
                                    } else {
                                        delay(500)
                                    }
                                } else {
                                    delay(500)
                                }
                            } catch (e: Exception) {
                                // 预加载失败不影响启动
                                android.util.Log.e("MainActivity", "预加载消息失败", e)
                                delay(500)
                            }
                        } else {
                            delay(500)
                        }

                        // 检查最新消息状态
                        userViewModel.checkLatestSentMessageStatus()
                    } else {
                        // 管理员用户，跳过消息检查
                        delay(500)
                    }

                    // 显示欢迎消息
                    viewModel.updateInitMessage("欢迎，${currentUser.name}")
                    delay(1000)
                } else {
                    viewModel.updateInitMessage("用户识别失败，使用默认配置")
                    delay(1500)
                }

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "初始化失败", e)
                viewModel.updateInitMessage("初始化失败，使用默认配置")
                delay(1500)
            } finally {
                // 完成初始化，进入主界面
                viewModel.completeInitialization()
            }
        }
    }

    /**
     * 处理返回键按下
     */
    private fun handleBackPress(
        uiState: com.echo.lutian.viewmodel.MainUiState,
        recordingState: com.echo.lutian.service.RecordingState,
        playbackState: com.echo.lutian.service.PlaybackState,
        onShowExitDialog: () -> Unit
    ) {
        when {
            // 录音进行中 - 显示确认对话框
            recordingState.isRecording -> {
                onShowExitDialog()
            }
            // 播放进行中 - 停止播放并返回
            playbackState.isPlaying -> {
                stopPlayback()
            }
            // 同步/加载中 - 取消并返回到之前的状态
            uiState.appState == AppState.SYNCING -> {
                viewModel.returnFromPlaying()
            }
            // 倒计时 - 取消并返回主界面
            uiState.appState == AppState.COUNTDOWN -> {
                viewModel.setIdleState()
            }
            // 管理员模式 - 返回主界面
            uiState.appState == AppState.ADMIN -> {
                viewModel.exitAdminMode()
            }
            // 用户对话详情 - 返回管理员模式
            uiState.appState == AppState.USER_CONVERSATION -> {
                userViewModel.clearSelectedUser()
                viewModel.enterAdminMode()
            }
            // 用户历史对话 - 返回主界面
            uiState.appState == AppState.USER_HISTORY -> {
                viewModel.setIdleState()
            }
            // 接收者选择界面 - 返回主界面
            uiState.appState == AppState.SELECTING_RECEIVER -> {
                viewModel.setIdleState()
            }
            // 确认发送界面 - 返回主界面
            uiState.appState == AppState.CONFIRMING -> {
                cancelRecording()
            }
            // 主界面 - 显示退出对话框
            uiState.appState == AppState.IDLE -> {
                onShowExitDialog()
            }
            // 其他情况 - 显示退出对话框
            else -> {
                onShowExitDialog()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 启用 NFC 前台调度
        nfcAdapter?.enableForegroundDispatch(
            this,
            android.app.PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_MUTABLE
            ),
            null,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        // 禁用 NFC 前台调度
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioService.release()
        audioPlayerService.release()
        writeSuccessJob?.cancel()
    }

    /**
     * 处理 NFC Intent
     */
    private fun handleNfcIntent(intent: Intent?) {
        // 如果正在写入 NFC，处理写入逻辑
        if (isWritingNfc && nfcActionToWrite != null) {
            handleNfcWrite(intent)
            return
        }

        // 正常的 NFC 读取逻辑
        when (intent?.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED -> {
                val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                }
                tag?.let {
                    val action = nfcManager.parseNdefMessage(it)
                    if (action != null) {
                        handleNfcAction(action)
                    } else {
                        Toast.makeText(this, "无法解析 NFC 标签", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理 NFC 动作
     */
    private fun handleNfcAction(action: String) {
        lifecycleScope.launch {
            when {
                action == "play" -> {
                    // 播放最新的未播放录音
                    playLatestAudio()
                }
                action.startsWith("play/") -> {
                    // 播放指定 ID 的录音
                    val audioId = action.removePrefix("play/").toLongOrNull()
                    if (audioId != null) {
                        playAudioById(audioId)
                    }
                }
                action == "record" -> {
                    // 开始录音
                    requestRecordingPermission()
                }
                action == "stop" -> {
                    // 停止录音
                    if (viewModel.uiState.value.appState == AppState.RECORDING) {
                        stopRecording()
                    }
                }
            }
        }
    }

    /**
     * 播放最新的音频（带云端同步）
     */
    @SuppressLint("HardwareIds")
    private suspend fun playLatestAudio() {
        // 记住当前状态
        val returnState = viewModel.uiState.value.appState
        android.util.Log.d("StateDebug", "playLatestAudio: saving returnState=$returnState")

        // 显示同步状态
        viewModel.setSyncingState("检查云端更新...")

        try {
            // 获取当前用户ID
            val currentUserId = userViewModel.currentUser.value?.userId ?: run {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "default_user"
                deviceId
            }

            // 获取管理员用户ID（普通用户只听管理员的消息）
            var adminUserId: String? = null

            // 如果用户列表为空，先同步
            if (userViewModel.users.value.isEmpty()) {
                val userInfoList = networkRepository.getAllUsers()
                if (userInfoList != null) {
                    // 直接从 API 响应中查找管理员
                    adminUserId = userInfoList.firstOrNull { it.role == "admin" }?.userId
                }
            } else {
                adminUserId = userViewModel.users.value.firstOrNull { it.role == "admin" }?.userId
            }

            // 1. 尝试从云端获取最新消息（只获取管理员发来的）
            val latestMessage = networkRepository.getLatestMessage(
                userId = currentUserId,
                fromUserId = adminUserId
            )

            if (latestMessage != null) {
                viewModel.updateSyncMessage("找到云端消息...")

                // 2. 检查本地是否已有此消息
                val localRecord = database.audioRecordDao().getRecordByCloudId(latestMessage.id)

                if (localRecord != null && File(localRecord.filePath).exists()) {
                    // 本地已有，直接播放
                    viewModel.updateSyncMessage("使用本地缓存...")
                    delay(500)

                    // 恢复到之前的状态再播放
                    android.util.Log.d("StateDebug", "playLatestAudio: restoring to $returnState before playing (cached)")
                    viewModel.restoreStateForPlaying(returnState)
                    playAudio(localRecord)
                } else {
                    // 本地没有，需要下载
                    viewModel.updateSyncMessage("下载中...")

                    // 生成本地文件路径
                    val localFileName = "cloud_${latestMessage.id}.m4a"
                    val localPath = File(filesDir, "audio/$localFileName").absolutePath

                    // 下载文件
                    val downloadSuccess = networkRepository.downloadAudio(latestMessage.fileUrl, localPath)

                    if (downloadSuccess) {
                        // 保存到数据库
                        val newRecord = AudioRecord(
                            filePath = localPath,
                            duration = latestMessage.duration,
                            createdAt = System.currentTimeMillis(),
                            nfcTagId = "default",
                            cloudId = latestMessage.id,
                            cloudUrl = latestMessage.fileUrl,
                            isUploaded = true
                        )

                        val recordId = database.audioRecordDao().insertRecord(newRecord)
                        val savedRecord = database.audioRecordDao().getRecordById(recordId)

                        if (savedRecord != null) {
                            viewModel.updateSyncMessage("下载完成，开始播放...")
                            delay(500)

                            // 恢复到之前的状态再播放
                            android.util.Log.d("StateDebug", "playLatestAudio: restoring to $returnState before playing (downloaded)")
                            viewModel.restoreStateForPlaying(returnState)
                            playAudio(savedRecord)

                            // 标记云端消息为已播放
                            if (currentUserId == latestMessage.receiverId) {
                                networkRepository.markAsPlayed(latestMessage.id)
                            }
                        } else {
                            throw Exception("保存记录失败")                }
                    } else {
                        throw Exception("下载失败")
                    }
                }
            } else {
                // 云端没有消息，使用本地最新的
                viewModel.updateSyncMessage("使用本地录音...")
                delay(500)

                val latestRecord = database.audioRecordDao().getLatestUnplayedRecord()
                    ?: database.audioRecordDao().getLatestRecordByTagId("default")

                if (latestRecord != null) {
                    // 恢复到之前的状态再播放
                    android.util.Log.d("StateDebug", "playLatestAudio: restoring to $returnState before playing (local)")
                    viewModel.restoreStateForPlaying(returnState)
                    playAudio(latestRecord)
                } else {
                    android.util.Log.d("StateDebug", "playLatestAudio: no audio found, going to IDLE")
                    viewModel.setIdleState()
                    Toast.makeText(this, "没有可播放的录音", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            // 网络错误，回退到本地播放
            android.util.Log.d("StateDebug", "playLatestAudio: error, trying local playback")
            viewModel.updateSyncMessage("网络错误，使用本地录音...")
            delay(500)

            val latestRecord = database.audioRecordDao().getLatestUnplayedRecord()
                ?: database.audioRecordDao().getLatestRecordByTagId("default")

            if (latestRecord != null) {
                // 恢复到之前的状态再播放
                android.util.Log.d("StateDebug", "playLatestAudio: restoring to $returnState before playing (local fallback)")
                viewModel.restoreStateForPlaying(returnState)
                playAudio(latestRecord)
            } else {
                android.util.Log.d("StateDebug", "playLatestAudio: no audio found after error, going to IDLE")
                viewModel.setIdleState()
                Toast.makeText(this, "没有可播放的录音", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 根据 ID 播放音频
     */
    private suspend fun playAudioById(audioId: Long) {
        val record = database.audioRecordDao().getRecordById(audioId)
        if (record != null) {
            playAudio(record)
        } else {
            Toast.makeText(this, "找不到指定的录音", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 播放音频
     */
    @SuppressLint("DiscouragedApi")
    private fun playAudio(record: AudioRecord) {
        // 检查文件是否存在
        val file = File(record.filePath)
        if (!file.exists()) {
            Toast.makeText(this, "音频文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        // 如果正在播放同一个文件，从头重放
        if (viewModel.uiState.value.appState == AppState.PLAYING &&
            viewModel.uiState.value.currentAudioId == record.id) {
            audioPlayerService.playAudio(record.filePath, record.id)
            return
        }

        // 播放引导语音
        val guideResourceId = resources.getIdentifier("play_start", "raw", packageName)
        if (guideResourceId != 0) {
            audioPlayerService.playGuideAudio(guideResourceId) {
                // 引导语音播放完成后，播放录音
                viewModel.setPlayingState(record.filePath, record.id)
                audioPlayerService.playAudio(record.filePath, record.id)

                // 标记为已播放
                lifecycleScope.launch {
                    val currentUserId = userViewModel.currentUser.value?.userId
                    if (record.receiverId == null || currentUserId == record.receiverId) {
                        database.audioRecordDao().markAsPlayed(record.id)
                    }
                }
            }
        } else {
            // 没有引导语音，直接播放
            viewModel.setPlayingState(record.filePath, record.id)
            audioPlayerService.playAudio(record.filePath, record.id)

            lifecycleScope.launch {
                val currentUserId = userViewModel.currentUser.value?.userId
                if (record.receiverId == null || currentUserId == record.receiverId) {
                    database.audioRecordDao().markAsPlayed(record.id)
                }
            }
        }
    }

    /**
     * 停止播放
     */
    private fun stopPlayback() {
        android.util.Log.d("StateDebug", "stopPlayback: stopping playback and calling returnFromPlaying()")
        audioPlayerService.stop()
        viewModel.returnFromPlaying()
    }

    /**
     * 暂停播放
     */
    private fun pausePlayback() {
        audioPlayerService.pause()
    }

    /**
     * 恢复播放
     */
    private fun resumePlayback() {
        audioPlayerService.resume()
    }

    /**
     * 跳转到指定位置
     */
    private fun seekToPosition(position: Long) {
        audioPlayerService.seekTo(position)
    }

    /**
     * 请求录音权限
     */
    private fun requestRecordingPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    /**
     * 开始录音
     */
    @SuppressLint("DiscouragedApi")
    private fun startRecording() {
        // 触发震动反馈
        triggerVibration()

        // 播放引导语音
        val guideResourceId = resources.getIdentifier("start_record", "raw", packageName)
        if (guideResourceId != 0) {
            audioPlayerService.playGuideAudio(guideResourceId) {
                // 引导语音播放完成后进入倒计时
                viewModel.setCountdownState()
            }
        } else {
            // 没有引导语音，直接进入倒计时
            viewModel.setCountdownState()
        }
    }

    /**
     * 内部录音启动逻辑（倒计时完成后调用）
     */
    private fun startRecordingInternal() {
        val filePath = audioService.startRecording()
        if (filePath != null) {
            viewModel.setRecordingState()

            // 监听录音状态，30秒后自动停止
            lifecycleScope.launch {
                audioService.recordingState.collect { state ->
                    if (!state.isRecording && state.filePath != null &&
                        viewModel.uiState.value.appState == AppState.RECORDING) {
                        // 录音完成（30秒倒计时结束），触发确认流程
                        triggerConfirmation(state.filePath)
                    }
                }
            }
        } else {
            Toast.makeText(this, "启动录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 触发确认流程（震动 + 引导语音 + 进入确认界面）
     */
    @SuppressLint("DiscouragedApi")
    private fun triggerConfirmation(filePath: String) {
        // 触发震动反馈
        triggerVibration()

        // 播放确认引导语音
        val guideResourceId = resources.getIdentifier("confirm_send", "raw", packageName)
        if (guideResourceId != 0) {
            audioPlayerService.playGuideAudio(guideResourceId) {
                // 引导语音播放完成后进入确认状态
                viewModel.setConfirmingState(filePath)
            }
        } else {
            // 没有引导语音，直接进入确认状态
            viewModel.setConfirmingState(filePath)
        }
    }

    /**
     * 触发震动反馈
     */
    private fun triggerVibration() {
        vibrator?.let {
            // Android 8.0+ 使用 VibrationEffect
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            it.vibrate(effect)
        }
    }

    /**
     * 开始 NFC 写入
     */
    private fun startNfcWrite(action: String) {
        isWritingNfc = true
        nfcWriteSuccess = false
        nfcActionToWrite = action
        // 触发重组
        lifecycleScope.launch {
            // 强制更新 UI
        }
    }

    /**
     * 取消 NFC 写入
     */
    private fun cancelNfcWrite() {
        isWritingNfc = false
        nfcWriteSuccess = false
        nfcActionToWrite = null
        writeSuccessJob?.cancel()
    }

    /**
     * 处理 NFC 写入
     */
    private fun handleNfcWrite(intent: Intent?) {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null && nfcActionToWrite != null) {
            lifecycleScope.launch {
                try {
                    // 尝试获取 Ndef 实例
                    var ndef = android.nfc.tech.Ndef.get(tag)

                    // 如果标签不支持 NDEF，尝试格式化
                    if (ndef == null) {
                        val ndefFormatable = android.nfc.tech.NdefFormatable.get(tag)
                        if (ndefFormatable != null) {
                            try {
                                ndefFormatable.connect()

                                // 创建 NDEF 消息
                                val message = nfcManager.createNdefMessage(nfcActionToWrite!!)

                                // 格式化并写入
                                ndefFormatable.format(message)
                                ndefFormatable.close()

                                // 格式化成功，显示成功状态
                                nfcWriteSuccess = true
                                isWritingNfc = false
                                triggerVibration()

                                // 5 秒后自动关闭
                                writeSuccessJob = lifecycleScope.launch {
                                    delay(5000)
                                    nfcWriteSuccess = false
                                    nfcActionToWrite = null
                                }
                                return@launch
                            } catch (e: Exception) {
                                ndefFormatable.close()
                                throw e
                            }
                        } else {
                            // 标签既不支持 NDEF 也不能格式化
                            Toast.makeText(
                                this@MainActivity,
                                "此标签不支持 NDEF 格式，请使用其他标签",
                                Toast.LENGTH_LONG
                            ).show()
                            isWritingNfc = false
                            nfcActionToWrite = null
                            return@launch
                        }
                    }

                    // 标签已经是 NDEF 格式，直接写入
                    ndef.connect()

                    // 检查标签是否可写
                    if (!ndef.isWritable) {
                        Toast.makeText(
                            this@MainActivity,
                            "此标签为只读，无法写入",
                            Toast.LENGTH_SHORT
                        ).show()
                        ndef.close()
                        isWritingNfc = false
                        nfcActionToWrite = null
                        return@launch
                    }

                    // 创建 NDEF 消息
                    val message = nfcManager.createNdefMessage(nfcActionToWrite!!)

                    // 检查标签容量
                    val messageSize = message.toByteArray().size
                    val maxSize = ndef.maxSize
                    if (messageSize > maxSize) {
                        Toast.makeText(
                            this@MainActivity,
                            "标签容量不足 (需要 $messageSize 字节，可用 $maxSize 字节)",
                            Toast.LENGTH_LONG
                        ).show()
                        ndef.close()
                        isWritingNfc = false
                        nfcActionToWrite = null
                        return@launch
                    }

                    // 写入标签
                    ndef.writeNdefMessage(message)
                    ndef.close()

                    // 显示写入成功状态
                    nfcWriteSuccess = true
                    isWritingNfc = false
                    triggerVibration()

                    // 5 秒后自动关闭
                    writeSuccessJob = lifecycleScope.launch {
                        delay(5000)
                        nfcWriteSuccess = false
                        nfcActionToWrite = null
                    }

                } catch (e: Exception) {
                    // 写入失败
                    val errorMsg = when {
                        e.message?.contains("Tag was lost") == true ->
                            "标签移开过快，请重试"
                        e.message?.contains("I/O") == true ->
                            "通信失败，请将标签贴近手机后重试"
                        else ->
                            "写入失败: ${e.message}"
                    }

                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                    isWritingNfc = false
                    nfcActionToWrite = null
                }
            }
        } else {
            isWritingNfc = false
            nfcActionToWrite = null
        }
    }

    /**
     * 删除音频
     */
    private fun deleteAudio(record: AudioRecord) {
        lifecycleScope.launch {
            try {
                // 删除文件
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }

                // 从数据库删除
                database.audioRecordDao().deleteRecord(record)

                Toast.makeText(this@MainActivity, "已删除录音 ID: ${record.id}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 停止录音
     */
    private fun stopRecording() {
        val filePath = audioService.stopRecording()
        if (filePath != null) {
            // 所有设备统一显示下方的确认界面（上半部取消下半部发送）
            android.util.Log.d("MainActivity", "录音结束：显示确认界面")
            triggerConfirmation(filePath)
        }
    }

    /**
     * 取消录音
     */
    private fun cancelRecording() {
        audioService.cancelRecording()
        viewModel.setIdleState()
        Toast.makeText(this, "已取消录音", Toast.LENGTH_SHORT).show()
    }



    /**
     * 获取音频文件时长（秒）
     */
    private suspend fun getAudioDuration(filePath: String): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()

            // 转换为秒
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            (durationMs / 1000).toInt()
        } catch (e: Exception) {
            // 如果获取失败，返回 0
            0
        }
    }

    /**
     * 播放云端消息
     */
    private suspend fun playCloudMessage(message: com.echo.lutian.network.MessageInfo) {
        // 记住当前状态（对话界面或历史界面）
        val returnState = viewModel.uiState.value.appState
        android.util.Log.d("StateDebug", "playCloudMessage: saving returnState=$returnState")

        viewModel.setSyncingState("准备播放...")

        try {
            // 检查本地是否已有此消息
            val localRecord = database.audioRecordDao().getRecordByCloudId(message.id)

            if (localRecord != null && File(localRecord.filePath).exists()) {
                // 本地已有，直接播放
                viewModel.updateSyncMessage("使用本地缓存...")
                delay(500)

                // 恢复到之前的状态再播放
                android.util.Log.d("StateDebug", "playCloudMessage: restoring to $returnState before playing (cached)")
                viewModel.restoreStateForPlaying(returnState)
                playAudio(localRecord)

                // 标记云端消息为已播放
                val currentUserId = userViewModel.currentUser.value?.userId
                if (currentUserId == message.receiverId) {
                    networkRepository.markAsPlayed(message.id)
                }
            } else {
                // 本地没有，需要下载
                viewModel.updateSyncMessage("下载中...")

                // 生成本地文件路径
                val localFileName = "cloud_${message.id}.m4a"
                val localPath = File(filesDir, "audio/$localFileName").absolutePath

                // 下载文件
                val downloadSuccess = networkRepository.downloadAudio(message.fileUrl, localPath)

                if (downloadSuccess) {
                    // 保存到数据库
                    val newRecord = com.echo.lutian.data.entity.AudioRecord(
                        filePath = localPath,
                        duration = message.duration,
                        createdAt = System.currentTimeMillis(),
                        nfcTagId = "default",
                        cloudId = message.id,
                        cloudUrl = message.fileUrl,
                        isUploaded = true,
                        senderId = message.senderId,
                        receiverId = message.receiverId
                    )

                    val recordId = database.audioRecordDao().insertRecord(newRecord)
                    val savedRecord = database.audioRecordDao().getRecordById(recordId)

                    if (savedRecord != null) {
                        viewModel.updateSyncMessage("下载完成，开始播放...")
                        delay(500)

                        // 恢复到之前的状态再播放
                        android.util.Log.d("StateDebug", "playCloudMessage: restoring to $returnState before playing (downloaded)")
                        viewModel.restoreStateForPlaying(returnState)
                        playAudio(savedRecord)

                        // 标记云端消息为已播放
                        val currentUserId = userViewModel.currentUser.value?.userId
                        if (currentUserId == message.receiverId) {
                            networkRepository.markAsPlayed(message.id)
                        }
                    } else {
                        throw Exception("保存记录失败")
                    }
                } else {
                    throw Exception("下载失败")
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("StateDebug", "playCloudMessage: error, restoring to $returnState")
            viewModel.restoreStateForPlaying(returnState)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 删除云端消息
     */
    private suspend fun deleteCloudMessage(message: com.echo.lutian.network.MessageInfo) {
        try {
            // 删除云端文件
            val success = networkRepository.deleteMessage(message.id)

            if (success) {
                // 删除本地缓存（如果存在）
                val localRecord = database.audioRecordDao().getRecordByCloudId(message.id)
                if (localRecord != null) {
                    val file = File(localRecord.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                    database.audioRecordDao().deleteRecord(localRecord)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "已删除", Toast.LENGTH_SHORT).show()
                }

                // 重新加载对话
                userViewModel.selectedUser.value?.let { user ->
                    userViewModel.selectUser(user)
                }
            } else {
                throw Exception("删除失败")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 发送给指定接收者
     */
    private fun confirmSendToReceiver(receiverId: String) {
        val audioPath = viewModel.uiState.value.currentAudioPath
        if (audioPath != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val duration = getAudioDuration(audioPath)

                // 获取当前用户ID
                val currentUserId = userViewModel.currentUser.value?.userId ?: "unknown"

                val record = AudioRecord(
                    filePath = audioPath,
                    duration = duration,
                    createdAt = System.currentTimeMillis(),
                    nfcTagId = "default",
                    senderId = currentUserId,
                    receiverId = receiverId
                )

                val recordId = database.audioRecordDao().insertRecord(record)
                val savedRecord = database.audioRecordDao().getRecordById(recordId)

                if (savedRecord != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "录音已保存", Toast.LENGTH_SHORT).show()
                    }

                    // 异步上传到云端
                    launch(Dispatchers.IO) {
                        try {
                            val result = networkRepository.uploadAudio(
                                savedRecord,
                                senderId = currentUserId,
                                receiverId = receiverId
                            )
                            if (result != null) {
                                database.audioRecordDao().updateCloudInfo(
                                    savedRecord.id,
                                    result.first,
                                    result.second
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "已发送",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "发送失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    viewModel.setIdleState()
                }
            }
        }
    }
}
