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
 * 接收者选择界面
 */
@Composable
fun ReceiverSelectionScreen(
    users: List<User>,
    currentUserId: String?,
    onReceiverSelected: (User) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E2E))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择接收者",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 用户列表
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无可选用户",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(users.filter { it.userId != currentUserId }) { user ->
                        ReceiverCard(
                            user = user,
                            onClick = { onReceiverSelected(user) }
                        )
                    }
                }
            }

            // 底部取消按钮
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF424242)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "取消",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 接收者卡片
 */
@Composable
fun ReceiverCard(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像（首字母）
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (user.role == "admin") {
                    Text(
                        text = "👑 管理员",
                        fontSize = 14.sp,
                        color = Color(0xFFFFC107)
                    )
                }
            }

            // 箭头
            Text(
                text = "▶",
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
