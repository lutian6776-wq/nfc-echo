# HeartEcho 网络同步功能说明

## 功能概述

HeartEcho 现已支持云端同步功能，录音可以自动上传到云端，并在播放时从云端获取最新的录音。

## 架构设计

### 后端（Node.js + MongoDB）

- **上传接口**：`POST /api/upload_audio` - 接收音频文件并保存到云存储
- **获取最新**：`GET /api/get_latest` - 返回最新的语音消息
- **标记已播放**：`POST /api/mark_played/:id` - 记录播放状态
- **健康检查**：`GET /health` - 服务状态检查

### 前端（Android + Retrofit）

- **网络模块**：使用 Retrofit 2.9.0 进行 HTTP 请求
- **本地缓存**：Room 数据库存储已下载的音频
- **离线优先**：优先使用本地缓存，网络失败时回退到本地
- **异步上传**：录音保存后在后台异步上传

## 工作流程

### 1. 录音上传流程

```
用户录音 → 确认发送 → 保存到本地数据库 → 后台异步上传到云端 → 更新云端信息
```

**实现细节**：
1. 用户完成录音并点击"发送"
2. 音频文件保存到本地 Room 数据库
3. 立即返回成功提示（不阻塞用户）
4. 后台启动协程上传到云端
5. 上传成功后更新数据库中的 `cloudId`、`cloudUrl` 和 `isUploaded` 字段
6. 上传失败不影响本地使用

### 2. 播放同步流程

```
触碰 NFC 播放标签 → 显示"检查更新" → 查询云端最新消息 → 检查本地缓存 → 下载（如需要）→ 播放
```

**实现细节**：
1. 用户触碰 NFC 播放标签
2. 进入 `SYNCING` 状态，显示"检查云端更新..."
3. 调用 `GET /api/get_latest` 获取最新消息
4. 根据 `cloudId` 查询本地数据库：
   - **已缓存**：直接播放本地文件
   - **未缓存**：下载文件 → 保存到数据库 → 播放
5. 网络失败时回退到本地最新录音
6. 播放完成后调用 `POST /api/mark_played/:id` 标记已播放

## 数据库变更

### AudioRecord 实体新增字段

```kotlin
data class AudioRecord(
    // ... 原有字段 ...

    // 云端同步字段
    val cloudId: String? = null,    // 云端消息 ID
    val cloudUrl: String? = null,   // 云端文件 URL
    val isUploaded: Boolean = false // 是否已上传到云端
)
```

### 数据库迁移

从版本 1 升级到版本 2，自动添加云端同步字段：

```sql
ALTER TABLE audio_records ADD COLUMN cloudId TEXT DEFAULT NULL
ALTER TABLE audio_records ADD COLUMN cloudUrl TEXT DEFAULT NULL
ALTER TABLE audio_records ADD COLUMN isUploaded INTEGER NOT NULL DEFAULT 0
```

## 网络模块

### 依赖库

```kotlin
// Retrofit for network requests
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Gson for JSON parsing
implementation("com.google.code.gson:gson:2.10.1")
```

### 核心类

1. **ApiService.kt** - Retrofit 接口定义
2. **RetrofitClient.kt** - Retrofit 客户端单例
3. **NetworkRepository.kt** - 网络请求封装
4. **ApiModels.kt** - 数据模型

## 配置说明

### 后端配置

1. **本地开发**：
   ```bash
   cd backend
   npm install
   cp .env.example .env
   # 编辑 .env 配置 MongoDB 连接
   npm start
   ```

2. **Sealos 部署**：
   - 创建 MongoDB 数据库实例
   - 创建云函数，上传 backend 代码
   - 配置环境变量：
     - `MONGO_URI`: MongoDB 连接字符串
     - `BASE_URL`: 云函数公网地址
     - `STORAGE_PATH`: 文件存储路径

### 前端配置

修改 `RetrofitClient.kt` 中的 `BASE_URL`：

```kotlin
// 本地测试（Android 模拟器访问本机）
private const val BASE_URL = "http://10.0.2.2:3000/"

// 生产环境（替换为你的 Sealos 云函数地址）
private const val BASE_URL = "https://your-app.cloud.sealos.io/"
```

## UI 变化

### 新增状态

- **SYNCING**：同步中状态，显示加载动画和同步消息

### 同步界面

```
┌─────────────────────┐
│                     │
│    ⭕ 加载动画      │
│                     │
│   检查云端更新...   │
│                     │
└─────────────────────┘
```

同步消息会动态更新：
- "检查云端更新..."
- "找到云端消息..."
- "下载中..."
- "下载完成，开始播放..."
- "使用本地录音..."
- "网络错误，使用本地录音..."

## 离线优化

### 智能缓存策略

1. **优先本地**：如果本地已有相同 `cloudId` 的录音，直接播放
2. **按需下载**：只下载本地没有的录音
3. **离线回退**：网络失败时自动使用本地最新录音
4. **后台上传**：上传不阻塞用户操作

### 存储管理

- 本地录音保存在 `filesDir/audio/` 目录
- 云端下载的文件命名为 `cloud_{cloudId}.m4a`
- 数据库记录文件路径和云端信息

## 安全性

### 当前实现

- ✅ HTTPS 传输（生产环境）
- ✅ 文件类型验证（仅允许 .m4a）
- ✅ 文件大小限制（10MB）
- ✅ 错误处理和日志记录

### 建议增强

- 🔲 添加 API Key 认证
- 🔲 添加用户登录系统
- 🔲 添加端到端加密
- 🔲 添加速率限制
- 🔲 添加文件访问权限控制

## 测试流程

### 1. 后端测试

```bash
# 启动后端
cd backend
npm start

# 健康检查
curl http://localhost:3000/health

# 测试上传
curl -X POST http://localhost:3000/api/upload_audio \
  -F "file=@test.m4a" \
  -F "sender=test_user" \
  -F "duration=30"

# 测试获取最新
curl http://localhost:3000/api/get_latest
```

### 2. 前端测试

1. **上传测试**：
   - 录制一段语音
   - 点击"发送"
   - 查看 Toast 提示"已同步到云端"
   - 在管理员模式查看录音列表，确认 `isUploaded` 为 true

2. **下载测试**：
   - 清空本地数据库（或使用另一台设备）
   - 触碰 NFC 播放标签
   - 观察同步界面显示"检查云端更新..." → "下载中..." → "下载完成"
   - 确认音频正常播放

3. **离线测试**：
   - 关闭后端服务或断开网络
   - 触碰 NFC 播放标签
   - 确认显示"网络错误，使用本地录音..."
   - 确认仍能播放本地录音

## 性能优化

### 已实现

- ✅ 异步上传（不阻塞 UI）
- ✅ 本地缓存（避免重复下载）
- ✅ 离线优先（快速响应）
- ✅ 连接超时设置（30秒）

### 可优化

- 🔲 添加下载进度显示
- 🔲 支持断点续传
- 🔲 压缩音频文件
- 🔲 使用 CDN 加速
- 🔲 批量上传未同步的录音

## 故障排除

### 上传失败

**症状**：Toast 显示"云端同步失败，已保存到本地"

**可能原因**：
1. 后端服务未启动
2. 网络连接问题
3. 文件格式不正确
4. 文件大小超过限制

**解决方法**：
1. 检查后端服务状态：`curl http://your-backend/health`
2. 检查网络连接
3. 查看 Logcat 日志：`adb logcat | grep NetworkRepository`

### 下载失败

**症状**：同步界面卡在"下载中..."

**可能原因**：
1. 文件 URL 无效
2. 网络超时
3. 存储空间不足

**解决方法**：
1. 检查云端文件是否存在：`curl {fileUrl}`
2. 增加超时时间
3. 清理本地存储空间

### 播放云端录音失败

**症状**：下载成功但无法播放

**可能原因**：
1. 文件损坏
2. 文件格式不兼容
3. 文件路径错误

**解决方法**：
1. 检查下载的文件是否完整
2. 确认文件格式为 .m4a
3. 查看数据库中的 `filePath` 是否正确

## API 文档

详见 `backend/README.md`

## 扩展功能

### 未来可添加

1. **多用户支持**：
   - 用户注册登录
   - 每个用户独立的录音列表
   - 好友系统

2. **消息管理**：
   - 消息已读/未读状态
   - 消息删除
   - 消息过期自动清理

3. **推送通知**：
   - 新消息推送
   - FCM 集成

4. **高级功能**：
   - 语音转文字
   - 消息加密
   - 多语言支持

## 总结

HeartEcho 的网络同步功能已完整实现，包括：

✅ 后端 API 服务（Node.js + MongoDB）
✅ 前端网络模块（Retrofit）
✅ 自动上传录音到云端
✅ 智能下载和缓存
✅ 离线优先策略
✅ 同步状态 UI
✅ 数据库迁移
✅ 错误处理和回退

用户可以在有网络的情况下享受云端同步的便利，在没有网络时仍能正常使用本地功能。
