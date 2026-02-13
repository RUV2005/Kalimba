// 文件名: AccessibilityHelper.kt
package com.danmo.kalimba.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.danmo.kalimba.AudioConstants
import java.util.Locale

/**
 * 统一的无障碍辅助类
 * 管理：TTS播报、触觉反馈、无障碍状态检测
 */
class AccessibilityHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // 用户是否手动启用语音播报（独立于TalkBack）
    var isSpeechEnabled = false

    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.CHINESE
            tts?.setSpeechRate(AudioConstants.TTS_SPEECH_RATE)
            isTtsReady = true
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    /**
     * 检查TalkBack是否启用
     */
    fun isTalkBackEnabled(): Boolean {
        return accessibilityManager.isEnabled &&
                accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * 播报文本
     * 策略：
     * 1. 如果TalkBack开启 -> 不播报（避免冲突，让TalkBack通过contentDescription播报）
     * 2. 如果用户手动开启语音播报 -> 使用TTS播报
     */
    fun speak(text: String, interrupt: Boolean = false) {
        try {
            // 如果TalkBack开启，不要手动播报，避免冲突
            if (isTalkBackEnabled()) {
                Log.d(TAG, "TalkBack enabled, skip manual speech")
                return
            }

            // 只有用户手动开启时才播报
            if (isTtsReady && isSpeechEnabled) {
                val queueMode = if (interrupt) {
                    TextToSpeech.QUEUE_FLUSH
                } else {
                    TextToSpeech.QUEUE_ADD
                }
                tts?.speak(text, queueMode, null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: $text", e)
        }
    }

    /**
     * 切换语音播报开关
     */
    fun toggleSpeech(): Boolean {
        isSpeechEnabled = !isSpeechEnabled

        // 播报状态变化
        val message = if (isSpeechEnabled) {
            "键位播报已启用"
        } else {
            "键位播报已禁用"
        }

        // 这里要强制播报状态，即使isSpeechEnabled是false
        if (isTtsReady && !isTalkBackEnabled()) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        return isSpeechEnabled
    }

    /**
     * 触觉反馈
     */
    fun vibrate(type: VibrationType) {
        try {
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    VibrationType.LIGHT -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    VibrationType.MEDIUM -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    VibrationType.STRONG -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    VibrationType.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    VibrationType.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    VibrationType.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val duration = when (type) {
                    VibrationType.LIGHT -> 30L
                    VibrationType.MEDIUM -> 50L
                    VibrationType.STRONG -> 100L
                    VibrationType.CLICK -> 50L
                    VibrationType.DOUBLE_CLICK -> 100L
                    VibrationType.TICK -> 20L
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    /**
     * 组合反馈：语音 + 触觉
     */
    fun provideFeedback(text: String, vibrationType: VibrationType, interrupt: Boolean = false) {
        speak(text, interrupt)
        vibrate(vibrationType)
    }

    fun isReady() = isTtsReady

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            Log.d(TAG, "AccessibilityHelper released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AccessibilityHelper", e)
        }
    }

    companion object {
        private const val TAG = "AccessibilityHelper"
    }
}

/**
 * 振动类型
 */
enum class VibrationType {
    LIGHT,      // 轻微振动 - 用于按钮hover
    MEDIUM,     // 中等振动 - 用于按钮点击
    STRONG,     // 强振动 - 用于重要操作
    CLICK,      // 系统点击效果
    DOUBLE_CLICK, // 双击效果
    TICK        // 勾选效果
}