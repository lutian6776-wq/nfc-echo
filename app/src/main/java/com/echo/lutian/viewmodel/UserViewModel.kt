package com.echo.lutian.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echo.lutian.data.database.HeartEchoDatabase
import com.echo.lutian.data.entity.User
import com.echo.lutian.network.MessageInfo
import com.echo.lutian.network.NetworkRepository
import com.echo.lutian.network.UserInfo
import com.echo.lutian.util.DeviceIdUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户管理 ViewModel
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HeartEchoDatabase.getDatabase(application)
    private val userDao = database.userDao()
    private val networkRepository = NetworkRepository(application)

    // 当前用户
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // 所有用户列表
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // 选中的用户（管理员查看对话用）
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    // 对话消息列表
    private val _conversationMessages = MutableStateFlow<List<MessageInfo>>(emptyList())
    val conversationMessages: StateFlow<List<MessageInfo>> = _conversationMessages.asStateFlow()

    // 最新发送消息的已读状态（用于主界面指示器）
    private val _latestSentMessageRead = MutableStateFlow<Boolean?>(null)
    val latestSentMessageRead: StateFlow<Boolean?> = _latestSentMessageRead.asStateFlow()

    // 是否有未读的新消息（管理员发给当前用户的）
    private val _hasUnreadNewMessage = MutableStateFlow<Boolean?>(null)
    val hasUnreadNewMessage: StateFlow<Boolean?> = _hasUnreadNewMessage.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 监听当前用户变化
        viewModelScope.launch {
            userDao.getCurrentUserFlow().collect { user ->
                _currentUser.value = user
            }
        }

        // 监听所有用户变化
        viewModelScope.launch {
            userDao.getAllUsers().collect { userList ->
                _users.value = userList
            }
        }
    }

    /**
     * 用户识别（基于设备ID自动登录）
     */
    fun identifyUser() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val deviceId = DeviceIdUtil.getDeviceId(getApplication())
                val deviceModel = DeviceIdUtil.getDeviceModel()

                android.util.Log.d("UserViewModel", "开始用户识别: deviceId=$deviceId, model=$deviceModel")

                // 先检查本地是否有缓存
                val cachedUser = userDao.getCurrentUser()
                if (cachedUser != null) {
                    android.util.Log.d("UserViewModel", "使用缓存的用户信息: ${cachedUser.name}")
                    // 缓存存在，立即使用（让用户快速进入）
                    _currentUser.value = cachedUser
                }

                // 后台从云端获取最新数据
                val userInfo = networkRepository.identifyUser(deviceId, deviceModel)

                if (userInfo != null) {
                    // 保存到本地数据库（更新缓存）
                    val localUser = User(
                        userId = userInfo.userId,
                        deviceId = userInfo.deviceId,
                        name = userInfo.name,
                        nfcTagId = userInfo.nfcTagId,
                        role = userInfo.role,
                        createdAt = System.currentTimeMillis(),
                        lastActiveAt = System.currentTimeMillis(),
                        isCurrentUser = true
                    )

                    // 先清除旧数据
                    userDao.clearCurrentUser()
                    // 插入新数据
                    userDao.insertUser(localUser)

                    android.util.Log.d("UserViewModel", "用户信息已更新: ${localUser.name}, 角色: ${localUser.role}")
                } else if (cachedUser == null) {
                    // 没有缓存且网络请求失败
                    android.util.Log.e("UserViewModel", "云端识别失败且无本地缓存")
                } else {
                    // 有缓存但网络请求失败，继续使用缓存
                    android.util.Log.w("UserViewModel", "云端识别失败，继续使用缓存数据")
                }

            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "用户识别失败", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 同步所有用户列表（管理员用）
     */
    fun syncAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userInfoList = networkRepository.getAllUsers()

                if (userInfoList != null) {
                    // 获取当前用户ID，以便保留其状态
                    val currentUserId = _currentUser.value?.userId

                    // 转换为本地实体并保存
                    val users = userInfoList.map { userInfo ->
                        User(
                            userId = userInfo.userId,
                            deviceId = userInfo.deviceId,
                            name = userInfo.name,
                            nfcTagId = userInfo.nfcTagId,
                            role = userInfo.role,
                            createdAt = System.currentTimeMillis(),
                            lastActiveAt = System.currentTimeMillis(),
                            isCurrentUser = userInfo.userId == currentUserId
                        )
                    }
                    userDao.insertUsers(users)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 选择用户（管理员查看对话）
     */
    fun selectUser(user: User) {
        _selectedUser.value = user
        loadConversation(user.userId)
    }

    /**
     * 加载对话消息
     */
    private fun loadConversation(otherUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val currentUserId = _currentUser.value?.userId
                if (currentUserId != null) {
                    val messages = networkRepository.getConversation(currentUserId, otherUserId)
                    if (messages != null) {
                        _conversationMessages.value = messages
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 刷新对话消息
     */
    fun refreshConversation() {
        _selectedUser.value?.let { user ->
            loadConversation(user.userId)
        }
    }

    /**
     * 加载当前用户的历史对话（与管理员）
     */
    fun loadMyHistory() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val currentUserId = _currentUser.value?.userId

                if (currentUserId != null) {
                    // 先同步用户列表以获取管理员信息
                    val userInfoList = networkRepository.getAllUsers()
                    var adminUserId: String? = null

                    if (userInfoList != null) {
                        // 直接从 API 响应中查找管理员
                        adminUserId = userInfoList.firstOrNull { it.role == "admin" }?.userId

                        // 保存到数据库
                        val users = userInfoList.map { userInfo ->
                            User(
                                userId = userInfo.userId,
                                deviceId = userInfo.deviceId,
                                name = userInfo.name,
                                nfcTagId = userInfo.nfcTagId,
                                role = userInfo.role,
                                createdAt = System.currentTimeMillis(),
                                lastActiveAt = System.currentTimeMillis(),
                                isCurrentUser = userInfo.userId == currentUserId
                            )
                        }
                        userDao.insertUsers(users)
                    }

                    // 使用从 API 获取的管理员 ID
                    if (adminUserId != null) {
                        val messages = networkRepository.getConversation(currentUserId, adminUserId)
                        if (messages != null) {
                            _conversationMessages.value = messages
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 检查最新发送消息的已读状态
     */
    fun checkLatestSentMessageStatus() {
        viewModelScope.launch {
            try {
                val currentUserId = _currentUser.value?.userId

                if (currentUserId != null) {
                    var adminUserId: String? = null

                    // 先确保有用户列表
                    if (_users.value.isEmpty()) {
                        val userInfoList = networkRepository.getAllUsers()
                        if (userInfoList != null) {
                            // 直接从 API 响应中查找管理员
                            adminUserId = userInfoList.firstOrNull { it.role == "admin" }?.userId

                            // 保存到数据库
                            val users = userInfoList.map { userInfo ->
                                User(
                                    userId = userInfo.userId,
                                    deviceId = userInfo.deviceId,
                                    name = userInfo.name,
                                    nfcTagId = userInfo.nfcTagId,
                                    role = userInfo.role,
                                    createdAt = System.currentTimeMillis(),
                                    lastActiveAt = System.currentTimeMillis(),
                                    isCurrentUser = userInfo.userId == currentUserId
                                )
                            }
                            userDao.insertUsers(users)
                        }
                    } else {
                        // 从已有的用户列表中查找
                        adminUserId = _users.value.firstOrNull { it.role == "admin" }?.userId
                    }

                    if (adminUserId != null) {
                        val messages = networkRepository.getConversation(currentUserId, adminUserId, limit = 10)
                        if (messages != null) {
                            // 找到最新的由当前用户发送的消息
                            val latestSentMessage = messages
                                .filter { it.senderId == currentUserId }
                                .maxByOrNull { it.createdAt }

                            _latestSentMessageRead.value = latestSentMessage?.isPlayed
                            
                            // 找到是否有未读的由管理员发送的新消息
                            val hasUnreadFromAdmin = messages
                                .any { it.senderId == adminUserId && !it.isPlayed }
                            _hasUnreadNewMessage.value = hasUnreadFromAdmin
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新用户信息（管理员用）
     */
    fun updateUser(userId: String, name: String? = null, nfcTagId: String? = null, role: String? = null) {
        viewModelScope.launch {
            try {
                val success = networkRepository.updateUser(userId, name, nfcTagId, role)
                if (success) {
                    // 重新同步用户列表
                    syncAllUsers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 绑定设备到用户（管理员用）
     */
    fun bindDevice(userId: String, deviceId: String) {
        viewModelScope.launch {
            try {
                val success = networkRepository.bindDevice(userId, deviceId)
                if (success) {
                    // 重新同步用户列表
                    syncAllUsers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 清除选中的用户
     */
    fun clearSelectedUser() {
        _selectedUser.value = null
        _conversationMessages.value = emptyList()
    }
}
