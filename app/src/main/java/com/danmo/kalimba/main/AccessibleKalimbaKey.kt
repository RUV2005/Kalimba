// 文件名: AccessibleKalimbaKey.kt
package com.danmo.kalimba.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType

/**
 * 无障碍优化的琴键按钮
 * 特点：
 * 1. 完善的语义信息
 * 2. 触觉反馈
 * 3. 状态播报
 * 4. 焦点管理
 */
@Composable
fun AccessibleKalimbaKey(
    key: KalimbaKey,
    onClick: () -> Unit,
    isRippling: Boolean,
    accessibilityHelper: AccessibilityHelper,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isRippling) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale"
    )

    val color = getOctaveColor(key.octave)

    // 构建完整的语义描述
    val semanticDescription = buildString {
        append(key.getPositionDescription()) // "下排第3键，中音1"

        // 添加状态信息
        if (isRippling) {
            append("，正在播放")
        }

        // 添加操作提示
        append("。双击可播放")
    }

    // 构建状态描述（用于状态变化播报）
    var stateDescription = if (isRippling) "播放中" else "未选中"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // 触觉反馈
                    accessibilityHelper.vibrate(VibrationType.MEDIUM)

                    // 只在非TalkBack模式下播报
                    if (!accessibilityHelper.isTalkBackEnabled() &&
                        accessibilityHelper.isSpeechEnabled) {
                        accessibilityHelper.speak(key.getPositionDescription())
                    }

                    onClick()
                }
            )
            .semantics {
                // 核心无障碍属性
                contentDescription = semanticDescription
                stateDescription = stateDescription
                role = Role.Button

                // 自定义操作
                onClick(label = "播放琴键") {
                    onClick()
                    true
                }
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

/**
 * 改进的控制按钮（带完整无障碍支持）
 */
@Composable
fun AccessibleControlButton(
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
    val fullDescription = buildString {
        append(label)
        if (!enabled) {
            append("，不可用")
        } else {
            append("，按钮。双击可执行")
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        accessibilityHelper.vibrate(VibrationType.CLICK)

                        // 播报操作结果
                        if (!accessibilityHelper.isTalkBackEnabled() &&
                            accessibilityHelper.isSpeechEnabled) {
                            accessibilityHelper.speak(semanticsDescription)
                        }

                        onClick()
                    }
                }
            )
            .semantics {
                contentDescription = fullDescription
                role = Role.Button

                if (!enabled) {
                    disabled()
                }

                onClick(label = semanticsDescription) {
                    if (enabled) {
                        onClick()
                        true
                    } else {
                        false
                    }
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
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconResId),
                contentDescription = null, // 已在外层提供
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