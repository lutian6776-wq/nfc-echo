# 如何自定义 HeartEcho 应用图标

## 当前图标配置

你的应用使用 Android 的 **Adaptive Icon** 系统，包含：
- **背景层** (Background): `ic_launcher_background.xml` - 绿色网格背景
- **前景层** (Foreground): `ic_launcher_foreground.xml` - Android 机器人图标
- **单色图标** (Monochrome): 用于主题图标

## 方法一：使用 Android Studio 图标生成器（推荐）

### 步骤：

1. **打开 Android Studio**
   - 打开你的项目

2. **启动图标生成器**
   - 右键点击 `app` 文件夹
   - 选择 `New` → `Image Asset`

3. **配置图标**
   - **Icon Type**: 选择 `Launcher Icons (Adaptive and Legacy)`
   - **Name**: 保持 `ic_launcher`

4. **选择图标来源**

   **选项 A - 使用图片文件：**
   - **Foreground Layer** → **Source Asset** → **Path**
   - 点击文件夹图标，选择你的图标图片（PNG、JPG、SVG）
   - 推荐尺寸：512x512 像素或更大
   - 推荐格式：PNG（透明背景）或 SVG

   **选项 B - 使用剪贴画：**
   - **Foreground Layer** → **Source Asset** → **Clip Art**
   - 点击图标按钮，从 Android 内置图标库选择
   - 可以搜索 "heart"、"audio"、"voice" 等关键词

   **选项 C - 使用文本：**
   - **Foreground Layer** → **Source Asset** → **Text**
   - 输入文字（如 "❤️" 或 "HE"）
   - 选择字体和样式

5. **自定义背景**
   - **Background Layer** → 选择颜色或图片
   - 推荐使用纯色背景
   - 建议颜色：
     - 红色：`#F44336` (爱心主题)
     - 绿色：`#4CAF50` (当前应用主色)
     - 粉色：`#E91E63` (温馨主题)
     - 深色：`#1E1E1E` (现代风格)

6. **调整图标**
   - **Resize**: 调整前景图标大小（建议 50-80%）
   - **Trim**: 自动裁剪透明边缘
   - **Padding**: 添加内边距

7. **预览效果**
   - 查看不同形状的预览（圆形、方形、圆角方形等）
   - 查看不同 Android 版本的效果

8. **生成图标**
   - 点击 `Next`
   - 确认要覆盖的文件
   - 点击 `Finish`

## 方法二：手动替换图标文件

### 准备图标文件

你需要准备以下尺寸的图标（PNG 格式）：

| 密度 | 尺寸 | 文件夹 |
|------|------|--------|
| mdpi | 48x48 | mipmap-mdpi |
| hdpi | 72x72 | mipmap-hdpi |
| xhdpi | 96x96 | mipmap-xhdpi |
| xxhdpi | 144x144 | mipmap-xxhdpi |
| xxxhdpi | 192x192 | mipmap-xxxhdpi |

### 步骤：

1. **准备图标**
   - 使用图像编辑软件（Photoshop、GIMP、Figma 等）
   - 创建上述各种尺寸的图标
   - 保存为 PNG 格式，透明背景

2. **替换文件**
   ```
   app/src/main/res/
   ├── mipmap-mdpi/
   │   ├── ic_launcher.png (48x48)
   │   └── ic_launcher_round.png (48x48)
   ├── mipmap-hdpi/
   │   ├── ic_launcher.png (72x72)
   │   └── ic_launcher_round.png (72x72)
   ├── mipmap-xhdpi/
   │   ├── ic_launcher.png (96x96)
   │   └── ic_launcher_round.png (96x96)
   ├── mipmap-xxhdpi/
   │   ├── ic_launcher.png (144x144)
   │   └── ic_launcher_round.png (144x144)
   └── mipmap-xxxhdpi/
       ├── ic_launcher.png (192x192)
       └── ic_launcher_round.png (192x192)
   ```

3. **删除旧的 WebP 文件**
   - 删除所有 `.webp` 文件
   - 或者直接覆盖，改为 `.png` 格式

## 方法三：自定义 Adaptive Icon（高级）

### 修改背景颜色

编辑 `app/src/main/res/drawable/ic_launcher_background.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- 修改这里的颜色 -->
    <path
        android:fillColor="#F44336"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

### 修改前景图标

编辑 `app/src/main/res/drawable/ic_launcher_foreground.xml`：

**示例 1 - 简单的心形图标：**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- 心形路径 -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,75 C54,75 30,55 30,40 C30,30 37,25 44,25 C48,25 52,27 54,30 C56,27 60,25 64,25 C71,25 78,30 78,40 C78,55 54,75 54,75 Z" />
</vector>
```

**示例 2 - 文字图标 "HE"：**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- 白色文字 HE -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M30,35 L30,73 L35,73 L35,56 L48,56 L48,73 L53,73 L53,35 L48,35 L48,51 L35,51 L35,35 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M60,35 L60,73 L78,73 L78,68 L65,68 L65,56 L76,56 L76,51 L65,51 L65,40 L78,40 L78,35 Z" />
</vector>
```

## 方法四：使用在线工具生成

### 推荐工具：

1. **Android Asset Studio**
   - 网址：https://romannurik.github.io/AndroidAssetStudio/
   - 功能：在线生成各种尺寸的图标
   - 支持：图片、剪贴画、文字

2. **App Icon Generator**
   - 网址：https://appicon.co/
   - 功能：一键生成所有平台图标
   - 支持：iOS、Android、Web

3. **Icon Kitchen**
   - 网址：https://icon.kitchen/
   - 功能：专为 Android Adaptive Icon 设计
   - 支持：实时预览、多种形状

### 使用步骤：

1. 访问工具网站
2. 上传你的图标图片（建议 1024x1024）
3. 调整背景颜色、图标大小等
4. 下载生成的资源包
5. 解压并复制到项目的 `res` 目录

## 设计建议

### 图标设计原则：

1. **简洁明了**
   - 避免过于复杂的细节
   - 在小尺寸下也要清晰可辨

2. **符合主题**
   - HeartEcho 是语音通讯应用
   - 可以使用：心形 ❤️、声波 🎵、麦克风 🎤、对话 💬

3. **颜色搭配**
   - 背景和前景要有足够对比度
   - 建议：深色背景 + 浅色图标，或反之

4. **适配性**
   - 考虑不同形状（圆形、方形、圆角方形）
   - 重要元素放在安全区域内（中心 66%）

### 推荐配色方案：

**方案 1 - 温馨红色：**
- 背景：`#F44336` (红色)
- 前景：`#FFFFFF` (白色心形或麦克风)

**方案 2 - 清新绿色：**
- 背景：`#4CAF50` (绿色)
- 前景：`#FFFFFF` (白色图标)

**方案 3 - 浪漫粉色：**
- 背景：`#E91E63` (粉色)
- 前景：`#FFFFFF` (白色心形)

**方案 4 - 现代深色：**
- 背景：`#1E1E1E` (深灰)
- 前景：`#4CAF50` (绿色图标)

## 测试图标

### 在模拟器/真机上测试：

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
   - 查看启动器中的图标
   - 查看设置中的应用图标
   - 查看最近任务中的图标

### 检查清单：

- [ ] 图标在不同背景下都清晰可见
- [ ] 圆形、方形、圆角方形都显示正常
- [ ] 图标不会被裁切
- [ ] 颜色搭配协调
- [ ] 符合应用主题

## 常见问题

**Q: 修改后图标没有变化？**
- A: 卸载应用后重新安装，或清除启动器缓存

**Q: 图标被裁切了？**
- A: 调整前景图标的大小和内边距，确保在安全区域内

**Q: 不同设备显示不一样？**
- A: 这是正常的，不同厂商的启动器会使用不同形状

**Q: 如何制作透明背景图标？**
- A: 使用 PNG 格式，在图像编辑软件中删除背景层

## 快速开始示例

如果你想快速创建一个心形图标：

1. 打开 Android Studio
2. 右键 `app` → `New` → `Image Asset`
3. 选择 **Clip Art**
4. 搜索 "favorite" 或 "heart"
5. 选择心形图标
6. 背景颜色设为 `#F44336` (红色)
7. 调整大小到 60%
8. 点击 `Finish`

完成！你的应用现在有一个漂亮的心形图标了。
