const express = require('express');
const multer = require('multer');
const { MongoClient, ObjectId } = require('mongodb');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors());
app.use(express.json());

// MongoDB 连接
const MONGO_URI = process.env.MONGO_URI || 'mongodb://root:3bZu2Wl92c8SJ803@heartecho-db-mongodb.ns-1rnrshn0.svc:27017';
const DB_NAME = 'heartecho';
let db;

MongoClient.connect(MONGO_URI)
  .then(client => {
    console.log('✅ MongoDB 连接成功');
    db = client.db(DB_NAME);

    // 创建索引
    db.collection('users').createIndex({ deviceId: 1 }, { unique: true, sparse: true });
    db.collection('users').createIndex({ nfcTagId: 1 }, { sparse: true });
    db.collection('messages').createIndex({ senderId: 1, receiverId: 1, createdAt: -1 });
  })
  .catch(err => {
    console.error('❌ MongoDB 连接失败:', err.message);
    process.exit(1);
  });

// 云存储配置（使用本地文件系统模拟，实际部署时替换为 Sealos Object Storage）
const STORAGE_PATH = process.env.STORAGE_PATH || './uploads';
const BASE_URL = process.env.BASE_URL || `http://localhost:${PORT}`;

// 确保上传目录存在
if (!fs.existsSync(STORAGE_PATH)) {
  fs.mkdirSync(STORAGE_PATH, { recursive: true });
}

// Multer 配置
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, STORAGE_PATH);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${Date.now()}-${Math.random().toString(36).substring(7)}.m4a`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB 限制
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'audio/mp4' || file.mimetype === 'audio/m4a' || file.originalname.endsWith('.m4a')) {
      cb(null, true);
    } else {
      cb(new Error('Only .m4a audio files are allowed'));
    }
  }
});

// 静态文件服务（用于访问上传的音频）
app.use('/files', express.static(STORAGE_PATH));

// ==================== 用户管理接口 ====================

/**
 * 创建或获取用户（基于设备ID自动识别）
 * POST /api/users/identify
 * Body: { deviceId: string, name?: string, nfcTagId?: string }
 */
app.post('/api/users/identify', async (req, res) => {
  try {
    const { deviceId, name, nfcTagId } = req.body;

    if (!deviceId) {
      return res.status(400).json({ error: 'deviceId is required' });
    }

    // 查找现有用户
    let user = await db.collection('users').findOne({ deviceId: deviceId });

    if (!user) {
      // 创建新用户
      const newUser = {
        deviceId: deviceId,
        name: name || `用户_${deviceId.substring(0, 8)}`,
        nfcTagId: nfcTagId || null,
        role: 'user', // user 或 admin
        createdAt: new Date(),
        lastActiveAt: new Date()
      };

      const result = await db.collection('users').insertOne(newUser);
      user = { ...newUser, _id: result.insertedId };
      console.log(`✅ 新用户创建: ${user.name} (${deviceId})`);
    } else {
      // 更新最后活跃时间
      await db.collection('users').updateOne(
        { _id: user._id },
        { $set: { lastActiveAt: new Date() } }
      );
    }

    res.json({
      success: true,
      user: {
        userId: user._id.toString(),
        deviceId: user.deviceId,
        name: user.name,
        nfcTagId: user.nfcTagId,
        role: user.role
      }
    });

  } catch (error) {
    console.error('Identify user error:', error);
    res.status(500).json({ error: 'Failed to identify user', details: error.message });
  }
});

/**
 * 获取所有用户列表（管理员用）
 * GET /api/users
 */
app.get('/api/users', async (req, res) => {
  try {
    const users = await db.collection('users')
      .find({})
      .sort({ createdAt: -1 })
      .toArray();

    res.json({
      success: true,
      count: users.length,
      users: users.map(u => ({
        userId: u._id.toString(),
        deviceId: u.deviceId,
        name: u.name,
        nfcTagId: u.nfcTagId,
        role: u.role,
        createdAt: u.createdAt,
        lastActiveAt: u.lastActiveAt
      }))
    });

  } catch (error) {
    console.error('Get users error:', error);
    res.status(500).json({ error: 'Failed to get users', details: error.message });
  }
});

/**
 * 更新用户信息（管理员用）
 * PUT /api/users/:userId
 * Body: { name?: string, nfcTagId?: string, role?: string }
 */
app.put('/api/users/:userId', async (req, res) => {
  try {
    const userId = req.params.userId;
    const { name, nfcTagId, role } = req.body;

    const updateFields = {};
    if (name) updateFields.name = name;
    if (nfcTagId !== undefined) updateFields.nfcTagId = nfcTagId;
    if (role) updateFields.role = role;
    updateFields.updatedAt = new Date();

    const result = await db.collection('users').updateOne(
      { _id: new ObjectId(userId) },
      { $set: updateFields }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ success: true, message: 'User updated successfully' });

  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({ error: 'Failed to update user', details: error.message });
  }
});

/**
 * 绑定设备ID到用户（管理员手动绑定）
 * POST /api/users/:userId/bind_device
 * Body: { deviceId: string }
 */
app.post('/api/users/:userId/bind_device', async (req, res) => {
  try {
    const userId = req.params.userId;
    const { deviceId } = req.body;

    if (!deviceId) {
      return res.status(400).json({ error: 'deviceId is required' });
    }

    // 检查设备ID是否已被其他用户使用
    const existingUser = await db.collection('users').findOne({
      deviceId: deviceId,
      _id: { $ne: new ObjectId(userId) }
    });

    if (existingUser) {
      return res.status(400).json({
        error: 'Device already bound to another user',
        existingUser: existingUser.name
      });
    }

    const result = await db.collection('users').updateOne(
      { _id: new ObjectId(userId) },
      { $set: { deviceId: deviceId, updatedAt: new Date() } }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ success: true, message: 'Device bound successfully' });

  } catch (error) {
    console.error('Bind device error:', error);
    res.status(500).json({ error: 'Failed to bind device', details: error.message });
  }
});

// ==================== 消息接口（升级版） ====================

/**
 * 上传音频接口（支持用户隔离）
 * POST /api/upload_audio
 * Body: multipart/form-data
 *   - file: 音频文件 (.m4a)
 *   - senderId: 发送者用户ID（必填）
 *   - receiverId: 接收者用户ID（必填）
 *   - duration: 音频时长（秒）
 */
app.post('/api/upload_audio', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    const senderId = req.body.senderId;
    const receiverId = req.body.receiverId;
    const duration = parseInt(req.body.duration) || 0;

    if (!senderId || !receiverId) {
      return res.status(400).json({ error: 'senderId and receiverId are required' });
    }

    const fileUrl = `${BASE_URL}/files/${req.file.filename}`;

    // 保存到数据库
    const message = {
      fileUrl: fileUrl,
      fileName: req.file.filename,
      senderId: senderId,
      receiverId: receiverId,
      duration: duration,
      fileSize: req.file.size,
      createdAt: new Date(),
      isPlayed: false
    };

    const result = await db.collection('messages').insertOne(message);

    console.log(`📤 消息上传: ${senderId} -> ${receiverId}`);

    res.json({
      success: true,
      id: result.insertedId.toString(),
      fileUrl: fileUrl,
      message: 'Audio uploaded successfully'
    });

  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({ error: 'Upload failed', details: error.message });
  }
});

/**
 * 获取最新语音接口（支持用户隔离）
 * GET /api/get_latest
 * Query params:
 *   - userId: 当前用户ID（必填）
 *   - fromUserId: 筛选来自特定用户的消息（可选，管理员用）
 */
app.get('/api/get_latest', async (req, res) => {
  try {
    const userId = req.query.userId;
    const fromUserId = req.query.fromUserId;

    if (!userId) {
      return res.status(400).json({ error: 'userId is required' });
    }

    // 构建查询条件：receiverId 必须是当前用户
    const query = { receiverId: userId };

    // 如果指定了 fromUserId，则只查询来自该用户的消息
    if (fromUserId) {
      query.senderId = fromUserId;
    }

    const latestMessage = await db.collection('messages')
      .find(query)
      .sort({ createdAt: -1 })
      .limit(1)
      .toArray();

    if (latestMessage.length === 0) {
      return res.status(404).json({ error: 'No messages found' });
    }

    const message = latestMessage[0];

    res.json({
      success: true,
      id: message._id.toString(),
      fileUrl: message.fileUrl,
      fileName: message.fileName,
      senderId: message.senderId,
      receiverId: message.receiverId,
      duration: message.duration,
      fileSize: message.fileSize,
      createdAt: message.createdAt,
      isPlayed: message.isPlayed
    });

  } catch (error) {
    console.error('Get latest error:', error);
    res.status(500).json({ error: 'Failed to get latest message', details: error.message });
  }
});

/**
 * 获取两个用户之间的消息列表（管理员用）
 * GET /api/messages/conversation
 * Query params:
 *   - userId1: 用户1的ID
 *   - userId2: 用户2的ID
 *   - limit: 返回数量限制（默认50）
 */
app.get('/api/messages/conversation', async (req, res) => {
  try {
    const userId1 = req.query.userId1;
    const userId2 = req.query.userId2;
    const limit = parseInt(req.query.limit) || 50;

    if (!userId1 || !userId2) {
      return res.status(400).json({ error: 'userId1 and userId2 are required' });
    }

    // 查询两个用户之间的所有消息
    const messages = await db.collection('messages')
      .find({
        $or: [
          { senderId: userId1, receiverId: userId2 },
          { senderId: userId2, receiverId: userId1 }
        ]
      })
      .sort({ createdAt: -1 })
      .limit(limit)
      .toArray();

    res.json({
      success: true,
      count: messages.length,
      messages: messages.map(m => ({
        id: m._id.toString(),
        fileUrl: m.fileUrl,
        fileName: m.fileName,
        senderId: m.senderId,
        receiverId: m.receiverId,
        duration: m.duration,
        fileSize: m.fileSize,
        createdAt: m.createdAt,
        isPlayed: m.isPlayed
      }))
    });

  } catch (error) {
    console.error('Get conversation error:', error);
    res.status(500).json({ error: 'Failed to get conversation', details: error.message });
  }
});

/**
 * 标记消息为已播放
 * POST /api/mark_played/:id
 */
app.post('/api/mark_played/:id', async (req, res) => {
  try {
    const messageId = req.params.id;

    const result = await db.collection('messages').updateOne(
      { _id: new ObjectId(messageId) },
      { $set: { isPlayed: true, playedAt: new Date() } }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: 'Message not found' });
    }

    res.json({ success: true, message: 'Marked as played' });

  } catch (error) {
    console.error('Mark played error:', error);
    res.status(500).json({ error: 'Failed to mark as played', details: error.message });
  }
});

/**
 * 删除消息（包括云端文件）
 * DELETE /api/messages/:id
 */
app.delete('/api/messages/:id', async (req, res) => {
  try {
    const messageId = req.params.id;

    // 获取消息信息
    const message = await db.collection('messages').findOne({ _id: new ObjectId(messageId) });

    if (!message) {
      return res.status(404).json({ error: 'Message not found' });
    }

    // 删除文件
    if (message.fileName) {
      const filePath = `${STORAGE_PATH}/${message.fileName}`;
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
        console.log(`🗑️ 文件已删除: ${message.fileName}`);
      }
    }

    // 从数据库删除
    await db.collection('messages').deleteOne({ _id: new ObjectId(messageId) });

    console.log(`🗑️ 消息已删除: ${messageId}`);

    res.json({ success: true, message: 'Message deleted successfully' });

  } catch (error) {
    console.error('Delete message error:', error);
    res.status(500).json({ error: 'Failed to delete message', details: error.message });
  }
});

/**
 * 获取所有消息列表（用于调试）
 * GET /api/messages
 */
app.get('/api/messages', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 10;
    const messages = await db.collection('messages')
      .find({})
      .sort({ createdAt: -1 })
      .limit(limit)
      .toArray();

    res.json({
      success: true,
      count: messages.length,
      messages: messages
    });

  } catch (error) {
    console.error('Get messages error:', error);
    res.status(500).json({ error: 'Failed to get messages', details: error.message });
  }
});

/**
 * 健康检查接口
 * GET /health
 */
app.get('/health', async (req, res) => {
  try {
    if (!db) {
      return res.status(500).json({ status: 'error', message: 'MongoDB 未连接' });
    }
    await db.admin().ping();
    res.json({
      status: 'ok',
      timestamp: new Date(),
      mongodb: 'connected',
      message: 'MongoDB 连接正常'
    });
  } catch (error) {
    res.status(500).json({
      status: 'error',
      mongodb: 'disconnected',
      message: error.message
    });
  }
});

// 启动服务器
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 HeartEcho backend running on port ${PORT}`);
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Storage path: ${STORAGE_PATH}`);
  console.log(`📝 支持多用户管理模式`);
});
