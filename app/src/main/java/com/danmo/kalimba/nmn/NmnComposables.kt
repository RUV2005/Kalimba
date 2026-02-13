// 文件名: NmnComponents.kt
package com.danmo.kalimba.nmn

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType

@Composable
fun EditableNoteDisplay(
    songName: String,
    segmentIndex: Int,
    currentNoteIndex: Int,
    totalNotes: Int,
    notes: List<NmnNote>,
    isPreviewing: Boolean,
    height: Dp,
    isEditingNew: Boolean
) {
    val note = notes.getOrNull(currentNoteIndex)
    val prevNote = notes.getOrNull(currentNoteIndex - 1)
    val nextNote = notes.getOrNull(currentNoteIndex + 1)

    // ⚠️ 判断所有音符都为空的情况
    val allEmpty = prevNote == null && note == null && nextNote == null

    // ⚠️ 构建按顺序播报的描述
    val fullDescription = buildString {
        if (allEmpty) {
            // 所有音符都为空
            append("《$songName》，第${segmentIndex}段，当前待录入新音符")
            if (isPreviewing) append("，预览中")
        } else {
            // 有音符，按顺序播报
            append("《$songName》，第${segmentIndex}段，共${totalNotes}个音符")
            if (isPreviewing) append("，预览中")
            append("。 ")

            // 1. 前一音
            when {
                prevNote?.isRestNote() == true -> append("前一音，停顿。 ")
                prevNote != null -> append("前一音，${KeyPositionHelper.getPitchName(prevNote.keyId)}。 ")
            }

            // 2. 当前音（一定有）
            when {
                note?.isRestNote() == true -> append("当前音，停顿。 ")
                note != null -> append("当前音，${KeyPositionHelper.getPitchName(note.keyId)}。 ")
                else -> append("当前待录入新音符。 ")
            }

            // 3. 后一音
            when {
                nextNote?.isRestNote() == true -> append("后一音，停顿")
                nextNote != null -> append("后一音，${KeyPositionHelper.getPitchName(nextNote.keyId)}")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            // ⚠️ 关键：使用 clearAndSetSemantics 让整个卡片作为一个无障碍节点
            .clearAndSetSemantics {
                contentDescription = fullDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPreviewing -> Color(0xFFE3F2FD)
                isEditingNew -> Color(0xFFFFF3E0)
                else -> Color(0xFFF5F5F5)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // 内部所有组件都不需要单独设置 semantics，因为父元素已清除
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "《$songName》",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF333333)
                )
                Text(
                    "第 $segmentIndex 段",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 内部组件仅用于视觉显示，不参与无障碍播报
                NotePreviewBoxInternal(prevNote, label = "前一音", isSmall = true)

                Spacer(modifier = Modifier.width(20.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        note != null -> {
                            if (note.isRestNote()) {
                                Text(
                                    text = "0",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    text = "停顿",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFF9800)
                                )
                            } else {
                                Text(
                                    text = KeyPositionHelper.getDisplayName(note.keyId),
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = KeyPositionHelper.getPitchName(note.keyId),
                                    fontSize = 14.sp,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                val nextLabel = if (isEditingNew && nextNote == null) "待录入" else "后一音"
                NotePreviewBoxInternal(nextNote, label = nextLabel, isSmall = true, isPlaceholder = isEditingNew && nextNote == null)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    val displayIndex = if (note != null) currentNoteIndex + 1 else currentNoteIndex
                    Text(
                        text = "位置: 第 $displayIndex 个音符",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                    ) {
                        val progress = if (totalNotes > 0) {
                            ((currentNoteIndex + if (note != null) 1 else 0).toFloat() / totalNotes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color(0xFF4CAF50))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "总计: $totalNotes 个音符",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// 内部使用的预览组件，不带 semantics
@Composable
private fun NotePreviewBoxInternal(
    note: NmnNote?,
    label: String,
    isSmall: Boolean,
    isPlaceholder: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (isPlaceholder) 0.5f else 0.7f)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        val displayText = when {
            note?.isRestNote() == true -> "0"
            note != null -> KeyPositionHelper.getDisplayName(note.keyId)
            isPlaceholder -> "+"
            else -> "-"
        }

        Text(
            text = displayText,
            fontSize = if (isSmall) 36.sp else 44.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                note?.isRestNote() == true -> Color(0xFFFF9800)
                isPlaceholder -> Color(0xFFFF9800)
                else -> Color(0.3f, 0.3f, 0.3f, 1f)
            }
        )
    }
}

@Composable
fun NotePreviewBox(note: NmnNote?, label: String, isSmall: Boolean, isPlaceholder: Boolean = false) {
    val description = when {
        note?.isRestNote() == true -> "$label，停顿"
        note != null -> "$label，${KeyPositionHelper.getPitchName(note.keyId)}"
        isPlaceholder -> "$label，待录入"
        else -> "$label，无"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (isPlaceholder) 0.5f else 0.7f)
            .semantics { contentDescription = description }
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

        val displayText = when {
            note?.isRestNote() == true -> "0"
            note != null -> KeyPositionHelper.getDisplayName(note.keyId)
            isPlaceholder -> "+"
            else -> "-"
        }

        Text(
            text = displayText,
            fontSize = if (isSmall) 36.sp else 44.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                note?.isRestNote() == true -> Color(0xFFFF9800)
                isPlaceholder -> Color(0xFFFF9800)
                else -> Color(0.3f, 0.3f, 0.3f, 1f)
            }
        )
    }
}

/**
 * 播放按钮状态数据类
 * 替代之前的Quintuple，更清晰易懂
 */
data class PlayButtonState(
    val label: String,
    val iconResId: Int?, // ⚠️ 改为图标资源ID
    val displayText: String, // 备用：当没有图标时显示文字
    val backgroundColor: Color,
    val enabled: Boolean,
    val semanticsDescription: String
)

/**
 * 播放当前音符按钮 - ⚠️ 重构版
 * 添加 AccessibilityHelper 支持，使用图标替代文字
 */
@Composable
fun PlayCurrentButton(
    currentNote: NmnNote?,
    isPlaying: Boolean,
    isPreviewing: Boolean,
    audioManager: NmnAudioManager,
    accessibilityHelper: AccessibilityHelper,
    onPlayingChanged: (Boolean) -> Unit,
    buttonSize: Dp = 80.dp
) {
    val iconSize = if (buttonSize < 70.dp) 28.dp else 36.dp

    val buttonState = when {
        currentNote == null -> PlayButtonState(
            label = "无音符",
            iconResId = null,
            displayText = "-",
            backgroundColor = Color(0xFFBDBDBD),
            enabled = false,
            semanticsDescription = "播放按钮，无音符可用"
        )
        currentNote.isRestNote() -> PlayButtonState(
            label = "停顿",
            iconResId = R.drawable.ic_rest, // ⚠️ 使用停顿图标
            displayText = "0",
            backgroundColor = Color(0xFFFF9800),
            enabled = false,
            semanticsDescription = "当前是停顿"
        )
        isPlaying -> PlayButtonState(
            label = "播放中",
            iconResId = null, // 播放中显示文字动画
            displayText = "...",
            backgroundColor = Color(0xFF90CAF9),
            enabled = false,
            semanticsDescription = "播放中"
        )
        isPreviewing -> PlayButtonState(
            label = "预览中",
            iconResId = null,
            displayText = "预览",
            backgroundColor = Color(0xFFBDBDBD),
            enabled = false,
            semanticsDescription = "预览中，播放不可用"
        )
        else -> {
            val pitchName = KeyPositionHelper.getPitchName(currentNote.keyId)
            PlayButtonState(
                label = "播放当前",
                iconResId = R.drawable.ic_play, // ⚠️ 使用播放图标
                displayText = "",
                backgroundColor = Color(0xFF2196F3),
                enabled = true,
                semanticsDescription = "播放当前音符 $pitchName"
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = buttonState.enabled,
                onClick = {
                    if (currentNote != null && !currentNote.isRestNote()) {
                        // ⚠️ 添加触觉反馈
                        accessibilityHelper.vibrate(VibrationType.MEDIUM)

                        onPlayingChanged(true)

                        audioManager.playSoundWithCallback(currentNote.keyId) {}

                        // ⚠️ 使用 AccessibilityHelper 播报
                        val pitchName = KeyPositionHelper.getPitchName(currentNote.keyId)
                        accessibilityHelper.speak(pitchName)

                        Handler(Looper.getMainLooper()).postDelayed({
                            onPlayingChanged(false)
                        }, 1000)
                    }
                }
            )
            .semantics {
                contentDescription = buttonState.semanticsDescription
            }
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(buttonState.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            // ⚠️ 优先显示图标，没有图标时显示文字
            if (buttonState.iconResId != null) {
                Icon(
                    painter = painterResource(id = buttonState.iconResId),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Text(
                    text = buttonState.displayText,
                    fontSize = if (buttonSize < 70.dp) 13.sp else 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = buttonState.label, fontSize = 11.sp, color = Color.Gray)
    }
}