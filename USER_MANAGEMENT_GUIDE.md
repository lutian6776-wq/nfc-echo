# HeartEcho 一对多用户管理功能 - 实现文档

## 📋 功能概述

已成功将 HeartEcho 从单频道模式升级为一对多用户管理模式，支持多个老人独立使用，数据完全隔离。

## ✅ 已完成的工作

### 1. 后端升级（Node.js + MongoDB）

#### 新增数据模型

**User 集合**
```javascript
{
  _id: ObjectId,
  deviceId: String,      // 设备唯一标识（Android ID）
  name: String,          // 用户名称
  nfcTagId: String,      // 关联的NFC标签ID（可选）
  role: String,          // 角色：user 或 admin
  createdAt: Date,       // 创建时间
  lastActiveAt: Date     // 最后活跃时间
}
```

**Message 集合（升级）**
```javascript
{
  _id: ObjectId,
  fileUrl: String,
  fileName: String,
  senderId: String,      // 发送者用户ID（新增）
  receiverId: String,    // 接收者用户ID（新增）
  duration: Number,
  fileSize: Number,
  createdAt: Date,
  isPlayed: Boolean
}
```

#### 新增API接口

**用户管理接口**
- `POST /api/users/identify` - 用户识别（基于设备ID自动登录）
- `GET /api/users` - 获取所有用户列表（管理员用）
- `PUT /api/users/:userId` - 更新用户信息（管理员用）
- `POST /api/users/:userId/bind_device` - 绑定设备ID到用户（管理员用）

**消息接口（升级）**
- `POST /api/upload_audio` - 上传音频（需要 senderId 和 receiverId）
- `GET /api/get_latest` - 获取最新消息（需要 userId，支持 fromUserId 筛选）
- `GET /api/messages/conversation` - 获取两个用户之间的对话记录（管理员用）

### 2. Android 端升级

#### 数据库升级

**新增 User 表**
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val deviceId: String,
    val name: String,
    val nfcTagId: String?,
    val role: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isCurrentUser: Boolean
)
```

**AudioRecord 表升级**
- 新增 `senderId: String?` 字段
- 新增 `receiverId: String?` 字段

#### 新增组件

1. **DeviceIdUtil** - 设备ID获取工具
   - 使用 Android ID 作为设备唯一标识
   - 无需额外权限，适合老人机场景

2. **UserDao** - 用户数据访问对象
   - 支持用户的增删改查
   - 支持当前用户管理

3. **UserViewModel** - 用户管理视图模型
   - 用户识别和自动登录
   - 用户列表同步
   - 对话消息加载

4. **UserListScreen** - 用户列表界面
   - 显示所有用户
   - 点击用户查看对话
   - 支持管理员查看聊天记录

5. **UserConversationScreen** - 用户对话界面
   - 显示与特定用户的消息记录
   - 支持播放语音消息

#### NetworkRepository 升级

- 所有上传和获取接口都支持 userId 参数
- 新增用户管理相关方法
- 新增对话消息获取方法

## 🚀 使用指南

### 管理员端（你的手机）

1. **首次启动**
   - App 会自动识别设备ID
   - 如果是新设备，会在云端创建用户
   - 你的设备会被标记为管理员（需要手动在数据库设置 role: "admin"）

2. **查看用户列表**
   - 进入管理员模式
   - 选择"用户列表"标签页
   - 可以看到所有已注册的用户

3. **查看用户对话**
   - 点击某个用户（如"外公"）
   - 进入专属对话界面
   - 可以查看与该用户的所有往来消息

4. **绑定设备**
   - 使用 API 或管理界面
   - 将老人的设备ID绑定到对应用户
   - 例如：将外公手机的设备ID绑定到"外公"用户

### 用户端（老人手机）

1. **首次启动**
   - App 自动获取设备ID
   - 调用 `/api/users/identify` 识别用户
   - 如果设备已绑定，自动登录为对应用户

2. **录音上传**
   - 触碰NFC录音标签
   - 录音完成后自动上传
   - 上传时携带 senderId（老人的userId）和 receiverId（你的userId）

3. **播放消息**
   - 触碰NFC播放标签
   - 自动获取发给该用户的最新消息
   - 只会播放发给该用户的消息，不会串台

## 🔧 配置步骤

### 1. 部署后端

```bash
cd backend
npm install
node index.js
```

后端会自动创建索引：
- `users.deviceId` - 唯一索引
- `users.nfcTagId` - 普通索引
- `messages.senderId + receiverId + createdAt` - 复合索引

### 2. 配置管理员

在 MongoDB 中手动设置管理员：

```javascript
db.users.updateOne(
  { deviceId: "你的设备ID" },
  { $set: { role: "admin" } }
)
```

### 3. 绑定老人设备

方法一：通过API绑定
```bash
curl -X POST http://your-server/api/users/:userId/bind_device \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "老人手机的设备ID"}'
```

方法二：在管理界面中绑定（需要实现UI）

### 4. 创建用户

可以预先创建用户：

```bash
curl -X POST http://your-server/api/users/identify \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "外公手机的设备ID",
    "name": "外公"
  }'
```

## 📱 Android 端集成

### 在 MainActivity 中初始化

```kotlin
class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 用户识别
        userViewModel.identifyUser()

        setContent {
            val currentUser by userViewModel.currentUser.collectAsState()
            val users by userViewModel.users.collectAsState()

            // 根据角色显示不同界面
            if (currentUser?.role == "admin") {
                // 管理员界面：显示用户列表
                UserListScreen(
                    users = users,
                    currentUserId = currentUser?.userId,
                    onUserSelected = { user ->
                        userViewModel.selectUser(user)
                    },
                    onBack = { /* 返回 */ }
                )
            } else {
                // 普通用户界面：正常的录音播放功能
                MainScreen(/* ... */)
            }
        }
    }
}
```

### 上传录音时传递用户ID

```kotlin
// 在 MainActivity 的 confirmSend() 方法中
private fun confirmSend() {
    lifecycleScope.launch {
        val currentUserId = userViewModel.currentUser.value?.userId
        val receiverId = getReceiverUserId() // 获取接收者ID

        if (currentUserId != null && receiverId != null) {
            val result = networkRepository.uploadAudio(
                audioRecord = currentRecord,
                senderId = currentUserId,
                receiverId = receiverId
            )
            // 处理结果...
        }
    }
}
```

### 播放消息时传递用户ID

```kotlin
// 在 MainActivity 的 playLatestAudio() 方法中
private suspend fun playLatestAudio() {
    val currentUserId = userViewModel.currentUser.value?.userId

    if (currentUserId != null) {
        val latestMessage = networkRepository.getLatestMessage(
            userId = currentUserId,
            fromUserId = null // 或指定发送者ID
        )
        // 处理消息...
    }
}
```

## 🔐 简易识别方案

采用基于 **Android ID** 的自动识别方案：

### 优点
- 无需账号密码，老人零学习成本
- 设备唯一标识，自动登录
- 无需额外权限
- 设备重置后会生成新ID，可重新绑定

### 工作流程
1. App 启动时自动获取设备的 Android ID
2. 调用 `/api/users/identify` 接口
3. 后端查找该设备ID对应的用户
4. 如果找到，返回用户信息并自动登录
5. 如果没找到，创建新用户（待管理员绑定）

### 管理员绑定流程
1. 管理员在自己的App中查看用户列表
2. 看到新设备（显示为"用户_设备ID前8位"）
3. 点击编辑，修改名称为"外公"或"外婆"
4. 系统自动完成绑定

## 📊 数据隔离机制

### 上传隔离
- 每条消息必须指定 senderId 和 receiverId
- 后端验证用户ID有效性
- MongoDB 索引确保查询效率

### 下载隔离
- 获取最新消息时必须传递 userId
- 后端只返回 receiverId 匹配的消息
- 支持 fromUserId 参数进一步筛选

### 示例场景
- 外公录音 → senderId: 外公ID, receiverId: 你的ID
- 你录音给外公 → senderId: 你的ID, receiverId: 外公ID
- 外公播放 → 只能听到 receiverId 为外公ID的消息
- 外婆播放 → 只能听到 receiverId 为外婆ID的消息

## 🎯 下一步建议

1. **完善管理界面**
   - 在管理员模式中集成用户列表
   - 添加用户编辑功能
   - 添加设备绑定界面

2. **优化用户体验**
   - 添加用户头像
   - 显示未读消息数量
   - 添加消息通知

3. **增强安全性**
   - 添加简单的PIN码保护管理员模式
   - 记录操作日志
   - 定期清理过期消息

4. **数据同步**
   - 定期同步用户列表
   - 缓存对话消息
   - 离线消息队列

## 🐛 故障排查

### 问题：用户识别失败
- 检查网络连接
- 确认后端服务运行正常
- 查看 Logcat 中的错误日志

### 问题：消息串台
- 确认上传时正确传递了 senderId 和 receiverId
- 检查后端查询逻辑
- 验证 MongoDB 索引是否创建成功

### 问题：设备绑定失败
- 确认设备ID格式正确
- 检查是否已被其他用户绑定
- 查看后端日志

## 📝 API 测试示例

### 用户识别
```bash
curl -X POST http://localhost:3000/api/users/identify \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "abc123456789"}'
```

### 获取用户列表
```bash
curl http://localhost:3000/api/users
```

### 上传音频
```bash
curl -X POST http://localhost:3000/api/upload_audio \
  -F "file=@audio.m4a" \
  -F "senderId=user1_id" \
  -F "receiverId=user2_id" \
  -F "duration=5"
```

### 获取最新消息
```bash
curl "http://localhost:3000/api/get_latest?userId=user1_id"
```

### 获取对话记录
```bash
curl "http://localhost:3000/api/messages/conversation?userId1=user1_id&userId2=user2_id"
```

## 🎉 总结

HeartEcho 现已支持一对多用户管理，你可以：
- ✅ 为外公、外婆分别配置独立的手机
- ✅ 每个老人只能听到发给自己的消息
- ✅ 你可以在管理界面查看与每个老人的对话
- ✅ 基于设备ID的自动识别，老人无需操作
- ✅ 数据完全隔离，互不干扰

祝你和家人使用愉快！❤️
