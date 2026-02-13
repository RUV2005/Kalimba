// 文件名: AudioConstants.kt
package com.danmo.kalimba

/**
 * 音频播放相关的常量配置
 * 统一管理延迟时间，避免硬编码
 */
object AudioConstants {
    // 动画延迟
    const val RIPPLE_ANIMATION_DELAY = 300L

    // 自动播放间隔
    const val AUTO_PLAY_INTERVAL = 500L
    const val AUTO_PLAY_ROUND_DELAY = 1000L

    // 音频时长
    const val SOUND_DURATION = 800L

    // TTS相关
    const val TTS_DURATION_BASE = 500L
    const val TTS_DURATION_PER_CHAR = 120L
    const val TTS_SPEECH_RATE = 1.3f

    // TTS音量
    const val TTS_VOLUME_NORMAL = 0.25f
    const val TTS_VOLUME_LOW = 0.2f
    const val TTS_VOLUME_VERY_LOW = 0.1f
}