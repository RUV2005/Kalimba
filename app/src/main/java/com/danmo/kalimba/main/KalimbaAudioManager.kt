// 文件名: KalimbaAudioManager.kt (简化版)
package com.danmo.kalimba.main

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.danmo.kalimba.SoundResources
import java.util.concurrent.ConcurrentHashMap

/**
 * 音频播放管理器（简化版）
 * ⚠️ 只负责播放声音，不再管理 TTS
 * TTS 功能由 AccessibilityHelper 统一管理
 */
class KalimbaAudioManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
    private var isSoundReady = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds(context)
    }

    private fun loadSounds(context: Context) {
        val soundList = SoundResources.SOUND_LIST

        var loadedCount = 0
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds[sampleId] = true
                loadedCount++
                if (loadedCount >= soundList.size) {
                    isSoundReady = true
                    Log.d(TAG, "All sounds loaded successfully")
                }
            } else {
                Log.e(TAG, "Failed to load sound, sampleId: $sampleId, status: $status")
            }
        }

        soundList.forEach { (id, resId) ->
            try {
                val soundId = soundPool.load(context, resId, 1)
                soundMap[id] = soundId
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sound for id: $id", e)
            }
        }
    }

    /**
     * 播放声音（只播放，不播报）
     */
    fun play(key: KalimbaKey) {
        try {
            val soundId = soundMap[key.id]
            if (soundId != null && loadedSounds[soundId] == true) {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                Log.w(TAG, "Sound not ready for key: ${key.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound for key: ${key.id}", e)
        }
    }

    fun isReady() = isSoundReady

    fun release() {
        try {
            soundPool.release()
            Log.d(TAG, "Audio manager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio manager", e)
        }
    }

    companion object {
        private const val TAG = "KalimbaAudioManager"
    }
}