# HeartEcho 权限控制方案

## 🔐 安全问题

**问题**：在之前的实现中，任何用户都可以通过长按左上角的隐藏按钮（3秒）进入管理员模式，这存在安全隐患。

## ✅ 解决方案

已实现基于用户角色的权限控制，只有管理员用户才能进入管理员模式。

## 🛡️ 实现细节

### 1. 用户角色系统

每个用户在数据库中都有一个 `role` 字段：
- `"admin"` - 管理员，可以访问管理员模式
- `"user"` - 普通用户，只能使用基本的录音和播放功能

### 2. 权限检查逻辑

在 `MainActivity.kt` 中，当用户尝试进入管理员模式时：

```kotlin
onEnterAdminMode = {
    // 权限检查：只有管理员才能进入管理员模式
    if (currentUser?.role == "admin") {
        viewModel.enterAdminMode()
    } else {
        Toast.makeText(
            this@MainActivity,
            "无权限访问管理员模式",
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

### 3. 用户识别流程

1. **App 启动时**：
   - 自动获取设备的 Android ID
   - 调用 `userViewModel.identifyUser()`
   - 后端根据设备ID返回用户信息（包括角色）

2. **首次使用**：
   - 如果设备ID未绑定，后端会创建新用户
   - 默认角色为 `"user"`（普通用户）
   - 管理员需要手动在数据库中设置角色

3. **角色验证**：
   - 每次尝试进入管理员模式时检查 `currentUser?.role`
   - 只有 `role == "admin"` 的用户才能进入

## 🔧 配置管理员

### 方法一：通过 MongoDB 直接设置

```javascript
// 连接到 MongoDB
db.users.updateOne(
  { deviceId: "你的设备ID" },
  { $set: { role: "admin" } }
)
```

### 方法二：通过 API 设置

```bash
curl -X PUT http://your-server/api/users/:userId \
  -H "Content-Type: application/json" \
  -d '{"role": "admin"}'
```

### 方法三：在用户识别时设置

修改后端 `backend/index.js` 中的用户创建逻辑：

```javascript
// 在 POST /api/users/identify 接口中
if (!user) {
  const newUser = {
    deviceId: deviceId,
    name: name || `用户_${deviceId.substring(0, 8)}`,
    nfcTagId: nfcTagId || null,
    role: 'user', // 默认为普通用户
    // 如果是特定设备，设置为管理员
    // role: deviceId === '你的设备ID' ? 'admin' : 'user',
    createdAt: new Date(),
    lastActiveAt: new Date()
  };
  // ...
}
```

## 📱 用户体验

### 管理员用户
1. 长按左上角隐藏按钮 3 秒
2. 进度圈填满后进入管理员模式
3. 可以查看用户列表、管理设备、查看对话记录

### 普通用户
1. 长按左上角隐藏按钮 3 秒
2. 进度圈填满后显示提示："无权限访问管理员模式"
3. 无法进入管理员界面

## 🎯 安全建议

### 1. 隐藏管理员入口（可选）

如果想进一步提高安全性，可以完全隐藏管理员入口按钮：

```kotlin
// 在 IdleScreen 中
if (currentUser?.role == "admin") {
    AdminModeEntrance(
        onEnterAdminMode = onEnterAdminMode,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
    )
}
```

### 2. 添加 PIN 码保护（可选）

可以在进入管理员模式前要求输入 PIN 码：

```kotlin
onEnterAdminMode = {
    if (currentUser?.role == "admin") {
        // 显示 PIN 码输入对话框
        showPinDialog { pin ->
            if (pin == "1234") {
                viewModel.enterAdminMode()
            } else {
                Toast.makeText(this, "PIN 码错误", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(this, "无权限访问", Toast.LENGTH_SHORT).show()
    }
}
```

### 3. 记录访问日志（可选）

在后端记录管理员操作：

```javascript
// 在需要权限的接口中添加日志
app.get('/api/users', async (req, res) => {
  try {
    // 记录访问日志
    await db.collection('admin_logs').insertOne({
      action: 'view_users',
      userId: req.query.adminId,
      timestamp: new Date()
    });

    // 返回用户列表
    // ...
  } catch (error) {
    // ...
  }
});
```

## 🔍 验证方法

### 测试普通用户
1. 在一台新设备上安装 App
2. 首次启动会自动创建普通用户
3. 尝试长按左上角按钮
4. 应该看到"无权限访问管理员模式"提示

### 测试管理员用户
1. 在数据库中将你的设备设置为管理员
2. 重启 App（或等待用户信息刷新）
3. 长按左上角按钮
4. 应该能成功进入管理员模式

## 📊 数据库查询示例

### 查看所有用户及其角色
```javascript
db.users.find({}, { name: 1, deviceId: 1, role: 1 })
```

### 查找所有管理员
```javascript
db.users.find({ role: "admin" })
```

### 将用户降级为普通用户
```javascript
db.users.updateOne(
  { deviceId: "某个设备ID" },
  { $set: { role: "user" } }
)
```

## 🎉 总结

现在 HeartEcho 已经实现了完善的权限控制：

✅ 基于用户角色的访问控制
✅ 自动设备识别和用户绑定
✅ 管理员和普通用户权限隔离
✅ 友好的权限拒绝提示
✅ 灵活的管理员配置方式

普通用户（老人）无法进入管理员模式，只能使用基本的录音和播放功能，确保了系统的安全性。
