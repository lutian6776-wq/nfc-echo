package com.echo.lutian.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.lutian.service.RecordingState
import com.echo.lutian.viewmodel.AppState
import com.echo.lutian.viewmodel.MainUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    uiState: MainUiState,
    recordingState: RecordingState,
    playbackState: com.echo.lutian.service.PlaybackState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onConfirmSend: () -> Unit,
    onPlayAudio: () -> Unit,
    onStopPlayback: () -> Unit,
    onPausePlayback: () -> Unit = {},
    onResumePlayback: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onEnterAdminMode: () -> Unit,
    onCountdownComplete: () -> Unit,
    isAdmin: Boolean = false,
    onViewHistory: () -> Unit = {},
    latestMessageRead: Boolean? = null,
    hasUnreadNewMessage: Boolean? = null,
    userName: String? = null,
    userInfo: String? = null,
    onCancelCountdown: () -> Unit = {},
    onCancelSync: () -> Unit = {}
) {
    // 根据状态显示不同的界面
    when (uiState.appState) {
        AppState.INITIALIZING -> InitializingScreen(
            message = uiState.syncMessage ?: "正在初始化..."
        )
        AppState.IDLE -> IdleScreen(
            onStartRecording = onStartRecording,
            onEnterAdminMode = onEnterAdminMode,
            onPlayLatestAudio = onPlayAudio,
            isAdmin = isAdmin,
            onViewHistory = onViewHistory,
            latestMessageRead = latestMessageRead,
            hasUnreadNewMessage = hasUnreadNewMessage,
            userName = userName,
            userInfo = userInfo,
            onEnterUserManagement = onEnterAdminMode
        )
        AppState.COUNTDOWN -> CountdownScreen(
            onComplete = onCountdownComplete,
            onCancel = onCancelCountdown
        )
        AppState.RECORDING -> RecordingScreen(
            recordingState = recordingState,
            onCancelRecording = onCancelRecording,
            onStopRecording = onStopRecording
        )
        AppState.CONFIRMING -> ConfirmingScreen(
            onCancel = onCancelRecording,
            onSend = onConfirmSend
        )
        AppState.SYNCING -> SyncingScreen(
            message = uiState.syncMessage ?: "同步中...",
            onCancel = onCancelSync
        )
        AppState.PLAYING -> PlayingScreen(
            playbackState = playbackState,
            onStop = onStopPlayback,
            onPause = onPausePlayback,
            onResume = onResumePlayback,
            onSeekTo = onSeekTo
        )
        AppState.ADMIN -> {
            // 管理员模式在 MainActivity 中单独处理
        }
        AppState.SELECTING_RECEIVER -> {
            // 接收者选择在 MainActivity 中单独处理
        }
        AppState.USER_CONVERSATION -> {
            // 用户对话详情在 MainActivity 中单独处理
        }
        AppState.USER_HISTORY -> {
            // 用户历史对话在 MainActivity 中单独处理
        }
    }
}

/**
 * 空闲状态界面
 */
@Composable
fun IdleScreen(
    onStartRecording: () -> Unit,
    onEnterAdminMode: () -> Unit,
    onPlayLatestAudio: () -> Unit,
    isAdmin: Boolean = false,
    onViewHistory: () -> Unit = {},
    latestMessageRead: Boolean? = null,
    hasUnreadNewMessage: Boolean? = null,
    userName: String? = null,
    userInfo: String? = null,
    onEnterUserManagement: () -> Unit = {}
) {
    var showUserInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // 用户名显示（顶部中央）
        if (userName != null) {
            var pressProgress by remember { mutableFloatStateOf(0f) }
            var isLongPressing by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var progressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(
                        color = Color(0xFF2E2E2E),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isLongPressing = true

                                progressJob = scope.launch {
                                    val startTime = System.currentTimeMillis()
                                    val duration = 1000L // 1秒

                                    while (isLongPressing && pressProgress < 1f) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        pressProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                                        if (pressProgress >= 1f) {
                                            showUserInfoDialog = true
                                            pressProgress = 0f
                                            isLongPressing = false
                                            break
                                        }

                                        delay(16)
                                    }
                                }

                                tryAwaitRelease()

                                progressJob?.cancel()
                                progressJob = null
                                pressProgress = 0f
                                isLongPressing = false
                            }
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // 长按进度指示
                if (isLongPressing && pressProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.3f * pressProgress),
                                shape = RoundedCornerShape(20.dp)
                            )
                    )
                }

                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 管理员模式入口（左上角）
        AdminModeEntrance(
            onEnterAdminMode = onEnterAdminMode,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        // 普通用户的消息状态指示器（右上角）
        if (!isAdmin && (latestMessageRead != null || hasUnreadNewMessage != null)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (latestMessageRead != null) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (latestMessageRead) Color(0xFF4CAF50) else Color(0xFFF44336),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (latestMessageRead) "已读" else "未读",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                if (hasUnreadNewMessage != null) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (hasUnreadNewMessage) Color(0xFFF44336) else Color(0xFF4CAF50),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (hasUnreadNewMessage) "有新消息" else "无新消息",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 中央按钮区域
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 普通用户显示历史对话按钮
            if (!isAdmin) {
                Button(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .width(200.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "📜 历史对话",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 开始录音按钮（绿色）
            Button(
                onClick = onStartRecording,
                modifier = Modifier.size(200.dp),
        colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = "开始录音",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // 管理员显示"用户管理"，普通用户显示"听取录音"
            Button(
                onClick = if (isAdmin) onEnterUserManagement else onPlayLatestAudio,
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdmin) Color(0xFF2196F3) else Color(0xFFFFC107)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = if (isAdmin) "用户管理" else "听取录音",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdmin) Color.White else Color.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // 用户信息对话框
    if (showUserInfoDialog && userInfo != null) {
        AlertDialog(
            onDismissRequest = { showUserInfoDialog = false },
            title = {
                Text(
                    text = "用户信息",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = userInfo,
                    fontSize = 14.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUserInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("确认")
                }
            },
            containerColor = Color(0xFF2E2E2E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

/**
 * 录音倒计时界面（3秒：红→蓝→绿）
 */
@Composable
fun CountdownScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit = {}
) {
    var countdown by remember { mutableIntStateOf(3) }

    // 根据倒计时显示不同颜色
    val backgroundColor = when (countdown) {
        3 -> Color(0xFFF44336) // 红色
        2 -> Color(0xFF2196F3) // 蓝色
        1 -> Color(0xFF4CAF50) // 绿色
        else -> Color(0xFF4CAF50)
    }

    // 倒计时逻辑
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
            if (countdown == 0) {
                onComplete()
            }
        }
    }

    // 只在倒计时大于0时显示界面
    if (countdown > 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            // 返回按钮（左上角）
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text("← 取消", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 倒计时数字
                Text(
                    text = "$countdown",
                    fontSize = 160.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 提示文字
                Text(
                    text = "准备说话",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * 同步状态界面
 */
@Composable
fun SyncingScreen(
    message: String,
    onCancel: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        // 返回按钮（左上角）
        Button(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242)
            )
        ) {
            Text("← 取消", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 加载动画
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = Color(0xFF4CAF50),
                strokeWidth = 6.dp
            )

            // 同步消息
            Text(
                text = message,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 录音状态界面
 */
@Composable
fun RecordingScreen(
    recordingState: RecordingState,
    onCancelRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    // 显示"开始"文字的状态（录音开始后0.5秒内显示）
    var showStartText by remember { mutableStateOf(true) }

    // 0.5秒后隐藏"开始"文字
    LaunchedEffect(Unit) {
        delay(500)
        showStartText = false
    }

    // 根据剩余时间计算背景色
    val targetColor = when {
        recordingState.remainingTime > 15 -> Color(0xFF4CAF50) // 绿色
        recordingState.remainingTime > 5 -> Color(0xFFFFC107)  // 黄色
        else -> Color(0xFFF44336)                               // 红色
    }

    // 小于 5 秒时添加闪烁效果
    val shouldBlink = recordingState.remainingTime <= 5
    val blinkAlpha by animateFloatAsState(
        targetValue = if (shouldBlink && (System.currentTimeMillis() / 300) % 2 == 0L) 0.3f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "blink_alpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "background_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = blinkAlpha))
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 主内容区域（左侧，不包含电平条）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 倒计时显示
                    Text(
                        text = "${recordingState.remainingTime}s",
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 已录制时长
                    Text(
                        text = "已录制: ${recordingState.duration}s",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 在上半部分显示"开始"文字（0.5秒）
                if (showStartText) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "开始",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 底部按钮区域（取消和结束按钮并排，在左侧区域居中）
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // 取消按钮（长按1.5秒）
                    CancelRecordingButton(
                        onCancel = onCancelRecording
                    )

                    // 结束按钮（长按1.5秒）
                    StopRecordingButton(
                        onStop = onStopRecording
                    )
                }
            }

            // 侧边音量电平条
            VolumeLevelBar(
                amplitude = recordingState.amplitude,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * 取消录音按钮 - 长按1.5秒取消
 */
@Composable
fun CancelRecordingButton(
    onCancel: () -> Unit
) {
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isLongPressing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var progressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 重置进度
    fun resetProgress() {
        progressJob?.cancel()
        progressJob = null
        pressProgress = 0f
        isLongPressing = false
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isLongPressing = true

                        // 启动进度更新协程
                        progressJob = scope.launch {
                            val startTime = System.currentTimeMillis()
                            val duration = 1500L // 1.5 秒

                            while (isLongPressing && pressProgress < 1f) {
                                val elapsed = System.currentTimeMillis() - startTime
                                pressProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                                if (pressProgress >= 1f) {
                                    onCancel()
                                    resetProgress()
                                    break
                                }

                                delay(16) // 约 60fps
                            }
                        }

                        // 等待松手
                        tryAwaitRelease()

                        // 松手后重置
                        resetProgress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 显示长按进度圈
        if (isLongPressing || pressProgress > 0f) {
            CircularProgressIndicator(
                progress = { pressProgress },
                modifier = Modifier.size(70.dp),
                color = Color.White.copy(alpha = 0.9f),
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }

        // 取消图标
        Text(
            text = "✕",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = if (isLongPressing) 0.9f else 0.6f)
        )
    }
}

/**
 * 结束录音按钮 - 长按1.5秒结束
 */
@Composable
fun StopRecordingButton(
    onStop: () -> Unit
) {
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isLongPressing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var progressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 重置进度
    fun resetProgress() {
        progressJob?.cancel()
        progressJob = null
        pressProgress = 0f
        isLongPressing = false
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isLongPressing = true

                        // 启动进度更新协程
                        progressJob = scope.launch {
                            val startTime = System.currentTimeMillis()
                            val duration = 1500L // 1.5 秒

                            while (isLongPressing && pressProgress < 1f) {
                                val elapsed = System.currentTimeMillis() - startTime
                                pressProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                                if (pressProgress >= 1f) {
                                    onStop()
                                    resetProgress()
                                    break
                                }

                                delay(16) // 约 60fps
                            }
                        }

                        // 等待松手
                        tryAwaitRelease()

                        // 松手后重置
                        resetProgress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 显示长按进度圈
        if (isLongPressing || pressProgress > 0f) {
            CircularProgressIndicator(
                progress = { pressProgress },
                modifier = Modifier.size(70.dp),
                color = Color.White.copy(alpha = 0.9f),
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }

        // 结束图标（方块）
        Text(
            text = "■",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = if (isLongPressing) 0.9f else 0.6f)
        )
    }
}

/**
 * 纵向音量电平条
 */
@Composable
fun VolumeLevelBar(
    amplitude: Int,
    modifier: Modifier = Modifier
) {
    // 归一化振幅值 (0-100)
    val normalizedAmplitude = (amplitude * 100 / 32767).coerceIn(0, 100)

    // 平滑动画
    val animatedLevel by animateFloatAsState(
        targetValue = normalizedAmplitude / 100f,
        animationSpec = tween(durationMillis = 100),
        label = "volume_level"
    )

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            // 电平条背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // 空白背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                )

                // 填充的电平
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedLevel)
                        .align(Alignment.BottomCenter)
                        .background(
                            color = when {
                                animatedLevel > 0.6f -> Color(0xFFF44336) // 红色
                                animatedLevel > 0.1f -> Color(0xFFFFC107) // 黄色
                                else -> Color(0xFF4CAF50)                  // 绿色
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }

        // 振幅数值显示
        Text(
            text = "$normalizedAmplitude",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }
}

/**
 * 确认界面（上半部取消，下半部发送）
 */
@Composable
fun ConfirmingScreen(
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 上半部：红色取消按钮
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336)
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "取消",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 下半部：绿色发送按钮
        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "发送",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 播放状态界面
 */
@Composable
fun PlayingScreen(
    playbackState: com.echo.lutian.service.PlaybackState,
    onStop: () -> Unit,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 波形动画
            AudioWaveformAnimation(
                isPlaying = playbackState.isPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(120.dp)
                    .padding(bottom = 32.dp)
            )

            // 播放图标
            Text(
                text = if (playbackState.isPlaying) "▶" else "⏸",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // 可拖动进度条
            DraggableProgressBar(
                progress = playbackState.progress,
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .padding(bottom = 32.dp)
            )

            // 时间显示
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playbackState.currentPosition),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = formatTime(playbackState.duration),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 控制按钮行
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 重放按钮
                Button(
                    onClick = { onSeekTo(0) },
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(40.dp)
                ) {
                    Text(
                        text = "⏮",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 暂停/播放按钮
                Button(
                    onClick = {
                        if (playbackState.isPlaying) {
                            onPause()
                        } else {
                            onResume()
                        }
                    },
                    modifier = Modifier.size(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (playbackState.isPlaying) "⏸" else "▶",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 停止按钮
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(40.dp)
                ) {
                    Text(
                        text = "■",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 音频波形动画
 */
@Composable
fun AudioWaveformAnimation(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // 波形条数量
    val barCount = 40

    // 为每个波形条创建动画状态
    val barHeights = remember {
        List(barCount) { mutableFloatStateOf(0.3f) }
    }

    // 动画效果
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                // 随机更新每个波形条的高度
                barHeights.forEachIndexed { index, heightState ->
                    // 使用正弦波和随机值创建更自然的波形
                    val time = System.currentTimeMillis() / 1000.0
                    val baseWave = kotlin.math.sin(time * 2 + index * 0.5)
                    val randomFactor = Math.random().toFloat() * 0.4f
                    val newHeight = (0.3f + baseWave.toFloat() * 0.3f + randomFactor).coerceIn(0.2f, 1f)
                    heightState.floatValue = newHeight
                }
                delay(100) // 更新频率
            }
        } else {
            // 暂停时，所有波形条回到最小高度
            barHeights.forEach { it.floatValue = 0.3f }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEachIndexed { index, heightState ->
            val animatedHeight by animateFloatAsState(
                targetValue = heightState.floatValue,
                animationSpec = tween(durationMillis = 150),
                label = "bar_height_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .padding(horizontal = 1.dp)
                    .background(
                        color = when {
                            animatedHeight > 0.7f -> Color(0xFF4CAF50) // 绿色
                            animatedHeight > 0.5f -> Color(0xFFFFC107) // 黄色
                            else -> Color(0xFF2196F3)                   // 蓝色
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * 可拖动进度条
 */
@Composable
fun DraggableProgressBar(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    // 当不在拖动时，使用实际进度
    val displayProgress = if (isDragging) dragProgress else progress

    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 200),
        label = "progress_animation"
    )

    Box(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(30.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    },
                    onDragEnd = {
                        isDragging = false
                        val newPosition = (dragProgress * duration).toLong()
                        onSeekTo(newPosition)
                    },
                    onDragCancel = {
                        isDragging = false
                        dragProgress = progress
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    val newPosition = (newProgress * duration).toLong()
                    onSeekTo(newPosition)
                }
            }
    ) {
        // 填充的进度
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(
                    Color(0xFF4CAF50),
                    RoundedCornerShape(30.dp)
                )
        )

        // 进度百分比文字
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        // 拖动指示器
        if (isDragging) {
            Box(
                modifier
                    .fillMaxWidth(animatedProgress)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

/**
 * 高对比度进度条
 */
@Composable
fun HighContrastProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "progress_animation"
    )

    Box(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(30.dp)
            )
    ) {
        // 填充的进度
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(
                    Color(0xFF4CAF50),
                    RoundedCornerShape(30.dp)
                )
        )

        // 进度百分比文字
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 格式化时间显示
 */
private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
}

/**
 * 初始化界面
 */
@Composable
fun InitializingScreen(message: String) {
    // 解析当前步骤
    val steps = listOf(
        "正在识别用户..." to "识别用户",
        "欢迎，" to "识别用户",
        "同步用户信息..." to "同步用户信息",
        "检查新消息..." to "检查新消息",
        "发现新消息，准备下载..." to "下载消息",
        "消息已准备就绪" to "下载消息",
        "用户识别失败，使用默认配置" to "识别用户",
        "初始化失败，使用默认配置" to "初始化"
    )

    val currentStepKey = steps.firstOrNull { message.startsWith(it.first) }?.second ?: "初始化"
    val isCompleted = message.startsWith("欢迎，")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // 中央内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            // Logo 或应用名称
            Text(
                text = "❤️",
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "HeartEcho",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 加载动画
            if (!isCompleted) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 6.dp
                )
            } else {
                // 完成时显示对勾
                Text(
                    text = "✓",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 当前步骤消息
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        // 底部步骤列表
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isCompleted) {
                // 加载步骤列表
                InitStepItem("识别用户", currentStepKey == "识别用户", currentStepKey)
                InitStepItem("同步用户信息", currentStepKey == "同步用户信息", currentStepKey)
                InitStepItem("检查新消息", currentStepKey == "检查新消息", currentStepKey)
                InitStepItem("下载消息", currentStepKey == "下载消息", currentStepKey)
            } else {
                // 完成时显示欢迎消息
                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 初始化步骤项
 */
@Composable
fun InitStepItem(
    stepName: String,
    isActive: Boolean,
    currentStepKey: String
) {
    // 判断步骤状态
    val stepOrder = listOf("识别用户", "同步用户信息", "检查新消息", "下载消息")
    val currentIndex = stepOrder.indexOf(currentStepKey)
    val thisIndex = stepOrder.indexOf(stepName)

    val isCompleted = thisIndex < currentIndex
    val color = when {
        isCompleted -> Color(0xFF4CAF50) // 已完成：绿色
        isActive -> Color.White.copy(alpha = 0.9f) // 进行中：白色
        else -> Color.White.copy(alpha = 0.4f) // 未开始：灰色
    }

    val icon = when {
        isCompleted -> "✓ "
        isActive -> "● "
        else -> "○ "
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon + stepName,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
