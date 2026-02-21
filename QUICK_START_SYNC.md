# HeartEcho 网络同步快速开始

## 1. 后端部署（5分钟）

### 本地测试

```bash
# 1. 安装 MongoDB（如果没有）
# Windows: https://www.mongodb.com/try/download/community
# Mac: brew install mongodb-community
# Linux: sudo apt-get install mongodb

# 2. 启动 MongoDB
mongod

# 3. 安装后端依赖
cd backend
npm install

# 4. 配置环境变量
cp .env.example .env
# 编辑 .env，确保 MONGO_URI 正确

# 5. 启动后端服务
npm start

# 6. 测试健康检查
curl http://localhost:3000/health
```

### Sealos 云部署

1. **创建 MongoDB 数据库**：
   - 登录 Sealos 控制台
   - 创建 MongoDB 应用
   - 记录连接字符串（格式：`mongodb://username:password@host:port/database`）

2. **部署云函数**：
   - 创建新的云函数应用
   - 上传 `backend/` 目录的所有文件
   - 配置环境变量：
     ```
     MONGO_URI=mongodb://username:password@mongodb-service:27017/heartecho
     BASE_URL=https://your-app.cloud.sealos.io
     STORAGE_PATH=/data/uploads
     PORT=3000
     ```
   - 保存并启动

3. **获取公网地址**：
   - 在云函数详情页找到公网访问地址
   - 例如：`https://heartecho-backend.cloud.sealos.io`

## 2. 前端配置（2分钟）

### 修改后端地址

��辑 `app/src/main/java/com/echo/lutian/network/RetrofitClient.kt`：

```kotlin
object RetrofitClient {
    // 本地测试（Android 模拟器）
    private const val BASE_URL = "http://10.0.2.2:3000/"

    // 本地测试（真机，替换为你的电脑 IP）
    // private const val BASE_URL = "http://192.168.1.100:3000/"

    // Sealos 生产环境（替换为你的云函数地址）
    // private const val BASE_URL = "https://heartecho-backend.cloud.sealos.io/"
}
```

### 构建并安装

```bash
cd /path/to/nfcecho
./gradlew assembleDebug
./gradlew installDebug
```

## 3. 测试流程

### 测试上传

1. 打开 HeartEcho 应用
2. 触碰 NFC 录音标签（或进入管理员模式手动录音）
3. 录制一段语音
4. 点击"发送"
5. 观察 Toast 提示：
   - "录音已保存 (ID: xxx)"
   - "已同步到云端"（如果上传成功）
   - "云端同步失败，已保存到本地"（如果上传失败）

### 测试下载

1. 清空应用数据（或使用另一台设备）
2. 触碰 NFC 播放标签
3. 观察同步界面：
   - "检查云端更新..."
   - "找到云端消息..."
   - "下载中..."
   - "下载完成，开始播放..."
4. 确认音频正常播放

### 测试离线模式

1. 关闭后端服务或断开网络
2. 触碰 NFC 播放标签
3. 观察提示："网络错误，使用本地录音..."
4. 确认仍能播放本地录音

## 4. 验证后端

### 使用 curl 测试

```bash
# 健康检查
curl http://localhost:3000/health

# 上传测试文件
curl -X POST http://localhost:3000/api/upload_audio \
  -F "file=@test.m4a" \
  -F "sender=test_user" \
  -F "duration=30"

# 获取最新消息
curl http://localhost:3000/api/get_latest

# 查看所有消息
curl http://localhost:3000/api/messages?limit=10
```

### 使用 Postman 测试

1. **上传音频**：
   - Method: POST
   - URL: `http://localhost:3000/api/upload_audio`
   - Body: form-data
     - file: 选择 .m4a 文件
     - sender: "test_user"
     - duration: "30"

2. **获取最新**：
   - Method: GET
   - URL: `http://localhost:3000/api/get_latest`

## 5. 查看日志

### Android 日志

```bash
# 查看网络请求日志
adb logcat | grep NetworkRepository

# 查看所有应用日志
adb logcat | grep "com.echo.lutian"
```

### 后端日志

```bash
# 查看后端控制台输出
# 日志会显示所有 HTTP 请求和错误信息
```

## 6. 常见问题

### Q: 上传失败，提示"云端同步失败"

**A**: 检查以下几点：
1. 后端服务是否正常运行：`curl http://localhost:3000/health`
2. Android 设备能否访问后端：
   - 模拟器：使用 `http://10.0.2.2:3000/`
   - 真机：使用电脑 IP，如 `http://192.168.1.100:3000/`
3. 查看 Logcat 日志查找具体错误

### Q: 下载失败，卡在"下载中..."

**A**: 检查以下几点：
1. 文件 URL 是否可访问：`curl {fileUrl}`
2. 网络连接是否正常
3. 存储空间是否充足
4. 查看 Logcat 日志

### Q: 模拟器无法连接到本地后端

**A**:
- Android 模拟器访问本机使用 `10.0.2.2` 而不是 `localhost` 或 `127.0.0.1`
- 确保防火墙允许 3000 端口

### Q: 真机无法连接到本地后端

**A**:
1. 确保手机和电脑在同一 WiFi 网络
2. 使用电脑的局域网 IP（如 `192.168.1.100`）
3. 确保防火墙允许局域网访问

## 7. 生产环境检查清单

部署到生产环境前，确保：

- [ ] 后端已部署到 Sealos 云函数
- [ ] MongoDB 数据库已创建并配置
- [ ] 环境变量已正确配置
- [ ] BASE_URL 已更新为生产地址
- [ ] HTTPS 已启用
- [ ] 文件存储路径已配置
- [ ] 日志监控已设置
- [ ] 备份策略已制定

## 8. 性能优化建议

### 后端优化

1. **使用 CDN**：将音频文件托管到 CDN
2. **添加缓存**：使用 Redis 缓存热门数据
3. **数据库索引**：为 `createdAt` 和 `sender` 字段创建索引
4. **压缩传输**：启用 gzip 压缩

### 前端优化

1. **批量上传**：收集多个未上传的录音，批量上传
2. **后台同步**：使用 WorkManager 在后台同步
3. **压缩音频**：上传前压缩音频文件
4. **智能重试**：网络失败时自动重试

## 9. 监控和维护

### 监控指标

- 上传成功率
- 下载成功率
- 平均响应时间
- 错误率
- 存储空间使用

### 日志分析

定期检查日志，关注：
- 频繁的上传/下载失败
- 异常的错误信息
- 性能瓶颈

### 数据备份

定期备份：
- MongoDB 数据库
- 音频文件存储

## 10. 下一步

网络同步功能已就绪，你可以：

1. **测试完整流程**：录音 → 上传 → 下载 → 播放
2. **优化用户体验**：调整同步提示文案
3. **添加高级功能**：用户系统、消息管理等
4. **部署到生产环境**：使用 Sealos 云服务

---

**需要帮助？**

- 查看详细文档：`NETWORK_SYNC.md`
- 查看后端文档：`backend/README.md`
- 查看 API 文档：后端 README 中的 API 接口部分
