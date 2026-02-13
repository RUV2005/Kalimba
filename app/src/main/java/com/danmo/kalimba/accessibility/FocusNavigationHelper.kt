// 文件名: FocusNavigationHelper.kt
package com.danmo.kalimba.accessibility

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * 焦点导航辅助类
 * 为视障用户优化键盘导航体验
 */

/**
 * 创建可导航的区域
 * 支持方向键导航和自动焦点管理
 */
@Composable
fun NavigableRegion(
    regionName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .semantics {
                testTag = regionName
            }
    ) {
        content()
    }
}

/**
 * 为琴键创建焦点组
 * 支持网格导航（上下左右）
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusableKeyGrid(
    keys: List<Any>, // 琴键列表
    columns: Int, // 列数
    onKeySelected: (Int) -> Unit, // 选中回调
    modifier: Modifier = Modifier,
    content: @Composable (Int, Modifier) -> Unit
) {
    val focusRequesters = remember(keys.size) {
        List(keys.size) { FocusRequester() }
    }

    Box(modifier = modifier) {
        keys.forEachIndexed { index, _ ->
            val row = index / columns
            val col = index % columns

            val keyModifier = Modifier
                .focusRequester(focusRequesters[index])
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            // 方向键导航
                            Key.DirectionUp -> {
                                val targetIndex = index - columns
                                if (targetIndex >= 0) {
                                    focusRequesters[targetIndex].requestFocus()
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                val targetIndex = index + columns
                                if (targetIndex < keys.size) {
                                    focusRequesters[targetIndex].requestFocus()
                                    true
                                } else false
                            }
                            Key.DirectionLeft -> {
                                if (col > 0) {
                                    focusRequesters[index - 1].requestFocus()
                                    true
                                } else false
                            }
                            Key.DirectionRight -> {
                                if (col < columns - 1 && index + 1 < keys.size) {
                                    focusRequesters[index + 1].requestFocus()
                                    true
                                } else false
                            }
                            // Enter/Space 触发选中
                            Key.Enter, Key.Spacebar -> {
                                onKeySelected(index)
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }

            content(index, keyModifier)
        }
    }

    // 自动聚焦第一个元素
    LaunchedEffect(Unit) {
        focusRequesters.firstOrNull()?.requestFocus()
    }
}

/**
 * 焦点顺序定义
 * 为复杂界面定义清晰的焦点遍历顺序
 */
class FocusOrderManager {
    private val focusOrder = mutableListOf<String>()
    private var currentIndex = 0

    /**
     * 添加焦点元素
     */
    fun addToOrder(tag: String) {
        focusOrder.add(tag)
    }

    /**
     * 获取下一个焦点
     */
    fun getNext(): String? {
        if (focusOrder.isEmpty()) return null
        currentIndex = (currentIndex + 1) % focusOrder.size
        return focusOrder[currentIndex]
    }

    /**
     * 获取上一个焦点
     */
    fun getPrevious(): String? {
        if (focusOrder.isEmpty()) return null
        currentIndex = if (currentIndex == 0) focusOrder.size - 1 else currentIndex - 1
        return focusOrder[currentIndex]
    }

    /**
     * 跳转到指定元素
     */
    fun jumpTo(tag: String): Boolean {
        val index = focusOrder.indexOf(tag)
        if (index != -1) {
            currentIndex = index
            return true
        }
        return false
    }

    /**
     * 清空顺序
     */
    fun clear() {
        focusOrder.clear()
        currentIndex = 0
    }
}

/**
 * 快捷导航标记
 * 为关键区域添加快捷键
 */
data class NavigationLandmark(
    val name: String,
    val shortcutKey: Key,
    val description: String
)

/**
 * 常用的导航标记
 */
object NavigationLandmarks {
    val KEYBOARD = NavigationLandmark(
        name = "keyboard",
        shortcutKey = Key.K,
        description = "跳转到琴键区域"
    )

    val CONTROLS = NavigationLandmark(
        name = "controls",
        shortcutKey = Key.C,
        description = "跳转到控制按钮"
    )

    val HISTORY = NavigationLandmark(
        name = "history",
        shortcutKey = Key.H,
        description = "跳转到历史记录"
    )

    val SETTINGS = NavigationLandmark(
        name = "settings",
        shortcutKey = Key.S,
        description = "跳转到设置"
    )
}

/**
 * 导航快捷键处理器
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.navigationShortcuts(
    landmarks: List<NavigationLandmark>,
    onNavigate: (String) -> Unit
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed) {
        landmarks.firstOrNull { it.shortcutKey == keyEvent.key }?.let { landmark ->
            onNavigate(landmark.name)
            return@onPreviewKeyEvent true
        }
    }
    false
}