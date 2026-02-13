package com.danmo.kalimba.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType

// ControlButton 修改 - 使用自定义图标
@Composable
fun ControlButton(
    iconResId: Int, // ⚠️ 改为 Int 资源ID
    label: String,
    enabled: Boolean = true,
    accessibilityHelper: AccessibilityHelper,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        accessibilityHelper.vibrate(VibrationType.CLICK)
                        onClick()
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (enabled) Color(0xFF64B5F6) else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId), // ⚠️ 使用 painterResource
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (enabled) Color(0xFF1976D2) else Color.Gray
        )
    }
}

@Composable
fun CurrentNoteDisplay(
    segment: PracticeSegment,
    currentNoteIndex: Int,
    isPreviewing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreviewing) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val note = segment.notes.getOrNull(currentNoteIndex)
            if (note != null && !note.isRest) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = KeyPositionHelper.getDisplayName(note.keyId),
                        fontSize = 48.sp,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "第 ${currentNoteIndex + 1} / ${segment.notes.size} 音",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Text("完成", fontSize = 24.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SegmentProgressIndicator(
    totalNotes: Int,
    currentIndex: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(totalNotes) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            index < currentIndex -> Color(0xFF4CAF50)
                            index == currentIndex -> Color(0xFF2196F3)
                            else -> Color(0xFFE0E0E0)
                        }
                    )
            )
        }
    }
}

@Composable
fun SegmentSelector(
    segments: List<PracticeSegment>,
    currentIndex: Int,
    accessibilityHelper: AccessibilityHelper, // ✅ 新增
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        segments.forEachIndexed { index, segment ->
            Button(
                onClick = {
                    accessibilityHelper.vibrate(VibrationType.CLICK)
                    onSelect(index)
                    accessibilityHelper.speak("第${index + 1}段，${segment.name}")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (index == currentIndex)
                        Color(0xFF1976D2) else Color(0xFFBBDEFB)
                ),
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "切换到第${index + 1}段"
                }
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 16.sp,
                    color = if (index == currentIndex) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun PlayButton(
    targetNote: PracticeNote?,
    isPlaying: Boolean,
    isPreviewing: Boolean,
    audioManager: PracticeAudioManager,
    accessibilityHelper: AccessibilityHelper, // ✅ 新增
    onPlayingChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        if (targetNote != null && !targetNote.isRest) {
            val targetDesc = KeyPositionHelper.getPositionDescription(targetNote.keyId)

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) Color(0xFF90CAF9) else Color(0xFF2196F3)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isPlaying && !isPreviewing,
                        onClick = {
                            onPlayingChanged(true)

                            // ✅ 1. 触觉反馈
                            accessibilityHelper.vibrate(VibrationType.STRONG)

                            // ✅ 2. 播放声音
                            audioManager.playSoundWithCallback(targetNote.keyId) {
                                onPlayingChanged(false)
                            }

                            // ✅ 3. 播报描述
                            accessibilityHelper.speak(targetDesc)
                        }
                    )
                    .semantics {
                        contentDescription = "播放 $targetDesc"
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = KeyPositionHelper.getDisplayName(targetNote.keyId),
                        fontSize = 56.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isPlaying) "播放中..." else "点击练习",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // 完成状态
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .semantics { contentDescription = "全部完成" },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", fontSize = 72.sp, color = Color.White)
            }
        }
    }
}