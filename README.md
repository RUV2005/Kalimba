# 🎹 卡林巴琴教学应用 (KalimbaApp)

<div align="center">

**一款安卓平台上的卡林巴琴虚拟教学助手**

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org)

[功能特色](#-功能特色) • [快速开始](#-快速开始) • [文件结构](#-文件结构) • [使用指南](#-使用指南)

</div>

---

## 📱 功能特色

### ✨ 虚拟卡林巴琴键盘
- **24个真实琴键设计**：分上下两排，精确还原卡林巴琴结构
- **实时音效播放**：集成原生 SoundPool，支持多声道同时播放
- **智能语音播报**：TextToSpeech 自动播报键位信息
- **视觉反馈**：琴键点击时色彩变化，提升交互体验

### 🎓 分段教学模式
- **6段精选乐句**：小星星、儿歌等入门级曲目
- **逐个音符练习**：可单个播放、前后导航、整段预览
- **进度追踪**：直观的进度条显示学习进度
- **自动演示**：支持整段自动播放，方便对照学习

### 🔊 音频管理
- **SoundPool 音频引擎**：低延迟、高效率的音效播放
- **TextToSpeech 语音合成**：中文语音播报键位和教学内容
- **并行处理**：音效和语音互不干扰，流畅同步

### 🏗️ 模块化代码架构
- **关注点分离**：Model、Audio、UI、Screen 各司其职
- **易于维护**：每个模块独立，改动互不影响
- **便于扩展**：添加新曲目或功能无需修改其他模块
- **代码复用**：UI 组件和工具类可跨模块使用

---

## 🚀 快速开始

### 环境要求

| 项目 | 要求 |
|------|------|
| **Android 版本** | 8.0 (API 26) 及以上 |
| **Kotlin 版本** | 1.9 及以上 |
| **Jetpack Compose** | 1.5 及以上 |
| **开发工具** | Android Studio Flamingo 及以上 |

### 克隆和导入

```bash
# 克隆项目
git clone https://github.com/yourusername/kalimba-app.git
cd kalimba-app

# 打开 Android Studio
# File → Open → 选择项目目录
```

### 配置音频资源

1. 创建资源目录：`app/src/main/res/raw`
2. 将所有音频文件放入该目录
3. 确保文件名与代码中的 `resId` 相匹配

**所需音频文件列表：**
```
res/raw/
├── d1_middle.wav
├── d1_high.wav
├── d1_high_high.wav
├── d2_high.wav
├── d2_high_high.wav
├── d3_middle.wav
├── d3_high.wav
├── d3_high_high.wav
├── d4_down.wav
├── d4_middle.wav
├── d4_high.wav
├── d4_high_high.wav
├── d5_middle.wav
├── d5_high.wav
├── d5_high_high.wav
├── d6_down.wav
├── d6_middle.wav
├── d6_high.wav
├── d6_high_high.wav
├── d7_middle.wav
├── d7_high.wav
├── u1_high.wav
├── u2_middle.wav
├── u2_high.wav
├── u2_high_high.wav
├── u3_high.wav
├── u3_high_high.wav
├── u4_middle.wav
├── u4_high.wav
├── u4_high_high.wav
├── u5_down.wav
├── u5_middle.wav
├── u5_high.wav
├── u6_middle.wav
├── u6_high.wav
├── u6_high_high.wav
├── u7_down.wav
└── u7_high.wav
```

### 编译和运行

```bash
# 编译项目
./gradlew build

# 运行在模拟器或真机
./gradlew installDebug
```

---

## 📁 文件结构

```
com/danmo/kalimba/
│
├── main/                              # 主屏幕模块
│   ├── KalimbaModels.kt              # 数据模型
│   │   ├── Octave (enum)             # 音域枚举
│   │   └── KalimbaKey (data class)   # 琴键数据结构
│   │
│   ├── KalimbaKeyData.kt             # 键盘数据集合
│   │   ├── dRowKeys (下排12个键)
│   │   └── uRowKeys (上排12个键)
│   │
│   ├── KalimbaAudioManager.kt        # 音频管理器
│   │   ├── loadSounds()              # 加载所有音效
│   │   ├── play(key)                 # 播放指定琴键
│   │   └── isReady()                 # 检查加载状态
│   │
│   ├── KalimbaComposables.kt         # UI 组件
│   │   ├── KeyRow()                  # 琴键行组件
│   │   └── KalimbaKeyItem()          # 单个琴键组件
│   │
│   └── KalimbaScreen.kt              # 主屏幕逻辑
│       ├── KalimbaScreen()           # 主屏幕 Composable
│       ├── LoadingView()             # 加载视图
│       └── KalimbaMainContent()      # 主要内容
│
├── practice/                          # 分段教学模块
│   ├── PracticeModels.kt             # 教学数据模型
│   │   ├── PracticeSegment           # 乐句分段
│   │   └── PracticeNote              # 单个音符
│   │
│   ├── PracticeData.kt               # 教学曲目数据
│   │   └── SEGMENTS (6段乐句)
│   │
│   ├── PracticeAudioManager.kt       # 教学音频管理
│   │   ├── playSoundWithCallback()   # 播放单个音符
│   │   ├── playCurrentOnly()         # 播放+播报位置
│   │   ├── previewSegment()          # 预览整段
│   │   └── speakOnly()               # 仅语音播报
│   │
│   ├── KeyPositionHelper.kt          # 键位映射工具
│   │   ├── getPositionDescription()  # 获取键位描述
│   │   ├── getDisplayName()          # 获取显示名称
│   │   └── getColor()                # 获取键的颜色
│   │
│   ├── PracticeComposables.kt        # 教学 UI 组件
│   │   ├── ControlButton()           # 控制按钮
│   │   ├── CurrentNoteDisplay()      # 当前音符显示
│   │   ├── SegmentProgressIndicator()# 进度指示器
│   │   ├── SegmentSelector()         # 分段选择器
│   │   └── PlayButton()              # 播放按钮
│   │
│   ├── PracticeScreen.kt             # 教学屏幕逻辑
│   │   └── PracticeScreen()          # 教学主屏幕
│   │
│   └── PracticeActivity.kt           # 教学 Activity
│
├── ui/theme/
│   └── KalimbaAppTheme.kt            # App 主题定义
│
└── MainActivity.kt                    # 应用入口 Activity
```

---

## 💡 使用指南

### 主屏幕 - 虚拟琴键盘

1. **点击琴键播放**  
   在屏幕上点击任意琴键，即可播放音效和语音播报

2. **上下两排琴键**
    - 🟢 **上排（绿色）**：较短的键，音域偏高
    - 🔵 **下排（蓝色）**：较长的键，音域偏低

3. **进入教学模式**  
   点击"进入分段教学模式"按钮进入教学界面

### 分段教学模式

1. **选择乐段**  
   点击顶部数字按钮（1-6）选择要练习的乐句

2. **单音练习**
    - 点击中间大圆形按钮播放当前音符
    - 点击"上一个"/"下一个"导航到前后音符

3. **整段预览**  
   点击顶部"播放"图标自动演示整个乐句

4. **进度追踪**
    - 绿色圆点：已完成的音符
    - 蓝色圆点：当前音符
    - 灰色圆点：未开始的音符

---

## 🏗️ 架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│     UI Layer (Compose)              │
│   KalimbaScreen / PracticeScreen    │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   Component Layer                   │
│ KeyRow / ControlButton / PlayButton │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   Business Logic Layer              │
│  KalimbaAudioManager /              │
│  PracticeAudioManager               │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   Data Layer                        │
│ KalimbaKeyData / PracticeData       │
└─────────────────────────────────────┘
```

### 关键类说明

#### KalimbaAudioManager
```kotlin
// 主屏幕音频管理
class KalimbaAudioManager(context: Context) {
    fun play(key: KalimbaKey)        // 播放琴键
    fun isReady(): Boolean           // 检查加载状态
    fun release()                    // 释放资源
}
```

#### PracticeAudioManager
```kotlin
// 教学模式音频管理
class PracticeAudioManager(context: Context) {
    fun playSoundWithCallback(...)                    // 播放+回调
    fun playCurrentOnly(keyId, description, ...)     // 播放+播报
    fun previewSegment(notes, onNote, onComplete)    // 预览整段
    fun speakOnly(text)                              // 仅语音播报
}
```

---

## 🔧 开发指南

### 添加新的卡林巴琴音符

1. **准备音频文件**：`res/raw/` 目录中添加 `.wav` 文件

2. **`KalimbaKeyData.kt` 中添加**：
```kotlin
val dRowKeys = listOf(
    // 现有的键...
    KalimbaKey(
        id = "d8m",
        row = "d",
        index = 12,
        pitch = 8,
        octave = Octave.MIDDLE,
        resId = R.raw.d8_middle,  // 新音频文件
        displayName = "8"
    )
)
```

### 添加新的教学乐句

1. **在 `PracticeData.kt` 中添加**：
```kotlin
PracticeSegment(
    id = 7,
    name = "新乐句",
    notes = listOf(
        note("d1m", 1, Octave.MIDDLE),
        note("d3m", 3, Octave.MIDDLE),
        note("d5m", 5, Octave.MIDDLE),
        // ...
    )
)
```

### 自定义主题色

编辑 `ui/theme/KalimbaAppTheme.kt`：
```kotlin
@Composable
fun KalimbaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 自定义调色板
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFFA5D6A7)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2196F3),
            secondary = Color(0xFF4CAF50)
        )
    }
    
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

---

## 🐛 常见问题

### Q: 音效播放有延迟或断续？
**A:** 确保：
1. 音频文件格式正确（推荐 44.1kHz, 16-bit WAV）
2. `SoundPool.Builder()` 的 `setMaxStreams()` 值足够大
3. 设备存储空间充足

### Q: 语音播报不清楚？
**A:** 调整 `KalimbaAudioManager` 中的：
```kotlin
tts?.setSpeechRate(1.2f)  // 调整语速（1.0 = 正常）
```

### Q: 如何禁用语音播报？
**A:** 在 `play()` 方法中注释掉 `tts?.speak()` 部分：
```kotlin
fun play(key: KalimbaKey) {
    // 播放音效
    val soundId = soundMap[key.id]
    if (soundId != null && loadedSounds[soundId] == true) {
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }
    
    // 注释掉这部分以禁用语音
    // if (isTtsReady) { ... }
}
```

---

## 📊 性能指标

| 指标 | 数值 |
|------|------|
| **App 启动时间** | ~2-3 秒 |
| **音效延迟** | < 100ms |
| **内存占用** | ~50-100MB |
| **APK 大小** | ~8-12MB（含资源） |

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献步骤

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 函数和类要有清晰的 KDoc 注释
- 单个函数不超过 50 行
- 使用有意义的变量名

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 👨‍💻 作者

- **开发者**: [Your Name]
- **联系方式**: [your.email@example.com]

---

## 🙏 鸣谢

感谢以下开源项目和资源：

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google 官方 UI 工具包
- [Material Design 3](https://m3.material.io/) - Material Design 最新版本
- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech) - 安卓原生语音合成

---

<div align="center">

⭐ 如果对你有帮助，请给个 Star！

[![Star](https://img.shields.io/github/stars/yourusername/kalimba-app?style=social)](https://github.com/yourusername/kalimba-app)

</div>