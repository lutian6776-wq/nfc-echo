package com.echo.lutian.network

import com.google.gson.annotations.SerializedName

// ==================== 系统管理相关 ====================

data class SystemStatusResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("isInitialized")
    val isInitialized: Boolean,
    @SerializedName("error")
    val error: String? = null
)

data class InitAdminRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("name")
    val name: String? = null
)

data class UpdateRoleRequest(
    @SerializedName("targetUserId")
    val targetUserId: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("adminPassword")
    val adminPassword: String,
    @SerializedName("newPassword")
    val newPassword: String? = null
)

// ==================== 用户管理相关 ====================

/**
 * 用户识别请求
 */
data class IdentifyUserRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("nfcTagId")
    val nfcTagId: String? = null
)

/**
 * 用户识别响应
 */
data class IdentifyUserResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("user")
    val user: UserInfo
)

/**
 * 用户信息
 */
data class UserInfo(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("nfcTagId")
    val nfcTagId: String?,
    @SerializedName("role")
    val role: String
)

/**
 * 用户列表响应
 */
data class UsersListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("count")
    val count: Int,
    @SerializedName("users")
    val users: List<UserInfo>
)

/**
 * 更新用户请求
 */
data class UpdateUserRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("nfcTagId")
    val nfcTagId: String? = null,
    @SerializedName("role")
    val role: String? = null
)

/**
 * 绑定设备请求
 */
data class BindDeviceRequest(
    @SerializedName("deviceId")
    val deviceId: String
)

/**
 * 基础响应
 */
data class BasicResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

// ==================== 消息相关 ====================

/**
 * 上传响应
 */
data class UploadResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("id")
    val id: String,
    @SerializedName("fileUrl")
    val fileUrl: String,
    @SerializedName("message")
    val message: String
)

/**
 * 获取最新消息响应
 */
data class LatestMessageResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("id")
    val id: String,
    @SerializedName("fileUrl")
    val fileUrl: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("receiverId")
    val receiverId: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("fileSize")
    val fileSize: Long,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("isPlayed")
    val isPlayed: Boolean
)

/**
 * 消息信息
 */
data class MessageInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("fileUrl")
    val fileUrl: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("receiverId")
    val receiverId: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("fileSize")
    val fileSize: Long,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("isPlayed")
    val isPlayed: Boolean
)

/**
 * 对话消息列表响应
 */
data class ConversationResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("count")
    val count: Int,
    @SerializedName("messages")
    val messages: List<MessageInfo>
)

/**
 * 标记已播放响应
 */
data class MarkPlayedResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

/**
 * 错误响应
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: String,
    @SerializedName("details")
    val details: String? = null
)

