// 文件位置: app/src/main/java/com/danmo/kalimba/metronome/MetronomeManager.kt
package com.danmo.kalimba.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.danmo.kalimba.R
import com.danmo.kalimba.accessibility.AccessibilityHelper
import com.danmo.kalimba.accessibility.VibrationType

/**
 * 节拍器管理器（增强版）
 * 支持从设置中读取配置
 */
class MetronomeManager(
    private val context: Context,
    private val accessibilityHelper: AccessibilityHelper
) {
    private val soundPool: SoundPool
    private var tickSoundId: Int = -1
    private var tockSoundId: Int = -1
    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var currentBpm = 80
    private var currentBeat = 0
    private var beatsPerMeasure = 4

    // ✅ 新增：配置参数
    var volume: Float = 0.8f
    var vibrationEnabled: Boolean = true
    var accentFirstBeat: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        // 加载音效
        tickSoundId = soundPool.load(context, R.raw.metronome_tick, 1)
        tockSoundId = soundPool.load(context, R.raw.metronome_tock, 1)
    }

    /**
     * ✅ 从设置启动节拍器
     */
    fun startWithSettings(
        bpm: Int = 80,
        beatsPerBar: Int = 4,
        metronomeVolume: Float = 0.8f,
        metronomeVibration: Boolean = true,
        accentFirst: Boolean = true
    ) {
        if (isRunning) return

        currentBpm = bpm
        beatsPerMeasure = beatsPerBar
        volume = metronomeVolume
        vibrationEnabled = metronomeVibration
        accentFirstBeat = accentFirst
        currentBeat = 0
        isRunning = true

        accessibilityHelper.speak("节拍器已启动，每分钟${bpm}拍，${beatsPerBar}拍一组")
        scheduleTick()
    }

    /**
     * 启动节拍器（兼容旧接口）
     */
    fun start(bpm: Int = 80, beatsPerBar: Int = 4) {
        startWithSettings(bpm, beatsPerBar)
    }

    /**
     * 停止节拍器
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        accessibilityHelper.speak("节拍器已停止")
    }

    /**
     * 调整BPM
     */
    fun adjustBpm(newBpm: Int) {
        currentBpm = newBpm.coerceIn(40, 200)
        if (isRunning) {
            accessibilityHelper.speak("速度调整为每分钟${currentBpm}拍")
        }
    }

    /**
     * ✅ 调整音量
     */
    fun adjustVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
    }

    private fun scheduleTick() {
        if (!isRunning) return

        val intervalMs = (60000 / currentBpm).toLong()

        handler.postDelayed({
            playTick()
            scheduleTick()
        }, intervalMs)
    }

    private fun playTick() {
        currentBeat = (currentBeat % beatsPerMeasure) + 1

        // ✅ 根据设置决定是否强调首拍
        val isFirstBeat = currentBeat == 1 && accentFirstBeat

        if (isFirstBeat) {
            soundPool.play(tockSoundId, volume, volume, 1, 0, 1.0f)
            if (vibrationEnabled) {
                accessibilityHelper.vibrate(VibrationType.MEDIUM)
            }
        } else {
            soundPool.play(tickSoundId, volume * 0.8f, volume * 0.8f, 1, 0, 1.0f)
            if (vibrationEnabled) {
                accessibilityHelper.vibrate(VibrationType.LIGHT)
            }
        }
    }

    fun isRunning() = isRunning

    fun getCurrentBeat() = currentBeat

    fun release() {
        stop()
        soundPool.release()
    }

    companion object {
        private const val TAG = "MetronomeManager"
    }
}