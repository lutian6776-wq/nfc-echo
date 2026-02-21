# 如何重命名应用

## 已完成的修改

✅ 应用名称已从 "My Application" 更改为 "HeartEcho"

## 修改的文件

### 1. 应用显示名称
**文件**: `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">HeartEcho</string>
</resources>
```

这个名称会显示在：
- 📱 设备主屏幕（启动器图标下方）
- ⚙️ 设置 → 应用列表
- 📋 最近任务列表
- 🔔 通知栏

## 如何自定义应用名称

### 方法 1：直接修改（最简单）

编辑 `app/src/main/res/values/strings.xml`：

```xml
<resources>
    <string name="app_name">你的应用名称</string>
</resources>
```

### 方法 2：支持多语言

如果你想为不同语言设置不同的应用名称：

**中文名称** - 创建 `app/src/main/res/values-zh/strings.xml`：
```xml
<resources>
    <string name="app_name">心声回响</string>
</resources>
```

**英文名称** - 保持 `app/src/main/res/values/strings.xml`：
```xml
<resources>
    <string name="app_name">HeartEcho</string>
</resources>
```

**日文名称** - 创建 `app/src/main/res/values-ja/strings.xml`：
```xml
<resources>
    <string name="app_name">ハートエコー</string>
</resources>
```

### 方法 3：在 AndroidManifest.xml 中直接设置（不推荐）

虽然可以在 `AndroidManifest.xml` 中直接设置，但不推荐：

```xml
<application
    android:label="HeartEcho"
    ...>
```

**为什么不推荐？**
- 无法支持多语言
- 不符合 Android 最佳实践
- 难以维护

## 应用名称设计建议

### 长度建议
- **最佳长度**: 8-12 个字符
- **最大长度**: 建议不超过 15 个字符
- **原因**: 太长会在图标下方被截断显示为 "..."

### 命名原则
1. **简洁易记**: 用户能轻松记住
2. **描述功能**: 让用户知道应用是做什么的
3. **独特性**: 避免与其他应用混淆
4. **避免特殊字符**: 不要使用 emoji（除非特别需要）

### HeartEcho 相关的其他名称建议
- **中文**: 心声回响、心语传声、心声传递
- **简短**: HE、心声、Echo
- **描述性**: 语音传心、声声传情

## 测试应用名称

### 步骤：

1. **清理并重新构建**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **卸载旧版本**
   - 从设备上完全卸载应用

3. **安装新版本**
   ```bash
   ./gradlew installDebug
   ```

4. **检查效果**
   - 查看主屏幕图标下方的名称
   - 查看设置 → 应用中的名称
   - 查看最近任务中的名称

## 常见问题

**Q: 修改后名称没有变化？**
- A: 卸载应用后重新安装，或清除启动器缓存

**Q: 名称显示不完整，被截断了？**
- A: 缩短应用名称，建议不超过 12 个字符

**Q: 如何在不同语言下显示不同名称？**
- A: 创建对应语言的 `values-xx` 文件夹和 `strings.xml`

**Q: 应用内部显示的标题也会改变吗？**
- A: 是的，如果你在代码中使用 `@string/app_name`，所有地方都会更新

## 其他相关配置

### 修改包名（高级）

如果你想修改应用的包名（com.echo.lutian），这是一个更复杂的过程：

1. 在 Android Studio 中：
   - 点击项目视图左上角的齿轮图标
   - 取消勾选 "Compact Middle Packages"
   - 右键包名 → Refactor → Rename
   - 选择 "Rename package"
   - 输入新包名

2. 修改 `build.gradle.kts` 中的 `applicationId`

3. 同步并重新构建项目

**注意**: 修改包名后，已安装的应用将被视为不同的应用。

### 修改应用版本

编辑 `app/build.gradle.kts`：

```kotlin
android {
    defaultConfig {
        versionCode = 2        // 版本号（整数，每次更新递增）
        versionName = "1.1.0"  // 版本名称（显示给用户）
    }
}
```

## 总结

✅ 应用名称已成功更改为 "HeartEcho"

下次构建并安装应用后，你将在设备上看到新的应用名称！
