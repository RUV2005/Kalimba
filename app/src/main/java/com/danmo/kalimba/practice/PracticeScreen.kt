// 文件名: PracticeScreen.kt (重构版 - 添加返回按钮和自定义图标)
package com.danmo.kalimba.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onNavigateBack: () -> Unit = {} // ⚠️ 新增：返回回调参数
) {
    val context = LocalContext.current
    // ✅ 初始化两个助手
    val audioManager = remember { PracticeAudioManager(context) }
    val accessibilityHelper = remember { AccessibilityHelper(context) }

    var isReady by remember { mutableStateOf(false) }
    var currentSegmentIndex by remember { mutableIntStateOf(0) }
    var currentNoteIndex by remember { mutableIntStateOf(0) }
    var isPreviewing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val currentSegment = PracticeData.SEGMENTS[currentSegmentIndex]

    LaunchedEffect(Unit) {
        // ✅ 修改等待逻辑，确保两者都就绪
        while (!audioManager.isReady() || !accessibilityHelper.isReady()) {
            delay(100L)
        }
        isReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopPreview()
            audioManager.release()
            // ✅ 记得释放
            accessibilityHelper.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // ✅ 使用 navigationIcon 槽位添加返回按钮
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 1. 触发触觉反馈
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            // 2. 停止当前所有音频（安全起见）
                            audioManager.stopPreview()
                            // 3. 执行导航返回
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            // ⚠️ 注意：根据你的目录，使用 ic_arrow_left 或自带的 ArrowBack
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "返回上一页"
                        )
                    }
                },
                title = { Text("分段教学 - ${currentSegment.name}") },
                actions = {
                    IconButton(
                        onClick = {
                            // ✅ 添加触觉反馈
                            accessibilityHelper.vibrate(VibrationType.CLICK)

                            if (isPreviewing) {
                                audioManager.stopPreview()
                                isPreviewing = false
                            } else {
                                isPreviewing = true
                                currentNoteIndex = 0

                                // ✅ 播报预览开始
                                accessibilityHelper.speak("开始预览第${currentSegmentIndex + 1}段")

                                audioManager.previewSegment(
                                    notes = currentSegment.notes,
                                    onNote = { index ->
                                        currentNoteIndex = index
                                        // ✅ 预览过程中的轻微震动
                                        accessibilityHelper.vibrate(VibrationType.TICK)
                                    },
                                    onComplete = {
                                        isPreviewing = false
                                        currentNoteIndex = 0
                                        // ⚠️ 移除了 accessibilityHelper.speak("预览结束")
                                    },
                                    bpm = 80
                                )
                            }
                        },
                        enabled = isReady && !isPreviewing
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPreviewing) R.drawable.ic_refresh else R.drawable.ic_play
                            ),
                            contentDescription = "预览整段"
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
                .padding(16.dp)
        ) {
            if (!isReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载中...")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 当前音符显示区
                    CurrentNoteDisplay(
                        segment = currentSegment,
                        currentNoteIndex = currentNoteIndex,
                        isPreviewing = isPreviewing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 进度指示器
                    SegmentProgressIndicator(
                        totalNotes = currentSegment.notes.size,
                        currentIndex = currentNoteIndex
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 分段选择器
                    SegmentSelector(
                        segments = PracticeData.SEGMENTS,
                        currentIndex = currentSegmentIndex,
                        accessibilityHelper = accessibilityHelper, // ✅ 传递参数
                        onSelect = { index ->
                            currentSegmentIndex = index
                            currentNoteIndex = 0
                            isPlaying = false
                            audioManager.stopPreview()
                            isPreviewing = false
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 控制区域：上一个 - 播放 - 下一个
                    val targetNote = currentSegment.notes.getOrNull(currentNoteIndex)
                    val hasPrevious = currentNoteIndex > 0
                    val hasNext = currentNoteIndex < currentSegment.notes.size - 1

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 【上一个】按钮
                        ControlButton(
                            iconResId = R.drawable.ic_arrow_left, // ⚠️ 使用自定义图标
                            label = "上一个",
                            enabled = hasPrevious && !isPlaying && !isPreviewing,
                            accessibilityHelper = accessibilityHelper, // ✅ 传递参数
                            onClick = {
                                if (hasPrevious) {
                                    currentNoteIndex--
                                    val prevNote = currentSegment.notes.getOrNull(currentNoteIndex)
                                    if (prevNote != null && !prevNote.isRest) {
                                        val desc = KeyPositionHelper.getPositionDescription(prevNote.keyId)
                                        // ✅ 使用 accessibilityHelper 替换 speakOnly
                                        accessibilityHelper.speak("上一个，$desc")
                                    }
                                }
                            }
                        )

                        // 【中间】主播放按钮
                        PlayButton(
                            targetNote = targetNote,
                            isPlaying = isPlaying,
                            isPreviewing = isPreviewing,
                            audioManager = audioManager,
                            accessibilityHelper = accessibilityHelper, // ✅ 传递参数
                            onPlayingChanged = { playing ->
                                isPlaying = playing
                            }
                        )

                        // 【下一个】按钮
                        ControlButton(
                            iconResId = R.drawable.ic_arrow_right, // ⚠️ 使用自定义图标
                            label = "下一个",
                            enabled = hasNext && !isPlaying && !isPreviewing,
                            accessibilityHelper = accessibilityHelper, // ✅ 传递参数
                            onClick = {
                                if (hasNext) {
                                    currentNoteIndex++
                                    val nextNote = currentSegment.notes.getOrNull(currentNoteIndex)
                                    if (nextNote != null && !nextNote.isRest) {
                                        val desc = KeyPositionHelper.getPositionDescription(nextNote.keyId)
                                        // ✅ 使用 accessibilityHelper 替换 speakOnly
                                        accessibilityHelper.speak("下一个，$desc")
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}