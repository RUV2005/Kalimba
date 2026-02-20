// 修改后的 PracticeScreen.kt
package com.danmo.kalimba.practice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.data.local.KalimbaDatabase
import com.danmo.kalimba.metronome.MetronomeManager
import com.danmo.kalimba.settings.AppSettings
import com.danmo.kalimba.settings.SettingsManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    sheetId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSmartPractice: (Int, Int) -> Unit  // ✅ 简化：只需要段落索引和BPM
) {
    val context = LocalContext.current
    val database = remember { KalimbaDatabase.getDatabase(context) }
    val viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.Factory(sheetId, database)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val audioManager = remember { PracticeAudioManager(context) }
    val accessibilityHelper = remember { AccessibilityHelper(context) }

    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AppSettings())

    val metronome = remember { MetronomeManager(context, accessibilityHelper) }
    var isMetronomeEnabled by remember { mutableStateOf(settings.metronomeEnabled) }

    var currentSegmentIndex by remember { mutableIntStateOf(0) }
    var currentNoteIndex by remember { mutableIntStateOf(0) }
    var isPreviewing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val segments = uiState.segments
    val currentSegment = segments.getOrNull(currentSegmentIndex)

    // 初始化音频
    LaunchedEffect(Unit) {
        while (!audioManager.isReady() || !accessibilityHelper.isReady()) {
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopPreview()
            audioManager.release()
            metronome.release()
            accessibilityHelper.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            audioManager.stopPreview()
                            metronome.stop()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "返回"
                        )
                    }
                },
                title = {
                    Text(
                        text = if (uiState.isLoading) "加载中..."
                        else "练习 - ${uiState.sheetName}"
                    )
                },
                actions = {
                    // ✅ 智能跟练按钮
                    if (currentSegment != null && currentSegment.notes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                accessibilityHelper.vibrate(VibrationType.CLICK)
                                accessibilityHelper.speak("进入智能跟练模式")
                                // ✅ 简化调用，只传索引和BPM
                                onNavigateToSmartPractice(currentSegmentIndex, uiState.bpm)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_music_note),
                                contentDescription = "智能跟练",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 节拍器按钮
                    IconButton(
                        onClick = {
                            isMetronomeEnabled = !isMetronomeEnabled
                            if (isMetronomeEnabled) {
                                metronome.startWithSettings(
                                    bpm = settings.defaultMetronomeBpm,
                                    beatsPerBar = settings.metronomeBeatsPerMeasure,
                                    metronomeVolume = settings.metronomeVolume,
                                    metronomeVibration = settings.metronomeVibrationEnabled,
                                    accentFirst = settings.metronomeAccentFirstBeat
                                )
                            } else {
                                metronome.stop()
                            }
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isMetronomeEnabled) R.drawable.ic_volume_up
                                else R.drawable.ic_volume_off
                            ),
                            contentDescription = if (isMetronomeEnabled) "关闭节拍器" else "开启节拍器",
                            tint = if (isMetronomeEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentSegment != null) {
                        Text(
                            text = "${currentSegmentIndex + 1}/${segments.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleMedium
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
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error!!,
                    onRetry = {
                        val intent = context.packageManager
                            .getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(intent)
                    }
                )
                segments.isEmpty() -> EmptyView()
                currentSegment == null -> {
                    LaunchedEffect(Unit) {
                        currentSegmentIndex = 0
                    }
                }
                else -> {
                    PracticeContent(
                        segment = currentSegment,
                        segments = segments,
                        currentSegmentIndex = currentSegmentIndex,
                        currentNoteIndex = currentNoteIndex,
                        isPreviewing = isPreviewing,
                        isPlaying = isPlaying,
                        bpm = uiState.bpm,
                        audioManager = audioManager,
                        accessibilityHelper = accessibilityHelper,
                        onSegmentChange = { index ->
                            currentSegmentIndex = index
                            currentNoteIndex = 0
                            isPlaying = false
                            audioManager.stopPreview()
                            isPreviewing = false
                        },
                        onNoteIndexChange = { index ->
                            currentNoteIndex = index
                        },
                        onPreviewingChange = { previewing ->
                            isPreviewing = previewing
                        },
                        onPlayingChange = { playing ->
                            isPlaying = playing
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeContent(
    segment: PracticeSegment,
    segments: List<PracticeSegment>,
    currentSegmentIndex: Int,
    currentNoteIndex: Int,
    isPreviewing: Boolean,
    isPlaying: Boolean,
    bpm: Int,
    audioManager: PracticeAudioManager,
    accessibilityHelper: AccessibilityHelper,
    onSegmentChange: (Int) -> Unit,
    onNoteIndexChange: (Int) -> Unit,
    onPreviewingChange: (Boolean) -> Unit,
    onPlayingChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当前音符显示区
        CurrentNoteDisplay(
            segment = segment,
            currentNoteIndex = currentNoteIndex,
            isPreviewing = isPreviewing
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 进度指示器
        SegmentProgressIndicator(
            totalNotes = segment.notes.size,
            currentIndex = currentNoteIndex
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 分段选择器
        SegmentSelector(
            segments = segments,
            currentIndex = currentSegmentIndex,
            accessibilityHelper = accessibilityHelper,
            onSelect = onSegmentChange
        )

        Spacer(modifier = Modifier.weight(1f))

        // 控制区域
        val targetNote = segment.notes.getOrNull(currentNoteIndex)
        val hasPrevious = currentNoteIndex > 0
        val hasNext = currentNoteIndex < segment.notes.size - 1

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一个按钮
            ControlButton(
                iconResId = R.drawable.ic_arrow_left,
                label = "上一个",
                enabled = hasPrevious && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    if (hasPrevious) {
                        onNoteIndexChange(currentNoteIndex - 1)
                        val prevNote = segment.notes.getOrNull(currentNoteIndex - 1)
                        if (prevNote != null && !prevNote.isRest) {
                            val desc = KeyPositionHelper.getPositionDescription(prevNote.keyId)
                            accessibilityHelper.speak("上一个，$desc")
                        }
                    }
                }
            )

            // 播放按钮
            PlayButton(
                targetNote = targetNote,
                isPlaying = isPlaying,
                isPreviewing = isPreviewing,
                audioManager = audioManager,
                accessibilityHelper = accessibilityHelper,
                onPlayingChanged = onPlayingChange
            )

            // 下一个按钮
            ControlButton(
                iconResId = R.drawable.ic_arrow_right,
                label = "下一个",
                enabled = hasNext && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    if (hasNext) {
                        onNoteIndexChange(currentNoteIndex + 1)
                        val nextNote = segment.notes.getOrNull(currentNoteIndex + 1)
                        if (nextNote != null && !nextNote.isRest) {
                            val desc = KeyPositionHelper.getPositionDescription(nextNote.keyId)
                            accessibilityHelper.speak("下一个，$desc")
                        }
                    }
                }
            )
        }

        // 预览整段按钮
        Button(
            onClick = {
                if (isPreviewing) {
                    audioManager.stopPreview()
                    onPreviewingChange(false)
                } else {
                    onPreviewingChange(true)
                    onNoteIndexChange(0)
                    accessibilityHelper.speak("开始预览第${currentSegmentIndex + 1}段")
                    audioManager.previewSegment(
                        notes = segment.notes,
                        onNote = { index ->
                            onNoteIndexChange(index)
                            accessibilityHelper.vibrate(VibrationType.TICK)
                        },
                        onComplete = {
                            onPreviewingChange(false)
                            onNoteIndexChange(0)
                        },
                        bpm = bpm
                    )
                }
            },
            enabled = !isPlaying,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPreviewing) R.drawable.ic_refresh else R.drawable.ic_play
                ),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPreviewing) "停止预览" else "预览整段")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("加载简谱中...")
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_off),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("该简谱没有可练习的内容", color = Color.Gray)
        }
    }
}