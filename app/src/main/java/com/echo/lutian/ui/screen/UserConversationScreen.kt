package com.echo.lutian.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.lutian.data.entity.User
import com.echo.lutian.network.MessageInfo

/**
 * 用户对话详情界面
 */
@Composable
fun UserConversationScreen(
    user: User,
    currentUserId: String?,
    messages: List<MessageInfo>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlayMessage: (MessageInfo) -> Unit,
    onDeleteMessage: (MessageInfo) -> Unit,
    onRefresh: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // 顶部标题栏
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
                text = "与 ${user.name} 的对话",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // 刷新按钮
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "刷新中..." else "🔄 刷新")
            }
        }

        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF4CAF50)
                )
            }
        } else if (messages.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无消息",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            // 消息列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true // 最新消息在底部
            ) {
                items(messages) { message ->
                    ConversationMessageItem(
                        message = message,
                        isSentByMe = message.senderId == currentUserId,
                        onPlay = { onPlayMessage(message) },
                        onDelete = { onDeleteMessage(message) }
                    )
                }
            }
        }
    }
}

/**
 * 对话消息列表项
 */
@Composable
fun ConversationMessageItem(
    message: MessageInfo,
    isSentByMe: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSentByMe) Color(0xFF4CAF50) else Color(0xFF424242)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isSentByMe) 16.dp else 4.dp,
                bottomEnd = if (isSentByMe) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 方向标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSentByMe) "发送" else "接收",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    // 已读状态指示
                    Text(
                        text = if (message.isPlayed) "✓ 已读" else "未读",
                        fontSize = 11.sp,
                        color = if (message.isPlayed) Color(0xFF4CAF50) else Color(0xFFFFC107),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放图标
                    Text(
                        text = if (message.isPlayed) "▶" else "🔴",
                        fontSize = 24.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${message.duration}秒",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formatMessageTime(message.createdAt),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 播放按钮
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("播放", fontSize = 14.sp)
                    }

                    // 删除按钮
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("删除", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * 格式化消息时间
 */
private fun formatMessageTime(timestamp: String): String {
    return try {
        // 解析 ISO 8601 格式的时间戳
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val date = inputFormat.parse(timestamp)

        // 格式化为本地时间
        val outputFormat = java.text.SimpleDateFormat("MM月dd日 HH:mm", java.util.Locale.getDefault())
        outputFormat.timeZone = java.util.TimeZone.getDefault()

        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        // 如果解析失败，尝试简单提取时间部分
        try {
            timestamp.substringAfter("T").substringBefore(".")
        } catch (e2: Exception) {
            timestamp
        }
    }
}
