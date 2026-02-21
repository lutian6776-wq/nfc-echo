package com.echo.lutian.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 音频记录实体
 */
@Entity(tableName = "audio_records")
data class AudioRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val filePath: String,           // 音频文件路径
    val duration: Int,              // 音频时长（秒）
    val createdAt: Long,            // 创建时间戳
    val nfcTagId: String? = null,   // 关联的 NFC 标签 ID
    val isPlayed: Boolean = false,  // 是否已播放

    // 用户隔离字段
    val senderId: String? = null,   // 发送者用户ID
    val receiverId: String? = null, // 接收者用户ID

    // 云端同步字段
    val cloudId: String? = null,    // 云端消息 ID
    val cloudUrl: String? = null,   // 云端文件 URL
    val isUploaded: Boolean = false // 是否已上传到云端
)
