package com.echo.lutian.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * HeartEcho API 服务接口
 */
interface ApiService {

    // ==================== 系统管理相关 ====================

    @GET("api/system/status")
    suspend fun getSystemStatus(): Response<SystemStatusResponse>

    @POST("api/admin/init")
    suspend fun initAdmin(
        @Body request: InitAdminRequest
    ): Response<BasicResponse>

    @PUT("api/users/role")
    suspend fun updateUserRole(
        @Body request: UpdateRoleRequest
    ): Response<BasicResponse>

    // ==================== 用户管理接口 ====================

    /**
     * 用户识别（基于设备ID自动登录）
     * @param request 包含 deviceId, name, nfcTagId
     */
    @POST("api/users/identify")
    suspend fun identifyUser(
        @Body request: IdentifyUserRequest
    ): Response<IdentifyUserResponse>

    /**
     * 获取所有用户列表（管理员用）
     */
    @GET("api/users")
    suspend fun getAllUsers(): Response<UsersListResponse>

    /**
     * 更新用户信息（管理员用）
     */
    @PUT("api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UpdateUserRequest
    ): Response<BasicResponse>

    /**
     * 绑定设备ID到用户（管理员用）
     */
    @POST("api/users/{userId}/bind_device")
    suspend fun bindDevice(
        @Path("userId") userId: String,
        @Body request: BindDeviceRequest
    ): Response<BasicResponse>

    // ==================== 消息接口 ====================

    /**
     * 上传音频文件
     * @param file 音频文件
     * @param senderId 发送者用户ID
     * @param receiverId 接收者用户ID
     * @param duration 音频时长（秒）
     */
    @Multipart
    @POST("api/upload_audio")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part("senderId") senderId: RequestBody,
        @Part("receiverId") receiverId: RequestBody,
        @Part("duration") duration: RequestBody
    ): Response<UploadResponse>

    /**
     * 获取最新的语音消息
     * @param userId 当前用户ID（必填）
     * @param fromUserId 筛选来自特定用户的消息（可选）
     */
    @GET("api/get_latest")
    suspend fun getLatestMessage(
        @Query("userId") userId: String,
        @Query("fromUserId") fromUserId: String? = null
    ): Response<LatestMessageResponse>

    /**
     * 获取两个用户之间的消息列表（管理员用）
     */
    @GET("api/messages/conversation")
    suspend fun getConversation(
        @Query("userId1") userId1: String,
        @Query("userId2") userId2: String,
        @Query("limit") limit: Int = 50
    ): Response<ConversationResponse>

    /**
     * 标记消息为已播放
     * @param id 消息 ID
     */
    @POST("api/mark_played/{id}")
    suspend fun markAsPlayed(
        @Path("id") id: String
    ): Response<MarkPlayedResponse>

    /**
     * 删除消息
     * @param id 消息 ID
     */
    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String
    ): Response<Map<String, Any>>

    /**
     * 健康检查
     */
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}
