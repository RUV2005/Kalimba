# Harmony Kalimba (和谐卡林巴)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)

**Harmony Kalimba** 是一款专为所有人设计的数字卡林巴琴（拇指琴）应用。除了提供纯净的音质体验外，本项目核心致力于**无障碍音乐创作**，通过深度的视觉、听觉和触觉反馈，让视障人士也能享受学习、创作和分享音乐的乐趣。

---

## ✨ 核心特性

### 🎹 1. 自由探索模式
- **24键双排设计：** 完美模拟专业卡林巴琴布局（上排12键 + 下排12键）
- **动态视觉反馈：** 涟漪动画与音符同步，提供沉浸式演奏体验
- **智能历史回放：** 自动记录即兴演奏，支持一键自动播放
- **实时演奏统计：** 显示探索时长、奏响次数、最近音符

### 📝 2. 全功能简谱编辑器 (NMN Editor)
- **数字化录入：** 针对简谱优化的分段式录入界面
- **段落管理：** 支持多段乐谱创作，便于管理复杂乐曲
- **实时预览：** 编辑过程中随时试听当前段落
- **自动保存：** 支持简谱本地持久化存储

### 🎓 3. 交互式练习模式
- **分步引导：** 像音游一样通过视觉和语音提示引导用户弹奏
- **进度追踪：** 实时显示当前音符位置，支持快速切换乐句
- **节拍器集成：** 可配置BPM、拍号、音量、振动反馈

### 🤖 4. 智能跟练模式 (AI-Powered)
- **实时音高检测：** 使用 YIN 算法 + 基频优先策略，准确识别演奏音符
- **即时反馈：** 正确/错误实时提示，统计准确率
- **防重复检测：** 智能去重机制，避免误判

### ♿ 5. 极致无障碍支持 (Accessibility First)
- **独立 TTS 引擎：** 独立于 TalkBack 的语音播报，播报键位、音高（如"下排第三键，中音1"）
- **多级触觉反馈：** 6种振动类型（轻/中/强/点击/双击/节拍）区分不同操作
- **语义化 UI：** 全面的 `contentDescription` 和 `clearAndSetSemantics` 优化

### ☁️ 6. 云端同步与分享
- **简谱市场：** 在广场浏览并下载他人创作的乐谱
- **分享码机制：** 通过 6 位分享码快速交换简谱作品
- **安全认证：** JWT + RefreshToken 双令牌机制，支持图形/语音验证码

---

## 🏗 架构设计

### 项目结构

```
app/src/main/java/com/danmo/kalimba/
├── MainActivity.kt                 # 应用入口，导航路由配置
│
├── accessibility/                  # 无障碍核心模块
│   ├── AccessibilityHelper.kt      # TTS + 振动统一管理层
│   └── FocusNavigationHelper.kt    # 焦点导航辅助
│
├── data/                           # 数据层
│   ├── local/                      # 本地数据
│   │   ├── KalimbaDatabase.kt      # Room 数据库配置
│   │   ├── SheetMusicEntity.kt     # 简谱实体定义
│   │   ├── SheetMusicDao.kt        # 数据访问对象
│   │   ├── Converters.kt           # Room 类型转换器
│   │   └── AuthDataStore.kt        # DataStore 用户认证存储
│   │
│   └── remote/                     # 网络数据
│       ├── AuthApiService.kt       # 认证 API (OkHttp)
│       ├── KalimbaApiService.kt    # 简谱 API (Retrofit)
│       └── TokenRefreshInterceptor.kt  # Token 自动刷新
│
├── main/                           # 自由探索模式
│   ├── KalimbaScreen.kt            # 主界面 Compose
│   ├── KalimbaAudioManager.kt      # SoundPool 音频管理
│   ├── KalimbaKeyData.kt           # 24键数据定义
│   └── KalimbaModels.kt            # 数据模型
│
├── nmn/                            # 简谱编辑器模块
│   ├── NmnScreen.kt                # 编辑器主界面
│   ├── NmnAudioManager.kt          # 编辑器音频管理
│   ├── NmnData.kt / NmnModels.kt   # 简谱数据模型
│   └── KeyPositionHelper.kt        # 键位与音高映射
│
├── practice/                       # 练习与跟练模块
│   ├── PracticeScreen.kt           # 练习模式界面
│   ├── SmartPracticeScreen.kt      # 智能跟练界面
│   ├── PracticeViewModel.kt        # 练习状态管理
│   ├── PracticeAudioManager.kt     # 练习音频控制
│   └── NativePitchDetector.kt      # 音高检测核心
│
├── metronome/                      # 节拍器模块
│   └── MetronomeManager.kt         # 节拍器音频+振动
│
├── pitch/                          # 音频处理
│   └── NativePitchDetector.kt      # YIN算法音高检测
│
├── settings/                       # 设置模块
│   ├── SettingsScreen.kt           # 设置界面
│   ├── SettingsManager.kt          # DataStore 设置管理
│   └── AccessibilitySettings.kt    # 无障碍设置
│
├── sheetlist/                      # 简谱库管理
│   └── SheetListScreen.kt          # 列表+云端浏览器
│
├── ui/auth/                        # 认证模块
│   ├── LoginScreen.kt              # 登录界面
│   └── AuthViewModel.kt            # 登录状态管理
│
└── ui/theme/                       # 主题样式
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

### 技术栈详解

| 层级 | 技术 | 用途 |
|-----|------|-----|
| **UI** | Jetpack Compose | 声明式 UI 框架 |
| **导航** | Navigation Compose | 类型安全导航 |
| **状态管理** | StateFlow / Compose State | 响应式状态 |
| **本地存储** | Room Database | 简谱结构化存储 |
| **偏好设置** | DataStore | 设置与Token持久化 |
| **网络** | Retrofit 2 + OkHttp 3 | RESTful API |
| **序列化** | Gson / Kotlinx Serialization | JSON 处理 |
| **音频播放** | SoundPool | 低延迟音效 |
| **音频录制** | AudioRecord | 麦克风音高检测 |
| **图片加载** | Coil | 验证码/头像加载 |

---

## 🔊 音频引擎设计

### SoundPool 音频管理
```kotlin
// KalimbaAudioManager.kt
class KalimbaAudioManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)                    // 最大并发流
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(USAGE_MEDIA)
                .setContentType(CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()
    
    // 异步加载 + 并发哈希表缓存
    private val soundMap = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
}
```

### YIN 算法音高检测
```kotlin
// NativePitchDetector.kt - 核心算法流程
1. AudioRecord 采集 44.1kHz 音频
2. 计算差分函数 (Difference Function)
3. 累积均值归一化 (CMNDF)
4. 多候选频率检测 + 基频优先选择
5. 稳定性阈值过滤 (3帧确认)
6. 频率-键位映射匹配
```

---

## ♿ 无障碍设计实现

### 语音播报策略
```kotlin
// AccessibilityHelper.kt
fun speak(text: String, interrupt: Boolean = false) {
    // 策略：TalkBack开启时不手动播报，避免冲突
    if (isTalkBackEnabled()) return
    
    // 仅用户手动开启时播报
    if (isTtsReady && isSpeechEnabled) {
        tts?.speak(text, queueMode, null, null)
    }
}
```

### 触觉反馈类型
| 类型 | 时长 | 使用场景 |
|-----|------|---------|
| `LIGHT` | 30ms | 按钮悬停 |
| `MEDIUM` | 50ms | 琴键点击 |
| `STRONG` | 100ms | 重要操作 |
| `CLICK` | 系统预设 | 普通点击 |
| `DOUBLE_CLICK` | 系统预设 | 正确反馈 |
| `TICK` | 系统预设 | 节拍提示 |

### 语义化示例
```kotlin
// NmnScreen.kt - 编辑器无障碍优化
FilteredKeyButton(
    modifier = Modifier
        .semantics {
            contentDescription = "中音1，当前选中"  // 覆盖子元素
        }
        .clearAndSetSemantics { /* 清除冗余信息 */ }
)
```

---

## 🔐 认证与安全

### JWT 双令牌机制
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   登录请求   │ ──▶ │  获取Token   │ ──▶ │ DataStore   │
│  + 验证码    │     │ Access+Refresh│     │  持久存储    │
└─────────────┘     └─────────────┘     └─────────────┘
                            │
                            ▼
                   ┌─────────────┐
                   │ TokenRefresh │
                   │ Interceptor  │  ◀── 自动刷新过期Token
                   └─────────────┘
```

### 验证码支持
- **图形验证码**：Base64 图片显示
- **语音验证码**：MP3 音频播放（无障碍友好）

---

## 🚀 快速开始

### 编译要求
- Android Studio Ladybug | 2024.2.1 或更高版本
- JDK 17
- Android SDK 36
- Kotlin 2.0+

### 安装步骤

1. 克隆仓库：
```bash
git clone https://github.com/ruv2005/Kalimba.git
cd Kalimba
```

2. 配置后端地址（如需云端功能）：
```kotlin
// MainActivity.kt 第 186 行
LoginScreen(
    baseUrl = "https://your-api-domain.com/api/"
)
```

3. 编译运行：
```bash
./gradlew assembleDebug
```

---

## 📂 数据模型

### 简谱结构
```kotlin
// SheetMusicEntity.kt
@Entity(tableName = "sheet_music")
data class SheetMusicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                      // 简谱名称
    val segments: String,                  // JSON: List<SegmentData>
    val totalNotes: Int,
    val totalSegments: Int,
    val bpm: Int = 80,
    val isUploaded: Boolean = false,       // 云端同步状态
    val shareCode: String? = null          // 6位分享码
)

data class SegmentData(
    val id: Int,
    val name: String,
    val notes: List<NoteData>
)

data class NoteData(
    val keyId: String,       // 如 "d1m" (下排中音1)
    val pitch: Int,          // 简谱数字 1-7
    val octave: String,      // DOWN/MIDDLE/HIGH/HIGH_HIGH
    val isRest: Boolean      // 休止符
)
```

### 键位映射
```
上排 (u行): u5d  u7d  u2m  u4m  u6m  u1h  u3h  u5h  u7h  u2hh u4hh u6hh
音高:       低音5 低音7 中音2 中音4 中音6 高音1 高音3 高音5 高音7 倍高2 倍高4 倍高6

下排 (d行): d4d  d6d  d1m  d3m  d5m  d7m  d2h  d4h  d6h  d1hh d3hh d5hh
音高:       低音4 低音6 中音1 中音3 中音5 中音7 高音2 高音4 高音6 倍高1 倍高3 倍高5
```

---

## 🧪 测试说明

### 单元测试
```bash
./gradlew test
```

### 仪器测试
```bash
./gradlew connectedAndroidTest
```

---

## 📜 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议开源。

---

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 Android UI 框架
- [YIN Algorithm](http://audition.ens.fr/adc/pdf/2002_JASA_YIN.pdf) - 音高检测算法
- [TarsosDSP](https://github.com/JorenSix/TarsosDSP) - 音频处理灵感

---

**Harmony Kalimba —— 让每一个人都能拨动心弦。**

如果你喜欢这个项目，请给一个 ⭐️ Star！

---
