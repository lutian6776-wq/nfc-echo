package com.echo.lutian.network

import android.content.Context
import android.util.Log
import com.echo.lutian.data.entity.AudioRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URL

import com.echo.lutian.util.AppPreferences

/**
 * 网络仓库，处理所有网络请求
 */
class NetworkRepository(private val context: Context) {

    private val prefs = AppPreferences(context)
    private val TAG = "NetworkRepository"

    init {
        RetrofitClient.init(prefs.serverUrl)
    }

    private val apiService: ApiService
        get() = RetrofitClient.apiService ?: throw IllegalStateException("Backend server URL is not configured")

    fun updateServerUrl(url: String) {
        prefs.serverUrl = url
        RetrofitClient.init(url)
    }

    // ==================== 系统管理相关 ====================

    suspend fun getSystemStatus(): SystemStatusResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSystemStatus()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "getSystemStatus error", e)
                null
            }
        }
    }

    suspend fun initAdmin(deviceId: String, password: String, name: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.initAdmin(InitAdminRequest(deviceId, password, name))
                response.isSuccessful && response.body()?.success == true
            } catch (e: Exception) {
                Log.e(TAG, "initAdmin error", e)
                false
            }
        }
    }

    suspend fun updateUserRole(targetUserId: String?, role: String?, adminPassword: String, newPassword: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateUserRole(UpdateRoleRequest(targetUserId, role, adminPassword, newPassword))
                response.isSuccessful && response.body()?.success == true
            } catch (e: Exception) {
                Log.e(TAG, "updateUserRole error", e)
                false
            }
        }
    }

    // ==================== 用户管理接口 ====================

    /**
     * 用户识别（基于设备ID自动登录）
     */
    suspend fun identifyUser(deviceId: String, name: String? = null): UserInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = IdentifyUserRequest(deviceId, name)
                val response = apiService.identifyUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "User identified: ${body.user.name} (${body.user.userId})")
                    return@withContext body.user
                } else {
                    Log.e(TAG, "Identify user failed: ${response.code()} ${response.message()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Identify user error", e)
                return@withContext null
            }
        }
    }

    /**
     * 获取所有用户列表（管理员用）
     */
    suspend fun getAllUsers(): List<UserInfo>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllUsers()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Got ${body.count} users")
                    return@withContext body.users
                } else {
                    Log.e(TAG, "Get users failed: ${response.code()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Get users error", e)
                return@withContext null
            }
        }
    }

    /**
     * 更新用户信息（管理员用）
     */
    suspend fun updateUser(userId: String, name: String? = null, nfcTagId: String? = null, role: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateUserRequest(name, nfcTagId, role)
                val response = apiService.updateUser(userId, request)

                if (response.isSuccessful) {
                    Log.d(TAG, "User updated: $userId")
                    return@withContext true
                } else {
                    Log.e(TAG, "Update user failed: ${response.code()}")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Update user error", e)
                return@withContext false
            }
        }
    }

    /**
     * 绑定设备ID到用户（管理员用）
     */
    suspend fun bindDevice(userId: String, deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = BindDeviceRequest(deviceId)
                val response = apiService.bindDevice(userId, request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Device bound: $deviceId -> $userId")
                    return@withContext true
                } else {
                    Log.e(TAG, "Bind device failed: ${response.code()}")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Bind device error", e)
                return@withContext false
            }
        }
    }

    // ==================== 消息接口 ====================

    /**
     * 上传音频文件到云端
     * @param audioRecord 音频记录
     * @param senderId 发送者用户ID
     * @param receiverId 接收者用户ID
     * @return 上传成功返回云端 ID 和 URL，失败返回 null
     */
    suspend fun uploadAudio(audioRecord: AudioRecord, senderId: String, receiverId: String): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(audioRecord.filePath)
                if (!file.exists()) {
                    Log.e(TAG, "File not found: ${audioRecord.filePath}")
                    return@withContext null
                }

                // 创建 multipart 请求
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val senderPart = senderId.toRequestBody("text/plain".toMediaTypeOrNull())
                val receiverPart = receiverId.toRequestBody("text/plain".toMediaTypeOrNull())
                val durationPart = audioRecord.duration.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // 发送请求
                val response = apiService.uploadAudio(filePart, senderPart, receiverPart, durationPart)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Upload successful: ${body.id}, ${body.fileUrl}")
                    return@withContext Pair(body.id, body.fileUrl)
                } else {
                    Log.e(TAG, "Upload failed: ${response.code()} ${response.message()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                return@withContext null
            }
        }
    }

    /**
     * 获取最新的语音消息
     * @param userId 当前用户ID
     * @param fromUserId 筛选来自特定用户的消息（可选）
     * @return 最新消息的元数据，失败返回 null
     */
    suspend fun getLatestMessage(userId: String, fromUserId: String? = null): LatestMessageResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLatestMessage(userId, fromUserId)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Got latest message: ${body.id}")
                    return@withContext body
                } else {
                    Log.e(TAG, "Get latest failed: ${response.code()} ${response.message()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Get latest error", e)
                return@withContext null
            }
        }
    }

    /**
     * 获取两个用户之间的消息列表（管理员用）
     */
    suspend fun getConversation(userId1: String, userId2: String, limit: Int = 50): List<MessageInfo>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversation(userId1, userId2, limit)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Got ${body.count} messages in conversation")
                    return@withContext body.messages
                } else {
                    Log.e(TAG, "Get conversation failed: ${response.code()}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Get conversation error", e)
                return@withContext null
            }
        }
    }

    /**
     * 下载音频文件到本地
     * @param fileUrl 文件 URL
     * @param localPath 本地保存路径
     * @return 下载成功返回 true
     */
    suspend fun downloadAudio(fileUrl: String, localPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading from: $fileUrl")
                Log.d(TAG, "Saving to: $localPath")

                val url = URL(fileUrl)
                val connection = url.openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val outputFile = File(localPath)

                // 确保父目录存在
                outputFile.parentFile?.mkdirs()

                FileOutputStream(outputFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Download successful: $localPath")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                return@withContext false
            }
        }
    }

    /**
     * 标记消息为已播放
     * @param cloudId 云端消息 ID
     */
    suspend fun markAsPlayed(cloudId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.markAsPlayed(cloudId)

                if (response.isSuccessful) {
                    Log.d(TAG, "Marked as played: $cloudId")
                    return@withContext true
                } else {
                    Log.e(TAG, "Mark played failed: ${response.code()}")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Mark played error", e)
                return@withContext false
            }
        }
    }

    /**
     * 删除消息（包括云端文件）
     */
    suspend fun deleteMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteMessage(messageId)

                if (response.isSuccessful) {
                    Log.d(TAG, "Message deleted: $messageId")
                    return@withContext true
                } else {
                    Log.e(TAG, "Delete message failed: ${response.code()}")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Delete message error", e)
                return@withContext false
            }
        }
    }

    /**
     * 健康检查
     */
    suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.healthCheck()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Health check error", e)
                false
            }
        }
    }
}
