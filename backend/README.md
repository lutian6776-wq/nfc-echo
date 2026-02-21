# HeartEcho Backend

HeartEcho 的云端后端服务，提供音频上传、下载和同步功能。

## 功能

- **上传音频**：接收 Android 端上传的 .m4a 音频文件
- **获取最新**：返回最新的语音消息
- **标记已播放**：记录消息播放状态
- **消息列表**：查询历史消息

## API 接口

### 1. 上传音频

```
POST /api/upload_audio
Content-Type: multipart/form-data

参数：
- file: 音频文件 (.m4a)
- sender: 发送者标识（可选）
- duration: 音频时长（秒）

响应：
{
  "success": true,
  "id": "消息ID",
  "fileUrl": "文件访问URL",
  "message": "Audio uploaded successfully"
}
```

### 2. 获取最新语音

```
GET /api/get_latest?sender=xxx

响应：
{
  "success": true,
  "id": "消息ID",
  "fileUrl": "文件URL",
  "fileName": "文件名",
  "sender": "发送者",
  "duration": 30,
  "fileSize": 123456,
  "createdAt": "2026-02-14T...",
  "isPlayed": false
}
```

### 3. 标记为已播放

```
POST /api/mark_played/:id

响应：
{
  "success": true,
  "message": "Marked as played"
}
```

### 4. 获取消息列表

```
GET /api/messages?limit=10

响应：
{
  "success": true,
  "count": 10,
  "messages": [...]
}
```

### 5. 健康检查

```
GET /health

响应：
{
  "status": "ok",
  "timestamp": "2026-02-14T...",
  "mongodb": "connected"
}
```

## 本地开发

### 1. 安装依赖

```bash
cd backend
npm install
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，配置 MongoDB 连接等
```

### 3. 启动服务

```bash
npm start
```

服务将在 http://localhost:3000 启动。

### 4. 测试接口

```bash
# 健康检查
curl http://localhost:3000/health

# 上传音频
curl -X POST http://localhost:3000/api/upload_audio \
  -F "file=@test.m4a" \
  -F "sender=test_user" \
  -F "duration=30"

# 获取最新
curl http://localhost:3000/api/get_latest
```

## Sealos 部署

### 1. 创建 MongoDB 数据库

在 Sealos 控制台创建 MongoDB 实例，获取连接字符串。

### 2. 创建对象存储（可选）

如果需要使用 Sealos Object Storage 而不是本地文件系统：

1. 创建 Bucket
2. 获取访问密钥
3. 修改代码使用 S3 兼容的 SDK

### 3. 部署云函数

1. 在 Sealos 控制台创建云函数
2. 上传代码
3. 配置环境变量：
   - `MONGO_URI`: MongoDB 连接字符串
   - `BASE_URL`: 云函数的公网访问地址
   - `STORAGE_PATH`: 文件存储路径（如 /data/uploads）

### 4. 配置域名（可选）

为云函数配置自定义域名，便于 Android 端访问。

## 数据库结构

### messages 集合

```javascript
{
  _id: ObjectId,
  fileUrl: String,        // 文件访问 URL
  fileName: String,       // 文件名
  sender: String,         // 发送者标识
  duration: Number,       // 音频时长（秒）
  fileSize: Number,       // 文件大小（字节）
  createdAt: Date,        // ���建时间
  isPlayed: Boolean,      // 是否已播放
  playedAt: Date          // 播放时间（可选）
}
```

## 安全建议

1. **认证授权**：添加 API Key 或 JWT 认证
2. **速率限制**：防止滥用上传接口
3. **文件验证**：严格验证上传文件类型和大小
4. **HTTPS**：生产环境必须使用 HTTPS
5. **CORS 配置**：限制允许的来源域名

## 性能优化

1. **CDN 加速**：使用 CDN 分发音频文件
2. **压缩传输**：启用 gzip 压缩
3. **缓存策略**：设置合理的缓存头
4. **数据库索引**：为 createdAt 和 sender 字段创建索引

## 监控和日志

建议添加：
- 请求日志记录
- 错误监控（如 Sentry）
- 性能监控（如 New Relic）
- 存储空间监控

## 扩展功能

可以考虑添加：
- 多用户支持（用户注册登录）
- 消息加密
- 消息过期自动删除
- 消息已读回执
- 推送通知
