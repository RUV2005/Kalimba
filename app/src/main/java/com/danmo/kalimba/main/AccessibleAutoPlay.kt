// 文件名: AccessibleAutoPlay.kt
package com.danmo.kalimba.main

import com.danmo.kalimba.AudioConstants
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 无障碍优化的自动播放管理器
 * 特点：
 * 1. 播放时实时播报当前音符
 * 2. 播放进度提示
 * 3. 触觉反馈
 */
class AccessibleAutoPlayManager(
    private val audioManager: KalimbaAudioManager,
    private val accessibilityHelper: AccessibilityHelper
) {
    private var isPlaying = false
    private var currentJob: Job? = null
    private val mutex = Mutex()

    /**
     * 开始自动播放
     * @param history 播放历史
     * @param onKeyPlay 每个音符播放时的回调
     * @param onComplete 播放完成回调
     */
    fun startAutoPlay(
        history: List<KalimbaKey>,
        scope: CoroutineScope,
        onKeyPlay: (KalimbaKey, Int, Int) -> Unit, // key, currentIndex, total
        onComplete: () -> Unit
    ) {
        if (history.isEmpty()) return

        // 停止当前播放
        stopAutoPlay()

        isPlaying = true

        // 播报开始
        accessibilityHelper.provideFeedback(
            text = "开始自动播放，共${history.size}个音符",
            vibrationType = VibrationType.STRONG,
            interrupt = true
        )

        currentJob = scope.launch {
            try {
                val reversed = history.reversed()
                var roundCount = 0

                while (isPlaying) {
                    roundCount++

                    // 如果不是第一轮，播报轮次
                    if (roundCount > 1) {
                        accessibilityHelper.speak("第${roundCount}轮")
                        delay(AudioConstants.AUTO_PLAY_ROUND_DELAY)
                        if (!isPlaying) break
                    }

                    // 播放每个音符
                    reversed.forEachIndexed { index, key ->
                        if (!isPlaying) return@forEachIndexed

                        mutex.withLock {
                            // 播放音频
                            audioManager.play(key)

                            // 触觉反馈
                            accessibilityHelper.vibrate(VibrationType.LIGHT)

                            // 实时播报（每5个音符播报一次进度）
                            if (accessibilityHelper.isSpeechEnabled &&
                                !accessibilityHelper.isTalkBackEnabled()) {

                                if (index % 5 == 0 && index > 0) {
                                    val remaining = reversed.size - index
                                    accessibilityHelper.speak(
                                        "已播放${index}个，还剩${remaining}个",
                                        interrupt = false
                                    )
                                } else {
                                    // 播报音符名称
                                    accessibilityHelper.speak(
                                        key.displayName,
                                        interrupt = false
                                    )
                                }
                            }

                            // UI回调
                            onKeyPlay(key, index, reversed.size)
                        }

                        delay(AudioConstants.AUTO_PLAY_INTERVAL)
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Exception) {
                accessibilityHelper.speak("播放出错", interrupt = true)
            } finally {
                isPlaying = false
                onComplete()

                // 播报结束
                accessibilityHelper.provideFeedback(
                    text = "自动播放已停止",
                    vibrationType = VibrationType.MEDIUM,
                    interrupt = true
                )
            }
        }
    }

    /**
     * 停止自动播放
     */
    fun stopAutoPlay() {
        isPlaying = false
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * 是否正在播放
     */
    fun isPlaying() = isPlaying
}

/**
 * 带进度播报的录音模式
 * 用于简谱编辑模式
 */
class AccessibleRecordingManager(
    private val accessibilityHelper: AccessibilityHelper
) {
    private var recordedCount = 0

    /**
     * 录入音符时调用
     */
    fun onNoteRecorded(note: String, totalNotes: Int) {
        recordedCount++

        // 触觉反馈
        accessibilityHelper.vibrate(VibrationType.TICK)

        // 播报进度（每录入5个或重要节点时播报）
        if (accessibilityHelper.isSpeechEnabled &&
            !accessibilityHelper.isTalkBackEnabled()) {

            when {
                recordedCount % 10 == 0 -> {
                    accessibilityHelper.speak(
                        "已录入${recordedCount}个音符",
                        interrupt = false
                    )
                }
                totalNotes > 0 && recordedCount == totalNotes -> {
                    accessibilityHelper.provideFeedback(
                        text = "录入完成，共${totalNotes}个音符",
                        vibrationType = VibrationType.STRONG,
                        interrupt = true
                    )
                }
                else -> {
                    accessibilityHelper.speak(note, interrupt = false)
                }
            }
        }
    }

    /**
     * 删除音符时调用
     */
    fun onNoteDeleted() {
        recordedCount = maxOf(0, recordedCount - 1)

        accessibilityHelper.provideFeedback(
            text = "已删除，还剩${recordedCount}个",
            vibrationType = VibrationType.MEDIUM,
            interrupt = true
        )
    }

    /**
     * 重置计数
     */
    fun reset() {
        recordedCount = 0
    }

    /**
     * 获取当前录入数量
     */
    fun getRecordedCount() = recordedCount
}