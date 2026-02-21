package com.echo.lutian.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
 * 用户编辑对话框
 */
@Composable
fun UserEditDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(user.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑用户",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "设备ID: ${user.deviceId.take(12)}...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("用户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onSave(newName)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
