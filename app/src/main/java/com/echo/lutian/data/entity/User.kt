package com.echo.lutian.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,              // 云端用户ID

    val deviceId: String,            // 设备ID (IMEI/Android ID)
    val name: String,                // 用户名称
    val nfcTagId: String? = null,    // 关联的NFC标签ID
    val role: String = "user",       // 角色: user 或 admin

    val createdAt: Long,             // 创建时间戳
    val lastActiveAt: Long,          // 最后活跃时间

    val isCurrentUser: Boolean = false // 是否为当前登录用户
)
