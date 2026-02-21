package com.echo.lutian.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.lutian.data.entity.AudioRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 管理员模式入口 - 隐形长按区域
 */
@Composable
fun AdminModeEntrance(
    onEnterAdminMode: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .size(80.dp)
            .background(
                color = Color.White.copy(alpha = 0.05f), // 轻微颜色差别
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isLongPressing = true

                        // 启动进度更新协程
                        progressJob = scope.launch {
                            val startTime = System.currentTimeMillis()
                            val duration = 3000L // 3 秒

                            while (isLongPressing && pressProgress < 1f) {
                                val elapsed = System.currentTimeMillis() - startTime
                                pressProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                                if (pressProgress >= 1f) {
                                    onEnterAdminMode()
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
                modifier = Modifier.size(60.dp),
                color = Color.White.copy(alpha = 0.8f),
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        // 提示图标
        Text(
            text = "⚙",
            fontSize = 32.sp,
            color = Color.White.copy(alpha = if (isLongPressing) 0.8f else 0.3f)
        )
    }
}

/**
 * 管理员模式主界面
 */
@Composable
fun AdminModeScreen(
    audioRecords: List<AudioRecord>,
    isWritingNfc: Boolean,
    nfcWriteSuccess: Boolean,
    onBack: () -> Unit,
    onWriteNfcTag: (String) -> Unit,
    onCancelNfcWrite: () -> Unit,
    onPlayAudio: (AudioRecord) -> Unit,
    onDeleteAudio: (AudioRecord) -> Unit,
    onTestRecord: () -> Unit,
    users: List<com.echo.lutian.data.entity.User> = emptyList(),
    onUpdateUserName: (String, String) -> Unit = { _, _ -> },
    onUserSelected: (com.echo.lutian.data.entity.User) -> Unit = {},
    currentUserId: String? = null,
    initialTab: Int = 0,
    serverUrl: String = "",
    onUpdateServerUrl: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // 顶部标题栏
        AdminModeHeader(onBack = onBack)

        // 标签页
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF2E2E2E),
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("NFC 写入器") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("调试面板") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("用户管理") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("缓存管理") }
            )
        }

        // 内容区域
        when (selectedTab) {
            0 -> NfcWriterPanel(
                onWriteNfcTag = onWriteNfcTag,
                isWriting = isWritingNfc,
                writeSuccess = nfcWriteSuccess,
                onCancelWrite = onCancelNfcWrite
            )
            1 -> DebugPanel(
                audioRecords = audioRecords,
                onPlayAudio = onPlayAudio,
                onDeleteAudio = onDeleteAudio,
                onTestRecord = onTestRecord,
                users = users,
                serverUrl = serverUrl,
                onUpdateServerUrl = onUpdateServerUrl
            )
            2 -> UserManagementPanel(
                users = users,
                onUpdateUserName = onUpdateUserName,
                onUserSelected = onUserSelected
            )
            3 -> CacheManagementPanel(
                audioRecords = audioRecords,
                currentUserId = currentUserId,
                onPlayAudio = onPlayAudio,
                onDeleteAudio = onDeleteAudio
            )
        }
    }
}

/**
 * 管理员模式头部
 */
@Composable
fun AdminModeHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E2E2E))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242)
            )
        ) {
            Text("← 返回")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "管理员模式",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * NFC 写入器面板
 */
@Composable
fun NfcWriterPanel(
    onWriteNfcTag: (String) -> Unit,
    isWriting: Boolean,
    writeSuccess: Boolean,
    onCancelWrite: () -> Unit
) {
    var selectedAction by remember { mutableStateOf("play") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "选择要写入的动作",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 动作选择按钮
            NfcActionButton(
                title = "播放标签",
                description = "heartecho://action/play",
                isSelected = selectedAction == "play",
                color = Color(0xFF2196F3),
                onClick = { selectedAction = "play" }
            )

            NfcActionButton(
                title = "录制标签",
                description = "heartecho://action/record",
                isSelected = selectedAction == "record",
                color = Color(0xFF4CAF50),
                onClick = { selectedAction = "record" }
            )

            NfcActionButton(
                title = "停止标签",
                description = "heartecho://action/stop",
                isSelected = selectedAction == "stop",
                color = Color(0xFFF44336),
                onClick = { selectedAction = "stop" }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 写入按钮
            Button(
                onClick = { onWriteNfcTag(selectedAction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isWriting
            ) {
                Text(
                    text = "写入 NFC 标签",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // 底部弹出的写入状态对话框
        if (isWriting || writeSuccess) {
            NfcWriteDialog(
                isWriting = isWriting,
                writeSuccess = writeSuccess,
                onCancel = onCancelWrite
            )
        }
    }
}

/**
 * NFC 写入对话框（底部弹出）
 */
@Composable
fun NfcWriteDialog(
    isWriting: Boolean,
    writeSuccess: Boolean,
    onCancel: () -> Unit
) {
    // 弹出动画
    val offsetY by animateDpAsState(
        targetValue = if (isWriting || writeSuccess) 0.dp else 1000.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialog_slide"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isWriting || writeSuccess) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "dialog_alpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * alpha))
                .pointerInput(Unit) {
                    detectTapGestures {
                        // 点击外部区域关闭（仅在写入成功时允许）
                        if (writeSuccess) {
                            onCancel()
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = offsetY)
                    .pointerInput(Unit) {
                        // 拦截卡片内部的点击，防止穿透到背景
                        detectTapGestures { }
                    },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部指示条
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (writeSuccess) {
                        // 写入成功状态
                        Icon(
                            text = "✓",
                            fontSize = 64.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "写入成功！",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "标签已配置完成",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "5 秒后自动关闭",
                            fontSize = 14.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    } else {
                        // 准备写入状态
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = Color(0xFF2196F3),
                                strokeWidth = 4.dp
                            )
                        }

                        Text(
                            text = "准备写入",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "请将 NFC 标签靠近手机背面",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 取消按钮
                        Button(
                            onClick = onCancel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEEEEEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "取消",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Icon(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

/**
 * NFC 动作按钮
 */
@Composable
fun NfcActionButton(
    title: String,
    description: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color(0xFF424242)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 调试面板
 */
@Composable
fun DebugPanel(
    audioRecords: List<AudioRecord>,
    onPlayAudio: (AudioRecord) -> Unit,
    onDeleteAudio: (AudioRecord) -> Unit,
    onTestRecord: () -> Unit,
    users: List<com.echo.lutian.data.entity.User> = emptyList(),
    serverUrl: String = "",
    onUpdateServerUrl: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 服务器地址配置
        Text(
            text = "系统配置",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        var inputUrl by remember(serverUrl) { mutableStateOf(serverUrl) }
        OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            label = { Text("后端服务器地址 (以 / 结尾)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF2196F3),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onUpdateServerUrl(inputUrl) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("保存配置并重启网络连接", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 测试录音按钮
        Button(
            onClick = onTestRecord,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "📝 手动录音测试",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "录音列表 (${audioRecords.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 录音列表
        if (audioRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无录音",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audioRecords) { record ->
                    AudioRecordItem(
                        record = record,
                        onPlay = { onPlayAudio(record) },
                        onDelete = { onDeleteAudio(record) },
                        users = users
                    )
                }
            }
        }
    }
}

/**
 * 录音列表项
 */
@Composable
fun AudioRecordItem(
    record: AudioRecord,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    users: List<com.echo.lutian.data.entity.User> = emptyList()
) {
    // 查找发送者和接收者的名称
    val senderName = users.firstOrNull { it.userId == record.senderId }?.name ?: record.senderId ?: "未知"
    val receiverName = users.firstOrNull { it.userId == record.receiverId }?.name ?: record.receiverId ?: "未知"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 发送者和接收者信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${record.id}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (record.isPlayed) {
                        Text(
                            text = "✓ 已读",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "未读",
                            fontSize = 11.sp,
                            color = Color(0xFFFFC107),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "发送者: $senderName",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "接收者: $receiverName",
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "时长: ${record.duration}s",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = formatTimestamp(record.createdAt),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // 播放按钮
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3))
            ) {
                Text(
                    text = "▶",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336))
            ) {
                Text(
                    text = "✕",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 用户管理面板
 */
@Composable
fun UserManagementPanel(
    users: List<com.echo.lutian.data.entity.User>,
    onUpdateUserName: (String, String) -> Unit,
    onUserSelected: (com.echo.lutian.data.entity.User) -> Unit
) {
    var editingUser by remember { mutableStateOf<com.echo.lutian.data.entity.User?>(null) }
    var expandedAdmins by remember { mutableStateOf(true) }
    var expandedUsers by remember { mutableStateOf(true) }

    // 分组用户
    val adminUsers = users.filter { it.role == "admin" }
    val normalUsers = users.filter { it.role != "admin" }
    val currentUser = users.firstOrNull { it.isCurrentUser }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 管理员组
        item {
            UserGroupHeader(
                title = "管理员",
                count = adminUsers.size,
                isExpanded = expandedAdmins,
                onToggle = { expandedAdmins = !expandedAdmins }
            )
        }

        if (expandedAdmins) {
            items(adminUsers) { user ->
                UserManagementItemNew(
                    user = user,
                    isCurrentUser = user.userId == currentUser?.userId,
                    onEdit = { editingUser = user },
                    onSelect = {
                        if (user.userId != currentUser?.userId) {
                            onUserSelected(user)
                        }
                    }
                )
            }
        }

        // 普通用户组
        item {
            Spacer(modifier = Modifier.height(8.dp))
            UserGroupHeader(
                title = "普通用户",
                count = normalUsers.size,
                isExpanded = expandedUsers,
                onToggle = { expandedUsers = !expandedUsers }
            )
        }

        if (expandedUsers) {
            items(normalUsers) { user ->
                UserManagementItemNew(
                    user = user,
                    isCurrentUser = user.userId == currentUser?.userId,
                    onEdit = { editingUser = user },
                    onSelect = {
                        if (user.userId != currentUser?.userId) {
                            onUserSelected(user)
                        }
                    }
                )
            }
        }
    }

    // 编辑对话框
    editingUser?.let { user ->
        UserEditDialog(
            user = user,
            onDismiss = { editingUser = null },
            onSave = { newName ->
                onUpdateUserName(user.userId, newName)
                editingUser = null
            }
        )
    }
}

/**
 * 用户组标题
 */
@Composable
fun UserGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3E3E3E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 展开/折叠箭头
            Text(
                text = if (isExpanded) "▼" else "▶",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(end = 12.dp)
            )

            // 标题
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // 数量徽章
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 用户管理列表项（新版，带边框和缩进）
 */
@Composable
fun UserManagementItemNew(
    user: com.echo.lutian.data.entity.User,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp) // 缩进
            .clickable(enabled = !isCurrentUser, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFF424242) else Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (isCurrentUser) Color(0xFF666666) else Color(0xFF4CAF50).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isCurrentUser) Color(0xFF666666) else Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(当前)",
                            fontSize = 14.sp,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设备: ${user.deviceId.take(13)}...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 改名按钮
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                ) {
                    Text(
                        text = "✎",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                // 查看对话按钮（当前用户不显示）
                if (!isCurrentUser) {
                    IconButton(
                        onClick = onSelect,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 缓存管理面板
 */
@Composable
fun CacheManagementPanel(
    audioRecords: List<AudioRecord>,
    currentUserId: String?,
    onPlayAudio: (AudioRecord) -> Unit,
    onDeleteAudio: (AudioRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "本地缓存 (${audioRecords.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 录音列表
        if (audioRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无缓存",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audioRecords) { record ->
                    CachedAudioItem(
                        record = record,
                        currentUserId = currentUserId,
                        onPlay = { onPlayAudio(record) },
                        onDelete = { onDeleteAudio(record) }
                    )
                }
            }
        }
    }
}

/**
 * 缓存音频列表项
 */
@Composable
fun CachedAudioItem(
    record: AudioRecord,
    currentUserId: String?,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 发送者/接收者信息
                val direction = when {
                    record.senderId == currentUserId -> "发送给: ${record.receiverId ?: "未知"}"
                    record.receiverId == currentUserId -> "来自: ${record.senderId ?: "未知"}"
                    else -> "ID: ${record.id}"
                }

                Text(
                    text = direction,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "时长: ${record.duration}s",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = formatTimestamp(record.createdAt),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (record.isPlayed) {
                    Text(
                        text = "✓ 已播放",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // 播放按钮
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3))
            ) {
                Text(
                    text = "▶",
                    fontSize = 20.sp,
                    color = Color.White
                )
        }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336))
            ) {
                Text(
                    text = "✕",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return format.format(date)
}
