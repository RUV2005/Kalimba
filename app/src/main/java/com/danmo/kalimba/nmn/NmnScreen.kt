// 文件名: NmnScreen.kt (重构版 - 添加返回按钮)
package com.danmo.kalimba.nmn

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import com.danmo.kalimba.main.KalimbaKey
import com.danmo.kalimba.main.KalimbaKeyData
import com.danmo.kalimba.main.Octave as MainOctave
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NmnScreen(
    onNavigateBack: () -> Unit = {} // ⚠️ 新增：返回回调参数
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ⚠️ 使用 AccessibilityHelper 替代原来的 announce
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    val audioManager = remember { NmnAudioManager(context) }

    var isReady by remember { mutableStateOf(false) }

    var segments by remember { mutableStateOf(listOf(NmnData.createEmptySegment(1))) }
    var currentSegmentIndex by remember { mutableIntStateOf(0) }
    var currentNoteIndex by remember { mutableIntStateOf(0) }
    var isPreviewing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedOctave by remember { mutableStateOf(MainOctave.MIDDLE) }
    var songName by remember { mutableStateOf("未命名简谱") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var tempSongName by remember { mutableStateOf("") }

    val currentSegment = segments.getOrNull(currentSegmentIndex) ?: segments.first()
    val hasNotes = currentSegment.notes.isNotEmpty()
    val currentNoteExists = currentNoteIndex < currentSegment.notes.size

    // ⚠️ 语音播报开关状态（提升到 NmnScreen 级别，默认开启）
    var isSpeechEnabled by remember { mutableStateOf(true) }

    // ⚠️ 同步 AccessibilityHelper 的状态
    LaunchedEffect(Unit) {
        while (!audioManager.isReady() || !accessibilityHelper.isReady()) {
            delay(100L)
        }
        isReady = true
        // 同步初始状态到 AccessibilityHelper
        accessibilityHelper.isSpeechEnabled = isSpeechEnabled
    }

    // ⚠️ 当开关状态变化时同步到 AccessibilityHelper
    LaunchedEffect(isSpeechEnabled) {
        accessibilityHelper.isSpeechEnabled = isSpeechEnabled
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopPreview()
            audioManager.release()
            accessibilityHelper.release()
        }
    }

    val currentStateDescription = buildString {
        append("《$songName》，第${currentSegmentIndex + 1}段，")
        if (currentNoteExists) {
            val note = currentSegment.notes[currentNoteIndex]
            append("第${currentNoteIndex + 1}个音符，${KeyPositionHelper.getPitchName(note.keyId)}")
        } else {
            append("待录入位置")
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("保存简谱") },
            text = {
                Column {
                    Text("请输入简谱名称：", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempSongName,
                        onValueChange = { tempSongName = it },
                        label = { Text("简谱名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempSongName.isNotBlank()) {
                            songName = tempSongName
                            // ⚠️ 使用 AccessibilityHelper
                            accessibilityHelper.provideFeedback(
                                text = "已保存为 $songName",
                                vibrationType = VibrationType.MEDIUM
                            )
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.semantics {
            contentDescription = currentStateDescription
        },
        topBar = {
            TopAppBar(
                // ⚠️ 新增：返回按钮
                navigationIcon = {
                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            onNavigateBack()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "返回"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Text(
                        text = "简谱编辑 - $songName",
                        // --- 核心修改部分 ---
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,                // 修改字体大小
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "当前编辑 $songName"
                        }
                    )
                },
                actions = {
                    // ⚠️ 语音播报开关按钮（移除了内部重复定义）
                    IconButton(
                        onClick = {
                            isSpeechEnabled = !isSpeechEnabled
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            // 使用 provideFeedback 来播报状态变化（即使关闭也能听到这次播报）
                            if (isSpeechEnabled) {
                                accessibilityHelper.speak("语音播报已开启", interrupt = true)
                            } else {
                                // 关闭时也要给用户反馈，但用振动代替
                                accessibilityHelper.vibrate(VibrationType.DOUBLE_CLICK)
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (isSpeechEnabled) "关闭语音播报" else "开启语音播报"
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isSpeechEnabled) R.drawable.ic_volume_on else R.drawable.ic_volume_off
                            ),
                            contentDescription = null,
                            tint = if (isSpeechEnabled) Color(0xFF4CAF50) else Color.Gray
                        )
                    }

                    // 新建按钮
                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            segments = listOf(NmnData.createEmptySegment(1))
                            currentSegmentIndex = 0
                            currentNoteIndex = 0
                            songName = "未命名简谱"
                            accessibilityHelper.speak("新建简谱")
                        },
                        enabled = isReady,
                        modifier = Modifier.semantics {
                            contentDescription = "新建简谱"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)

                            if (isPreviewing) {
                                audioManager.stopPreview()
                                isPreviewing = false
                            } else if (hasNotes) {
                                isPreviewing = true
                                currentNoteIndex = 0

                                accessibilityHelper.speak("开始预览第${currentSegmentIndex + 1}段")

                                audioManager.previewSegment(
                                    notes = currentSegment.notes,
                                    onNote = { index ->
                                        currentNoteIndex = index
                                        // ⚠️ 预览时添加轻微振动
                                        accessibilityHelper.vibrate(VibrationType.TICK)
                                    },
                                    onComplete = {
                                        isPreviewing = false
                                        currentNoteIndex = currentNoteIndex.coerceAtMost(currentSegment.notes.size - 1)
                                        // ⚠️ 移除了 accessibilityHelper.speak("预览结束")
                                    },
                                    bpm = 80
                                )
                            }
                        },
                        enabled = isReady && !isPreviewing && hasNotes,
                        modifier = Modifier.semantics {
                            contentDescription = if (isPreviewing) "停止预览" else "预览当前段落"
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPreviewing) R.drawable.ic_refresh else R.drawable.ic_play
                            ),
                            contentDescription = null
                        )
                    }

                    // 保存按钮
                    IconButton(
                        onClick = {
                            accessibilityHelper.vibrate(VibrationType.CLICK)
                            tempSongName = if (songName == "未命名简谱") "" else songName
                            showRenameDialog = true
                        },
                        enabled = isReady && hasNotes,
                        modifier = Modifier.semantics {
                            contentDescription = "结束编辑并保存"
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = if (hasNotes) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLandscape) {
                    OctaveSelector(selectedOctave, { selectedOctave = it }, true)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EditableNoteDisplay(
                        songName = songName,
                        segmentIndex = currentSegmentIndex + 1,
                        currentNoteIndex = currentNoteIndex,
                        totalNotes = currentSegment.notes.size,
                        notes = currentSegment.notes,
                        isPreviewing = isPreviewing,
                        height = if (isLandscape) 100.dp else 220.dp,
                        isEditingNew = !currentNoteExists && !isPreviewing
                    )

                    if (!isLandscape) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OctaveSelector(selectedOctave, { selectedOctave = it }, false)
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val highlightedKeyId = if (currentNoteExists && !isPreviewing) {
                        currentSegment.notes.getOrNull(currentNoteIndex)?.keyId
                    } else null

                    val filteredKeys = KalimbaKeyData.getAllKeys()
                        .filter { it.octave == selectedOctave }
                        .sortedBy { it.index }

                    FilteredKeyRow(
                        keys = filteredKeys,
                        keyHeight = if (isLandscape) 48.dp else 86.dp,
                        highlightedKeyId = highlightedKeyId,
                        accessibilityHelper = accessibilityHelper,
                        onKeyClick = { key ->
                            if (isPreviewing) return@FilteredKeyRow

                            // ⚠️ 添加触觉反馈
                            accessibilityHelper.vibrate(VibrationType.MEDIUM)

                            audioManager.playSoundWithCallback(key.id) {}

                            val newNote = NmnNote(key.id, key.pitch, Octave.valueOf(key.octave.name))
                            val currentNotes = currentSegment.notes.toMutableList()

                            if (currentNoteExists) {
                                currentNotes[currentNoteIndex] = newNote
                            } else {
                                currentNotes.add(currentNoteIndex, newNote)
                            }

                            val updatedSegment = currentSegment.copy(notes = currentNotes)
                            val newSegments = segments.toMutableList()
                            newSegments[currentSegmentIndex] = updatedSegment
                            segments = newSegments

                            currentNoteIndex = (currentNoteIndex + 1).coerceAtMost(currentNotes.size)

                            val pitchName = KeyPositionHelper.getPitchName(key.id)
                            // ⚠️ 使用 AccessibilityHelper
                            accessibilityHelper.speak("录入 $pitchName")
                        }
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))

                    // 控制区域
                    EditorControlArea(
                        currentSegment = currentSegment,
                        currentSegmentIndex = currentSegmentIndex,
                        currentNoteIndex = currentNoteIndex,
                        totalSegments = segments.size,
                        isPlaying = isPlaying,
                        isPreviewing = isPreviewing,
                        audioManager = audioManager,
                        accessibilityHelper = accessibilityHelper,
                        onSegmentChange = { idx ->
                            currentSegmentIndex = idx
                            currentNoteIndex = 0
                        },
                        onNoteChange = { idx -> currentNoteIndex = idx },
                        onPlayingChanged = { isPlaying = it },
                        onAddSegment = {
                            val newId = segments.size + 1
                            segments = segments + NmnData.createEmptySegment(newId)
                            currentSegmentIndex = segments.size - 1
                            currentNoteIndex = 0
                            accessibilityHelper.speak("新增第${newId}段")
                        },
                        onDeleteSegment = {
                            if (segments.size > 1) {
                                val newSegments = segments.toMutableList()
                                newSegments.removeAt(currentSegmentIndex)
                                segments = newSegments
                                currentSegmentIndex = currentSegmentIndex.coerceAtMost(segments.size - 1)
                                currentNoteIndex = 0
                                accessibilityHelper.speak("删除段落")
                            }
                        },
                        onDeleteNote = { newIndex, newNotes ->
                            val updatedSegment = currentSegment.copy(notes = newNotes)
                            val newSegments = segments.toMutableList()
                            newSegments[currentSegmentIndex] = updatedSegment
                            segments = newSegments
                            currentNoteIndex = newIndex
                        },
                        onAddRest = { newNotes ->
                            val updatedSegment = currentSegment.copy(notes = newNotes)
                            val newSegments = segments.toMutableList()
                            newSegments[currentSegmentIndex] = updatedSegment
                            segments = newSegments
                            currentNoteIndex = currentNoteIndex + 1
                            accessibilityHelper.speak("添加停顿")
                        },
                        isLandscape = isLandscape
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))
                }
            }
        }
    }
}

// ⚠️ 修改后的 FilteredKeyRow - 添加 accessibilityHelper 参数
@Composable
fun FilteredKeyRow(
    keys: List<KalimbaKey>,
    keyHeight: Dp,
    highlightedKeyId: String?,
    accessibilityHelper: AccessibilityHelper,
    onKeyClick: (KalimbaKey) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (keys.isEmpty()) return

    val keyItems = keys.map { key ->
        key to KeyPositionHelper.getPitchName(key.id)
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            keyItems.forEach { (key, pitchName) ->
                // ⚠️ 移除外层的 Box 和 semantics，直接在 FilteredKeyButton 中处理
                FilteredKeyButton(
                    key = key,
                    pitchName = pitchName, // ⚠️ 传递 pitchName
                    keyHeight = keyHeight,
                    isHighlighted = key.id == highlightedKeyId,
                    accessibilityHelper = accessibilityHelper,
                    onClick = { onKeyClick(key) }
                )
            }
        }
    } else {
        val splitIndex = keyItems.size / 2
        val firstRow = keyItems.take(splitIndex)
        val secondRow = keyItems.drop(splitIndex)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                firstRow.forEach { (key, pitchName) ->
                    FilteredKeyButton(
                        key = key,
                        pitchName = pitchName, // ⚠️ 传递 pitchName
                        keyHeight = keyHeight,
                        isHighlighted = key.id == highlightedKeyId,
                        accessibilityHelper = accessibilityHelper,
                        onClick = { onKeyClick(key) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                secondRow.forEach { (key, pitchName) ->
                    FilteredKeyButton(
                        key = key,
                        pitchName = pitchName, // ⚠️ 传递 pitchName
                        keyHeight = keyHeight,
                        isHighlighted = key.id == highlightedKeyId,
                        accessibilityHelper = accessibilityHelper,
                        onClick = { onKeyClick(key) }
                    )
                }
            }
        }
    }
}

// ⚠️ 修改后的 FilteredKeyButton - 添加触觉反馈
// ⚠️ 修改后的 FilteredKeyButton - 添加触觉反馈和正确的无障碍支持
@Composable
fun FilteredKeyButton(
    key: KalimbaKey,
    pitchName: String, // ⚠️ 新增参数：完整的音高名称
    keyHeight: Dp,
    isHighlighted: Boolean,
    accessibilityHelper: AccessibilityHelper,
    onClick: () -> Unit
) {
    val baseColor = getOctaveColor(key.octave)

    // ⚠️ 构建完整的无障碍描述
    val semanticsDescription = buildString {
        append(pitchName) // 使用完整的音高名称，如"中音1"
        if (isHighlighted) append("，当前选中")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // ⚠️ 添加触觉反馈
                    accessibilityHelper.vibrate(VibrationType.MEDIUM)
                    onClick()
                }
            )
            .semantics {
                // ⚠️ 关键：设置 contentDescription，这会覆盖子元素的文本
                contentDescription = semanticsDescription
            }
            // ⚠️ 关键：清除子元素的无障碍信息，避免 TalkBack 读取数字
            .clearAndSetSemantics {
                contentDescription = semanticsDescription
            }
    ) {
        Box(
            modifier = Modifier
                .size(keyHeight)
                .clip(CircleShape)
                .background(if (isHighlighted) Color.Yellow else baseColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key.displayName, // 视觉显示可以是数字
                fontSize = (keyHeight.value * 0.32f).sp,
                color = if (isHighlighted) Color.Red else Color.White,
                fontWeight = FontWeight.ExtraBold,
                // ⚠️ 禁用此文本的无障碍功能，避免被 TalkBack 读取
                modifier = Modifier.semantics {
                    contentDescription = ""
                }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = key.pitch.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color.Red else Color.DarkGray,
            // ⚠️ 禁用此文本的无障碍功能
            modifier = Modifier.semantics {
                contentDescription = ""
            }
        )
    }
}

// ⚠️ EditorControlArea - 添加 accessibilityHelper 参数
@Composable
fun EditorControlArea(
    currentSegment: NmnSegment,
    currentSegmentIndex: Int,
    currentNoteIndex: Int,
    totalSegments: Int,
    isPlaying: Boolean,
    isPreviewing: Boolean,
    audioManager: NmnAudioManager,
    accessibilityHelper: AccessibilityHelper,
    onSegmentChange: (Int) -> Unit,
    onNoteChange: (Int) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onAddSegment: () -> Unit,
    onDeleteSegment: () -> Unit,
    onDeleteNote: (Int, List<NmnNote>) -> Unit,
    onAddRest: (List<NmnNote>) -> Unit,
    isLandscape: Boolean
) {
    val hasPreviousNote = currentNoteIndex > 0
    val hasNextNote = currentNoteIndex < currentSegment.notes.size
    val isFirstSegment = currentSegmentIndex <= 0
    val isLastSegment = currentSegmentIndex >= totalSegments - 1
    val canDeleteSegment = totalSegments > 1

    val currentNotes = currentSegment.notes
    val hasNotes = currentNotes.isNotEmpty()
    val canDeleteNote = hasNotes && currentNoteIndex > 0

    val btnSize = if (isLandscape) 36.dp else 48.dp
    val iconSize = if (isLandscape) 18.dp else 24.dp
    val playBtnSize = if (isLandscape) 48.dp else 60.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp)
    ) {
        // 第一行：段落控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_arrow_left,
                label = "上一段",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFFE53935),
                enabled = !isFirstSegment && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    onSegmentChange(currentSegmentIndex - 1)
                    accessibilityHelper.speak("第${currentSegmentIndex}段")
                },
                semanticsDescription = "上一段"
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_add,
                label = "新段",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFF4CAF50),
                enabled = !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = { onAddSegment() },
                semanticsDescription = "新增段落"
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_arrow_right,
                label = "下一段",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFFE53935),
                enabled = !isLastSegment && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    onSegmentChange(currentSegmentIndex + 1)
                    accessibilityHelper.speak("第${currentSegmentIndex + 2}段")
                },
                semanticsDescription = "下一段"
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_delete,
                label = "删除段",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFF757575),
                enabled = canDeleteSegment && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = { onDeleteSegment() },
                semanticsDescription = "删除当前段落"
            )
        }

        // 第二行：音符控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_rest,
                label = "停顿",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFFFF9800),
                enabled = !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    val restNote = NmnNote.createRest(durationMs = 1000)
                    val newNotes = currentNotes.toMutableList()
                    if (currentNoteIndex < newNotes.size) {
                        newNotes.add(currentNoteIndex, restNote)
                    } else {
                        newNotes.add(restNote)
                    }
                    onAddRest(newNotes)
                },
                semanticsDescription = "添加1秒停顿"
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_arrow_left,
                label = "上个音",
                buttonSize = btnSize,
                iconSize = iconSize,
                enabled = hasPreviousNote && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    val newIndex = currentNoteIndex - 1
                    onNoteChange(newIndex)
                    val note = currentSegment.notes.getOrNull(newIndex)
                    if (note != null) {
                        val pitchName = KeyPositionHelper.getPitchName(note.keyId)
                        accessibilityHelper.speak("第${newIndex + 1}音，$pitchName")
                    }
                },
                semanticsDescription = "上一个音符"
            )

            val currentNote = currentSegment.notes.getOrNull(currentNoteIndex)
            PlayCurrentButton(
                currentNote = currentNote,
                isPlaying = isPlaying,
                isPreviewing = isPreviewing,
                audioManager = audioManager,
                accessibilityHelper = accessibilityHelper,
                onPlayingChanged = onPlayingChanged,
                buttonSize = playBtnSize
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_arrow_right,
                label = "下个音",
                buttonSize = btnSize,
                iconSize = iconSize,
                enabled = hasNextNote && !isPlaying && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    val newIndex = currentNoteIndex + 1
                    onNoteChange(newIndex)
                    val note = currentSegment.notes.getOrNull(newIndex)
                    if (note != null) {
                        val pitchName = KeyPositionHelper.getPitchName(note.keyId)
                        accessibilityHelper.speak("第${newIndex + 1}音，$pitchName")
                    } else {
                        accessibilityHelper.speak("待录入")
                    }
                },
                semanticsDescription = "下一个音符"
            )

            ControlButtonWithDrawable(
                iconResId = R.drawable.ic_delete,
                label = "删除",
                buttonSize = btnSize,
                iconSize = iconSize,
                backgroundColor = Color(0xFF757575),
                enabled = canDeleteNote && !isPreviewing,
                accessibilityHelper = accessibilityHelper,
                onClick = {
                    val newIndex = (currentNoteIndex - 1).coerceAtLeast(0)
                    val newNotes = currentNotes.toMutableList().apply {
                        if (currentNoteIndex < size) {
                            removeAt(currentNoteIndex)
                        } else if (isNotEmpty()) {
                            removeAt(size - 1)
                        }
                    }
                    onDeleteNote(newIndex, newNotes)
                    accessibilityHelper.speak("已删除")
                },
                semanticsDescription = "删除当前音符"
            )
        }
    }
}

// ⚠️ ControlButtonWithDrawable - 添加触觉反馈
@Composable
fun ControlButtonWithDrawable(
    iconResId: Int,
    label: String,
    enabled: Boolean = true,
    buttonSize: Dp = 64.dp,
    iconSize: Dp = 32.dp,
    backgroundColor: Color = Color(0xFF64B5F6),
    accessibilityHelper: AccessibilityHelper,
    semanticsDescription: String = label,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        // ⚠️ 添加触觉反馈
                        accessibilityHelper.vibrate(VibrationType.CLICK)
                        onClick()
                    }
                }
            )
            .semantics {
                contentDescription = if (enabled) {
                    semanticsDescription
                } else {
                    "$semanticsDescription，不可用"
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = when {
                !enabled -> Color.Gray
                backgroundColor == Color(0xFFE53935) -> Color(0xFFC62828)
                backgroundColor == Color(0xFF4CAF50) -> Color(0xFF2E7D32)
                backgroundColor == Color(0xFF757575) -> Color(0xFF424242)
                backgroundColor == Color(0xFFFF9800) -> Color(0xFFE65100)
                else -> Color(0xFF1976D2)
            }
        )
    }
}

@Composable
fun OctaveSelector(
    selectedOctave: MainOctave,
    onOctaveSelected: (MainOctave) -> Unit,
    isVertical: Boolean
) {
    val octaves = listOf(
        MainOctave.DOWN to "低音",
        MainOctave.MIDDLE to "中音",
        MainOctave.HIGH to "高音",
        MainOctave.HIGH_HIGH to "倍高"
    )

    if (isVertical) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(Color(0xFFF0F0F0))
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            octaves.forEach { (octave, label) ->
                OctaveButton(
                    label,
                    octave == selectedOctave,
                    getOctaveColor(octave),
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics {
                            contentDescription = "选择$label${if (octave == selectedOctave) "，已选中" else ""}"
                        }
                ) { onOctaveSelected(octave) }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            octaves.forEach { (octave, label) ->
                OctaveButton(
                    label,
                    octave == selectedOctave,
                    getOctaveColor(octave),
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(42.dp)
                        .semantics {
                            contentDescription = "选择$label${if (octave == selectedOctave) "，已选中" else ""}"
                        }
                ) { onOctaveSelected(octave) }
            }
        }
    }
}

@Composable
fun OctaveButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color(0xFFD1D1D1)
        ),
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

fun getOctaveColor(octave: MainOctave): Color = when (octave) {
    MainOctave.DOWN -> Color(0xFF795548)
    MainOctave.MIDDLE -> Color(0xFF4CAF50)
    MainOctave.HIGH -> Color(0xFF2196F3)
    MainOctave.HIGH_HIGH -> Color(0xFF9C27B0)
    else -> Color.Gray
}