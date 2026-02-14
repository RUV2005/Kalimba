package com.danmo.kalimba.main

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 卡林巴琴主屏幕 - 自由探索模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalimbaScreen(
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val autoPlayScope = rememberCoroutineScope()
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    val audioManager = remember { KalimbaAudioManager(context) }

    var isReady by remember { mutableStateOf(false) }
    var isAutoPlay by remember { mutableStateOf(false) }
    var playSessionId by remember { mutableIntStateOf(0) }
    var lastPlayedKey by remember { mutableStateOf<KalimbaKey?>(null) }
    var playHistory by remember { mutableStateOf<List<KalimbaKey>>(emptyList()) }
    var totalPlays by remember { mutableIntStateOf(0) }
    var exploreStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentRippleKeyId by remember { mutableStateOf<String?>(null) }

    val clickMutex = remember { Mutex() }

    LaunchedEffect(Unit) {
        while (!audioManager.isReady() || !accessibilityHelper.isReady()) {
            delay(100)
        }
        isReady = true
        exploreStartTime = System.currentTimeMillis()
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.release()
            accessibilityHelper.release()
        }
    }

    suspend fun playKey(key: KalimbaKey, skipAnnounce: Boolean) {
        clickMutex.withLock {
            audioManager.play(key)
            lastPlayedKey = key
            totalPlays++
            playHistory = listOf(key) + playHistory
            currentRippleKeyId = key.id
        }
        delay(50)
        clickMutex.withLock {
            if (currentRippleKeyId == key.id) {
                currentRippleKeyId = null
            }
        }
        if (!skipAnnounce &&
            accessibilityHelper.isSpeechEnabled &&
            !accessibilityHelper.isTalkBackEnabled()
        ) {
            accessibilityHelper.speak(key.getPositionDescription())
        }
    }

    val handleKeyClick: (KalimbaKey) -> Unit = { key ->
        autoPlayScope.launch {
            playKey(key, skipAnnounce = false)
        }
    }

    LaunchedEffect(playSessionId) {
        if (playSessionId < 0) return@LaunchedEffect
        if (!isAutoPlay) return@LaunchedEffect

        val historySnapshot = playHistory.reversed()
        if (historySnapshot.isEmpty()) {
            isAutoPlay = false
            return@LaunchedEffect
        }

        accessibilityHelper.provideFeedback(
            text = "开始自动播放，共${historySnapshot.size}个音符",
            vibrationType = VibrationType.STRONG,
            interrupt = true
        )

        var isFirstRound = true

        while (isAutoPlay) {
            if (!isFirstRound) {
                delay(1000)
                if (!isAutoPlay) break
            }
            isFirstRound = false

            historySnapshot.forEachIndexed { index, key ->
                if (!isAutoPlay) return@forEachIndexed

                playKey(key, skipAnnounce = true)
                accessibilityHelper.vibrate(VibrationType.LIGHT)

                if (accessibilityHelper.isSpeechEnabled &&
                    !accessibilityHelper.isTalkBackEnabled() &&
                    index % 5 == 0 && index > 0
                ) {
                    val remaining = historySnapshot.size - index
                    accessibilityHelper.speak(
                        "已播放${index}个，还剩${remaining}个",
                        interrupt = false
                    )
                }
                delay(500)
            }
        }

        accessibilityHelper.provideFeedback(
            text = "自动播放已停止",
            vibrationType = VibrationType.MEDIUM,
            interrupt = true
        )
    }

    fun toggleAutoPlay() {
        if (isAutoPlay) {
            isAutoPlay = false
        } else {
            isAutoPlay = true
            playSessionId++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("卡林巴琴 - 自由探索")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "已探索 ${formatDuration(System.currentTimeMillis() - exploreStartTime)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (totalPlays > 0) {
                                Text(
                                    " | 奏响: $totalPlays 次",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            lastPlayedKey?.let {
                                Text(
                                    " | 最近: ${it.displayName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 设置按钮 - 使用自定义图标
                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            onNavigateToSettings()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "打开设置"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 播放/停止按钮
                    IconButton(
                        onClick = {
                            toggleAutoPlay()
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                        },
                        enabled = isReady && playHistory.isNotEmpty(),
                        modifier = Modifier.semantics {
                            contentDescription = if (isAutoPlay) {
                                "停止自动播放"
                            } else {
                                "自动播放历史记录，共${playHistory.size}个音符"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = when {
                                !isReady || playHistory.isEmpty() -> Color.Gray
                                isAutoPlay -> Color(0xFFE53935)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }

                    // 语音播报开关
                    IconButton(
                        onClick = {
                            accessibilityHelper.toggleSpeech()
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (accessibilityHelper.isSpeechEnabled) {
                                "关闭语音播报"
                            } else {
                                "开启语音播报"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (accessibilityHelper.isSpeechEnabled) {
                                Color(0xFF4CAF50)
                            } else {
                                Color.Gray
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isReady) {
                LoadingView()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAutoPlay) {
                        Text(
                            "自动播放中...",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .semantics {
                                    contentDescription = "正在自动播放历史记录"
                                }
                        )
                    }

                    Text(
                        "点击琴键开始探索",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OctaveKeyboardColumn(
                                title = "上排音键",
                                keys = KalimbaKeyData.uRowKeys,
                                onKeyClick = handleKeyClick,
                                activeRippleKeyId = currentRippleKeyId,
                                accessibilityHelper = accessibilityHelper,
                                modifier = Modifier.weight(1f)
                            )

                            OctaveKeyboardColumn(
                                title = "下排音键",
                                keys = KalimbaKeyData.dRowKeys,
                                onKeyClick = handleKeyClick,
                                activeRippleKeyId = currentRippleKeyId,
                                accessibilityHelper = accessibilityHelper,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        OctaveKeyboardRow(
                            title = "上排音键",
                            keys = KalimbaKeyData.uRowKeys,
                            onKeyClick = handleKeyClick,
                            activeRippleKeyId = currentRippleKeyId,
                            accessibilityHelper = accessibilityHelper,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OctaveKeyboardRow(
                            title = "下排音键",
                            keys = KalimbaKeyData.dRowKeys,
                            onKeyClick = handleKeyClick,
                            activeRippleKeyId = currentRippleKeyId,
                            accessibilityHelper = accessibilityHelper,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OctaveLegend(modifier = Modifier.padding(bottom = 16.dp))

                    ModeSwitchButtons(
                        onNavigateToLibrary = onNavigateToEditor,  // 只保留一个按钮
                        accessibilityHelper = accessibilityHelper,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun OctaveKeyboardRow(
    title: String,
    keys: List<KalimbaKey>,
    onKeyClick: (KalimbaKey) -> Unit,
    activeRippleKeyId: String?,
    accessibilityHelper: AccessibilityHelper,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        val firstRow = keys.take(6)
        val secondRow = keys.drop(6)

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyRowCompact(
                keys = firstRow,
                onKeyClick = onKeyClick,
                activeRippleKeyId = activeRippleKeyId,
                accessibilityHelper = accessibilityHelper
            )
            KeyRowCompact(
                keys = secondRow,
                onKeyClick = onKeyClick,
                activeRippleKeyId = activeRippleKeyId,
                accessibilityHelper = accessibilityHelper
            )
        }
    }
}

@Composable
fun OctaveKeyboardColumn(
    title: String,
    keys: List<KalimbaKey>,
    onKeyClick: (KalimbaKey) -> Unit,
    activeRippleKeyId: String?,
    accessibilityHelper: AccessibilityHelper,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { key ->
                    CompactKeyButton(
                        key = key,
                        onClick = { onKeyClick(key) },
                        isRippling = activeRippleKeyId == key.id,
                        accessibilityHelper = accessibilityHelper,
                        size = 48.dp
                    )
                }
            }
        }
    }
}

@Composable
fun KeyRowCompact(
    keys: List<KalimbaKey>,
    onKeyClick: (KalimbaKey) -> Unit,
    activeRippleKeyId: String?,
    accessibilityHelper: AccessibilityHelper
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEach { key ->
            CompactKeyButton(
                key = key,
                onClick = { onKeyClick(key) },
                isRippling = activeRippleKeyId == key.id,
                accessibilityHelper = accessibilityHelper,
                size = 52.dp
            )
        }
    }
}

@Composable
fun CompactKeyButton(
    key: KalimbaKey,
    onClick: () -> Unit,
    isRippling: Boolean,
    accessibilityHelper: AccessibilityHelper,
    size: androidx.compose.ui.unit.Dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isRippling) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale"
    )

    val color = getOctaveColor(key.octave)

    val semanticDescription = buildString {
        append(key.getPositionDescription())
        if (isRippling) {
            append("，正在播放")
        }
        append("。双击可播放")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    accessibilityHelper.vibrate(VibrationType.MEDIUM)
                    if (!accessibilityHelper.isTalkBackEnabled() &&
                        accessibilityHelper.isSpeechEnabled) {
                        accessibilityHelper.speak(key.getPositionDescription())
                    }
                    onClick()
                }
            )
            .semantics {
                contentDescription = semanticDescription
            }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isRippling) color.copy(alpha = 0.3f) else color.copy(alpha = 0.15f)
                )
                .border(
                    width = if (isRippling) 3.dp else 2.dp,
                    color = if (isRippling) color else color.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key.displayName,
                fontSize = (size.value * 0.35f).sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = key.pitch.toString(),
            fontSize = 10.sp,
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OctaveLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OctaveLegendItem("低音", Color(0xFF795548))
        OctaveLegendItem("中音", Color(0xFF4CAF50))
        OctaveLegendItem("高音", Color(0xFF2196F3))
        OctaveLegendItem("倍高音", Color(0xFF9C27B0))
    }
}

@Composable
fun OctaveLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun ModeSwitchButtons(
    onNavigateToLibrary: () -> Unit,
    accessibilityHelper: AccessibilityHelper,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 简谱库入口（包含编辑和练习）
        Button(
            onClick = {
                accessibilityHelper.vibrate(VibrationType.CLICK)
                onNavigateToLibrary()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "进入简谱库，管理乐谱、编辑或开始练习"
                },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("简谱库", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("浏览、编辑乐谱或开始练习", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在准备乐器...", fontSize = 16.sp, color = Color.Gray)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}小时${minutes % 60}分"
        minutes > 0 -> "${minutes}分${seconds % 60}秒"
        else -> "${seconds}秒"
    }
}

fun getOctaveColor(octave: Octave): Color = when (octave) {
    Octave.DOWN -> Color(0xFF795548)
    Octave.MIDDLE -> Color(0xFF4CAF50)
    Octave.HIGH -> Color(0xFF2196F3)
    Octave.HIGH_HIGH -> Color(0xFF9C27B0)
}