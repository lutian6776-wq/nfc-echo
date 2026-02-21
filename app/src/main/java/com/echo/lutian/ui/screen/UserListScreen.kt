package com.echo.lutian.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.lutian.data.entity.User

/**
 * 用户列表界面（管理员用）
 */
@Composable
fun UserListScreen(
    users: List<User>,
    currentUserId: String?,
    onUserSelected: (User) -> Unit,
    onBack: () -> Unit
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
                text = "用户列表",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 用户列表
        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无用户",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserListItem(
                        user = user,
                        isCurrentUser = user.userId == currentUserId,
                        onClick = { onUserSelected(user) }
                    )
                }
            }
        }
    }
}

/**
 * 用户列表项
 */
@Composable
fun UserListItem(
    user: User,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFF4CAF50) else Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像（使用首字母）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.3f) else Color(0xFF4CAF50),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "设备: ${user.deviceId.take(12)}...",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (user.role == "admin") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "👑 管理员",
                        fontSize = 12.sp,
                        color = Color(0xFFFFC107)
                    )
                }
            }

            // 箭头图标
            Text(
                text = "▶",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 用户对话界面（管理员查看与某个用户的聊天记录）
 */
@Composable
fun UserConversationScreen(
    user: User,
    adminUserId: String,
    messages: List<com.echo.lutian.network.MessageInfo>,
    onBack: () -> Unit,
    onPlayMessage: (String) -> Unit
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
                color = Color.White
            )
        }

        // 消息列表
        if (messages.isEmpty()) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true // 最新消息在底部
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isSentByMe = message.senderId == adminUserId,
                        onPlay = { onPlayMessage(message.id) }
                    )
                }
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
fun MessageBubble(
    message: com.echo.lutian.network.MessageInfo,
    isSentByMe: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable(onClick = onPlay),
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
            Row(
                modifier = Modifier.padding(12.dp),
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
        }
    }
}

/**
 * 格式化消息时间
 */
private fun formatMessageTime(timestamp: String): String {
    // 简单格式化，实际应该使用 SimpleDateFormat
    return timestamp.substringAfter("T").substringBefore(".")
}
